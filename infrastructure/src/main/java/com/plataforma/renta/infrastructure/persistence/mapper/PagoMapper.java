package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.PagoEntity;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public Pago toDomain(PagoEntity entity) {
        if (entity == null) return null;
        return new Pago(
                entity.getId(),
                entity.getReserva() != null ? entity.getReserva().getId() : null,
                entity.getPagoOriginal() != null ? entity.getPagoOriginal().getId() : null,
                entity.getFactura() != null ? entity.getFactura().getId() : null,
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
        return new PagoEntity(
                domain.getId(),
                null,
                null,
                toEntityEstado(domain.getEstado()),
                domain.getProveedorPago(),
                domain.getTransaccionId(),
                domain.getMonto().getMonto(),
                domain.getMoneda(),
                domain.getMotivo(),
                domain.getFechaPago(),
                domain.getCreadoEn()
        );
    }

    private PagoEstado toDomainEstado(com.plataforma.renta.infrastructure.persistence.entity.PagoEstado estado) {
        return PagoEstado.valueOf(estado.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.PagoEstado toEntityEstado(PagoEstado estado) {
        return com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.valueOf(estado.name());
    }
}
