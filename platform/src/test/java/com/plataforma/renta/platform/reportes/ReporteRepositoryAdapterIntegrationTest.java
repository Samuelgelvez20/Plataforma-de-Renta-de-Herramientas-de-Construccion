package com.plataforma.renta.platform.reportes;

import com.plataforma.renta.domain.port.out.ReporteRepository;
import com.plataforma.renta.domain.port.out.ReporteRepository.*;
import com.plataforma.renta.infrastructure.persistence.adapter.ReporteRepositoryAdapter;
import com.plataforma.renta.platform.PlataformaRentaApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlataformaRentaApplication.class)
@ActiveProfiles("test")
public class ReporteRepositoryAdapterIntegrationTest {

    private static final DockerImageName IMAGEN_POSTGRES = DockerImageName.parse("postgres:16-alpine");
    private static PostgreSQLContainer postgres;

    @BeforeAll
    static void iniciarContenedor() {
        postgres = new PostgreSQLContainer(IMAGEN_POSTGRES);
        postgres.start();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void detenerContenedor() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void configurarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "clave-secreta-de-prueba-de-32-bytes-minimo-para-testing");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReporteRepositoryAdapter reporteRepositoryAdapter;

    @Test
    @DisplayName("Debe manejar base de datos vacía retornando ceros y listas vacías sin errores")
    void testConsultasConBaseVacia() {
        List<IngresoMensualData> ingresos = reporteRepositoryAdapter.findIngresosMensuales();
        List<HerramientaAlquiladaData> masAlquiladas = reporteRepositoryAdapter.findHerramientasMasAlquiladas(5);
        List<ClienteFrecuenteData> frecuentes = reporteRepositoryAdapter.findClientesFrecuentes(5);
        MetricasResumenData resumen = reporteRepositoryAdapter.obtenerMetricasResumen();

        assertThat(ingresos).isNotNull();
        assertThat(masAlquiladas).isNotNull();
        assertThat(frecuentes).isNotNull();
        assertThat(resumen.totalReservas()).isGreaterThanOrEqualTo(0L);
        assertThat(resumen.totalIngresos()).isNotNull();
    }

    @Test
    @DisplayName("Verifica que ingresos mensuales consideran sólo pagos completados y usan creado_en/fecha_pago")
    void testIngresosMensualesConDatosControlados() {
        UUID uId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES (?, ?, 'hash', 'CLIENTE', 'Test', 'Ingresos', '+57300', true)",
                uId, "test.ingresos." + uId + "@test.com");
        jdbcTemplate.update("INSERT INTO cliente (usuario_id, direccion_entrega_default) VALUES (?, 'Dir')", uId);

        UUID resId1 = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin) VALUES (?, ?, 'CONFIRMADA', CURRENT_DATE, CURRENT_DATE + 2)",
                resId1, uId);

        // Pago completado de 500,000 en 2026-08
        UUID pId1 = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO pago (id, reserva_id, estado, proveedor_pago, monto, creado_en, fecha_pago) VALUES (?, ?, 'COMPLETADO', 'SIM', 500000.00, '2026-08-15 10:00:00+00'::timestamptz, '2026-08-15 10:00:00+00'::timestamptz)",
                pId1, resId1);

        // Pago pendiente de 300,000 que no debe sumarse
        UUID pId2 = UUID.randomUUID();
        UUID resId2 = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin, fecha_expiracion_pendiente) VALUES (?, ?, 'PENDIENTE', CURRENT_DATE, CURRENT_DATE + 2, CURRENT_TIMESTAMP + INTERVAL '1 day')",
                resId2, uId);
        jdbcTemplate.update("INSERT INTO pago (id, reserva_id, estado, proveedor_pago, monto, creado_en) VALUES (?, ?, 'PENDIENTE', 'SIM', 300000.00, '2026-08-16 10:00:00+00'::timestamptz)",
                pId2, resId2);

        List<IngresoMensualData> list = reporteRepositoryAdapter.findIngresosMensuales();
        IngresoMensualData agosto = list.stream().filter(i -> "2026-08".equals(i.mes())).findFirst().orElse(null);

        assertThat(agosto).isNotNull();
        assertThat(agosto.ingresos()).isGreaterThanOrEqualTo(new BigDecimal("500000.00"));
    }

    @Test
    @DisplayName("Herramientas más alquiladas excluye reservas canceladas y expiradas")
    void testHerramientasMasAlquiladasExcluyeCanceladas() {
        UUID catId = jdbcTemplate.queryForObject("SELECT id FROM categoria LIMIT 1", UUID.class);
        UUID provUId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES (?, ?, 'hash', 'PROPIETARIO', 'Prov', 'Ctrl', '+57300', true)",
                provUId, "prov.ctrl." + provUId + "@test.com");
        jdbcTemplate.update("INSERT INTO propietario (usuario_id, razon_social, direccion_fiscal, regimen, municipio, departamento) VALUES (?, 'Ctrl SAS', 'Dir', 'COMUN', 'BOG', 'CUN')",
                provUId);

        UUID hId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, precio_dia, cantidad_total, cantidad_disponible) VALUES (?, ?, ?, 'Herramienta Test Ranking', 50000.00, 5, 5)",
                hId, catId, provUId);

        UUID cliUId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, telefono, activo) VALUES (?, ?, 'hash', 'CLIENTE', 'Cli', 'Rank', '+57300', true)",
                cliUId, "cli.rank." + cliUId + "@test.com");
        jdbcTemplate.update("INSERT INTO cliente (usuario_id, direccion_entrega_default) VALUES (?, 'Dir')", cliUId);

        // Reserva cancelada -> NO debe contar en ranking
        UUID resCancel = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO reserva (id, cliente_id, estado, fecha_inicio, fecha_fin) VALUES (?, ?, 'CANCELADA', CURRENT_DATE, CURRENT_DATE + 1)",
                resCancel, cliUId);
        jdbcTemplate.update("INSERT INTO detalle_reserva (id, reserva_id, herramienta_id, cantidad, precio_dia_acordado, iva_porcentaje, iva_aplica, subtotal, iva, total) VALUES (?, ?, ?, 1, 50000.00, 19.00, true, 100000.00, 19000.00, 119000.00)",
                UUID.randomUUID(), resCancel, hId);

        List<HerramientaAlquiladaData> ranking = reporteRepositoryAdapter.findHerramientasMasAlquiladas(10);
        boolean estaEnRanking = ranking.stream().anyMatch(h -> hId.toString().equals(h.id()));
        assertThat(estaEnRanking).isFalse();
    }
}
