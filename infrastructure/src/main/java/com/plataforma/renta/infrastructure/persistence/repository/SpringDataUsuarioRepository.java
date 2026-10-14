package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UsuarioEntity u WHERE u.id = :id")
    Optional<UsuarioEntity> findByIdForUpdate(@Param("id") UUID id);
}
