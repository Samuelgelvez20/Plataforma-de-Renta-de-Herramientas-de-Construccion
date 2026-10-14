package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.port.out.AdvisoryLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AdvisoryLockServiceAdapter implements AdvisoryLockService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdvisoryLockServiceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void acquireLocks(List<UUID> herramientaIds) {
        for (UUID hid : herramientaIds) {
            long lockKey = hashUUID(hid);
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + lockKey + ")");
        }
    }

    @Override
    public void releaseLocks() {
        // Los advisory locks de transacción se liberan automáticamente al commit/rollback
    }

    private long hashUUID(UUID uuid) {
        return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
    }
}