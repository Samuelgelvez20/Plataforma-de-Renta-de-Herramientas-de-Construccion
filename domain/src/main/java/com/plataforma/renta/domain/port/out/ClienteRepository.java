package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Cliente;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    Optional<Cliente> findByUsuarioId(UUID usuarioId);
}