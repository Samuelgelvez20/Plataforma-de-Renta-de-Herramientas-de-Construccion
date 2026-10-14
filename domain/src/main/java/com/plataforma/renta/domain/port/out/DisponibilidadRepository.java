package com.plataforma.renta.domain.port.out;

import java.time.LocalDate;
import java.util.UUID;

public interface DisponibilidadRepository {

    int calcularDisponibilidad(UUID herramientaId, LocalDate fechaInicio, LocalDate fechaFin, UUID excluirReservaId);
}