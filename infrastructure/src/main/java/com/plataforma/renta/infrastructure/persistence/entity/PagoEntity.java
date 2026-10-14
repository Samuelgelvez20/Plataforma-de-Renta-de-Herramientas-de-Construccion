package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pago")
public class PagoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaEntity reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_original_id")
    private PagoEntity pagoOriginal;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.type.PostgreSQLEnumJdbcType.class)
    @Column(name = "estado", nullable = false, columnDefinition = "pago_estado")
    private PagoEstado estado;

    @Column(name = "proveedor_pago", nullable = false, length = 30)
    private String proveedorPago;

    @Column(name = "transaccion_id", length = 100)
    private String transaccionId;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fecha_pago")
    private Instant fechaPago;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public PagoEntity() {}

    public PagoEntity(UUID id, ReservaEntity reserva, PagoEntity pagoOriginal,
                      PagoEstado estado, String proveedorPago, String transaccionId,
                      BigDecimal monto, String moneda, String motivo,
                      Instant fechaPago, Instant creadoEn) {
        this.id = id;
        this.reserva = reserva;
        this.pagoOriginal = pagoOriginal;
        this.estado = estado;
        this.proveedorPago = proveedorPago;
        this.transaccionId = transaccionId;
        this.monto = monto;
        this.moneda = moneda;
        this.motivo = motivo;
        this.fechaPago = fechaPago;
        this.creadoEn = creadoEn;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (creadoEn == null) creadoEn = Instant.now();
        if (estado == null) estado = PagoEstado.PENDIENTE;
        if (proveedorPago == null) proveedorPago = "SIMULADO";
        if (moneda == null) moneda = "COP";
    }

    public UUID getId() { return id; }
    public ReservaEntity getReserva() { return reserva; }
    public PagoEntity getPagoOriginal() { return pagoOriginal; }
    public PagoEstado getEstado() { return estado; }
    public String getProveedorPago() { return proveedorPago; }
    public String getTransaccionId() { return transaccionId; }
    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public String getMotivo() { return motivo; }
    public Instant getFechaPago() { return fechaPago; }
    public Instant getCreadoEn() { return creadoEn; }

    public void setReserva(ReservaEntity reserva) { this.reserva = reserva; }
    public void setPagoOriginal(PagoEntity pagoOriginal) { this.pagoOriginal = pagoOriginal; }
    public void setEstado(PagoEstado estado) { this.estado = estado; }
    public void setProveedorPago(String proveedorPago) { this.proveedorPago = proveedorPago; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public void setFechaPago(Instant fechaPago) { this.fechaPago = fechaPago; }
}