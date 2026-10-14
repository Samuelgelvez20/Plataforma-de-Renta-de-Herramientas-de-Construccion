package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.PagoEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PagoMapper {

    public Pago toDomain(PagoEntity entity) {
        if (entity == null) return null;
        return new Pago(
                entity.getId(),
                entity.getReserva().getId(),
                entity.getPagoOriginal() != null ? entity.getPagoOriginal().getId() : null,
                toDomainEstado(entity.getEstado()),
                entity.getProveedorPago(),
                entity.getTransaccionId(),
                Dinero.cop(entity.getMonto()),
                entity.getMotivo(),
                entity.getFechaPago(),
                entity.getCreadoEn()
        );
    }

    public PagoEntity toEntity(Pago domain) {
        if (domain == null) return null;
        PagoEntity entity = new PagoEntity(
                domain.getId(),
                null, // reserva se setea aparte
                domain.getPagoOriginalId() != null ? new PagoEntity() : null, // se setea aparte
                toEntityEstado(domain.getEstado()),
                domain.getProveedorPago(),
                domain.getTransaccionId(),
                domain.getMonto().getMonto(),
                domain.getMoneda(),
                domain.getMotivo(),
                domain.getFechaPago(),
                domain.getCreadoEn()
        );
        return entity;
    }

    private PagoEstado toDomainEstado(com.plataforma.renta.infrastructure.persistence.entity.PagoEstado estado) {
        return PagoEstado.valueOf(estado.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.PagoEstado toEntityEstado(PagoEstado estado) {
        return com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.valueOf(estado.name());
    }
}