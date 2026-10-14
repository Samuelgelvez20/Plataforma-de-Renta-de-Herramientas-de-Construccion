package com.plataforma.renta.infrastructure.persistence.converter;

import com.plataforma.renta.infrastructure.persistence.entity.HerramientaPubEstado;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HerramientaPubEstadoConverter implements AttributeConverter<HerramientaPubEstado, String> {

    @Override
    public String convertToDatabaseColumn(HerramientaPubEstado attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public HerramientaPubEstado convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HerramientaPubEstado.valueOf(dbData);
    }
}