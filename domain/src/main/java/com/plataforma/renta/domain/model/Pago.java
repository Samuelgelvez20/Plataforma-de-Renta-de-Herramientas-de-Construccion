package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Dinero;

import java.time.Instant;
import java.util.UUID;

public final class Pago {

    private final UUID id;
    private final UUID reservaId;
    private final UUID pagoOriginalId;
    private PagoEstado estado;
    private final String proveedorPago;
    private String transaccionId;
    private final Dinero monto;
    private final String moneda;
    private String motivo;
    private Instant fechaPago;
    private final Instant creadoEn;

    public Pago(UUID id, UUID reservaId, UUID pagoOriginalId,
                PagoEstado estado, String proveedorPago, String transaccionId,
                Dinero monto, String motivo, Instant fechaPago, Instant creadoEn) {
        this.id = id;
        this.reservaId = reservaId;
        this.pagoOriginalId = pagoOriginalId;
        this.estado = estado;
        this.proveedorPago = proveedorPago;
        this.transaccionId = transaccionId;
        this.monto = monto;
        this.moneda = "COP";
        this.motivo = motivo;
        this.fechaPago = fechaPago;
        this.creadoEn = creadoEn;
    }

    public static Pago pendiente(UUID reservaId, Dinero monto, String proveedorPago) {
        return new Pago(UUID.randomUUID(), reservaId, null,
                PagoEstado.PENDIENTE, proveedorPago, null, monto, null, null, Instant.now());
    }

    public static Pago completado(UUID reservaId, Dinero monto, String proveedorPago, String transaccionId) {
        return new Pago(UUID.randomUUID(), reservaId, null,
                PagoEstado.COMPLETADO, proveedorPago, transaccionId, monto, "Pago confirmado", Instant.now(), Instant.now());
    }

    public static Pago reembolso(UUID reservaId, UUID pagoOriginalId, Dinero monto, String motivo) {
        return new Pago(UUID.randomUUID(), reservaId, pagoOriginalId,
                PagoEstado.REEMBOLSADO, "SIMULADO", null, monto, motivo, Instant.now(), Instant.now());
    }

    public void completar(String transaccionId) {
        if (estado != PagoEstado.PENDIENTE) throw new IllegalStateException("Solo PENDIENTE puede completarse");
        this.estado = PagoEstado.COMPLETADO;
        this.transaccionId = transaccionId;
        this.fechaPago = Instant.now();
    }

    public void fallar() {
        if (estado != PagoEstado.PENDIENTE) throw new IllegalStateException("Solo PENDIENTE puede fallar");
        this.estado = PagoEstado.FALLIDO;
    }

    public void rechazar(String motivoRechazo) {
        if (estado != PagoEstado.PENDIENTE) throw new IllegalStateException("Solo PENDIENTE puede ser rechazado");
        this.estado = PagoEstado.FALLIDO;
        if (motivoRechazo != null && !motivoRechazo.isBlank()) {
            this.motivo = this.motivo != null ? this.motivo + " | Rechazo: " + motivoRechazo : motivoRechazo;
        }
    }

    public void rechazar() {
        rechazar(null);
    }

    public void cambiarEstado(PagoEstado nuevoEstado) {
        if (estado != PagoEstado.PENDIENTE) throw new IllegalStateException("Solo PENDIENTE puede cambiar de estado");
        this.estado = nuevoEstado;
    }

    public boolean esCompletado() { return estado == PagoEstado.COMPLETADO; }
    public boolean esReembolso() { return estado == PagoEstado.REEMBOLSADO; }
    public boolean tienePagoOriginal() { return pagoOriginalId != null; }

    public UUID getId() { return id; }
    public UUID getReservaId() { return reservaId; }
    public UUID getPagoOriginalId() { return pagoOriginalId; }
    public PagoEstado getEstado() { return estado; }
    public String getProveedorPago() { return proveedorPago; }
    public String getTransaccionId() { return transaccionId; }
    public Dinero getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public String getMotivo() { return motivo; }
    public Instant getFechaPago() { return fechaPago; }
    public Instant getCreadoEn() { return creadoEn; }
}