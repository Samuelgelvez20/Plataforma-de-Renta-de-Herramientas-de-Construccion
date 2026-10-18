package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.AuthDtos.UserResponse;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.domain.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administración", description = "Operaciones restringidas para administradores de la plataforma")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios registrados en el sistema")
    public ResponseEntity<List<UserResponse>> listarUsuarios() {
        var users = authService.listarTodos().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/usuarios/{id}/toggle-activo")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Cambiar estado de usuario", description = "Habilita o deshabilita la cuenta de un usuario")
    public ResponseEntity<UserResponse> toggleActivo(@PathVariable UUID id) {
        Usuario u = authService.toggleActivo(id);
        return ResponseEntity.ok(toResponse(u));
    }

    private UserResponse toResponse(Usuario u) {
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
