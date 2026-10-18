package com.plataforma.renta.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") @Email String email,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String nombre,
            @NotBlank String apellido,
            String rol,
            String telefono,
            String razonSocial,
            String nit,
            String direccionFiscal,
            String direccionEntregaDefault
    ) {}

    public record UserResponse(
            UUID id,
            String email,
            String nombre,
            String apellido,
            String rol,
            String telefono
    ) {}

    public record AuthResponse(
            String token,
            UserResponse usuario
    ) {}
}
