package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reporte_dano")
public class ReporteDanoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_reserva_id", nullable = false)
    private DetalleReservaEntity detalleReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "herramienta_id", nullable = false)
    private HerramientaEntity herramienta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportado_por_usuario_id", nullable = false)
    private UsuarioEntity reportadoPorUsuario;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(com.plataforma.renta.infrastructure.persistence.type.CustomPostgreSQLEnumJdbcType.class)
    @Column(name = "severidad", nullable = false, columnDefinition = "dano_severidad")
    private DanoSeveridad severidad;

    @Column(name = "costo_estimado_reparacion", precision = 14, scale = 2)
    private BigDecimal costoEstimadoReparacion;

    @Column(name = "fecha_reporte", nullable = false)
    private Instant fechaReporte;

    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    @Column(name = "resolucion", columnDefinition = "TEXT")
    private String resolucion;

    protected ReporteDanoEntity() {}

    public ReporteDanoEntity(UUID id, DetalleReservaEntity detalleReserva,
                             HerramientaEntity herramienta, UsuarioEntity reportadoPorUsuario,
                             String descripcion, DanoSeveridad severidad,
                             BigDecimal costoEstimadoReparacion, Instant fechaReporte,
                             Instant resueltoEn, String resolucion) {
        this.id = id;
        this.detalleReserva = detalleReserva;
        this.herramienta = herramienta;
        this.reportadoPorUsuario = reportadoPorUsuario;
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.costoEstimadoReparacion = costoEstimadoReparacion;
        this.fechaReporte = fechaReporte;
        this.resueltoEn = resueltoEn;
        this.resolucion = resolucion;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (fechaReporte == null) fechaReporte = Instant.now();
    }

    public UUID getId() { return id; }
    public DetalleReservaEntity getDetalleReserva() { return detalleReserva; }
    public HerramientaEntity getHerramienta() { return herramienta; }
    public UsuarioEntity getReportadoPorUsuario() { return reportadoPorUsuario; }
    public String getDescripcion() { return descripcion; }
    public DanoSeveridad getSeveridad() { return severidad; }
    public BigDecimal getCostoEstimadoReparacion() { return costoEstimadoReparacion; }
    public Instant getFechaReporte() { return fechaReporte; }
    public Instant getResueltoEn() { return resueltoEn; }
    public String getResolucion() { return resolucion; }

    public void setDetalleReserva(DetalleReservaEntity detalleReserva) { this.detalleReserva = detalleReserva; }
    public void setHerramienta(HerramientaEntity herramienta) { this.herramienta = herramienta; }
    public void setReportadoPorUsuario(UsuarioEntity reportadoPorUsuario) { this.reportadoPorUsuario = reportadoPorUsuario; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setSeveridad(DanoSeveridad severidad) { this.severidad = severidad; }
    public void setCostoEstimadoReparacion(BigDecimal costoEstimadoReparacion) { this.costoEstimadoReparacion = costoEstimadoReparacion; }
    public void setFechaReporte(Instant fechaReporte) { this.fechaReporte = fechaReporte; }
    public void setResueltoEn(Instant resueltoEn) { this.resueltoEn = resueltoEn; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }
}