package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(UUID id);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdForUpdate(UUID id);

    List<Usuario> findByRol(UserRol rol);

    List<Usuario> findAll();

    void deleteById(UUID id);
}
