package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.HerramientaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.HerramientaPubEstado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataHerramientaRepository extends JpaRepository<HerramientaEntity, UUID> {

    List<HerramientaEntity> findByPropietarioUsuarioId(UUID propietarioId);

    List<HerramientaEntity> findByCategoriaId(UUID categoriaId);

    List<HerramientaEntity> findByEstadoPublicacion(HerramientaPubEstado estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HerramientaEntity h WHERE h.id IN :ids")
    List<HerramientaEntity> findByIdsForUpdate(@Param("ids") List<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HerramientaEntity h WHERE h.id = :id")
    Optional<HerramientaEntity> findByIdForUpdate(@Param("id") UUID id);
}
