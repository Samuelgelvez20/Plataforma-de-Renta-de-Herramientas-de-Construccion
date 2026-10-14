package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository {

    Reserva save(Reserva reserva);

    Optional<Reserva> findById(UUID id);

    Optional<Reserva> findByIdForUpdate(UUID id);

    List<Reserva> findByClienteId(UUID clienteId);

    List<Reserva> findByEstado(ReservaEstado estado);

    List<Reserva> findPendientesVencidas(Instant ahora);

    boolean existsById(UUID id);
}