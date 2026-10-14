package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.CategoriaRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.value.Dinero;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class HerramientaService {

    private final HerramientaRepository herramientaRepository;
    private final CategoriaRepository categoriaRepository;
    private final PropietarioRepository propietarioRepository;

    public HerramientaService(HerramientaRepository herramientaRepository,
                              CategoriaRepository categoriaRepository,
                              PropietarioRepository propietarioRepository) {
        this.herramientaRepository = herramientaRepository;
        this.categoriaRepository = categoriaRepository;
        this.propietarioRepository = propietarioRepository;
    }

    @Transactional
    public Herramienta crearHerramienta(UUID propietarioId, UUID categoriaId,
                                        String nombre, String descripcion,
                                        String marca, String modelo,
                                        Dinero precioDia, int cantidadTotal,
                                        BigDecimal ivaPorcentaje, boolean ivaAplica) {
        
        var propietario = propietarioRepository.findById(propietarioId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Propietario no encontrado"));
        
        var categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Categoría no encontrada"));

        Herramienta herramienta = new Herramienta(
                UUID.randomUUID(), categoriaId, propietarioId,
                nombre, descripcion, marca, modelo,
                precioDia, cantidadTotal, HerramientaPubEstado.PUBLICADA,
                ivaPorcentaje, ivaAplica,
                java.time.Instant.now(), java.time.Instant.now()
        );

        return herramientaRepository.save(herramienta);
    }

    public List<Herramienta> listarPorPropietario(UUID propietarioId) {
        return herramientaRepository.findByPropietarioId(propietarioId);
    }

    public List<Herramienta> listarPorCategoria(UUID categoriaId) {
        return herramientaRepository.findByCategoriaId(categoriaId);
    }

    public List<Herramienta> listarPublicadas() {
        return herramientaRepository.findByEstadoPublicacion(com.plataforma.renta.domain.model.HerramientaPubEstado.PUBLICADA);
    }

    @Transactional
    public Herramienta actualizarEstado(UUID id, HerramientaPubEstado estado) {
        Herramienta h = herramientaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Herramienta no encontrada"));
        h.setEstadoPublicacion(estado);
        return herramientaRepository.save(h);
    }

    @Transactional
    public Herramienta actualizarPrecio(UUID id, Dinero precioDia) {
        Herramienta h = herramientaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Herramienta no encontrada"));
        h.setPrecioDia(precioDia);
        return herramientaRepository.save(h);
    }
}