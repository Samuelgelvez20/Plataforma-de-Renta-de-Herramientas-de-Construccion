package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HerramientaRepository {

    Herramienta save(Herramienta herramienta);

    Optional<Herramienta> findById(UUID id);

    Optional<Herramienta> findByIdForUpdate(UUID id);

    List<Herramienta> findByIdsForUpdate(List<UUID> ids);

    List<Herramienta> findByPropietarioId(UUID propietarioId);

    List<Herramienta> findByCategoriaId(UUID categoriaId);

    List<Herramienta> findByEstadoPublicacion(HerramientaPubEstado estado);
}