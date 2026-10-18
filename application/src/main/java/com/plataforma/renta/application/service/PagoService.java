package com.plataforma.renta.application.service;

import com.plataforma.renta.application.command.ConfirmarPagoCommand;
import com.plataforma.renta.application.command.CrearReembolsoCommand;
import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.exception.EstadoInvalidoException;
import com.plataforma.renta.domain.exception.MontoInvalidoException;
import com.plataforma.renta.domain.exception.MontoReembolsoInvalidoException;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.exception.PagoYaConfirmadoException;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.AdvisoryLockService;
import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.port.out.DisponibilidadRepository;
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
    private final HerramientaRepository herramientaRepository;
    private final AdvisoryLockService advisoryLockService;
    private final ConfiguracionFiscalRepository configuracionFiscalRepository;
    private final FacturaService facturaService;
    private final DisponibilidadRepository disponibilidadRepository;

    public PagoService(PagoRepository pagoRepository,
                       ReservaRepository reservaRepository,
                       FacturaRepository facturaRepository,
                       HerramientaRepository herramientaRepository,
                       AdvisoryLockService advisoryLockService,
                       ConfiguracionFiscalRepository configuracionFiscalRepository,
                       FacturaService facturaService,
                       DisponibilidadRepository disponibilidadRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.facturaRepository = facturaRepository;
        this.herramientaRepository = herramientaRepository;
        this.advisoryLockService = advisoryLockService;
        this.configuracionFiscalRepository = configuracionFiscalRepository;
        this.facturaService = facturaService;
        this.disponibilidadRepository = disponibilidadRepository;
    }

