package com.plataforma.renta.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.plataforma.renta.domain.exception.StockInsuficienteException;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.infrastructure.persistence.entity.ClienteEntity;
import com.plataforma.renta.infrastructure.persistence.entity.UsuarioEntity;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataClienteRepository;
import com.plataforma.renta.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.plataforma.renta.platform.PlataformaRentaApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.plataforma.renta.infrastructure.persistence.entity.UserRol;

@SpringBootTest(classes = PlataformaRentaApplication.class)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrenciaReservaSimpleTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final java.util.concurrent.atomic.AtomicInteger nitCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final String[] VALID_NITS = {
        "900123450-4", "900123451-1", "900123452-9", "900123453-6",
        "900123454-3", "900123455-0", "900123456-8", "900123457-5",
        "900123458-2", "900123459-1"
    };

    @BeforeAll
    static void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    ReservaService reservaService;

    @Autowired
    HerramientaService herramientaService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SpringDataUsuarioRepository usuarioRepository;

    @Autowired
    SpringDataClienteRepository clienteRepository;

    private UUID propietarioId;
    private UUID categoriaId;
    private UUID clienteId;

    @BeforeEach
    void setupTestData() {
        propietarioId = UUID.randomUUID();
        categoriaId = UUID.randomUUID();
        clienteId = UUID.randomUUID();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String propietarioEmail = "propietario-" + uniqueSuffix + "@test.com";
        
        String nit = VALID_NITS[nitCounter.getAndIncrement() % VALID_NITS.length];
        String categoriaNombre = "Taladros-" + uniqueSuffix;
        
        jdbcTemplate.update("INSERT INTO usuario (id, email, password_hash, rol, nombre, apellido, creado_en, actualizado_en) VALUES (?, ?, ?, ?::user_rol, ?, ?, now(), now())",
                propietarioId, propietarioEmail, "hash", "PROPIETARIO", "Test", "Propietario");
        jdbcTemplate.update("INSERT INTO propietario (usuario_id, razon_social, nit, direccion_fiscal, telefono_contacto, regimen, municipio, departamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                propietarioId, "Test Propietario", nit, "Dirección fiscal test", "3001234567", "COMUN", "Bogotá", "Cundinamarca");
        jdbcTemplate.update("INSERT INTO categoria (id, nombre, descripcion, creado_en) VALUES (?, ?, ?, now())",
                categoriaId, "Taladros-" + uniqueSuffix, "Taladros de prueba");
    }

    private UUID crearClienteEnJPA() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setEmail("cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        usuario.setPasswordHash("hash");
        usuario.setApellido("Cliente");
        usuario.setNombre("Test");
        usuario.setRol(UserRol.CLIENTE);
        usuario = usuarioRepository.save(usuario);

        ClienteEntity cliente = new ClienteEntity(usuario);
        clienteRepository.save(cliente);
        return usuario.getId();
    }

    @Test
    void reservaConcurrenteMismoStockSoloUnaExitosa() throws InterruptedException {
        UUID clienteUsuarioId = crearClienteEnJPA();

        UUID herramientaId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO herramienta (id, categoria_id, propietario_id, nombre, descripcion, marca, modelo, precio_dia, cantidad_total, estado_publicacion, iva_porcentaje, iva_aplica, creado_en, actualizado_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::herramienta_pub_estado, ?, ?, now(), now())",
                herramientaId, categoriaId, propietarioId, "Taladro Test", "Desc", "Marca", "Modelo",
                new BigDecimal("10000"), 1, "PUBLICADA", new BigDecimal("19.00"), true);

        int initialAvailability = jdbcTemplate.queryForObject(
            "SELECT calcular_disponibilidad_herramienta(?, ?, ?, ?)", 
            Integer.class, herramientaId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), (UUID) null);
        System.out.println("Initial availability: " + initialAvailability);

        int hilos = 10;
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(hilos);
        AtomicInteger exitos = new AtomicInteger();
        java.util.concurrent.ConcurrentLinkedQueue<Throwable> errores = new java.util.concurrent.ConcurrentLinkedQueue<>();

        ExecutorService ex = Executors.newFixedThreadPool(hilos);

        for (int i = 0; i < hilos; i++) {
            ex.submit(() -> {
                try {
                    inicio.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fin.countDown();
                    return;
                }
                try {
                    reservaService.crearReserva(new com.plataforma.renta.application.command.CrearReservaCommand(
                            clienteUsuarioId,
                            LocalDate.now().plusDays(1),
                            LocalDate.now().plusDays(2),
                            List.of(new com.plataforma.renta.application.command.CrearReservaCommand.DetalleCommand(herramientaId, 1))
                    ));
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    errores.add(e);
                } finally {
                    fin.countDown();
                }
            });
        }

        inicio.countDown();
        assertTrue(fin.await(30, TimeUnit.SECONDS));

        int finalAvailability = jdbcTemplate.queryForObject(
            "SELECT calcular_disponibilidad_herramienta(?, ?, ?, ?)", 
            Integer.class, herramientaId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), (UUID) null);
        System.out.println("Final availability: " + finalAvailability);
        
        assertEquals(1, exitos.get(), "Solo una reserva debe tener éxito con stock=1");
        assertEquals(hilos - 1, errores.size(), "Los otros hilos deben haber fallado");
        for (Throwable t : errores) {
            assertTrue(t instanceof StockInsuficienteException,
                "El fallo debe ser por stock insuficiente, pero fue: " + t.getClass().getName() + " - " + t.getMessage());
        }
    }
}