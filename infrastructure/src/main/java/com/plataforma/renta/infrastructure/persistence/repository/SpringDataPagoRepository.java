package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.PagoEntity;
import com.plataforma.renta.infrastructure.persistence.entity.PagoEstado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPagoRepository extends JpaRepository<PagoEntity, UUID> {

    List<PagoEntity> findByReservaId(UUID reservaId);

    @Query("SELECT p FROM PagoEntity p WHERE p.reserva.id = :reservaId AND p.estado = :estado")
    Optional<PagoEntity> findByReservaIdAndEstado(UUID reservaId, PagoEstado estado);

    @Query("SELECT p FROM PagoEntity p WHERE p.pagoOriginal.id = :pagoOriginalId")
    List<PagoEntity> findByPagoOriginalId(UUID pagoOriginalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoEntity p WHERE p.reserva.id = :reservaId AND p.estado = :estado")
    Optional<PagoEntity> findCompletadoByReservaIdForUpdate(UUID reservaId, PagoEstado estado);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoEntity p WHERE p.pagoOriginal.id = :pagoOriginalId AND p.estado = :estado")
    BigDecimal sumMontoByPagoOriginalIdAndEstado(@Param("pagoOriginalId") UUID pagoOriginalId, @Param("estado") PagoEstado estado);

    default BigDecimal sumMontoByPagoOriginalId(UUID pagoOriginalId) {
        return sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO);
    }

    boolean existsByReservaIdAndEstado(UUID reservaId, PagoEstado estado);

    default boolean existsCompletadoByReservaId(UUID reservaId) {
        return existsByReservaIdAndEstado(reservaId, PagoEstado.COMPLETADO);
    }

    boolean existsByPagoOriginalIdAndEstado(UUID pagoOriginalId, PagoEstado estado);

    boolean existsByReservaId(UUID reservaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoEntity p WHERE p.id = :id")
    Optional<PagoEntity> findByIdForUpdate(@Param("id") UUID id);
}
