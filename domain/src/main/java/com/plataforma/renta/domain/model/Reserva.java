package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Dinero;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Reserva {

    private final UUID id;
    private final UUID clienteId;
    private ReservaEstado estado;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final Instant fechaCreacion;
    private Instant fechaConfirmacion;
    private Instant fechaExpiracionPendiente;
    private Instant fechaCancelacion;
    private String motivoCancelacion;
    private UUID canceladoPorUsuarioId;
    private Instant actualizadoEn;
    private final List<DetalleReserva> detalles = new ArrayList<>();

    public Reserva(UUID id, UUID clienteId, ReservaEstado estado,
                   LocalDate fechaInicio, LocalDate fechaFin,
                   Instant fechaCreacion, Instant fechaExpiracionPendiente) {
        this.id = id;
        this.clienteId = clienteId;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracionPendiente = fechaExpiracionPendiente;
        this.actualizadoEn = Instant.now();
    }

    public static Reserva reconstruir(
            UUID id, UUID clienteId, ReservaEstado estado,
            LocalDate fechaInicio, LocalDate fechaFin,
            Instant fechaCreacion, Instant fechaConfirmacion,
            Instant fechaExpiracionPendiente, Instant fechaCancelacion,
            String motivoCancelacion, UUID canceladoPorUsuarioId,
            Instant actualizadoEn) {
        Reserva r = new Reserva(id, clienteId, estado, fechaInicio, fechaFin, fechaCreacion, fechaExpiracionPendiente);
        r.fechaConfirmacion = fechaConfirmacion;
        r.fechaCancelacion = fechaCancelacion;
        r.motivoCancelacion = motivoCancelacion;
        r.canceladoPorUsuarioId = canceladoPorUsuarioId;
        r.actualizadoEn = actualizadoEn != null ? actualizadoEn : Instant.now();
        return r;
    }

    public void agregarDetalle(DetalleReserva detalle) {
        if (detalle.getReservaId() != null && !detalle.getReservaId().equals(this.id)) {
            throw new IllegalArgumentException("El detalle pertenece a otra reserva");
        }
        detalles.add(detalle);
    }

    public void removerDetalle(DetalleReserva detalle) {
        detalles.remove(detalle);
    }

    public Dinero calcularTotal() {
        return detalles.stream()
                .map(DetalleReserva::getTotal)
                .reduce(Dinero.cero(), Dinero::sumar);
    }

    public Dinero calcularSubtotal() {
        return detalles.stream()
                .map(DetalleReserva::getSubtotal)
                .reduce(Dinero.cero(), Dinero::sumar);
    }

    public Dinero calcularIva() {
        return detalles.stream()
                .map(DetalleReserva::getIva)
                .reduce(Dinero.cero(), Dinero::sumar);
    }

    public boolean estaVencida(Instant ahora) {
        return estado == ReservaEstado.PENDIENTE
                && fechaExpiracionPendiente != null
                && fechaExpiracionPendiente.isBefore(ahora);
    }

    public boolean puedeConfirmar() {
        return estado == ReservaEstado.PENDIENTE;
    }

    public boolean puedeCancelar() {
        return estado == ReservaEstado.PENDIENTE
                || estado == ReservaEstado.CONFIRMADA
                || estado == ReservaEstado.EN_CURSO;
    }

    public void confirmar(Instant ahora) {
        if (!puedeConfirmar()) throw new IllegalStateException("Reserva no puede confirmarse desde estado: " + estado);
        this.estado = ReservaEstado.CONFIRMADA;
        this.fechaConfirmacion = ahora;
    }

    public void expirar(Instant ahora) {
        if (estado != ReservaEstado.PENDIENTE) throw new IllegalStateException("Solo PENDIENTE puede expirar");
        this.estado = ReservaEstado.EXPIRADA;
        this.fechaCancelacion = ahora;
        this.motivoCancelacion = "EXPIRADA_SIN_PAGO";
    }

    public void cancelar(Instant ahora, String motivo, UUID canceladoPor) {
        if (!puedeCancelar()) throw new IllegalStateException("Reserva no puede cancelarse desde estado: " + estado);
        this.estado = ReservaEstado.CANCELADA;
        this.fechaCancelacion = ahora;
        this.motivoCancelacion = motivo;
        this.canceladoPorUsuarioId = canceladoPor;
    }

    public void iniciar(Instant ahora) {
        if (estado != ReservaEstado.CONFIRMADA) throw new IllegalStateException("Solo CONFIRMADA puede iniciar");
        this.estado = ReservaEstado.EN_CURSO;
    }

    public void finalizar(Instant ahora) {
        if (estado != ReservaEstado.EN_CURSO) throw new IllegalStateException("Solo EN_CURSO puede finalizar");
        this.estado = ReservaEstado.FINALIZADA;
    }

    public UUID getId() { return id; }
    public UUID getClienteId() { return clienteId; }
    public ReservaEstado getEstado() { return estado; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaConfirmacion() { return fechaConfirmacion; }
    public Instant getFechaExpiracionPendiente() { return fechaExpiracionPendiente; }
    public Instant getFechaCancelacion() { return fechaCancelacion; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public UUID getCanceladoPorUsuarioId() { return canceladoPorUsuarioId; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public List<DetalleReserva> getDetalles() { return List.copyOf(detalles); }
}
