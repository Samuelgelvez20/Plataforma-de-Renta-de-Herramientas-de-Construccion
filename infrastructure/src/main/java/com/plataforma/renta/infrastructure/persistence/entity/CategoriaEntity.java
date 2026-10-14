package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categoria")
public class CategoriaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected CategoriaEntity() {}

    public CategoriaEntity(UUID id, String nombre, String descripcion, Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadoEn = creadoEn;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (creadoEn == null) creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Instant getCreadoEn() { return creadoEn; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}