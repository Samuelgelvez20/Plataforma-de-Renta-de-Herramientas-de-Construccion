package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "detalle_reserva")
public class DetalleReservaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaEntity reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "herramienta_id", nullable = false)
    private HerramientaEntity herramienta;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_dia_acordado", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioDiaAcordado;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "iva", nullable = false, precision = 14, scale = 2)
    private BigDecimal iva;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "iva_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje;

    @Column(name = "iva_aplica", nullable = false)
    private Boolean ivaAplica;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected DetalleReservaEntity() {}

    public DetalleReservaEntity(UUID id, ReservaEntity reserva, HerramientaEntity herramienta,
                                Integer cantidad, BigDecimal precioDiaAcordado,
                                BigDecimal subtotal, BigDecimal iva, BigDecimal total,
                                BigDecimal ivaPorcentaje, Boolean ivaAplica, Instant creadoEn) {
        this.id = id;
        this.reserva = reserva;
        this.herramienta = herramienta;
        this.cantidad = cantidad;
        this.precioDiaAcordado = precioDiaAcordado;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.ivaPorcentaje = ivaPorcentaje;
        this.ivaAplica = ivaAplica;
        this.creadoEn = creadoEn;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (creadoEn == null) creadoEn = Instant.now();
        if (ivaAplica == null) ivaAplica = true;
    }

    public UUID getId() { return id; }
    public ReservaEntity getReserva() { return reserva; }
    public HerramientaEntity getHerramienta() { return herramienta; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioDiaAcordado() { return precioDiaAcordado; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public Boolean getIvaAplica() { return ivaAplica; }
    public Instant getCreadoEn() { return creadoEn; }

    public void setReserva(ReservaEntity reserva) { this.reserva = reserva; }
    public void setHerramienta(HerramientaEntity herramienta) { this.herramienta = herramienta; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public void setPrecioDiaAcordado(BigDecimal precioDiaAcordado) { this.precioDiaAcordado = precioDiaAcordado; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public void setIvaAplica(Boolean ivaAplica) { this.ivaAplica = ivaAplica; }
}