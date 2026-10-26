package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.FacturaEntity;
import org.springframework.stereotype.Component;

@Component
public class FacturaMapper {

    public Factura toDomain(FacturaEntity entity) {
        if (entity == null) return null;
        return new Factura(
                entity.getId(),
                entity.getReserva() != null ? entity.getReserva().getId() : null,
                entity.getNumeroFactura(),
                toDomainTipo(entity.getTipo()),
                entity.getFacturaOriginal() != null ? entity.getFacturaOriginal().getId() : null,
                entity.getPagoReembolso() != null ? entity.getPagoReembolso().getId() : null,
                Dinero.cop(entity.getSubtotal()),
                Dinero.cop(entity.getIva()),
                Dinero.cop(entity.getTotal()),
                entity.getIvaPorcentaje(),
                entity.getIvaAplica(),
                entity.getDatosEmisor(),
                entity.getDatosReceptor(),
                entity.getFechaEmision(),
                entity.getCreadoEn(),
                entity.getMoneda(),
                entity.getEstado() != null ? entity.getEstado() : (entity.getTipo() == com.plataforma.renta.infrastructure.persistence.entity.FacturaTipo.REPARACION ? "PENDIENTE" : "PAGADA"),
                entity.getDescripcionDano(),
                entity.getPago() != null ? entity.getPago().getId() : null
        );
    }

    public FacturaEntity toEntity(Factura domain) {
        if (domain == null) return null;
        FacturaEntity entity = new FacturaEntity(
                domain.getId(),
                null,
                domain.getNumeroFactura(),
                toEntityTipo(domain.getTipo()),
                null,
                null,
                domain.getSubtotal().getMonto(),
                domain.getIva().getMonto(),
                domain.getTotal().getMonto(),
                domain.getMoneda(),
                domain.getIvaPorcentaje(),
                domain.isIvaAplica(),
                domain.getDatosEmisor(),
                domain.getDatosReceptor(),
                domain.getFechaEmision(),
                domain.getCreadoEn()
        );
        entity.setEstado(domain.getEstado());
        entity.setDescripcionDano(domain.getDescripcionDano());
        return entity;
    }

    private FacturaTipo toDomainTipo(com.plataforma.renta.infrastructure.persistence.entity.FacturaTipo tipo) {
        return FacturaTipo.valueOf(tipo.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.FacturaTipo toEntityTipo(FacturaTipo tipo) {
        return com.plataforma.renta.infrastructure.persistence.entity.FacturaTipo.valueOf(tipo.name());
    }
}
