package com.plataforma.renta.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "configuracion_fiscal")
public class ConfiguracionFiscalEntity {

    @Id
    @Column(name = "clave", nullable = false, length = 50)
    private String clave;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected ConfiguracionFiscalEntity() {}

    public ConfiguracionFiscalEntity(String clave, String valor, String descripcion, Instant actualizadoEn) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
        this.actualizadoEn = actualizadoEn;
    }

    @PreUpdate
    void onUpdate() {
        // actualizado_en se maneja por trigger en BD
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public String getDescripcion() { return descripcion; }
    public Instant getActualizadoEn() { return actualizadoEn; }

    public void setValor(String valor) { this.valor = valor; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}