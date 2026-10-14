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

    public Factura(UUID id, UUID reservaId, String numeroFactura,
                   FacturaTipo tipo, UUID facturaOriginalId, UUID pagoReembolsoId,
                   Dinero subtotal, Dinero iva, Dinero total,
                   BigDecimal ivaPorcentaje, boolean ivaAplica,
                   String datosEmisor, String datosReceptor,
                   Instant fechaEmision, Instant creadoEn, String moneda) {
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
    }

    public static Factura original(UUID reservaId, String numeroFactura,
                                   Dinero subtotal, Dinero iva, Dinero total,
                                   BigDecimal ivaPorcentaje, boolean ivaAplica,
                                   String datosEmisor, String datosReceptor) {
        return new Factura(UUID.randomUUID(), reservaId, numeroFactura,
                FacturaTipo.FACTURA, null, null,
                subtotal, iva, total, ivaPorcentaje, ivaAplica,
                datosEmisor, datosReceptor, Instant.now(), Instant.now(), "COP");
    }

public static Factura notaCredito(UUID reservaId, String numeroFactura,
                                       UUID facturaOriginalId, UUID pagoReembolsoId,
                                       Dinero subtotal, Dinero iva, Dinero total,
                                       BigDecimal ivaPorcentaje, boolean ivaAplica,
                                       String datosEmisor, String datosReceptor) {
        return new Factura(UUID.randomUUID(), reservaId, numeroFactura,
                FacturaTipo.NOTA_CREDITO, facturaOriginalId, pagoReembolsoId,
                subtotal, iva, total, ivaPorcentaje, ivaAplica,
                datosEmisor, datosReceptor, Instant.now(), Instant.now(), "COP");
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
}