package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.FacturaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.FacturaTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataFacturaRepository extends JpaRepository<FacturaEntity, UUID> {

    List<FacturaEntity> findByReservaId(UUID reservaId);

    @Query("SELECT f FROM FacturaEntity f WHERE f.reserva.id = :reservaId AND f.tipo = :tipo")
    List<FacturaEntity> findByReservaIdAndTipo(UUID reservaId, FacturaTipo tipo);

    @Query("SELECT f FROM FacturaEntity f WHERE f.reserva.id = :reservaId AND f.tipo = 'FACTURA'")
    Optional<FacturaEntity> findOriginalByReservaId(UUID reservaId);

    @Query("SELECT f FROM FacturaEntity f WHERE f.facturaOriginal.id = :facturaOriginalId")
    List<FacturaEntity> findNotasCreditoByFacturaOriginalId(UUID facturaOriginalId);

    @Query("SELECT f FROM FacturaEntity f WHERE f.pagoReembolso.id = :pagoReembolsoId")
    Optional<FacturaEntity> findByPagoReembolsoId(UUID pagoReembolsoId);

    boolean existsByNumeroFactura(String numeroFactura);
}