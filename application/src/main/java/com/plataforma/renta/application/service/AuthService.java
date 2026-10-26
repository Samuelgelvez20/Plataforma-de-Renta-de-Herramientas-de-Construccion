package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResult login(String email, String password);

    AuthResult register(RegisterUserCommand cmd);

    Usuario obtenerUsuarioPorEmail(String email);

    Usuario obtenerUsuarioPorId(UUID id);

    List<Usuario> listarTodos();

    Usuario toggleActivo(UUID id, UUID actorId);

    Usuario actualizarUsuario(UUID id, String nombre, String apellido, String telefono, String direccion, String razonSocial);

    void eliminarUsuario(UUID id, UUID actorId);

    Propietario obtenerPropietarioPorUsuarioId(UUID usuarioId);

    Cliente obtenerClientePorUsuarioId(UUID usuarioId);

    record AuthResult(String token, Usuario usuario) {}

    record RegisterUserCommand(
            String email,
            String password,
            String nombre,
            String apellido,
            UserRol rol,
            String telefono,
            String razonSocial,
            String direccionFiscal,
            String direccionEntregaDefault
    ) {}
}
