package com.plataforma.renta.platform;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MigracionFlywayTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "clave-secreta-de-prueba-de-32-bytes-minimo-para-testing");
    }

    @BeforeAll
    static void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void migracionV1SeEjecutaCorrectamente() {
        // Verificar que las tablas principales existen
        Long countUsuario = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuario", Long.class);
        Long countHerramienta = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM herramienta", Long.class);
        Long countReserva = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reserva", Long.class);
        Long countDetalle = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM detalle_reserva", Long.class);
        Long countPago = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pago", Long.class);
        Long countFactura = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM factura", Long.class);
        Long countConfig = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM configuracion_fiscal", Long.class);

        assertNotNull(countUsuario);
        assertNotNull(countHerramienta);
        assertNotNull(countReserva);
        assertNotNull(countDetalle);
        assertNotNull(countPago);
        assertNotNull(countFactura);
        assertNotNull(countConfig);
        
        // Verificar configuración fiscal por defecto
        String iva = jdbcTemplate.queryForObject("SELECT valor FROM configuracion_fiscal WHERE clave = 'iva_porcentaje'", String.class);
        assertEquals("19.00", iva);
        
        String expiracion = jdbcTemplate.queryForObject("SELECT valor FROM configuracion_fiscal WHERE clave = 'reserva_pendiente_expiracion_minutos'", String.class);
        assertEquals("30", expiracion);
    }

    @Test
    void triggersExisten() {
        // Verificar que los triggers existen
        Long triggers = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.triggers WHERE event_object_table IN ('reserva', 'detalle_reserva', 'pago', 'factura')", 
            Long.class
        );
        assertNotNull(triggers);
        assertTrue(triggers > 0);
    }

    @Test
    void indicesUnicosExisten() {
        // Verificar índices únicos parciales
        Long idxPago = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uq_pago_completado_por_reserva'", 
            Long.class
        );
        assertEquals(1L, idxPago);

        Long idxFactura = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uq_factura_original_por_reserva'", 
            Long.class
        );
        assertEquals(1L, idxFactura);
    }

    @Test
    void fkCompuestaFactura() {
        // Verificar FK compuesta en factura
        Long fk = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_name = 'fk_nota_credito_original' AND table_name = 'factura'", 
            Long.class
        );
        assertEquals(1L, fk);
    }
}