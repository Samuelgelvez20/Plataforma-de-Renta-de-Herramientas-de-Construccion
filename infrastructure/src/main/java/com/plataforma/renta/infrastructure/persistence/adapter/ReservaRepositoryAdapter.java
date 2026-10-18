package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.port.out.ReservaRepository;
import com.plataforma.renta.infrastructure.persistence.entity.ClienteEntity;
import com.plataforma.renta.infrastructure.persistence.entity.DetalleReservaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.ReservaEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.DetalleReservaMapper;
import com.plataforma.renta.infrastructure.persistence.mapper.ReservaMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataClienteRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataDetalleReservaRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataHerramientaRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataReservaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReservaRepositoryAdapter implements ReservaRepository {

    private final SpringDataReservaRepository springRepo;
    private final SpringDataDetalleReservaRepository detalleRepo;
    private final SpringDataClienteRepository clienteRepo;
    private final SpringDataHerramientaRepository herramientaRepo;
    private final ReservaMapper reservaMapper;
    private final DetalleReservaMapper detalleMapper;

    public ReservaRepositoryAdapter(SpringDataReservaRepository springRepo,
                                    SpringDataDetalleReservaRepository detalleRepo,
                                    SpringDataClienteRepository clienteRepo,
                                    SpringDataHerramientaRepository herramientaRepo,
                                    ReservaMapper reservaMapper,
                                    DetalleReservaMapper detalleMapper) {
        this.springRepo = springRepo;
        this.detalleRepo = detalleRepo;
        this.clienteRepo = clienteRepo;
        this.herramientaRepo = herramientaRepo;
        this.reservaMapper = reservaMapper;
        this.detalleMapper = detalleMapper;
    }

    @Override
    public Reserva save(Reserva reserva) {
        Optional<ReservaEntity> existingOpt = springRepo.findById(reserva.getId());
        ReservaEntity saved;
        if (existingOpt.isPresent()) {
            ReservaEntity entity = existingOpt.get();
            entity.setEstado(com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado.valueOf(reserva.getEstado().name()));
            entity.setFechaConfirmacion(reserva.getFechaConfirmacion());
            entity.setFechaCancelacion(reserva.getFechaCancelacion());
            entity.setMotivoCancelacion(reserva.getMotivoCancelacion());
            entity.setCanceladoPorUsuarioId(reserva.getCanceladoPorUsuarioId());
            saved = springRepo.save(entity);
        } else {
            ReservaEntity entity = reservaMapper.toEntity(reserva);
            ClienteEntity clienteEntity = clienteRepo.findById(reserva.getClienteId()).orElseThrow(
                () -> new IllegalArgumentException("Cliente no encontrado: " + reserva.getClienteId()));
            entity.setCliente(clienteEntity);
            saved = springRepo.save(entity);
            if (reserva.getDetalles() != null && !reserva.getDetalles().isEmpty()) {
                for (DetalleReserva d : reserva.getDetalles()) {
                    DetalleReservaEntity detEntity = detalleMapper.toEntity(d);
                    detEntity.setReserva(saved);
                    detEntity.setHerramienta(herramientaRepo.getReferenceById(d.getHerramientaId()));
                    detalleRepo.save(detEntity);
                }
            }
        }
        return mapWithDetalles(saved);
    }

    @Override
    public Optional<Reserva> findById(UUID id) {
        return springRepo.findById(id).map(this::mapWithDetalles);
    }

    @Override
    public Optional<Reserva> findByIdForUpdate(UUID id) {
        return springRepo.findByIdForUpdate(id).map(this::mapWithDetalles);
    }

    @Override
    public List<Reserva> findByClienteId(UUID clienteId) {
        return springRepo.findByClienteUsuarioId(clienteId).stream()
                .map(this::mapWithDetalles)
                .collect(Collectors.toList());
    }

    @Override
    public List<Reserva> findByEstado(com.plataforma.renta.domain.model.ReservaEstado estado) {
        return springRepo.findByEstado(com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado.valueOf(estado.name())).stream()
                .map(this::mapWithDetalles)
                .collect(Collectors.toList());
    }

    @Override
    public List<Reserva> findPendientesVencidas(Instant ahora) {
        return springRepo.findPendientesVencidas(com.plataforma.renta.infrastructure.persistence.entity.ReservaEstado.PENDIENTE, ahora).stream()
                .map(this::mapWithDetalles)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UUID id) {
        return springRepo.existsById(id);
    }

    private Reserva mapWithDetalles(ReservaEntity entity) {
        Reserva reserva = reservaMapper.toDomain(entity);
        List<DetalleReservaEntity> detalles = detalleRepo.findByReservaId(entity.getId());
        for (DetalleReservaEntity d : detalles) {
            reserva.agregarDetalle(detalleMapper.toDomain(d));
        }
        return reserva;
    }

    @Override
    public List<Reserva> findAll() {
        return springRepo.findAll().stream()
                .map(this::mapWithDetalles)
                .collect(Collectors.toList());
    }
}
