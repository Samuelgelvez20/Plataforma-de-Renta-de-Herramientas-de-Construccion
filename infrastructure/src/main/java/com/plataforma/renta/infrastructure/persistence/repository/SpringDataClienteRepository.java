package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataClienteRepository extends JpaRepository<ClienteEntity, UUID> {

    ClienteEntity getReferenceById(UUID id);

    @Query("SELECT c FROM ClienteEntity c WHERE c.usuarioId = :id")
    ClienteEntity findByUsuarioId(UUID id);
}