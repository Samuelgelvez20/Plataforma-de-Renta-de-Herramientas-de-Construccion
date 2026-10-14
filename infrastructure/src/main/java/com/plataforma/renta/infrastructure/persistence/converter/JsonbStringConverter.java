package com.plataforma.renta.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class JsonbStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? "{}" : attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? "{}" : dbData;
    }
}