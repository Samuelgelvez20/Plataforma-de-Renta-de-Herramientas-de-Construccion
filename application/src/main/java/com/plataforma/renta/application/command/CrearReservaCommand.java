package com.plataforma.renta.application.command;

import com.plataforma.renta.domain.value.Dinero;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CrearReservaCommand(
        UUID clienteId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<DetalleCommand> detalles
) {
    public record DetalleCommand(
            UUID herramientaId,
            int cantidad
    ) {}
}