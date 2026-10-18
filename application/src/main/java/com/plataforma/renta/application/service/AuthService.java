package com.plataforma.renta.application.service;

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

    Usuario toggleActivo(UUID id);

    record AuthResult(String token, Usuario usuario) {}

    record RegisterUserCommand(
            String email,
            String password,
            String nombre,
            String apellido,
            UserRol rol,
            String telefono,
            String razonSocial,
            String nit,
            String direccionFiscal,
            String direccionEntregaDefault
    ) {}
}
