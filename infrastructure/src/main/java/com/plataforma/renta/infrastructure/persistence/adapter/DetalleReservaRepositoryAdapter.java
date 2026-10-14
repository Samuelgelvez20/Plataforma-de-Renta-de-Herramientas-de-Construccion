package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.port.out.DetalleReservaRepository;
import com.plataforma.renta.infrastructure.persistence.entity.DetalleReservaEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.DetalleReservaMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataDetalleReservaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DetalleReservaRepositoryAdapter implements DetalleReservaRepository {

    private final SpringDataDetalleReservaRepository springRepo;
    private final DetalleReservaMapper mapper;

    public DetalleReservaRepositoryAdapter(SpringDataDetalleReservaRepository springRepo, DetalleReservaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public List<DetalleReserva> findByReservaId(UUID reservaId) {
        return springRepo.findByReservaId(reservaId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UUID> findHerramientaIdsByReservaId(UUID reservaId) {
        return springRepo.findHerramientaIdsByReservaId(reservaId);
    }

    @Override
    public Optional<DetalleReserva> findByReservaIdAndHerramientaId(UUID reservaId, UUID herramientaId) {
        return springRepo.findByReservaIdAndHerramientaId(reservaId, herramientaId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByReservaId(UUID reservaId) {
        return springRepo.existsByReservaId(reservaId);
    }
}