package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.HerramientaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class HerramientaMapper {

    public Herramienta toDomain(HerramientaEntity entity) {
        if (entity == null) return null;
        return new Herramienta(
                entity.getId(),
                entity.getCategoria().getId(),
                entity.getPropietario().getUsuarioId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getMarca(),
                entity.getModelo(),
                Dinero.cop(entity.getPrecioDia()),
                entity.getCantidadTotal(),
                toDomainEstado(entity.getEstadoPublicacion()),
                entity.getIvaPorcentaje(),
                entity.getIvaAplica(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public HerramientaEntity toEntity(Herramienta domain) {
        if (domain == null) return null;
        HerramientaEntity entity = new HerramientaEntity(
                domain.getId(),
                null, // categoria se setea aparte
                null, // propietario se setea aparte
                domain.getNombre(),
                domain.getDescripcion(),
                domain.getMarca(),
                domain.getModelo(),
                domain.getPrecioDia().getMonto(),
                domain.getCantidadTotal(),
                toEntityEstado(domain.getEstadoPublicacion()),
                domain.getIvaPorcentaje(),
                domain.isIvaAplica(),
                domain.getCreadoEn(),
                domain.getActualizadoEn()
        );
        return entity;
    }

    private HerramientaPubEstado toDomainEstado(com.plataforma.renta.infrastructure.persistence.entity.HerramientaPubEstado estado) {
        return HerramientaPubEstado.valueOf(estado.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.HerramientaPubEstado toEntityEstado(HerramientaPubEstado estado) {
        return com.plataforma.renta.infrastructure.persistence.entity.HerramientaPubEstado.valueOf(estado.name());
    }
}