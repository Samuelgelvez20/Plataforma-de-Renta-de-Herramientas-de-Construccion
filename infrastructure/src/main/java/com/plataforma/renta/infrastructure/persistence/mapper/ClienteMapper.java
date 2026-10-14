package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.infrastructure.persistence.entity.ClienteEntity;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClienteMapper {

    public Cliente toDomain(ClienteEntity entity) {
        if (entity == null) return null;
        UsuarioEntity u = entity.getUsuario();
        return new Cliente(
                entity.getUsuarioId(),
                u != null ? Email.of(u.getEmail()) : null,
                u != null ? u.getNombre() : null,
                u != null ? u.getApellido() : null,
                u != null ? u.getTelefono() : null,
                entity.getDireccionEntregaDefault(),
                entity.getDatosFacturacion()
        );
    }

    public ClienteEntity toEntity(Cliente domain, UsuarioEntity usuarioEntity) {
        if (domain == null) return null;
        return new ClienteEntity(usuarioEntity, domain.getDireccionEntregaDefault(), domain.getDatosFacturacion());
    }
}