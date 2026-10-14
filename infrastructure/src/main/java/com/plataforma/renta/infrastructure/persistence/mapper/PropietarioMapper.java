package com.plataforma.renta.infrastructure.persistence.mapper;

import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.domain.value.Nit;
import com.plataforma.renta.infrastructure.persistence.entity.PropietarioEntity;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class PropietarioMapper {

    public Propietario toDomain(PropietarioEntity entity) {
        if (entity == null) return null;
        UsuarioEntity u = entity.getUsuario();
        return new Propietario(
                entity.getUsuarioId(),
                u != null ? Email.of(u.getEmail()) : null,
                Nit.of(entity.getNit()),
                entity.getRazonSocial(),
                entity.getDireccionFiscal(),
                entity.getTelefonoContacto(),
                entity.getRegimen(),
                entity.getMunicipio(),
                entity.getDepartamento(),
                entity.getVerificadoEn()
        );
    }

    public PropietarioEntity toEntity(Propietario domain, UsuarioEntity usuarioEntity) {
        if (domain == null) return null;
        PropietarioEntity entity = new PropietarioEntity(
                usuarioEntity,
                domain.getRazonSocial(),
                domain.getNit().getValor(),
                domain.getDireccionFiscal(),
                domain.getTelefonoContacto(),
                domain.getRegimen(),
                domain.getMunicipio(),
                domain.getDepartamento()
        );
        entity.setVerificadoEn(domain.getVerificadoEn());
        return entity;
    }
}
