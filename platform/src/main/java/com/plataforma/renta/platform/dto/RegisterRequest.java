package com.plataforma.renta.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String nombre,
        @NotBlank String apellido,
        String telefono,
        @NotNull @Pattern(regexp = "CLIENTE|PROPIETARIO", message = "Rol debe ser CLIENTE o PROPIETARIO") String rol,
        // Campos para PROPIETARIO
        String razonSocial,
        String nit,
        String direccionFiscal,
        String telefonoContacto,
        String regimen,
        String municipio,
        String departamento
) {}