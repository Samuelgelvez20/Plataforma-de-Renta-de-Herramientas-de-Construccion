package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "propietario")
public class PropietarioEntity implements Persistable<UUID> {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @Transient
    private boolean isNew = true;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "direccion_fiscal", nullable = false, columnDefinition = "TEXT")
    private String direccionFiscal;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "regimen", nullable = false, length = 30)
    private String regimen;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "departamento", nullable = false, length = 100)
    private String departamento;

    @Column(name = "verificado_en")
    private Instant verificadoEn;

    protected PropietarioEntity() {}

    public PropietarioEntity(UsuarioEntity usuario, String razonSocial,
                             String direccionFiscal, String telefonoContacto,
                             String regimen, String municipio, String departamento) {
        this.usuario = usuario;
        if (usuario != null) {
            this.usuarioId = usuario.getId();
        }
        this.razonSocial = razonSocial;
        this.direccionFiscal = direccionFiscal;
        this.telefonoContacto = telefonoContacto;
        this.regimen = regimen;
        this.municipio = municipio;
        this.departamento = departamento;
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

    public UUID getUsuarioId() { return usuarioId; }
    public UsuarioEntity getUsuario() { return usuario; }
    public String getRazonSocial() { return razonSocial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public String getRegimen() { return regimen; }
    public String getMunicipio() { return municipio; }
    public String getDepartamento() { return departamento; }
    public Instant getVerificadoEn() { return verificadoEn; }
    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        if (usuario != null) {
            this.usuarioId = usuario.getId();
        }
    }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public void setDireccionFiscal(String direccionFiscal) { this.direccionFiscal = direccionFiscal; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }
    public void setRegimen(String regimen) { this.regimen = regimen; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public void setVerificadoEn(Instant verificadoEn) { this.verificadoEn = verificadoEn; }
}