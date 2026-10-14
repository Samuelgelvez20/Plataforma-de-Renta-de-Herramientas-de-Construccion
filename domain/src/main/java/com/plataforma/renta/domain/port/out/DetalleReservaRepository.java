package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.DetalleReserva;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetalleReservaRepository {

    List<DetalleReserva> findByReservaId(UUID reservaId);

    List<UUID> findHerramientaIdsByReservaId(UUID reservaId);

    Optional<DetalleReserva> findByReservaIdAndHerramientaId(UUID reservaId, UUID herramientaId);

    boolean existsByReservaId(UUID reservaId);
}