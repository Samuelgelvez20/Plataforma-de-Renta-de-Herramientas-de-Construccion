package com.plataforma.renta.infrastructure.persistence.repository;

import com.plataforma.renta.infrastructure.persistence.entity.ConfiguracionFiscalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataConfiguracionFiscalRepository extends JpaRepository<ConfiguracionFiscalEntity, String> {
}