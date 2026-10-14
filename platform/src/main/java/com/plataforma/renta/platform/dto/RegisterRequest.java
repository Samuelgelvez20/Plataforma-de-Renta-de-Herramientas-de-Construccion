package com.plataforma.renta.platform.dto;

public record RegisterRequest(
        String email,
        String password,
        String nombre,
        String apellido,
        String telefono,
        String rol, // ADMIN, PROPIETARIO, CLIENTE
        // Campos para PROPIETARIO
        String razonSocial,
        String nit,
        String direccionFiscal,
        String telefonoContacto,
        String regimen,
        String municipio,
        String departamento
) {}