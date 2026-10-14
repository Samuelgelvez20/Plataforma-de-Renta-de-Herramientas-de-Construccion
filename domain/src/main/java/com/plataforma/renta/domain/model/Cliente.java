package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Email;

import java.util.UUID;

public final class Cliente {

    private final UUID id;
    private final Email email;
    private final String nombre;
    private final String apellido;
    private final String telefono;
    private final String direccionEntregaDefault;
    private final String datosFacturacion;

    public Cliente(UUID id, Email email, String nombre, String apellido,
                   String telefono, String direccionEntregaDefault, String datosFacturacion) {
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.direccionEntregaDefault = direccionEntregaDefault;
        this.datosFacturacion = datosFacturacion;
    }

    public UUID getId() { return id; }
    public Email getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getTelefono() { return telefono; }
    public String getDireccionEntregaDefault() { return direccionEntregaDefault; }
    public String getDatosFacturacion() { return datosFacturacion; }
}