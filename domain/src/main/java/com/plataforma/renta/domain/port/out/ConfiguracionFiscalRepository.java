package com.plataforma.renta.domain.port.out;

import java.util.Optional;

public interface ConfiguracionFiscalRepository {

    Optional<String> findValor(String clave);

    void save(String clave, String valor);
}