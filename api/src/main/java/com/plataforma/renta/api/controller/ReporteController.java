package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.ReporteDtos.DashboardMetricsResponse;
import com.plataforma.renta.application.service.HerramientaService;
import com.plataforma.renta.application.service.PagoService;
import com.plataforma.renta.application.service.ReservaService;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.model.UserRol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes y Métricas", description = "Métricas operativas y financieras del dashboard")
public class ReporteController {

    private final HerramientaService herramientaService;
    private final ReservaService reservaService;
    private final PagoService pagoService;

    public ReporteController(HerramientaService herramientaService,
                             ReservaService reservaService,
                             PagoService pagoService) {
        this.herramientaService = herramientaService;
        this.reservaService = reservaService;
        this.pagoService = pagoService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Métricas de dashboard", description = "Retorna indicadores consolidados de equipos, reservas e ingresos")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        var herramientas = herramientaService.listarPublicadas();
        long totalH = herramientas.size();
        long disponiblesH = herramientas.stream().filter(h -> h.getCantidadTotal() > 0).count();

        var reservas = reservaService.listarPorUsuario(UUID.randomUUID(), UserRol.ADMIN);
        long reservasActivas = reservas.stream()
                .filter(r -> r.getEstado() == ReservaEstado.CONFIRMADA || r.getEstado() == ReservaEstado.EN_CURSO)
                .count();

        var pagos = pagoService.listarTodos();
        BigDecimal ingresos = pagos.stream()
                .filter(p -> p.getEstado() == PagoEstado.COMPLETADO)
                .map(p -> p.getMonto().getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long reembolsosPend = pagos.stream()
                .filter(p -> p.getPagoOriginalId() != null && p.getEstado() == PagoEstado.PENDIENTE)
                .count();

        return ResponseEntity.ok(new DashboardMetricsResponse(
                totalH, disponiblesH, reservasActivas, ingresos, reembolsosPend
        ));
    }
}
