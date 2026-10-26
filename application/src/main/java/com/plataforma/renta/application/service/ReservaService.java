package com.plataforma.renta.application.service;

import com.plataforma.renta.application.command.CancelarReservaCommand;
import com.plataforma.renta.application.command.CrearReservaCommand;
import com.plataforma.renta.application.command.ExpirarReservasCommand;
import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.exception.EstadoInvalidoException;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.exception.StockInsuficienteException;
import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.port.out.AdvisoryLockService;
import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.domain.port.out.DisponibilidadRepository;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.ReservaRepository;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.domain.value.Dinero;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HerramientaRepository herramientaRepository;
    private final AdvisoryLockService advisoryLockService;
    private final ConfiguracionFiscalRepository configuracionFiscalRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final PagoRepository pagoRepository;
    private final FacturaService facturaService;

    public ReservaService(ReservaRepository reservaRepository,
                          HerramientaRepository herramientaRepository,
                          AdvisoryLockService advisoryLockService,
                          ConfiguracionFiscalRepository configuracionFiscalRepository,
                          DisponibilidadRepository disponibilidadRepository,
                          PagoRepository pagoRepository,
                          FacturaService facturaService) {
        this.reservaRepository = reservaRepository;
        this.herramientaRepository = herramientaRepository;
        this.advisoryLockService = advisoryLockService;
        this.configuracionFiscalRepository = configuracionFiscalRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.pagoRepository = pagoRepository;
        this.facturaService = facturaService;
    }

    @Transactional
    public Reserva crearReserva(CrearReservaCommand cmd, UUID currentUserId, UserRol currentUserRol) {
        // 1. Validar que el cliente de la reserva es el usuario autenticado
        // El cliente no puede crear una reserva en nombre de otro usuario (Regla 10)
        if (currentUserRol != UserRol.CLIENTE || !cmd.clienteId().equals(currentUserId)) {
            throw new OperacionInvalidaException("No autorizado: solo el cliente titular puede crear reservas");
        }

        // 2. Validar cliente existe (se asume validación previa o FK en BD)

        // 2. Obtener herramientas y validar publicación + stock preliminar
        List<UUID> herramientaIds = cmd.detalles().stream()
                .map(CrearReservaCommand.DetalleCommand::herramientaId)
                .distinct()
                .toList();

        List<Herramienta> herramientas = herramientaRepository.findByIdsForUpdate(herramientaIds);
        if (herramientas.size() != herramientaIds.size()) {
            throw new EntidadNoEncontradaException("Una o más herramientas no encontradas");
        }

        // 3. Adquirir advisory locks en orden determinístico
        List<UUID> idsOrdenados = herramientaIds.stream().sorted().toList();
        advisoryLockService.acquireLocks(idsOrdenados);

        try {
            // 4. Re-validar stock DENTRO de los locks
            for (CrearReservaCommand.DetalleCommand d : cmd.detalles()) {
                Herramienta h = herramientas.stream()
                        .filter(hh -> hh.getId().equals(d.herramientaId()))
                        .findFirst().orElseThrow();

                if (h.getEstadoPublicacion() != com.plataforma.renta.domain.model.HerramientaPubEstado.PUBLICADA) {
                    throw new OperacionInvalidaException("Herramienta no está publicada: " + d.herramientaId());
                }

                // Validar disponibilidad usando función de BD
                int disponible = disponibilidadRepository.calcularDisponibilidad(h.getId(), cmd.fechaInicio(), cmd.fechaFin(), null);
                if (disponible < d.cantidad()) {
                    throw new StockInsuficienteException(d.herramientaId(), disponible, d.cantidad());
                }
            }

            // 5. Calcular expiración
            int minutosExpiracion = Integer.parseInt(
                    configuracionFiscalRepository.findValor("reserva_pendiente_expiracion_minutos").orElse("30"));
            Instant expiracion = Instant.now().plusSeconds(minutosExpiracion * 60L);

            // 6. Crear reserva
            Reserva reserva = new Reserva(
                    UUID.randomUUID(), cmd.clienteId(), com.plataforma.renta.domain.model.ReservaEstado.PENDIENTE,
                    cmd.fechaInicio(), cmd.fechaFin(), Instant.now(), expiracion
            );

            // 7. Crear detalles con snapshot de precio y tratamiento fiscal
            for (CrearReservaCommand.DetalleCommand d : cmd.detalles()) {
                Herramienta h = herramientas.stream()
                        .filter(hh -> hh.getId().equals(d.herramientaId()))
                        .findFirst().orElseThrow();

                Dinero precioDia = h.getPrecioDia();
                BigDecimal ivaPct = h.getIvaPorcentaje();
                boolean ivaAplica = h.isIvaAplica();

                DetalleReserva detalle = DetalleReserva.crear(
                        reserva.getId(), d.herramientaId(), d.cantidad(),
                        precioDia, cmd.fechaInicio(), cmd.fechaFin(), ivaPct, ivaAplica
                );
                reserva.agregarDetalle(detalle);
            }

            return reservaRepository.save(reserva);
        } finally {
            advisoryLockService.releaseLocks();
        }
    }

    @Transactional
    public Reserva cancelarReserva(com.plataforma.renta.application.command.CancelarReservaCommand cmd, UUID currentUserId, UserRol currentUserRol) {
        Reserva reserva = reservaRepository.findByIdForUpdate(cmd.reservaId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + cmd.reservaId()));

        // Validar autorización: solo cliente titular o administrador pueden cancelar
        // El propietario de una herramienta no obtiene permiso por ser propietario (Regla 9)
        if (currentUserRol != UserRol.ADMIN && (currentUserRol != UserRol.CLIENTE || !reserva.getClienteId().equals(currentUserId))) {
            throw new OperacionInvalidaException("No autorizado: solo el cliente titular o un administrador pueden cancelar esta reserva");
        }

        if (!reserva.puedeCancelar()) {
            throw new EstadoInvalidoException("Reserva no puede cancelarse desde estado: " + reserva.getEstado());
        }

        boolean pagada = estaPagada(reserva.getId());
        String motivoFinal = cmd.motivo();
        if (pagada) {
            motivoFinal = (motivoFinal != null ? motivoFinal : "Cancelada por el cliente") +
                    " [PAGADA: Transacción en custodia; elegible para trámite de reembolso]";
        }

        // Obtener herramientas para liberar stock
        List<UUID> herramientaIds = reserva.getDetalles().stream()
                .map(DetalleReserva -> DetalleReserva.getHerramientaId())
                .distinct()
                .sorted()
                .toList();

        advisoryLockService.acquireLocks(herramientaIds);

        try {
            reserva.cancelar(Instant.now(), motivoFinal, currentUserId);
            return reservaRepository.save(reserva);
        } finally {
            advisoryLockService.releaseLocks();
        }
    }

    @Transactional
    public Reserva cancelarReserva(UUID reservaId, String motivo, UUID currentUserId, UserRol currentUserRol) {
        return cancelarReserva(new com.plataforma.renta.application.command.CancelarReservaCommand(
                reservaId, motivo != null ? motivo : "Cancelada por usuario", currentUserId),
                currentUserId, currentUserRol);
    }

    @Transactional
    public void expirarReservas(com.plataforma.renta.application.command.ExpirarReservasCommand cmd) {
        Instant ahora = Instant.now();
        List<Reserva> vencidas = reservaRepository.findPendientesVencidas(ahora);

        for (Reserva r : vencidas) {
            // Lock fila reserva
            Optional<Reserva> reservaOpt = reservaRepository.findByIdForUpdate(r.getId());
            if (reservaOpt.isEmpty()) continue;
            Reserva reserva = reservaOpt.get();

            if (reserva.getEstado() != com.plataforma.renta.domain.model.ReservaEstado.PENDIENTE) continue;
            if (!reserva.estaVencida(ahora)) continue;

            // Locks herramientas
            List<UUID> herramientaIds = reserva.getDetalles().stream()
                    .map(DetalleReserva::getHerramientaId)
                    .distinct()
                    .sorted()
                    .toList();

            advisoryLockService.acquireLocks(herramientaIds);

            try {
                // Re-validar dentro de TX
                if (reserva.getEstado() != com.plataforma.renta.domain.model.ReservaEstado.PENDIENTE) continue;
                if (!reserva.estaVencida(Instant.now())) continue;

                reserva.expirar(Instant.now());
                reservaRepository.save(reserva);
            } finally {
                advisoryLockService.releaseLocks();
            }
        }
    }

    public List<Reserva> listarPorUsuario(UUID usuarioId, UserRol rol) {
        if (rol == UserRol.CLIENTE) {
            return reservaRepository.findByClienteId(usuarioId);
        } else if (rol == UserRol.ADMIN) {
            return reservaRepository.findAll();
        } else {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(usuarioId).stream()
                    .map(com.plataforma.renta.domain.model.Herramienta::getId)
                    .toList();
            return reservaRepository.findAll().stream()
                    .filter(r -> r.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId())))
                    .toList();
        }
    }

    public Reserva obtenerPorId(UUID id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + id));
    }

    public boolean estaPagada(UUID reservaId) {
        return pagoRepository.existsCompletadoByReservaId(reservaId);
    }

    @Transactional
    public Reserva aceptarReserva(UUID id, UUID currentUserId, UserRol currentUserRol) {
        Reserva r = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + id));

        if (currentUserRol != UserRol.ADMIN) {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(currentUserId).stream()
                    .map(Herramienta::getId)
                    .toList();
            boolean esPropietario = r.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId()));
            if (!esPropietario) {
                throw new OperacionInvalidaException("No autorizado: no es el propietario de la herramienta para aceptar la reserva");
            }
        }

        r.confirmar(Instant.now());
        return reservaRepository.save(r);
    }

    @Transactional
    public Reserva rechazarReserva(UUID id, String motivo, UUID currentUserId, UserRol currentUserRol) {
        cancelarReserva(new com.plataforma.renta.application.command.CancelarReservaCommand(id, motivo != null ? motivo : "Rechazada por propietario", currentUserId), currentUserId, currentUserRol);
        return obtenerPorId(id);
    }

    @Transactional
    public Reserva entregarEquipo(UUID id, UUID currentUserId, UserRol currentUserRol) {
        Reserva r = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + id));

        // Validar autorización: solo el propietario o ADMIN pueden entregar
        if (currentUserRol != UserRol.ADMIN) {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(currentUserId).stream()
                    .map(Herramienta::getId)
                    .toList();
            boolean esPropietario = r.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId()));
            if (!esPropietario) {
                throw new OperacionInvalidaException("No autorizado: no es el propietario de la herramienta a entregar");
            }
        }

        // Validar que el pago requerido esté completado antes de permitir la entrega
        if (!pagoRepository.existsCompletadoByReservaId(r.getId())) {
            throw new OperacionInvalidaException("No se puede registrar la entrega: el pago requerido no se ha completado");
        }

        r.iniciar(Instant.now());
        return reservaRepository.save(r);
    }

        @Transactional
    public Reserva solicitarDevolucion(UUID id, String notaCliente, UUID currentUserId, UserRol currentUserRol) {
        Reserva r = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + id));

        if (currentUserRol != UserRol.ADMIN && !r.getClienteId().equals(currentUserId)) {
            throw new OperacionInvalidaException("No autorizado: solo el cliente titular de la reserva puede solicitar la devolución");
        }

        r.solicitarDevolucion(notaCliente);
        return reservaRepository.save(r);
    }

    @Transactional
    public Reserva devolverEquipo(UUID id, boolean aceptada, boolean presentaDanos, String descripcionDano,
                                  BigDecimal costoReparacion, String observaciones,
                                  UUID currentUserId, UserRol currentUserRol) {
        Reserva r = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada: " + id));

        if (currentUserRol != UserRol.ADMIN) {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(currentUserId).stream()
                    .map(Herramienta::getId)
                    .toList();
            boolean esPropietario = r.getDetalles() != null && r.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId()));
            boolean esCliente = r.getClienteId().equals(currentUserId);

            if (presentaDanos || (costoReparacion != null && costoReparacion.compareTo(BigDecimal.ZERO) > 0)) {
                if (!esPropietario) {
                    throw new OperacionInvalidaException("No autorizado: solo el propietario de la herramienta o un administrador puede registrar daños y costos de reparación");
                }
            } else if (!esPropietario && !esCliente) {
                throw new OperacionInvalidaException("No autorizado: no es el propietario de la herramienta ni el cliente titular de la reserva para registrar la devolución");
            }
        }

        r.setSolicitudDevolucion(true);
        r.setFechaDevolucion(Instant.now());
        r.setDescripcionDano(presentaDanos ? descripcionDano : null);
        r.setCostoReparacion(presentaDanos && costoReparacion != null ? Dinero.cop(costoReparacion) : Dinero.cero());
        r.setObservacionPropietario(observaciones);

        if (aceptada) {
            if (r.getEstado() != ReservaEstado.FINALIZADA) {
                r.finalizar(Instant.now());
            }
            r.setEstadoDevolucion(presentaDanos ? "ACEPTADA_CON_DANOS" : "ACEPTADA");
        } else {
            r.setEstadoDevolucion("RECHAZADA_POR_DANOS");
        }

        Reserva guardada = reservaRepository.save(r);

        if (presentaDanos && costoReparacion != null && costoReparacion.compareTo(BigDecimal.ZERO) > 0 && facturaService != null) {
            facturaService.emitirFacturaReparacion(guardada, Dinero.cop(costoReparacion), descripcionDano);
        }

        return guardada;
    }

    @Transactional
    public Reserva devolverEquipo(UUID id, boolean presentaDanos, String descripcionDano, BigDecimal costoReparacion, UUID currentUserId, UserRol currentUserRol) {
        return devolverEquipo(id, true, presentaDanos, descripcionDano, costoReparacion, null, currentUserId, currentUserRol);
    }


}
