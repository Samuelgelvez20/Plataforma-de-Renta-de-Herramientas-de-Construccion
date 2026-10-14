package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.port.out.DisponibilidadRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class DisponibilidadRepositoryAdapter implements DisponibilidadRepository {

    private final JdbcTemplate jdbcTemplate;

    public DisponibilidadRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int calcularDisponibilidad(UUID herramientaId, LocalDate fechaInicio, LocalDate fechaFin, UUID excluirReservaId) {
        String sql = "SELECT calcular_disponibilidad_herramienta(?, ?, ?, ?)";
        return jdbcTemplate.queryForObject(sql, Integer.class, herramientaId, fechaInicio, fechaFin, excluirReservaId);
    }
}