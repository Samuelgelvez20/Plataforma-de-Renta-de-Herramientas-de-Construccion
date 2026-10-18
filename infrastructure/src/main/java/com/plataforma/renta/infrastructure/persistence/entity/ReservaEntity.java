package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reserva")
public class ReservaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(com.plataforma.renta.infrastructure.persistence.type.CustomPostgreSQLEnumJdbcType.class)
    @Column(name = "estado", nullable = false, columnDefinition = "reserva_estado")
    private ReservaEstado estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_confirmacion")
    private Instant fechaConfirmacion;

    @Column(name = "fecha_expiracion_pendiente")
    private Instant fechaExpiracionPendiente;

    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    @Column(name = "cancelado_por_usuario_id")
    private UUID canceladoPorUsuarioId;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToMany(mappedBy = "reserva", fetch = FetchType.LAZY)
    private List<DetalleReservaEntity> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", fetch = FetchType.LAZY)
    private List<PagoEntity> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", fetch = FetchType.LAZY)
    private List<FacturaEntity> facturas = new ArrayList<>();

    protected ReservaEntity() {}

    public ReservaEntity(UUID id, ClienteEntity cliente, ReservaEstado estado,
                         LocalDate fechaInicio, LocalDate fechaFin,
                         Instant fechaCreacion, Instant fechaExpiracionPendiente,
                         Instant actualizadoEn) {
        this.id = id;
        this.cliente = cliente;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracionPendiente = fechaExpiracionPendiente;
        this.actualizadoEn = actualizadoEn;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (fechaCreacion == null) fechaCreacion = Instant.now();
        if (estado == null) estado = ReservaEstado.PENDIENTE;
        if (actualizadoEn == null) actualizadoEn = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public ClienteEntity getCliente() { return cliente; }
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
    public List<DetalleReservaEntity> getDetalles() { return detalles; }
    public List<PagoEntity> getPagos() { return pagos; }
    public List<FacturaEntity> getFacturas() { return facturas; }

    public void setCliente(ClienteEntity cliente) { this.cliente = cliente; }
    public void setEstado(ReservaEstado estado) { this.estado = estado; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public void setFechaConfirmacion(Instant fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }
    public void setFechaExpiracionPendiente(Instant fechaExpiracionPendiente) { this.fechaExpiracionPendiente = fechaExpiracionPendiente; }
    public void setFechaCancelacion(Instant fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public void setCanceladoPorUsuarioId(UUID canceladoPorUsuarioId) { this.canceladoPorUsuarioId = canceladoPorUsuarioId; }
    public void addDetalle(DetalleReservaEntity detalle) {
        detalles.add(detalle);
        detalle.setReserva(this);
    }
    public void removeDetalle(DetalleReservaEntity detalle) {
        detalles.remove(detalle);
        detalle.setReserva(null);
    }
}