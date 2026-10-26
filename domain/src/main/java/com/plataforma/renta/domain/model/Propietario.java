package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Email;

import java.time.Instant;
import java.util.UUID;

public final class Propietario {

    private final UUID id;
    private final Email email;
    private final String razonSocial;
    private final String direccionFiscal;
    private final String telefonoContacto;
    private final String regimen;
    private final String municipio;
    private final String departamento;
    private final Instant verificadoEn;

    public Propietario(UUID id, Email email,
                       String razonSocial, String direccionFiscal,
                       String telefonoContacto, String regimen,
                       String municipio, String departamento,
                       Instant verificadoEn) {
        this.id = id;
        this.email = email;
        this.razonSocial = razonSocial;
        this.direccionFiscal = direccionFiscal;
        this.telefonoContacto = telefonoContacto;
        this.regimen = regimen;
        this.municipio = municipio;
        this.departamento = departamento;
        this.verificadoEn = verificadoEn;
    }

    public UUID getId() { return id; }
    public Email getEmail() { return email; }
    public String getRazonSocial() { return razonSocial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public String getRegimen() { return regimen; }
    public String getMunicipio() { return municipio; }
    public String getDepartamento() { return departamento; }
    public Instant getVerificadoEn() { return verificadoEn; }
}