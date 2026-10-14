package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.ReservaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataReservaRepository extends JpaRepository<ReservaEntity, UUID> {

    List<ReservaEntity> findByClienteUsuarioId(UUID clienteId);

    List<ReservaEntity> findByEstado(ReservaEstado estado);

    @Query("SELECT r FROM ReservaEntity r WHERE r.estado = :estado AND r.fechaExpiracionPendiente IS NOT NULL AND r.fechaExpiracionPendiente < :now")
    List<ReservaEntity> findPendientesVencidas(ReservaEstado estado, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaEntity r WHERE r.id = :id")
    Optional<ReservaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaEntity r WHERE r.id IN :ids")
    List<ReservaEntity> findByIdsForUpdate(@Param("ids") List<UUID> ids);

    boolean existsById(UUID id);
}
