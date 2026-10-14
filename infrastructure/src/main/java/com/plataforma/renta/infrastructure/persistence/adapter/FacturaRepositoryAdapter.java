package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.port.out.FacturaRepository;
import com.plataforma.renta.infrastructure.persistence.entity.FacturaEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.FacturaMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataFacturaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FacturaRepositoryAdapter implements FacturaRepository {

    private final SpringDataFacturaRepository springRepo;
    private final FacturaMapper mapper;

    public FacturaRepositoryAdapter(SpringDataFacturaRepository springRepo, FacturaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Factura save(Factura factura) {
        FacturaEntity entity = mapper.toEntity(factura);
        return mapper.toDomain(springRepo.save(entity));
    }

    @Override
    public Optional<Factura> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Factura> findByReservaId(UUID reservaId) {
        return springRepo.findByReservaId(reservaId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Factura> findOriginalByReservaId(UUID reservaId) {
        return springRepo.findOriginalByReservaId(reservaId).map(mapper::toDomain);
    }

    @Override
    public List<Factura> findNotasCreditoByReservaId(UUID reservaId) {
        return springRepo.findByReservaIdAndTipo(reservaId, com.plataforma.renta.infrastructure.persistence.entity.FacturaTipo.valueOf(com.plataforma.renta.domain.model.FacturaTipo.NOTA_CREDITO.name())).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Factura> findByPagoReembolsoId(UUID pagoReembolsoId) {
        return springRepo.findByPagoReembolsoId(pagoReembolsoId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNumeroFactura(String numeroFactura) {
        return springRepo.existsByNumeroFactura(numeroFactura);
    }
}
