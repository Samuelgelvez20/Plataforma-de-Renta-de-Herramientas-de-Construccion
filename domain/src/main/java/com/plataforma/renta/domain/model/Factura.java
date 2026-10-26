package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Dinero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Factura {

    private final UUID id;
    private final UUID reservaId;
    private final String numeroFactura;
    private final FacturaTipo tipo;
    private final UUID facturaOriginalId;
    private final UUID pagoReembolsoId;
    private final Dinero subtotal;
    private final Dinero iva;
    private final Dinero total;
    private final BigDecimal ivaPorcentaje;
    private final boolean ivaAplica;
    private final String datosEmisor;
    private final String datosReceptor;
    private final Instant fechaEmision;
    private final Instant creadoEn;
    private final String moneda;
    private final String estado;
    private final String descripcionDano;
    private final UUID pagoId;

    public Factura(UUID id, UUID reservaId, String numeroFactura,
                   FacturaTipo tipo, UUID facturaOriginalId, UUID pagoReembolsoId,
                   Dinero subtotal, Dinero iva, Dinero total,
                   BigDecimal ivaPorcentaje, boolean ivaAplica,
                   String datosEmisor, String datosReceptor,
                   Instant fechaEmision, Instant creadoEn, String moneda) {
        this(id, reservaId, numeroFactura, tipo, facturaOriginalId, pagoReembolsoId,
             subtotal, iva, total, ivaPorcentaje, ivaAplica,
             datosEmisor, datosReceptor, fechaEmision, creadoEn, moneda,
             tipo == FacturaTipo.REPARACION ? "PENDIENTE" : "PAGADA", null, null);
    }

    public Factura(UUID id, UUID reservaId, String numeroFactura,
                   FacturaTipo tipo, UUID facturaOriginalId, UUID pagoReembolsoId,
                   Dinero subtotal, Dinero iva, Dinero total,
                   BigDecimal ivaPorcentaje, boolean ivaAplica,
                   String datosEmisor, String datosReceptor,
                   Instant fechaEmision, Instant creadoEn, String moneda,
                   String estado, String descripcionDano, UUID pagoId) {
        this.id = id;
        this.reservaId = reservaId;
        this.numeroFactura = numeroFactura;
        this.tipo = tipo;
        this.facturaOriginalId = facturaOriginalId;
        this.pagoReembolsoId = pagoReembolsoId;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.ivaPorcentaje = ivaPorcentaje;
        this.ivaAplica = ivaAplica;
        this.datosEmisor = datosEmisor;
        this.datosReceptor = datosReceptor;
        this.fechaEmision = fechaEmision;
        this.creadoEn = creadoEn;
        this.moneda = moneda;
        this.estado = estado != null ? estado : "PAGADA";
        this.descripcionDano = descripcionDano;
        this.pagoId = pagoId;
    }

    public static Factura original(UUID reservaId, String numeroFactura,
                                   Dinero subtotal, Dinero iva, Dinero total,
                                   BigDecimal ivaPorcentaje, boolean ivaAplica,
                                   String datosEmisor, String datosReceptor) {
        return new Factura(UUID.randomUUID(), reservaId, numeroFactura,
                FacturaTipo.FACTURA, null, null,
                subtotal, iva, total, ivaPorcentaje, ivaAplica,
                datosEmisor, datosReceptor, Instant.now(), Instant.now(), "COP",
                "PAGADA", null, null);
    }

    public static Factura notaCredito(UUID reservaId, String numeroFactura,
                                       UUID facturaOriginalId, UUID pagoReembolsoId,
                                       Dinero subtotal, Dinero iva, Dinero total,
                                       BigDecimal ivaPorcentaje, boolean ivaAplica,
                                       String datosEmisor, String datosReceptor) {
        return new Factura(UUID.randomUUID(), reservaId, numeroFactura,
                FacturaTipo.NOTA_CREDITO, facturaOriginalId, pagoReembolsoId,
                subtotal, iva, total, ivaPorcentaje, ivaAplica,
                datosEmisor, datosReceptor, Instant.now(), Instant.now(), "COP",
                "PAGADA", null, null);
    }

    public static Factura reparacion(UUID reservaId, String numeroFactura,
                                     Dinero subtotal, Dinero iva, Dinero total,
                                     BigDecimal ivaPorcentaje, boolean ivaAplica,
                                     String datosEmisor, String datosReceptor,
                                     String descripcionDano) {
        return new Factura(UUID.randomUUID(), reservaId, numeroFactura,
                FacturaTipo.REPARACION, null, null,
                subtotal, iva, total, ivaPorcentaje, ivaAplica,
                datosEmisor, datosReceptor, Instant.now(), Instant.now(), "COP",
                "PENDIENTE", descripcionDano, null);
    }

    public Factura conEstadoPagada(UUID pagoId) {
        return new Factura(this.id, this.reservaId, this.numeroFactura,
                this.tipo, this.facturaOriginalId, this.pagoReembolsoId,
                this.subtotal, this.iva, this.total, this.ivaPorcentaje, this.ivaAplica,
                this.datosEmisor, this.datosReceptor, this.fechaEmision, this.creadoEn, this.moneda,
                "PAGADA", this.descripcionDano, pagoId);
    }

    public UUID getId() { return id; }
    public UUID getReservaId() { return reservaId; }
    public String getNumeroFactura() { return numeroFactura; }
    public FacturaTipo getTipo() { return tipo; }
    public UUID getFacturaOriginalId() { return facturaOriginalId; }
    public UUID getPagoReembolsoId() { return pagoReembolsoId; }
    public Dinero getSubtotal() { return subtotal; }
    public Dinero getIva() { return iva; }
    public Dinero getTotal() { return total; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public boolean isIvaAplica() { return ivaAplica; }
    public String getDatosEmisor() { return datosEmisor; }
    public String getDatosReceptor() { return datosReceptor; }
    public Instant getFechaEmision() { return fechaEmision; }
    public Instant getCreadoEn() { return creadoEn; }
    public String getMoneda() { return moneda; }
    public String getEstado() { return estado; }
    public String getDescripcionDano() { return descripcionDano; }
    public UUID getPagoId() { return pagoId; }
}