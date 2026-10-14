package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "cliente")
public class ClienteEntity implements Persistable<UUID> {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @Column(name = "direccion_entrega_default", columnDefinition = "TEXT")
    private String direccionEntregaDefault;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "datos_facturacion", columnDefinition = "jsonb")
    private String datosFacturacion;

    @Transient
    private boolean isNew = true;

    public ClienteEntity() {}

    public ClienteEntity(UsuarioEntity usuario) {
        this.usuario = usuario;
        if (usuario != null) {
            this.usuarioId = usuario.getId();
        }
    }

    @Override
    public UUID getId() {
        return usuarioId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public ClienteEntity(UsuarioEntity usuario, String direccionEntregaDefault, String datosFacturacion) {
        this.usuario = usuario;
        if (usuario != null) {
            this.usuarioId = usuario.getId();
        }
        this.direccionEntregaDefault = direccionEntregaDefault;
        this.datosFacturacion = datosFacturacion;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public UsuarioEntity getUsuario() { return usuario; }
    public String getDireccionEntregaDefault() { return direccionEntregaDefault; }
    public String getDatosFacturacion() { return datosFacturacion; }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        if (usuario != null) {
            this.usuarioId = usuario.getId();
        }
    }
    public void setDireccionEntregaDefault(String direccionEntregaDefault) { this.direccionEntregaDefault = direccionEntregaDefault; }
    public void setDatosFacturacion(String datosFacturacion) { this.datosFacturacion = datosFacturacion; }
}
