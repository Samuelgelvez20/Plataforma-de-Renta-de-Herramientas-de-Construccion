package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Categoria;
import com.plataforma.renta.domain.port.out.CategoriaRepository;
import com.plataforma.renta.infrastructure.persistence.entity.CategoriaEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.CategoriaMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataCategoriaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CategoriaRepositoryAdapter implements CategoriaRepository {

    private final SpringDataCategoriaRepository springRepo;
    private final CategoriaMapper mapper;

    public CategoriaRepositoryAdapter(SpringDataCategoriaRepository springRepo, CategoriaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<Categoria> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> findAll() {
        return springRepo.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}