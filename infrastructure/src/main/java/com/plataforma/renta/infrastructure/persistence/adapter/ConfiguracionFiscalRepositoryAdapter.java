package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.infrastructure.persistence.entity.ConfiguracionFiscalEntity;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataConfiguracionFiscalRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfiguracionFiscalRepositoryAdapter implements ConfiguracionFiscalRepository {

    private final SpringDataConfiguracionFiscalRepository springRepo;

    public ConfiguracionFiscalRepositoryAdapter(SpringDataConfiguracionFiscalRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<String> findValor(String clave) {
        return springRepo.findById(clave).map(ConfiguracionFiscalEntity::getValor);
    }

    @Override
    public void save(String clave, String valor) {
        ConfiguracionFiscalEntity entity = new ConfiguracionFiscalEntity(clave, valor, null, java.time.Instant.now());
        springRepo.save(entity);
    }
}