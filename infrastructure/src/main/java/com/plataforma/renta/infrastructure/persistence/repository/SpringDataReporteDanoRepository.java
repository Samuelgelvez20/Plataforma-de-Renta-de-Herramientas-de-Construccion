package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.ReporteDanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataReporteDanoRepository extends JpaRepository<ReporteDanoEntity, UUID> {

    List<ReporteDanoEntity> findByDetalleReservaId(UUID detalleReservaId);

    List<ReporteDanoEntity> findByHerramientaId(UUID herramientaId);
}