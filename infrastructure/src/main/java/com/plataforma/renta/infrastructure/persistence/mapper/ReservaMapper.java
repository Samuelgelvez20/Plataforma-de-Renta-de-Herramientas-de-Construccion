package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.DetalleReservaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.ReservaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ReservaMapper {

    public Reserva toDomain(ReservaEntity entity) {
        if (entity == null) return null;
        UUID clienteId = entity.getCliente() != null ? entity.getCliente().getUsuarioId() : null;
        Reserva res = Reserva.reconstruir(
                entity.getId(),
                clienteId,
                toDomainEstado(entity.getEstado()),
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getFechaCreacion(),
                entity.getFechaConfirmacion(),
                entity.getFechaExpiracionPendiente(),
                entity.getFechaCancelacion(),
                entity.getMotivoCancelacion(),
                entity.getCanceladoPorUsuarioId(),
                entity.getActualizadoEn()
        );
        res.setSolicitudDevolucion(entity.getSolicitudDevolucion());
        res.setEstadoDevolucion(entity.getEstadoDevolucion());
        res.setNotaDevolucionCliente(entity.getNotaDevolucionCliente());
        res.setCostoReparacion(Dinero.cop(entity.getCostoReparacion()));
        res.setDescripcionDano(entity.getDescripcionDano());
        res.setObservacionPropietario(entity.getObservacionPropietario());
        res.setFechaDevolucion(entity.getFechaDevolucion());
        return res;
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
        entity.setSolicitudDevolucion(domain.isSolicitudDevolucion());
        entity.setEstadoDevolucion(domain.getEstadoDevolucion());
        entity.setNotaDevolucionCliente(domain.getNotaDevolucionCliente());
        entity.setCostoReparacion(domain.getCostoReparacion() != null ? domain.getCostoReparacion().getMonto() : java.math.BigDecimal.ZERO);
        entity.setDescripcionDano(domain.getDescripcionDano());
        entity.setObservacionPropietario(domain.getObservacionPropietario());
        entity.setFechaDevolucion(domain.getFechaDevolucion());
        return entity;
    }

    public DetalleReserva detalleToDomain(DetalleReservaEntity entity) {
        if (entity == null) return null;
        return new DetalleReserva(
                entity.getId(),
                entity.getReserva() != null ? entity.getReserva().getId() : null,
                entity.getHerramienta() != null ? entity.getHerramienta().getId() : null,
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

    private ReservaEstado toDomainEstado(com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado estado) {
        return ReservaEstado.valueOf(estado.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado toEntityEstado(ReservaEstado estado) {
        return com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado.valueOf(estado.name());
    }
}
