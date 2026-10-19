package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.AuthDtos.*;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios registrados con perfiles consolidados")
    public ResponseEntity<List<UserResponse>> listarUsuarios() {
        var users = authService.listarTodos().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Consultar detalle de usuario", description = "Retorna la información completa de cuenta y perfil asociado")
    public ResponseEntity<AdminUserDetailResponse> obtenerDetalleUsuario(@PathVariable UUID id) {
        Usuario u = authService.obtenerUsuarioPorId(id);
        Propietario p = u.getRol() == UserRol.PROPIETARIO ? authService.obtenerPropietarioPorUsuarioId(id) : null;
        Cliente c = u.getRol() == UserRol.CLIENTE ? authService.obtenerClientePorUsuarioId(id) : null;

        String nit = p != null ? p.getNit().getValor() : null;
        String razonSocial = p != null ? p.getRazonSocial() : null;
        String dirFiscal = p != null ? p.getDireccionFiscal() : null;
        String dirEntrega = c != null ? c.getDireccionEntregaDefault() : null;

        return ResponseEntity.ok(new AdminUserDetailResponse(
                u.getId(),
                u.getEmail().getValor(),
                u.getNombre(),
                u.getApellido(),
                u.getRol().name(),
                u.getTelefono(),
                u.isActivo(),
                nit,
                razonSocial,
                dirFiscal,
                dirEntrega,
                u.getCreadoEn()
        ));
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Crear usuario administrativo", description = "Registra un cliente o proveedor directamente desde el panel")
    public ResponseEntity<UserResponse> crearUsuario(@Valid @RequestBody AdminCreateUserRequest req) {
        UserRol rol = UserRol.valueOf(req.rol().toUpperCase());
        var cmd = new AuthService.RegisterUserCommand(
                req.email(),
                req.password(),
                req.nombre(),
                req.apellido(),
                rol,
                req.telefono(),
                req.razonSocial(),
                req.nit(),
                req.direccionFiscal(),
                req.direccionEntregaDefault()
        );
        var res = authService.register(cmd);
        return ResponseEntity.ok(toResponse(res.usuario()));
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Actualizar datos de usuario", description = "Modifica información de contacto, identificación o perfil")
    public ResponseEntity<UserResponse> actualizarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest req) {
        Usuario u = authService.actualizarUsuario(
                id,
                req.nombre(),
                req.apellido(),
                req.telefono(),
                req.direccion(),
                req.razonSocial(),
                req.nit()
        );
        return ResponseEntity.ok(toResponse(u));
    }

    @PutMapping("/usuarios/{id}/toggle-activo")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Cambiar estado de usuario", description = "Habilita o suspende la cuenta de un usuario")
    public ResponseEntity<UserResponse> toggleActivo(@PathVariable UUID id, Authentication auth) {
        UUID actorId = null;
        if (auth != null) {
            Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
            actorId = actor.getId();
        }
        Usuario u = authService.toggleActivo(id, actorId);
        return ResponseEntity.ok(toResponse(u));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Eliminar usuario de forma segura", description = "Elimina la cuenta si no posee registros históricos ni financieros")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable UUID id, Authentication auth) {
        UUID actorId = null;
        if (auth != null) {
            Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
            actorId = actor.getId();
        }
        authService.eliminarUsuario(id, actorId);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(Usuario u) {
        Propietario p = u.getRol() == UserRol.PROPIETARIO ? authService.obtenerPropietarioPorUsuarioId(u.getId()) : null;
        Cliente c = u.getRol() == UserRol.CLIENTE ? authService.obtenerClientePorUsuarioId(u.getId()) : null;

        String nit = p != null ? p.getNit().getValor() : null;
        String razonSocial = p != null ? p.getRazonSocial() : null;
        String dir = p != null ? p.getDireccionFiscal() : (c != null ? c.getDireccionEntregaDefault() : null);

        return new UserResponse(
                u.getId(),
                u.getEmail().getValor(),
                u.getNombre(),
                u.getApellido(),
                u.getRol().name(),
                u.getTelefono(),
                u.isActivo(),
                nit,
                razonSocial,
                dir
        );
    }
}
