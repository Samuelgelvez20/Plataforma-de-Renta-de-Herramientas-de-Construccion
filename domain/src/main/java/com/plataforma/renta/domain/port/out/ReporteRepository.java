package com.plataforma.renta.domain.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface ReporteRepository {

    record IngresoMensualData(String mes, BigDecimal ingresos) {}

    record HerramientaAlquiladaData(String id, String nombre, long vecesAlquilada, BigDecimal precioPorDia) {}

    record ClienteFrecuenteData(String nombre, long reservas, BigDecimal gasto) {}

    record MetricasResumenData(
            long totalUsuarios,
            long totalClientes,
            long totalPropietarios,
            long totalHerramientas,
            long herramientasDisponibles,
            long totalReservas,
            long reservasActivas,
            BigDecimal totalIngresos,
            long totalFacturas,
            long reembolsosPendientes
    ) {}

    List<IngresoMensualData> findIngresosMensuales();

    List<HerramientaAlquiladaData> findHerramientasMasAlquiladas(int limite);

    List<ClienteFrecuenteData> findClientesFrecuentes(int limite);

    MetricasResumenData obtenerMetricasResumen();
}
