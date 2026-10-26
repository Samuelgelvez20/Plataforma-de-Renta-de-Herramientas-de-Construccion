package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.AuthDtos.*;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.AuthService.RegisterUserCommand;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Endpoints de inicio de sesión, registro y perfil de usuario")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements()
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y genera un JWT válido")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        var res = authService.login(req.email(), req.password());
        var userResp = toUserResponse(res.usuario());
        return ResponseEntity.ok(new AuthResponse(res.token(), userResp));
    }

    @PostMapping("/register")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements()
    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta con perfil asociado y genera un JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserRol rol;
        if (req.rol() == null || req.rol().isBlank()) {
            rol = UserRol.CLIENTE;
        } else {
            String rolStr = req.rol().trim().toUpperCase();
            if (rolStr.equals("CLIENTE") || rolStr.equals("ROLE_CLIENTE")) {
                rol = UserRol.CLIENTE;
            } else if (rolStr.equals("PROPIETARIO") || rolStr.equals("ROLE_PROPIETARIO")) {
                rol = UserRol.PROPIETARIO;
            } else if (rolStr.equals("ADMIN") || rolStr.equals("ROLE_ADMIN") || rolStr.contains("ADMIN")) {
                throw new com.plataforma.renta.domain.exception.OperacionInvalidaException("No está permitido el autorregistro con rol de administrador");
            } else {
                throw new com.plataforma.renta.domain.exception.OperacionInvalidaException("Rol no válido para registro público: " + req.rol() + ". Solo se permiten CLIENTE o PROPIETARIO");
            }
        }
        var cmd = new RegisterUserCommand(
                req.email(), req.password(), req.nombre(), req.apellido(), rol,
                req.telefono(), req.razonSocial(), req.direccionFiscal(),
                req.direccionEntregaDefault()
        );
        var res = authService.register(cmd);
        var userResp = toUserResponse(res.usuario());
        return ResponseEntity.ok(new AuthResponse(res.token(), userResp));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario actual", description = "Retorna los datos del usuario autenticado")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = authService.obtenerUsuarioPorEmail(auth.getName());
        return ResponseEntity.ok(toUserResponse(usuario));
    }

    private UserResponse toUserResponse(Usuario u) {
        return new UserResponse(
                u.getId(),
                u.getEmail().getValor(),
                u.getNombre(),
                u.getApellido(),
                u.getRol().name(),
                u.getTelefono()
        );
    }
}
