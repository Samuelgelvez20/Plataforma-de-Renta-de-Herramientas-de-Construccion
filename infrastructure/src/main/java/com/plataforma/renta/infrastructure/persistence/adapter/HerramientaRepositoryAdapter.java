package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.infrastructure.persistence.entity.HerramientaEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.HerramientaMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataCategoriaRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataHerramientaRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataPropietarioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class HerramientaRepositoryAdapter implements HerramientaRepository {

    private final SpringDataHerramientaRepository springRepo;
    private final SpringDataCategoriaRepository springDataCategoriaRepository;
    private final SpringDataPropietarioRepository springDataPropietarioRepository;
    private final HerramientaMapper mapper;

    public HerramientaRepositoryAdapter(SpringDataHerramientaRepository springRepo,
                                        SpringDataCategoriaRepository springDataCategoriaRepository,
                                        SpringDataPropietarioRepository springDataPropietarioRepository,
                                        HerramientaMapper mapper) {
        this.springRepo = springRepo;
        this.springDataCategoriaRepository = springDataCategoriaRepository;
        this.springDataPropietarioRepository = springDataPropietarioRepository;
        this.mapper = mapper;
    }

    @Override
    public Herramienta save(Herramienta herramienta) {
        HerramientaEntity entity = mapper.toEntity(herramienta);
        entity.setCategoria(springDataCategoriaRepository.getReferenceById(herramienta.getCategoriaId()));
        entity.setPropietario(springDataPropietarioRepository.getReferenceById(herramienta.getPropietarioId()));
        return mapper.toDomain(springRepo.save(entity));
    }

    @Override
    public Optional<Herramienta> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Herramienta> findByIdForUpdate(UUID id) {
        return springRepo.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<Herramienta> findByIdsForUpdate(List<UUID> ids) {
        return springRepo.findByIdsForUpdate(ids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Herramienta> findByPropietarioId(UUID propietarioId) {
        return springRepo.findByPropietarioUsuarioId(propietarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Herramienta> findByCategoriaId(UUID categoriaId) {
        return springRepo.findByCategoriaId(categoriaId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Herramienta> findByEstadoPublicacion(HerramientaPubEstado estado) {
        return springRepo.findByEstadoPublicacion(com.plataforma.renta.infrastructure.persistence.entity.HerramientaPubEstado.valueOf(estado.name())).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
