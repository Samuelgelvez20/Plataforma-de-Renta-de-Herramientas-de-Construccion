package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "factura")
public class FacturaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaEntity reserva;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 30)
    private String numeroFactura;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.type.PostgreSQLEnumJdbcType.class)
    @Column(name = "tipo", nullable = false, columnDefinition = "factura_tipo")
    private FacturaTipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_original_id")
    private FacturaEntity facturaOriginal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_reembolso_id")
    private PagoEntity pagoReembolso;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "iva", nullable = false, precision = 14, scale = 2)
    private BigDecimal iva;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "iva_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje;

    @Column(name = "iva_aplica", nullable = false)
    private Boolean ivaAplica;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "datos_emisor", nullable = false, columnDefinition = "jsonb")
    private String datosEmisor;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "datos_receptor", nullable = false, columnDefinition = "jsonb")
    private String datosReceptor;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public FacturaEntity() {}

    public FacturaEntity(UUID id, ReservaEntity reserva, String numeroFactura,
                         FacturaTipo tipo, FacturaEntity facturaOriginal,
                         PagoEntity pagoReembolso, BigDecimal subtotal, BigDecimal iva,
                         BigDecimal total, String moneda, BigDecimal ivaPorcentaje,
                         Boolean ivaAplica, String datosEmisor, String datosReceptor,
                         Instant fechaEmision, Instant creadoEn) {
        this.id = id;
        this.reserva = reserva;
        this.numeroFactura = numeroFactura;
        this.tipo = tipo;
        this.facturaOriginal = facturaOriginal;
        this.pagoReembolso = pagoReembolso;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.moneda = moneda;
        this.ivaPorcentaje = ivaPorcentaje;
        this.ivaAplica = ivaAplica;
        this.datosEmisor = datosEmisor;
        this.datosReceptor = datosReceptor;
        this.fechaEmision = fechaEmision;
        this.creadoEn = creadoEn;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (creadoEn == null) creadoEn = Instant.now();
        if (tipo == null) tipo = FacturaTipo.FACTURA;
        if (moneda == null) moneda = "COP";
        if (ivaAplica == null) ivaAplica = true;
    }

    public UUID getId() { return id; }
    public ReservaEntity getReserva() { return reserva; }
    public String getNumeroFactura() { return numeroFactura; }
    public FacturaTipo getTipo() { return tipo; }
    public FacturaEntity getFacturaOriginal() { return facturaOriginal; }
    public PagoEntity getPagoReembolso() { return pagoReembolso; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getTotal() { return total; }
    public String getMoneda() { return moneda; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public Boolean getIvaAplica() { return ivaAplica; }
    public String getDatosEmisor() { return datosEmisor; }
    public String getDatosReceptor() { return datosReceptor; }
    public Instant getFechaEmision() { return fechaEmision; }
    public Instant getCreadoEn() { return creadoEn; }

    public void setReserva(ReservaEntity reserva) { this.reserva = reserva; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    public void setTipo(FacturaTipo tipo) { this.tipo = tipo; }
    public void setFacturaOriginal(FacturaEntity facturaOriginal) { this.facturaOriginal = facturaOriginal; }
    public void setPagoReembolso(PagoEntity pagoReembolso) { this.pagoReembolso = pagoReembolso; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public void setIvaAplica(Boolean ivaAplica) { this.ivaAplica = ivaAplica; }
    public void setDatosEmisor(String datosEmisor) { this.datosEmisor = datosEmisor; }
    public void setDatosReceptor(String datosReceptor) { this.datosReceptor = datosReceptor; }
    public void setFechaEmision(Instant fechaEmision) { this.fechaEmision = fechaEmision; }
}