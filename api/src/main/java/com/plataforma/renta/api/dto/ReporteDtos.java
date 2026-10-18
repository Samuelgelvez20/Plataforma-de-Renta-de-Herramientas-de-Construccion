package com.plataforma.renta.api.dto;

import java.math.BigDecimal;

public class ReporteDtos {

    public record DashboardMetricsResponse(
            long totalHerramientas,
            long herramientasDisponibles,
            long reservasActivas,
            BigDecimal ingresosTotales,
            long reembolsosPendientes
    ) {}
}