@Transactional
    public Pago confirmarPago(ConfirmarPagoCommand cmd, UUID currentUserId, UserRol currentUserRol) {
        // 1. Lock fila reserva
        Reserva reserva = reservaRepository.findByIdForUpdate(cmd.reservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + cmd.reservaId()));

        // Validar autorización: cliente titular o administrador
        if (currentUserRol != UserRol.ADMIN && (currentUserRol != UserRol.CLIENTE || !reserva.getClienteId().equals(currentUserId))) {
            throw new OperacionInvalidaException("No autorizado: solo el cliente titular o un administrador puede confirmar pago");
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
    public Pago crearReembolso(CrearReembolsoCommand cmd, UUID currentUserId, UserRol currentUserRol) {
        // 1. Lock fila reserva
        Reserva reserva = reservaRepository.findByIdForUpdate(cmd.reservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + cmd.reservaId()));

        // Validar que el usuario actual es el cliente titular de la reserva o administrador
        if (currentUserRol != UserRol.ADMIN && (currentUserRol != UserRol.CLIENTE || !reserva.getClienteId().equals(currentUserId))) {
            throw new OperacionInvalidaException("No autorizado: solo el cliente titular puede solicitar un reembolso");
        }

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

            // 5. Prevenir solicitudes duplicadas: no permitir otro reembolso PENDIENTE para el mismo pago original
            if (pagoRepository.existsByPagoOriginalIdAndEstado(pagoOriginal.getId(), PagoEstado.PENDIENTE)) {
                throw new OperacionInvalidaException("Ya existe una solicitud de reembolso pendiente para este pago original");
            }

            // 5. Crear reembolso en estado PENDIENTE (solicitud pendida de aprobación)
            Pago reembolso = new Pago(
                UUID.randomUUID(), cmd.reservaId(), pagoOriginal.getId(),
                PagoEstado.PENDIENTE, "SIMULADO", null, cmd.montoConIva(), cmd.motivo(),
                Instant.now(), Instant.now()
            );
            // Nota: no se usa Pago.reembolso() aquí para evitar estado REEMBOLSADO inmediato
            // El estado cambiará a REEMBOLSADO tras la aprobación en aprobarReembolso()
            pagoRepository.save(reembolso);

            // 6. NO emitir nota de crédito hasta que sea aprobado (en aprobarReembolso)
            // 7. NO cancelar automáticamente
            return reembolso;
        } finally {
            advisoryLockService.releaseLocks();
        }
    }

    /**
     * Aprobar una solicitud de reembolso.
     * Solo un usuario con rol ADMIN o un usuario con rol PROPIETARIO que sea propietario
     * de al menos una herramienta incluida en la reserva pueden aprobar.
     * El cliente NUNCA puede aprobar reembolsos, independientemente de su identidad.
     *
     * @param reembolsoId ID del reembolso a aprobar
     * @param currentUserId ID del usuario autenticado
     * @param currentUserRol Rol formal del usuario autenticado
     * @throws OperacionInvalidaException si el cliente intenta aprobar, no tiene permisos, o el reembolso no es válido
     * @throws EntidadNoEncontradaException si no existe el reembolso
     */
    @Transactional
    public void aprobarReembolso(UUID reembolsoId, UUID currentUserId, UserRol currentUserRol) {
        // El cliente NUNCA puede aprobar reembolsos, independientemente de su identidad
        if (currentUserRol == UserRol.CLIENTE) {
            throw new OperacionInvalidaException("No autorizado: el cliente no puede aprobar reembolsos");
        }

        // Validar que existe el reembolso solicitado
        Pago reembolso = pagoRepository.findByIdForUpdate(reembolsoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reembolso no encontrado: " + reembolsoId));

        // Validar que el reembolso está en estado PENDIENTE
        if (reembolso.getEstado() != PagoEstado.PENDIENTE) {
            throw new OperacionInvalidaException("No se puede aprobar: el reembolso no está en estado pendiente o ya ha sido procesado");
        }

        // Validar que el reembolso tiene un pago original asociado
        if (reembolso.getPagoOriginalId() == null) {
            throw new OperacionInvalidaException("Reembolso inválido: no tiene pago original asociado");
        }

        // Cargar la reserva para validar autorización de propietario
        Reserva reserva = reservaRepository.findByIdForUpdate(reembolso.getReservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + reembolso.getReservaId()));

        // Validar autorización basada en rol formal
        boolean autorizado = false;

        if (currentUserRol == UserRol.ADMIN) {
            // ADMIN puede aprobar sin ser propietario de la herramienta
            autorizado = true;
        } else if (currentUserRol == UserRol.PROPIETARIO) {
            // PROPIETARIO debe ser propietario de alguna herramienta de la reserva
            List<Herramienta> herramientas;
            try {
                herramientas = herramientaRepository.findByIdsForUpdate(
                    reserva.getDetalles().stream()
                        .map(DetalleReserva::getHerramientaId)
                        .distinct()
                        .toList()
                );
                for (Herramienta h : herramientas) {
                    if (h.getPropietarioId().equals(currentUserId)) {
                        autorizado = true;
                        break;
                    }
                }
            } catch (Exception e) {
                // Si falla la carga de herramientas, denegar por seguridad
            }
        }

        if (!autorizado) {
            throw new OperacionInvalidaException("No autorizado: solo el propietario relacionado o un administrador pueden aprobar un reembolso");
        }

        // Cambiar el estado del reembolso a REEMBOLSADO tras la aprobación
        // Solo cambiar el reembolso solicitado, no todos los pagos indiscriminadamente
        reembolso.cambiarEstado(PagoEstado.REEMBOLSADO);
        pagoRepository.save(reembolso);

        // Emitir nota de crédito vinculada al reembolso ya ejecutado
        facturaService.emitirNotaCredito(reserva, reembolso.getMonto(), reembolso.getId());
    }

    private int calcularDisponibilidad(UUID herramientaId, java.time.LocalDate inicio, java.time.LocalDate fin, UUID excluirReservaId) {
        if (disponibilidadRepository != null) {
            return disponibilidadRepository.calcularDisponibilidad(herramientaId, inicio, fin, excluirReservaId);
        }
        return Integer.MAX_VALUE;
    }

    public List<Pago> listarReembolsos() {
        return pagoRepository.findAll().stream()
                .filter(p -> p.getPagoOriginalId() != null)
                .toList();
    }

    public List<Pago> listarReembolsosPorUsuario(UUID usuarioId, UserRol rol) {
        List<Pago> todosReembolsos = listarReembolsos();

        if (rol == UserRol.ADMIN) {
            return todosReembolsos;
        }

        if (rol == UserRol.CLIENTE) {
            List<UUID> misReservas = reservaRepository.findByClienteId(usuarioId).stream()
                    .map(Reserva::getId)
                    .toList();
            return todosReembolsos.stream()
                    .filter(p -> misReservas.contains(p.getReservaId()))
                    .toList();
        }

        if (rol == UserRol.PROPIETARIO) {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(usuarioId).stream()
                    .map(Herramienta::getId)
                    .toList();
            List<UUID> reservasDeMisHerramientas = reservaRepository.findAll().stream()
                    .filter(r -> r.getDetalles() != null && r.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId())))
                    .map(Reserva::getId)
                    .toList();
            return todosReembolsos.stream()
                    .filter(p -> reservasDeMisHerramientas.contains(p.getReservaId()))
                    .toList();
        }

        return List.of();
    }

    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    public Pago obtenerPorId(UUID id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Pago no encontrado: " + id));
    }

}
