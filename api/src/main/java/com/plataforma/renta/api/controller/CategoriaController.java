package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.CategoriaDtos.CategoriaResponse;
import com.plataforma.renta.application.service.HerramientaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Catálogo de categorías de herramientas")
public class CategoriaController {

    private final HerramientaService herramientaService;

    public CategoriaController(HerramientaService herramientaService) {
        this.herramientaService = herramientaService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías", description = "Obtiene todas las categorías de herramientas disponibles")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        var categorias = herramientaService.listarCategorias().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion()))
                .toList();
        return ResponseEntity.ok(categorias);
    }
}
