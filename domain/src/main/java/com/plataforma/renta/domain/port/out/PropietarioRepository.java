package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Propietario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropietarioRepository {

    Propietario save(Propietario propietario);

    Optional<Propietario> findById(UUID id);

    List<UUID> findHerramientaIdsByPropietarioId(UUID propietarioId);
}