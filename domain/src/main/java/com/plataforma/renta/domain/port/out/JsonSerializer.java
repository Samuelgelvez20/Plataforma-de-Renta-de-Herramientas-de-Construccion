package com.plataforma.renta.domain.port.out;

public interface JsonSerializer {

    String toJson(Object object);

    boolean isValidJson(String json);
}
