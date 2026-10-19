package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.port.out.ReporteRepository;
import com.plataforma.renta.domain.port.out.ReporteRepository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteService {

    private final ReporteRepository reporteRepository;

    public ReporteService(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    public List<IngresoMensualData> obtenerIngresosMensuales() {
        return reporteRepository.findIngresosMensuales();
    }

    public List<HerramientaAlquiladaData> obtenerHerramientasMasAlquiladas(int limite) {
        return reporteRepository.findHerramientasMasAlquiladas(limite > 0 ? limite : 5);
    }

    public List<ClienteFrecuenteData> obtenerClientesFrecuentes(int limite) {
        return reporteRepository.findClientesFrecuentes(limite > 0 ? limite : 5);
    }

    public MetricasResumenData obtenerMetricasResumen() {
        return reporteRepository.obtenerMetricasResumen();
    }
}
