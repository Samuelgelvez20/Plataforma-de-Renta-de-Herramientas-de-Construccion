package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) return null;
        return new Usuario(
                entity.getId(),
                Email.of(entity.getEmail()),
                entity.getPasswordHash(),
                toDomainRol(entity.getRol()),
                entity.getNombre(),
                entity.getApellido(),
                entity.getTelefono(),
                entity.isActivo(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public UsuarioEntity toEntity(Usuario domain) {
        if (domain == null) return null;
        UsuarioEntity entity = new UsuarioEntity(
                domain.getId(),
                domain.getEmail().getValor(),
                domain.getPasswordHash(),
                toEntityRol(domain.getRol()),
                domain.getNombre(),
                domain.getApellido(),
                domain.getTelefono(),
                domain.isActivo(),
                domain.getCreadoEn(),
                domain.getActualizadoEn()
        );
        return entity;
    }

    private UserRol toDomainRol(com.plataforma.renta.infrastructure.persistence.entity.UserRol rol) {
        return UserRol.valueOf(rol.name());
    }

    private com.plataforma.renta.infrastructure.persistence.entity.UserRol toEntityRol(UserRol rol) {
        return com.plataforma.renta.infrastructure.persistence.entity.UserRol.valueOf(rol.name());
    }
}
