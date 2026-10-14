package com.plataforma.renta.application.service;

import com.plataforma.renta.application.command.ConfirmarPagoCommand;
import com.plataforma.renta.application.command.CrearReembolsoCommand;
import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.exception.EstadoInvalidoException;
import com.plataforma.renta.domain.exception.MontoInvalidoException;
import com.plataforma.renta.domain.exception.MontoReembolsoInvalidoException;
import com.plataforma.renta.domain.exception.PagoYaConfirmadoException;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.port.out.AdvisoryLockService;
import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.domain.port.out.FacturaRepository;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.domain.port.out.ReservaRepository;
import com.plataforma.renta.domain.value.Dinero;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final FacturaRepository facturaRepository;
    private final AdvisoryLockService advisoryLockService;
    private final ConfiguracionFiscalRepository configuracionFiscalRepository;
    private final FacturaService facturaService;

    public PagoService(PagoRepository pagoRepository,
                       ReservaRepository reservaRepository,
                       FacturaRepository facturaRepository,
                       AdvisoryLockService advisoryLockService,
                       ConfiguracionFiscalRepository configuracionFiscalRepository,
                       FacturaService facturaService) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.facturaRepository = facturaRepository;
        this.advisoryLockService = advisoryLockService;
        this.configuracionFiscalRepository = configuracionFiscalRepository;
        this.facturaService = facturaService;
    }

    @Transactional
    public Pago confirmarPago(ConfirmarPagoCommand cmd) {
        // 1. Lock fila reserva
        Reserva reserva = reservaRepository.findByIdForUpdate(cmd.reservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + cmd.reservaId()));

        // 2. Validar estado
        if (reserva.getEstado() != ReservaEstado.PENDIENTE) {
            throw new EstadoInvalidoException("Solo PENDIENTE puede confirmar pago. Estado: " + reserva.getEstado());
        }

        // 3. Advisory locks herramientas
        List<UUID> herramientaIds = reserva.getDetalles().stream()
                .map(d -> d.getHerramientaId())
                .distinct()
                .sorted()
                .toList();
        
        advisoryLockService.acquireLocks(herramientaIds);

        try {
            // 4. REVALIDAR VENCIMIENTO CON clock_timestamp() de BD
            // En Spring/JPA usamos Instant.now() como aproximación; la BD valida con trigger
            if (reserva.estaVencida(Instant.now())) {
                throw new EstadoInvalidoException("Reserva expirada según reloj de BD");
            }

            // 5. Re-validar stock
            for (var d : reserva.getDetalles()) {
                int disponible = calcularDisponibilidad(d.getHerramientaId(), reserva.getFechaInicio(), reserva.getFechaFin(), reserva.getId());
                if (disponible < d.getCantidad()) {
                    throw new com.plataforma.renta.domain.exception.StockInsuficienteException(d.getHerramientaId(), disponible, d.getCantidad());
                }
            }

            // 6. Validar importe
            Dinero totalCalculado = reserva.calcularTotal();
            if (cmd.monto().getMonto().compareTo(totalCalculado.getMonto()) != 0) {
                throw new MontoInvalidoException("Monto " + cmd.monto() + " no coincide con total " + totalCalculado);
            }

            // 7. Protección reintentos
            if (pagoRepository.existsCompletadoByReservaId(cmd.reservaId())) {
                throw new PagoYaConfirmadoException("Reserva ya tiene pago completado");
            }

            // 8. Transiciones atómicas
            reserva.confirmar(Instant.now());
            reservaRepository.save(reserva);

            // 9. Pago COMPLETADO
            Pago pago = Pago.completado(cmd.reservaId(), totalCalculado, cmd.proveedorPago(), cmd.transaccionId());
            pagoRepository.save(pago);

            // 10. Factura original
            facturaService.emitirFacturaOriginal(reserva, pago, totalCalculado);

            return pago;
        } finally {
            advisoryLockService.releaseLocks();
        }
    }

    @Transactional
    public Pago crearReembolso(CrearReembolsoCommand cmd) {
        // 1. Lock fila reserva
        Reserva reserva = reservaRepository.findByIdForUpdate(cmd.reservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + cmd.reservaId()));
        
        if (reserva.getEstado() != ReservaEstado.CONFIRMADA && reserva.getEstado() != ReservaEstado.EN_CURSO) {
            throw new EstadoInvalidoException("Solo CONFIRMADA/EN_CURSO admiten reembolso");
        }

        // 2. Lock fila pago original
        Pago pagoOriginal = pagoRepository.findCompletadoByReservaIdForUpdate(cmd.reservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("No hay pago completado para esta reserva"));

        // 3. Advisory locks herramientas
        List<UUID> herramientaIds = reserva.getDetalles().stream()
                .map(d -> d.getHerramientaId())
                .distinct()
                .sorted()
                .toList();
        advisoryLockService.acquireLocks(herramientaIds);

        try {
            // 4. Recalcular saldo
            var yaReembolsado = pagoRepository.sumMontoByPagoOriginalId(pagoOriginal.getId());
            var disponible = pagoOriginal.getMonto().restar(Dinero.cop(yaReembolsado));

            if (cmd.montoConIva().getMonto().compareTo(Dinero.cero().getMonto()) <= 0 || cmd.montoConIva().getMonto().compareTo(disponible.getMonto()) > 0) {
                throw new MontoReembolsoInvalidoException("Monto inválido. Disponible: " + disponible);
            }

            // 5. Crear reembolso
            Pago reembolso = Pago.reembolso(cmd.reservaId(), pagoOriginal.getId(), cmd.montoConIva(), cmd.motivo());
            pagoRepository.save(reembolso);

            // 6. Nota de crédito con mismo total
            facturaService.emitirNotaCredito(reserva, cmd.montoConIva(), reembolso.getId());

            // 7. NO cancelar automáticamente
            return reembolso;
        } finally {
            advisoryLockService.releaseLocks();
        }
    }

    private int calcularDisponibilidad(UUID herramientaId, java.time.LocalDate inicio, java.time.LocalDate fin, UUID excluirReservaId) {
        return 0; // TODO: query nativa
    }
}