package com.plataforma.renta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public class HerramientaDtos {

    public record HerramientaResponse(
            UUID id,
            UUID categoriaId,
            UUID propietarioId,
            String nombre,
            String descripcion,
            String marca,
            String modelo,
            BigDecimal precioDia,
            int cantidadTotal,
            String estadoPublicacion,
            BigDecimal ivaPorcentaje,
            boolean ivaAplica
    ) {}

    public record CrearHerramientaRequest(
            @NotBlank String nombre,
            String descripcion,
            String marca,
            String modelo,
            @NotNull @Positive BigDecimal precioDia,
            @Positive int cantidadTotal,
            @NotNull UUID categoriaId,
            UUID propietarioId
    ) {}

    public record ActualizarHerramientaRequest(
            BigDecimal precioDia,
            String estadoPublicacion
    ) {}

    public record DisponibilidadResponse(
            boolean disponible,
            int cantidadDisponible,
            int cantidadTotal,
            String estadoOperativo
    ) {}
}
