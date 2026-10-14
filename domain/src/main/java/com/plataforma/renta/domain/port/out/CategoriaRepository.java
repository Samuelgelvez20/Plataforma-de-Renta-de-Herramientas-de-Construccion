package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Categoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository {

    Optional<Categoria> findById(UUID id);

    List<Categoria> findAll();
}