package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.UsuarioMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final SpringDataUsuarioRepository springRepo;
    private final UsuarioMapper mapper;

    public UsuarioRepositoryAdapter(SpringDataUsuarioRepository springRepo, UsuarioMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity entity = mapper.toEntity(usuario);
        return mapper.toDomain(springRepo.save(entity));
    }

    @Override
    public Optional<Usuario> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return springRepo.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByIdForUpdate(UUID id) {
        return springRepo.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<Usuario> findByRol(UserRol rol) {
        return springRepo.findAll().stream()
                .filter(u -> com.plataforma.renta.domain.model.UserRol.valueOf(u.getRol().name()) == rol)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Usuario> findAll() {
        return springRepo.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}