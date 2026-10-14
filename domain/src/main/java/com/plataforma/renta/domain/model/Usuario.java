package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Email;

import java.time.Instant;
import java.util.UUID;

public final class Usuario {

    private final UUID id;
    private final Email email;
    private final String passwordHash;
    private final UserRol rol;
    private final String nombre;
    private final String apellido;
    private final String telefono;
    private final Instant creadoEn;
    private final Instant actualizadoEn;

    public Usuario(UUID id, Email email, String passwordHash, UserRol rol,
                   String nombre, String apellido, String telefono,
                   Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public UUID getId() { return id; }
    public Email getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRol getRol() { return rol; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getTelefono() { return telefono; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}