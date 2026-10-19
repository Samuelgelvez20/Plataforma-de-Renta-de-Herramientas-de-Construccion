package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "herramienta")
public class HerramientaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaEntity categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propietario_id", nullable = false)
    private PropietarioEntity propietario;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "marca", length = 50)
    private String marca;

    @Column(name = "modelo", length = 50)
    private String modelo;

    @Column(name = "precio_dia", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioDia;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "cantidad_disponible")
    private Integer cantidadDisponible;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(com.plataforma.renta.infrastructure.persistence.type.CustomPostgreSQLEnumJdbcType.class)
    @Column(name = "estado_publicacion", nullable = false, columnDefinition = "herramienta_pub_estado")
    private HerramientaPubEstado estadoPublicacion;

    @Column(name = "iva_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje;

    @Column(name = "iva_aplica", nullable = false)
    private Boolean ivaAplica;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected HerramientaEntity() {}

    public HerramientaEntity(UUID id, CategoriaEntity categoria, PropietarioEntity propietario,
                             String nombre, String descripcion, String marca, String modelo,
                             BigDecimal precioDia, Integer cantidadTotal,
                             HerramientaPubEstado estadoPublicacion,
                             BigDecimal ivaPorcentaje, Boolean ivaAplica,
                             Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.categoria = categoria;
        this.propietario = propietario;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.modelo = modelo;
        this.precioDia = precioDia;
        this.cantidadTotal = cantidadTotal;
        this.estadoPublicacion = estadoPublicacion;
        this.ivaPorcentaje = ivaPorcentaje;
        this.ivaAplica = ivaAplica;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (creadoEn == null) creadoEn = Instant.now();
        if (actualizadoEn == null) actualizadoEn = Instant.now();
        if (estadoPublicacion == null) estadoPublicacion = HerramientaPubEstado.PUBLICADA;
        if (cantidadDisponible == null) cantidadDisponible = cantidadTotal;
        if (ivaPorcentaje == null) ivaPorcentaje = new BigDecimal("19.00");
        if (ivaAplica == null) ivaAplica = true;
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public CategoriaEntity getCategoria() { return categoria; }
    public PropietarioEntity getPropietario() { return propietario; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public BigDecimal getPrecioDia() { return precioDia; }
    public Integer getCantidadTotal() { return cantidadTotal; }
    public Integer getCantidadDisponible() { return cantidadDisponible != null ? cantidadDisponible : cantidadTotal; }
    public void setCantidadDisponible(Integer cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }

    public HerramientaPubEstado getEstadoPublicacion() { return estadoPublicacion; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public Boolean getIvaAplica() { return ivaAplica; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }

    public void setCategoria(CategoriaEntity categoria) { this.categoria = categoria; }
    public void setPropietario(PropietarioEntity propietario) { this.propietario = propietario; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setPrecioDia(BigDecimal precioDia) { this.precioDia = precioDia; }
    public void setCantidadTotal(Integer cantidadTotal) { this.cantidadTotal = cantidadTotal; }
    public void setEstadoPublicacion(HerramientaPubEstado estadoPublicacion) { this.estadoPublicacion = estadoPublicacion; }
    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public void setIvaAplica(Boolean ivaAplica) { this.ivaAplica = ivaAplica; }
}