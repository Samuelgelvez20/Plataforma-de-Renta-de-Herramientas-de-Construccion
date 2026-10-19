package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Dinero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Herramienta {

    private final UUID id;
    private final UUID categoriaId;
    private final UUID propietarioId;
    private final String nombre;
    private final String descripcion;
    private final String marca;
    private final String modelo;
    private Dinero precioDia;
    private final int cantidadTotal;
    private final int cantidadDisponible;
    private HerramientaPubEstado estadoPublicacion;
    private BigDecimal ivaPorcentaje;
    private boolean ivaAplica;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public Herramienta(UUID id, UUID categoriaId, UUID propietarioId,
                       String nombre, String descripcion, String marca, String modelo,
                       Dinero precioDia, int cantidadTotal, HerramientaPubEstado estadoPublicacion,
                       BigDecimal ivaPorcentaje, boolean ivaAplica,
                       Instant creadoEn, Instant actualizadoEn) {
        this(id, categoriaId, propietarioId, nombre, descripcion, marca, modelo,
             precioDia, cantidadTotal, cantidadTotal, estadoPublicacion,
             ivaPorcentaje, ivaAplica, creadoEn, actualizadoEn);
    }

    public Herramienta(UUID id, UUID categoriaId, UUID propietarioId,
                       String nombre, String descripcion, String marca, String modelo,
                       Dinero precioDia, int cantidadTotal, int cantidadDisponible,
                       HerramientaPubEstado estadoPublicacion,
                       BigDecimal ivaPorcentaje, boolean ivaAplica,
                       Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.propietarioId = propietarioId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.modelo = modelo;
        this.precioDia = precioDia;
        this.cantidadTotal = cantidadTotal;
        this.cantidadDisponible = cantidadDisponible;
        this.estadoPublicacion = estadoPublicacion;
        this.ivaPorcentaje = ivaPorcentaje;
        this.ivaAplica = ivaAplica;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public UUID getId() { return id; }
    public UUID getCategoriaId() { return categoriaId; }
    public UUID getPropietarioId() { return propietarioId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Dinero getPrecioDia() { return precioDia; }
    public int getCantidadTotal() { return cantidadTotal; }
    public int getCantidadDisponible() { return cantidadDisponible; }
    public HerramientaPubEstado getEstadoPublicacion() { return estadoPublicacion; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public boolean isIvaAplica() { return ivaAplica; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }

    public void setEstadoPublicacion(HerramientaPubEstado estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
        this.actualizadoEn = Instant.now();
    }

    public void setPrecioDia(Dinero precioDia) {
        this.precioDia = precioDia;
        this.actualizadoEn = Instant.now();
    }

    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) {
        this.ivaPorcentaje = ivaPorcentaje;
        this.actualizadoEn = Instant.now();
    }

    public void setIvaAplica(boolean ivaAplica) {
        this.ivaAplica = ivaAplica;
        this.actualizadoEn = Instant.now();
    }
}
