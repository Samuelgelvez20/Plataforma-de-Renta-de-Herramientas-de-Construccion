package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagoRepository {

    Pago save(Pago pago);

    Optional<Pago> findById(UUID id);

    Optional<Pago> findByIdForUpdate(UUID id);

    List<Pago> findByReservaId(UUID reservaId);

    Optional<Pago> findCompletadoByReservaId(UUID reservaId);

    Optional<Pago> findCompletadoByReservaIdForUpdate(UUID reservaId);

    List<Pago> findByPagoOriginalId(UUID pagoOriginalId);

    BigDecimal sumMontoByPagoOriginalIdAndEstado(UUID pagoOriginalId, PagoEstado estado);

    default BigDecimal sumMontoByPagoOriginalId(UUID pagoOriginalId) {
        return sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO);
    }

    boolean existsCompletadoByReservaId(UUID reservaId);

    boolean existsByPagoOriginalIdAndEstado(UUID pagoOriginalId, PagoEstado estado);

    boolean existsByReservaId(UUID reservaId);

    List<Pago> findAll();
}