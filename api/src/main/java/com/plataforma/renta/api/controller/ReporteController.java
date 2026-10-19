package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.ReporteDtos.*;
import com.plataforma.renta.application.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes y Métricas", description = "Métricas operativas y financieras del dashboard")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Métricas completas de dashboard", description = "Retorna indicadores consolidados, evolución mensual y rankings")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        var resumenData = reporteService.obtenerMetricasResumen();
        var masAlquiladasData = reporteService.obtenerHerramientasMasAlquiladas(5);
        var clientesFrecuentesData = reporteService.obtenerClientesFrecuentes(5);
        var ingresosMensualesData = reporteService.obtenerIngresosMensuales();

        var resumenDto = new ResumenMetricsDto(
                resumenData.totalUsuarios(),
                resumenData.totalClientes(),
                resumenData.totalPropietarios(),
                resumenData.totalHerramientas(),
                resumenData.totalReservas(),
                resumenData.reservasActivas(),
                resumenData.totalIngresos(),
                resumenData.totalFacturas(),
                resumenData.reembolsosPendientes()
        );

        long alquiladas = Math.max(0, resumenData.totalHerramientas() - resumenData.herramientasDisponibles());
        var disponibilidadDto = new DisponibilidadInventarioDto(
                resumenData.herramientasDisponibles(),
                alquiladas,
                0L
        );

        var masAlquiladasDto = masAlquiladasData.stream()
                .map(h -> new HerramientaAlquiladaDto(h.id(), h.nombre(), h.vecesAlquilada(), h.precioPorDia()))
                .toList();

        var clientesDto = clientesFrecuentesData.stream()
                .map(c -> new ClienteFrecuenteDto(c.nombre(), c.reservas(), c.gasto()))
                .toList();

        var ingresosDto = ingresosMensualesData.stream()
                .map(i -> new IngresoMensualDto(i.mes(), i.ingresos()))
                .toList();

        return ResponseEntity.ok(new DashboardMetricsResponse(
                resumenData.totalHerramientas(),
                resumenData.herramientasDisponibles(),
                resumenData.reservasActivas(),
                resumenData.totalIngresos(),
                resumenData.reembolsosPendientes(),
                resumenDto,
                disponibilidadDto,
                masAlquiladasDto,
                clientesDto,
                ingresosDto
        ));
    }

    @GetMapping("/ingresos-mensuales")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Evolución de ingresos mensuales", description = "Agrupación temporal de pagos completados en pesos colombianos")
    public ResponseEntity<List<IngresoMensualDto>> getIngresosMensuales() {
        var list = reporteService.obtenerIngresosMensuales().stream()
                .map(i -> new IngresoMensualDto(i.mes(), i.ingresos()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/herramientas-mas-alquiladas")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Herramientas más alquiladas", description = "Ranking de herramientas con mayor frecuencia de contratación")
    public ResponseEntity<List<HerramientaAlquiladaDto>> getHerramientasMasAlquiladas(
            @RequestParam(defaultValue = "5") int limite) {
        var list = reporteService.obtenerHerramientasMasAlquiladas(limite).stream()
                .map(h -> new HerramientaAlquiladaDto(h.id(), h.nombre(), h.vecesAlquilada(), h.precioPorDia()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/clientes-frecuentes")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Clientes más frecuentes", description = "Ranking de clientes con mayor número de reservas finalizadas o en curso")
    public ResponseEntity<List<ClienteFrecuenteDto>> getClientesFrecuentes(
            @RequestParam(defaultValue = "5") int limite) {
        var list = reporteService.obtenerClientesFrecuentes(limite).stream()
                .map(c -> new ClienteFrecuenteDto(c.nombre(), c.reservas(), c.gasto()))
                .toList();
        return ResponseEntity.ok(list);
    }
}
