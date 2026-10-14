package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.infrastructure.persistence.entity.PagoEntity;
import com.plataforma.renta.infrastructure.persistence.entity.PagoEstado;
import com.plataforma.renta.infrastructure.persistence.mapper.PagoMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataPagoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PagoRepositoryAdapter implements PagoRepository {

    private final SpringDataPagoRepository springRepo;
    private final PagoMapper mapper;

    public PagoRepositoryAdapter(SpringDataPagoRepository springRepo, PagoMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Pago save(Pago pago) {
        PagoEntity entity = mapper.toEntity(pago);
        return mapper.toDomain(springRepo.save(entity));
    }

    @Override
    public Optional<Pago> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Pago> findByIdForUpdate(UUID id) {
        return springRepo.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<Pago> findByReservaId(UUID reservaId) {
        return springRepo.findByReservaId(reservaId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Pago> findCompletadoByReservaId(UUID reservaId) {
        return springRepo.findByReservaIdAndEstado(reservaId, com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.COMPLETADO).map(mapper::toDomain);
    }

    @Override
    public Optional<Pago> findCompletadoByReservaIdForUpdate(UUID reservaId) {
        return springRepo.findCompletadoByReservaIdForUpdate(reservaId, com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.COMPLETADO).map(mapper::toDomain);
    }

    @Override
    public List<Pago> findByPagoOriginalId(UUID pagoOriginalId) {
        return springRepo.findByPagoOriginalId(pagoOriginalId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumMontoByPagoOriginalId(UUID pagoOriginalId) {
        return springRepo.sumMontoByPagoOriginalId(pagoOriginalId);
    }

    @Override
    public boolean existsCompletadoByReservaId(UUID reservaId) {
        return springRepo.existsCompletadoByReservaId(reservaId);
    }
}
