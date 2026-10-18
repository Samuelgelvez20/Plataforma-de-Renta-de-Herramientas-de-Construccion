package com.plataforma.renta.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataforma.renta.domain.port.out.JsonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JacksonJsonSerializer implements JsonSerializer {

    private final ObjectMapper objectMapper;

    public JacksonJsonSerializer() {
        this(new ObjectMapper());
    }

    @Autowired
    public JacksonJsonSerializer(@Autowired(required = false) ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @Override
    public String toJson(Object object) {
        if (object == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar objeto a JSON", e);
        }
    }

    @Override
    public boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
