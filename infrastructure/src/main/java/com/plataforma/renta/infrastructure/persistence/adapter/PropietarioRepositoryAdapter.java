package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.infrastructure.persistence.entity.HerramientaEntity;
import com.plataforma.renta.infrastructure.persistence.entity.PropietarioEntity;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.PropietarioMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataPropietarioRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataHerramientaRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PropietarioRepositoryAdapter implements PropietarioRepository {

    private final SpringDataPropietarioRepository springRepo;
    private final SpringDataHerramientaRepository herramientaRepo;
    private final SpringDataUsuarioRepository usuarioRepo;
    private final PropietarioMapper mapper;

    public PropietarioRepositoryAdapter(SpringDataPropietarioRepository springRepo,
                                        SpringDataHerramientaRepository herramientaRepo,
                                        SpringDataUsuarioRepository usuarioRepo,
                                        PropietarioMapper mapper) {
        this.springRepo = springRepo;
        this.herramientaRepo = herramientaRepo;
        this.usuarioRepo = usuarioRepo;
        this.mapper = mapper;
    }

    @Override
    public Propietario save(Propietario propietario) {
        UsuarioEntity usuario = usuarioRepo.findById(propietario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para propietario: " + propietario.getId()));
        PropietarioEntity entity = mapper.toEntity(propietario, usuario);
        PropietarioEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Propietario> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<UUID> findHerramientaIdsByPropietarioId(UUID propietarioId) {
        return herramientaRepo.findByPropietarioUsuarioId(propietarioId).stream()
                .map(HerramientaEntity::getId)
                .collect(Collectors.toList());
    }
}