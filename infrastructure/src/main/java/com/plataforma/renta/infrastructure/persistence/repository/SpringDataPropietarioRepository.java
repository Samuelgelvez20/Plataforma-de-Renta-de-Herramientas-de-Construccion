package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.PropietarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPropietarioRepository extends JpaRepository<PropietarioEntity, UUID> {

    Optional<PropietarioEntity> findByNit(String nit);
}