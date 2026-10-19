package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.HerramientaDtos.*;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.HerramientaService;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.value.Dinero;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/herramientas")
@Tag(name = "Herramientas", description = "Gestión de herramientas, catálogo público y consulta de disponibilidad")
public class HerramientaController {

    private final HerramientaService herramientaService;
    private final AuthService authService;

    public HerramientaController(HerramientaService herramientaService, AuthService authService) {
        this.herramientaService = herramientaService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Listar herramientas", description = "Consulta el catálogo con filtros opcionales de categoría, búsqueda, precio y propietario")
    public ResponseEntity<List<HerramientaResponse>> listar(
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(required = false) UUID propietarioId
    ) {
        List<Herramienta> lista;
        if (propietarioId != null) {
            lista = herramientaService.listarPorPropietario(propietarioId);
        } else if (categoriaId != null) {
            lista = herramientaService.listarPorCategoria(categoriaId);
        } else {
            lista = herramientaService.listarPublicadas();
        }

        // Filtros en memoria para query y maxPrice
        var res = lista.stream()
                .filter(h -> query == null || query.isBlank() || h.getNombre().toLowerCase().contains(query.toLowerCase()) || (h.getDescripcion() != null && h.getDescripcion().toLowerCase().contains(query.toLowerCase())))
                .filter(h -> maxPrice == null || h.getPrecioDia().getMonto().compareTo(maxPrice) <= 0)
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de herramienta", description = "Obtiene los datos completos de una herramienta por su ID")
    public ResponseEntity<HerramientaResponse> detalle(@PathVariable UUID id) {
        Herramienta h = herramientaService.obtenerPorId(id);
        return ResponseEntity.ok(toResponse(h));
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "Consultar disponibilidad", description = "Verifica la cantidad disponible de la herramienta para un rango de fechas")
    public ResponseEntity<DisponibilidadResponse> consultarDisponibilidad(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        Herramienta h = herramientaService.obtenerPorId(id);
        int cantDisponible = herramientaService.calcularDisponibilidad(id, fechaInicio, fechaFin);
        boolean disp = cantDisponible > 0 && h.getEstadoPublicacion() == HerramientaPubEstado.PUBLICADA;
        return ResponseEntity.ok(new DisponibilidadResponse(
                disp,
                cantDisponible,
                h.getCantidadTotal(),
                h.getEstadoPublicacion().name()
        ));
    }

    @PostMapping
    @Operation(summary = "Crear herramienta", description = "Registra un nuevo equipo de construcción en el catálogo")
    public ResponseEntity<HerramientaResponse> crear(@Valid @RequestBody CrearHerramientaRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        UUID propId = req.propietarioId() != null ? req.propietarioId() : actor.getId();

        Herramienta h = herramientaService.crearHerramienta(
                actor.getId(), actor.getRol(),
                propId, req.categoriaId(),
                req.nombre(), req.descripcion(), req.marca(), req.modelo(),
                Dinero.cop(req.precioDia()), req.cantidadTotal(),
                new BigDecimal("19.00"), true
        );
        return ResponseEntity.ok(toResponse(h));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar herramienta", description = "Modifica el precio o estado de publicación de un equipo")
    public ResponseEntity<HerramientaResponse> actualizar(
            @PathVariable UUID id,
            @RequestBody ActualizarHerramientaRequest req,
            Authentication auth
    ) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        Herramienta h = herramientaService.obtenerPorId(id);

        if (req.precioDia() != null) {
            h = herramientaService.actualizarPrecio(id, Dinero.cop(req.precioDia()), actor.getId(), actor.getRol());
        }
        if (req.estadoPublicacion() != null) {
            h = herramientaService.actualizarEstado(id, HerramientaPubEstado.valueOf(req.estadoPublicacion().toUpperCase()), actor.getId(), actor.getRol());
        }
        return ResponseEntity.ok(toResponse(h));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Pausar/retirar herramienta", description = "Cambia el estado de una herramienta a RETIRADA")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        herramientaService.actualizarEstado(id, HerramientaPubEstado.RETIRADA, actor.getId(), actor.getRol());
        return ResponseEntity.noContent().build();
    }

    private HerramientaResponse toResponse(Herramienta h) {
        return new HerramientaResponse(
                h.getId(),
                h.getCategoriaId(),
                h.getPropietarioId(),
                h.getNombre(),
                h.getDescripcion(),
                h.getMarca(),
                h.getModelo(),
                h.getPrecioDia().getMonto(),
                h.getCantidadTotal(),
                h.getEstadoPublicacion().name(),
                h.getIvaPorcentaje(),
                h.isIvaAplica(),
                h.getCantidadDisponible()
        );
    }
}
