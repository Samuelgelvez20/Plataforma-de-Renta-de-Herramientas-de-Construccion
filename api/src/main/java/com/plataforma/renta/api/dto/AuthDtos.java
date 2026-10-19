package com.plataforma.renta.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
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
            String telefono,
            boolean activo,
            String nit,
            String razonSocial,
            String direccion
    ) {
        public UserResponse(UUID id, String email, String nombre, String apellido, String rol, String telefono, boolean activo) {
            this(id, email, nombre, apellido, rol, telefono, activo, null, null, null);
        }

        public UserResponse(UUID id, String email, String nombre, String apellido, String rol, String telefono) {
            this(id, email, nombre, apellido, rol, telefono, true, null, null, null);
        }
    }

    public record AdminUserDetailResponse(
            UUID id,
            String email,
            String nombre,
            String apellido,
            String rol,
            String telefono,
            boolean activo,
            String nit,
            String razonSocial,
            String direccionFiscal,
            String direccionEntrega,
            Instant creadoEn
    ) {}

    public record AdminUpdateUserRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El apellido es obligatorio") String apellido,
            String telefono,
            String direccion,
            String razonSocial,
            String nit
    ) {}

    public record AdminCreateUserRequest(
            @NotBlank(message = "El correo es obligatorio") @Email String email,
            @NotBlank(message = "La contraseña es obligatoria") String password,
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El apellido es obligatorio") String apellido,
            @NotBlank(message = "El rol es obligatorio") String rol,
            String telefono,
            String razonSocial,
            String nit,
            String direccionFiscal,
            String direccionEntregaDefault
    ) {}

    public record AuthResponse(
            String token,
            UserResponse usuario
    ) {}
}
