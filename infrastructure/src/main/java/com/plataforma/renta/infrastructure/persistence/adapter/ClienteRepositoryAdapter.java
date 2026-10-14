package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.infrastructure.persistence.entity.ClienteEntity;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import com.plataforma.renta.infrastructure.persistence.mapper.ClienteMapper;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataClienteRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final SpringDataClienteRepository springRepo;
    private final SpringDataUsuarioRepository usuarioRepo;
    private final ClienteMapper mapper;

    public ClienteRepositoryAdapter(SpringDataClienteRepository springRepo,
                                    SpringDataUsuarioRepository usuarioRepo,
                                    ClienteMapper mapper) {
        this.springRepo = springRepo;
        this.usuarioRepo = usuarioRepo;
        this.mapper = mapper;
    }

    @Override
    public Cliente save(Cliente cliente) {
        UsuarioEntity usuario = usuarioRepo.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para cliente: " + cliente.getId()));
        ClienteEntity entity = mapper.toEntity(cliente, usuario);
        ClienteEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Cliente> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByUsuarioId(UUID usuarioId) {
        return springRepo.findById(usuarioId).map(mapper::toDomain);
    }
}
