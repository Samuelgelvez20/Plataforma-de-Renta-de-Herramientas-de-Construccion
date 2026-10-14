package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Categoria;
import com.plataforma.renta.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CategoriaMapper {

    public Categoria toDomain(CategoriaEntity entity) {
        if (entity == null) return null;
        return new Categoria(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getCreadoEn()
        );
    }

    public CategoriaEntity toEntity(Categoria domain) {
        if (domain == null) return null;
        return new CategoriaEntity(
                domain.getId(),
                domain.getNombre(),
                domain.getDescripcion(),
                domain.getCreadoEn()
        );
    }
}