package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCategoriaRepository extends JpaRepository<CategoriaEntity, UUID> {

    Optional<CategoriaEntity> findByNombre(String nombre);
}