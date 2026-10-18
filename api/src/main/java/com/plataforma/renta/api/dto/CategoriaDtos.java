package com.plataforma.renta.api.dto;

import java.util.UUID;

public class CategoriaDtos {

    public record CategoriaResponse(
            UUID id,
            String nombre,
            String descripcion
    ) {}
}
