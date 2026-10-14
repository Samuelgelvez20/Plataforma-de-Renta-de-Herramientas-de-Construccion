package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.DetalleReservaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.ReservaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ReservaMapper {

    public Reserva toDomain(ReservaEntity entity) {
        if (entity == null) return null;
        Reserva reserva = new Reserva(
                entity.getId(),
                entity.getCliente().getUsuarioId(),
                toDomainEstado(entity.getEstado()),
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getFechaCreacion(),
                entity.getFechaExpiracionPendiente()
        );
        if (entity.getDetalles() != null) {
            for (DetalleReservaEntity d : entity.getDetalles()) {
                reserva.agregarDetalle(detalleToDomain(d));
            }
        }
        if (entity.getFechaConfirmacion() != null) reserva.confirmar(entity.getFechaConfirmacion());
        if (entity.getFechaCancelacion() != null) {
            reserva.cancelar(entity.getFechaCancelacion(), entity.getMotivoCancelacion(), entity.getCanceladoPorUsuarioId());
        }
        return reserva;
    }

    public ReservaEntity toEntity(Reserva domain) {
        if (domain == null) return null;
        ReservaEntity entity = new ReservaEntity(
                domain.getId(),
                null,
                toEntityEstado(domain.getEstado()),
                domain.getFechaInicio(),
                domain.getFechaFin(),
                domain.getFechaCreacion(),
                domain.getFechaExpiracionPendiente(),
                domain.getActualizadoEn() != null ? domain.getActualizadoEn() : Instant.now()
        );
        entity.setFechaConfirmacion(domain.getFechaConfirmacion());
        entity.setFechaCancelacion(domain.getFechaCancelacion());
        entity.setMotivoCancelacion(domain.getMotivoCancelacion());
        entity.setCanceladoPorUsuarioId(domain.getCanceladoPorUsuarioId());
        return entity;
    }

    public DetalleReserva detalleToDomain(DetalleReservaEntity entity) {
        if (entity == null) return null;
        return new DetalleReserva(
                entity.getId(),
                entity.getReserva().getId(),
                entity.getHerramienta().getId(),
                entity.getCantidad(),
                com.plataforma.renta.domain.value.Dinero.cop(entity.getPrecioDiaAcordado()),
                com.plataforma.renta.domain.value.Dinero.cop(entity.getSubtotal()),
                com.plataforma.renta.domain.value.Dinero.cop(entity.getIva()),
                com.plataforma.renta.domain.value.Dinero.cop(entity.getTotal()),
                entity.getIvaPorcentaje(),
                entity.getIvaAplica(),
                entity.getCreadoEn()
        );
    }

    private ReservaEstado toDomainEstado(com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado estado) {
        return ReservaEstado.valueOf(estado.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado toEntityEstado(ReservaEstado estado) {
        return com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado.valueOf(estado.name());
    }
}
