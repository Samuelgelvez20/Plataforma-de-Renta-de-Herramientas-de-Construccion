package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.port.out.ReporteRepository;
import com.plataforma.renta.domain.port.out.ReporteRepository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        reporteService = new ReporteService(reporteRepository);
    }

    @Test
    @DisplayName("Debe retornar evolución de ingresos mensuales agregados")
    void testObtenerIngresosMensuales() {
        when(reporteRepository.findIngresosMensuales()).thenReturn(List.of(
                new IngresoMensualData("2026-08", new BigDecimal("1500000.00")),
                new IngresoMensualData("2026-09", new BigDecimal("3200000.00"))
        ));

        var result = reporteService.obtenerIngresosMensuales();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).mes()).isEqualTo("2026-08");
        assertThat(result.get(0).ingresos()).isEqualByComparingTo("1500000.00");
        verify(reporteRepository).findIngresosMensuales();
    }

    @Test
    @DisplayName("Debe retornar ranking de herramientas más alquiladas con límite configurable")
    void testObtenerHerramientasMasAlquiladas() {
        when(reporteRepository.findHerramientasMasAlquiladas(anyInt())).thenReturn(List.of(
                new HerramientaAlquiladaData("tool-1", "Excavadora 20T", 15L, new BigDecimal("750000.00")),
                new HerramientaAlquiladaData("tool-2", "Rotomartillo", 12L, new BigDecimal("65000.00"))
        ));

        var result = reporteService.obtenerHerramientasMasAlquiladas(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nombre()).isEqualTo("Excavadora 20T");
        assertThat(result.get(0).vecesAlquilada()).isEqualTo(15L);
        verify(reporteRepository).findHerramientasMasAlquiladas(5);
    }

    @Test
    @DisplayName("Debe retornar clientes frecuentes con reservas y gasto acumulado")
    void testObtenerClientesFrecuentes() {
        when(reporteRepository.findClientesFrecuentes(anyInt())).thenReturn(List.of(
                new ClienteFrecuenteData("Mateo Gómez", 4L, new BigDecimal("2400000.00"))
        ));

        var result = reporteService.obtenerClientesFrecuentes(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nombre()).isEqualTo("Mateo Gómez");
        assertThat(result.get(0).reservas()).isEqualTo(4L);
        assertThat(result.get(0).gasto()).isEqualByComparingTo("2400000.00");
    }

    @Test
    @DisplayName("Debe manejar correctamente una base de datos vacía sin errores ni NPE")
    void testManejoBaseDeDatosVacia() {
        when(reporteRepository.findIngresosMensuales()).thenReturn(Collections.emptyList());
        when(reporteRepository.findHerramientasMasAlquiladas(anyInt())).thenReturn(Collections.emptyList());
        when(reporteRepository.findClientesFrecuentes(anyInt())).thenReturn(Collections.emptyList());
        when(reporteRepository.obtenerMetricasResumen()).thenReturn(new MetricasResumenData(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L
        ));

        var ingresos = reporteService.obtenerIngresosMensuales();
        var masAlquiladas = reporteService.obtenerHerramientasMasAlquiladas(5);
        var frecuentes = reporteService.obtenerClientesFrecuentes(5);
        var resumen = reporteService.obtenerMetricasResumen();

        assertThat(ingresos).isNotNull().isEmpty();
        assertThat(masAlquiladas).isNotNull().isEmpty();
        assertThat(frecuentes).isNotNull().isEmpty();
        assertThat(resumen.totalUsuarios()).isZero();
        assertThat(resumen.totalIngresos()).isEqualTo(BigDecimal.ZERO);
    }
}
