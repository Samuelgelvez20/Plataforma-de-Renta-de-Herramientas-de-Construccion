package com.plataforma.renta.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ReservaDtos {

    public record CrearReservaRequest(
            @NotNull UUID herramientaId,
            int cantidad,
            @NotNull LocalDate fechaInicio,
            @NotNull LocalDate fechaFin,
            String notasCliente
    ) {}

    public record DetalleReservaResponse(
            UUID id,
            UUID herramientaId,
            int cantidad,
            BigDecimal precioDia,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total
    ) {}

    public record ReservaResponse(
            UUID id,
            UUID clienteId,
            String estado,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal total,
            BigDecimal subtotal,
            BigDecimal iva,
            boolean pagada,
            List<DetalleReservaResponse> detalles,
            boolean solicitudDevolucion,
            String estadoDevolucion,
            String notaDevolucionCliente,
            BigDecimal costoReparacion,
            String descripcionDano,
            String observacionPropietario
    ) {}

    public record RechazarReservaRequest(
            String motivo
    ) {}

    public record SolicitarDevolucionRequest(
            String notaCliente
    ) {}

    public record DevolverReservaRequest(
            Boolean aceptada,
            boolean presentaDanos,
            String descripcionDano,
            BigDecimal costoReparacion,
            String observaciones,
            String notaCliente
    ) {}

    public record CancelarReservaRequest(
            String motivo
    ) {}
}
