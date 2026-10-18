package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.CategoriaRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.DisponibilidadRepository;
import java.time.LocalDate;
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
    private final DisponibilidadRepository disponibilidadRepository;

    public HerramientaService(HerramientaRepository herramientaRepository,
                              CategoriaRepository categoriaRepository,
                              PropietarioRepository propietarioRepository,
                              DisponibilidadRepository disponibilidadRepository) {
        this.herramientaRepository = herramientaRepository;
        this.categoriaRepository = categoriaRepository;
        this.propietarioRepository = propietarioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
    }

    @Transactional
    public Herramienta crearHerramienta(UUID actorId, UserRol actorRol,
                                        UUID propietarioId, UUID categoriaId,
                                        String nombre, String descripcion,
                                        String marca, String modelo,
                                        Dinero precioDia, int cantidadTotal,
                                        BigDecimal ivaPorcentaje, boolean ivaAplica) {
        if (actorRol != UserRol.ADMIN && (actorRol != UserRol.PROPIETARIO || !actorId.equals(propietarioId))) {
            throw new OperacionInvalidaException("No autorizado: solo el propietario titular o un administrador pueden crear herramientas");
        }
        
        var propietario = propietarioRepository.findById(propietarioId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Propietario no encontrado: " + propietarioId));
        
        var categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Categoría no encontrada: " + categoriaId));

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
    public Herramienta actualizarEstado(UUID id, HerramientaPubEstado estado, UUID actorId, UserRol actorRol) {
        Herramienta h = herramientaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Herramienta no encontrada: " + id));
        if (actorRol != UserRol.ADMIN && (actorRol != UserRol.PROPIETARIO || !actorId.equals(h.getPropietarioId()))) {
            throw new OperacionInvalidaException("No autorizado: solo el propietario titular o un administrador pueden modificar el estado");
        }
        h.setEstadoPublicacion(estado);
        return herramientaRepository.save(h);
    }

    @Transactional
    public Herramienta actualizarPrecio(UUID id, Dinero precioDia, UUID actorId, UserRol actorRol) {
        Herramienta h = herramientaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Herramienta no encontrada: " + id));
        if (actorRol != UserRol.ADMIN && (actorRol != UserRol.PROPIETARIO || !actorId.equals(h.getPropietarioId()))) {
            throw new OperacionInvalidaException("No autorizado: solo el propietario titular o un administrador pueden modificar el precio");
        }
        h.setPrecioDia(precioDia);
        return herramientaRepository.save(h);
    }

    public Herramienta obtenerPorId(UUID id) {
        return herramientaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Herramienta no encontrada: " + id));
    }


    public List<com.plataforma.renta.domain.model.Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }


    public int calcularDisponibilidad(UUID herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new OperacionInvalidaException("Las fechas de inicio y fin son obligatorias");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new OperacionInvalidaException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new OperacionInvalidaException("La fecha de inicio no puede ser anterior a la fecha actual");
        }
        Herramienta h = obtenerPorId(herramientaId);
        if (h.getEstadoPublicacion() != HerramientaPubEstado.PUBLICADA) {
            return 0;
        }
        return disponibilidadRepository.calcularDisponibilidad(herramientaId, fechaInicio, fechaFin, null);
    }

}
