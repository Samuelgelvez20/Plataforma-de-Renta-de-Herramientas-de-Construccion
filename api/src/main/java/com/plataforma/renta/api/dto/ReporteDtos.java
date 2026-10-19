package com.plataforma.renta.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReporteDtos {

    public record ResumenMetricsDto(
            long totalUsuarios,
            long totalClientes,
            long totalPropietarios,
            long totalHerramientas,
            long totalReservas,
            long reservasActivas,
            BigDecimal totalIngresos,
            long totalFacturas,
            long reembolsosPendientes
    ) {}

    public record DisponibilidadInventarioDto(
            long disponibles,
            long alquiladas,
            long enMantenimiento
    ) {}

    public record HerramientaAlquiladaDto(
            String id,
            String nombre,
            long vecesAlquilada,
            BigDecimal precioPorDia
    ) {}

    public record ClienteFrecuenteDto(
            String nombre,
            long reservas,
            BigDecimal gasto
    ) {}

    public record IngresoMensualDto(
            String mes,
            BigDecimal ingresos
    ) {}

    public record DashboardMetricsResponse(
            long totalHerramientas,
            long herramientasDisponibles,
            long reservasActivas,
            BigDecimal ingresosTotales,
            long reembolsosPendientes,
            ResumenMetricsDto resumen,
            DisponibilidadInventarioDto disponibilidadInventario,
            List<HerramientaAlquiladaDto> masAlquiladas,
            List<ClienteFrecuenteDto> clientesFrecuentes,
            List<IngresoMensualDto> ingresosMensuales
    ) {}
}
