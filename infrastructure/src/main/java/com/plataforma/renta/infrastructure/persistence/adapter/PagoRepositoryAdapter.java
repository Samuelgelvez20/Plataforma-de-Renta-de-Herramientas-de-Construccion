package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.infrastructure.persistence.entity.PagoEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.PagoMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataPagoRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataReservaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PagoRepositoryAdapter implements PagoRepository {

    private final SpringDataPagoRepository springRepo;
    private final SpringDataReservaRepository reservaRepo;
    private final com.plataforma.renta.infrastructure.persistence.repository.SpringDataFacturaRepository facturaRepo;
    private final PagoMapper mapper;

    public PagoRepositoryAdapter(SpringDataPagoRepository springRepo,
                                 SpringDataReservaRepository reservaRepo,
                                 com.plataforma.renta.infrastructure.persistence.repository.SpringDataFacturaRepository facturaRepo,
                                 PagoMapper mapper) {
        this.springRepo = springRepo;
        this.reservaRepo = reservaRepo;
        this.facturaRepo = facturaRepo;
        this.mapper = mapper;
    }

    @Override
    public Pago save(Pago pago) {
        PagoEntity entity = mapper.toEntity(pago);
        if (pago.getReservaId() != null) {
            entity.setReserva(reservaRepo.getReferenceById(pago.getReservaId()));
        }
        if (pago.getPagoOriginalId() != null) {
            entity.setPagoOriginal(springRepo.getReferenceById(pago.getPagoOriginalId()));
        }
        if (pago.getFacturaId() != null) {
            entity.setFactura(facturaRepo.getReferenceById(pago.getFacturaId()));
        }
        return mapper.toDomain(springRepo.saveAndFlush(entity));
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
    public BigDecimal sumMontoByPagoOriginalIdAndEstado(UUID pagoOriginalId, com.plataforma.renta.domain.model.PagoEstado estado) {
        return springRepo.sumMontoByPagoOriginalIdAndEstado(
                pagoOriginalId,
                com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.valueOf(estado.name())
        );
    }

    @Override
    public BigDecimal sumMontoByPagoOriginalId(UUID pagoOriginalId) {
        return sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, com.plataforma.renta.domain.model.PagoEstado.REEMBOLSADO);
    }

    @Override
    public boolean existsCompletadoByReservaId(UUID reservaId) {
        return springRepo.existsCompletadoByReservaId(reservaId);
    }

    @Override
    public boolean existsByPagoOriginalIdAndEstado(UUID pagoOriginalId, com.plataforma.renta.domain.model.PagoEstado estado) {
        return springRepo.existsByPagoOriginalIdAndEstado(pagoOriginalId, com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.valueOf(estado.name()));
    }

    @Override
    public boolean existsByReservaId(UUID reservaId) {
        return springRepo.existsByReservaId(reservaId);
    }

    @Override
    public boolean existsByFacturaIdAndEstado(UUID facturaId, com.plataforma.renta.domain.model.PagoEstado estado) {
        return springRepo.existsByFacturaIdAndEstado(facturaId, com.plataforma.renta.infrastructure.persistence.entity.PagoEstado.valueOf(estado.name()));
    }

    @Override
    public Optional<Pago> findByFacturaId(UUID facturaId) {
        return springRepo.findByFacturaId(facturaId).map(mapper::toDomain);
    }

    @Override
    public List<Pago> findAll() {
        return springRepo.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
