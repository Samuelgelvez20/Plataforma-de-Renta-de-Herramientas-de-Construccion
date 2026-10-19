package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(com.plataforma.renta.infrastructure.persistence.type.CustomPostgreSQLEnumJdbcType.class)
    @Column(name = "rol", nullable = false, columnDefinition = "user_rol")
    private UserRol rol;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PropietarioEntity propietario;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ClienteEntity cliente;

    public UsuarioEntity() {}

    public UsuarioEntity(UUID id, String email, String passwordHash, UserRol rol,
                         String nombre, String apellido, String telefono, boolean activo,
                         Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.activo = activo;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public UsuarioEntity(UUID id, String email, String passwordHash, UserRol rol,
                         String nombre, String apellido, String telefono,
                         Instant creadoEn, Instant actualizadoEn) {
        this(id, email, passwordHash, rol, nombre, apellido, telefono, true, creadoEn, actualizadoEn);
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (creadoEn == null) creadoEn = Instant.now();
        if (actualizadoEn == null) actualizadoEn = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRol getRol() { return rol; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getTelefono() { return telefono; }
    public boolean isActivo() { return activo; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public PropietarioEntity getPropietario() { return propietario; }
    public ClienteEntity getCliente() { return cliente; }

    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRol(UserRol rol) { this.rol = rol; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setPropietario(PropietarioEntity propietario) { this.propietario = propietario; }
    public void setCliente(ClienteEntity cliente) { this.cliente = cliente; }
}
