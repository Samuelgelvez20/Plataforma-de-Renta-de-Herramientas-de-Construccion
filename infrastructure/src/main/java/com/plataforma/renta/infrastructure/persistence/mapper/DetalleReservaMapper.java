package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.DetalleReservaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class DetalleReservaMapper {

    public DetalleReserva toDomain(DetalleReservaEntity entity) {
        if (entity == null) return null;
        return new DetalleReserva(
                entity.getId(),
                entity.getReserva().getId(),
                entity.getHerramienta().getId(),
                entity.getCantidad(),
                Dinero.cop(entity.getPrecioDiaAcordado()),
                Dinero.cop(entity.getSubtotal()),
                Dinero.cop(entity.getIva()),
                Dinero.cop(entity.getTotal()),
                entity.getIvaPorcentaje(),
                entity.getIvaAplica(),
                entity.getCreadoEn()
        );
    }

    public DetalleReservaEntity toEntity(com.plataforma.renta.domain.model.DetalleReserva domain) {
        if (domain == null) return null;
        DetalleReservaEntity entity = new DetalleReservaEntity(
                domain.getId(),
                null, // reserva se setea aparte
                null, // herramienta se setea aparte
                domain.getCantidad(),
                domain.getPrecioDiaAcordado().getMonto(),
                domain.getSubtotal().getMonto(),
                domain.getIva().getMonto(),
                domain.getTotal().getMonto(),
                domain.getIvaPorcentaje(),
                domain.isIvaAplica(),
                domain.getCreadoEn()
        );
        return entity;
    }
}
