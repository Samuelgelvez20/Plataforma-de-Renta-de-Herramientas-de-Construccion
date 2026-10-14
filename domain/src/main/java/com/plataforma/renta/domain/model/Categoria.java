package com.plataforma.renta.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class Categoria {

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    private final Instant creadoEn;

    public Categoria(UUID id, String nombre, String descripcion, Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Instant getCreadoEn() { return creadoEn; }
}