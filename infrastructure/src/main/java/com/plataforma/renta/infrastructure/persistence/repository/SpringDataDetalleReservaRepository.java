package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.DetalleReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDetalleReservaRepository extends JpaRepository<DetalleReservaEntity, UUID> {

    List<DetalleReservaEntity> findByReservaId(UUID reservaId);

    List<UUID> findHerramientaIdsByReservaId(UUID reservaId);

    @Query("SELECT dr FROM DetalleReservaEntity dr WHERE dr.reserva.id = :reservaId AND dr.herramienta.id = :herramientaId")
    Optional<DetalleReservaEntity> findByReservaIdAndHerramientaId(UUID reservaId, UUID herramientaId);

    boolean existsByReservaId(UUID reservaId);
}