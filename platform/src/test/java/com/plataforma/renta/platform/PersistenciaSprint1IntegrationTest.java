package com.plataforma.renta.platform;

import com.plataforma.renta.infrastructure.persistence.entity.*;
import com.plataforma.renta.infrastructure.persistence.repository.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlataformaRentaApplication.class)
@ActiveProfiles("test")
@Testcontainers
@Transactional
class PersistenciaSprint1IntegrationTest {

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
    private SpringDataUsuarioRepository usuarioRepository;

    @Autowired
    private SpringDataPropietarioRepository propietarioRepository;

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    @Autowired
    private SpringDataCategoriaRepository categoriaRepository;

    @Autowired
    private SpringDataHerramientaRepository herramientaRepository;

    @Autowired
    private SpringDataReservaRepository reservaRepository;

    @Autowired
    private SpringDataPagoRepository pagoRepository;

    @Autowired
    private SpringDataFacturaRepository facturaRepository;

    @Test
    @DisplayName("Persistencia completa de Usuario y Propietario con @MapsId y enum USER_ROL")
    void persistirYRecuperarUsuarioYPropietario() {
        UsuarioEntity usuario = new UsuarioEntity(
                UUID.randomUUID(),
                "propietario-" + UUID.randomUUID().toString().substring(0, 8) + "@constructora.com",
                "$2a$10$hashedpasswordstring",
                UserRol.PROPIETARIO,
                "Pedro",
                "Martínez",
                "3115551234",
                Instant.now(),
                Instant.now()
        );
        usuario = usuarioRepository.saveAndFlush(usuario);
        assertThat(usuario.getId()).isNotNull();

        String nitUnico = "900" + (100000 + (int)(Math.random() * 800000)) + "-1";
        PropietarioEntity propietario = new PropietarioEntity(
                usuario,
                "Construcciones Andinas SAS",
                nitUnico,
                "Av Eldorado # 68-45",
                "3115551234",
                "COMUN",
                "Bogota",
                "Cundinamarca"
        );
        propietario = propietarioRepository.saveAndFlush(propietario);

        assertThat(propietario.getId()).isEqualTo(usuario.getId());
        assertThat(propietario.getUsuarioId()).isEqualTo(usuario.getId());

        // Recuperar y verificar
        Optional<PropietarioEntity> recuperado = propietarioRepository.findById(usuario.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getRazonSocial()).isEqualTo("Construcciones Andinas SAS");
        assertThat(recuperado.get().getNit()).isEqualTo(nitUnico);
        assertThat(recuperado.get().getUsuario().getRol()).isEqualTo(UserRol.PROPIETARIO);

        // Actualizar verificación
        recuperado.get().setVerificadoEn(Instant.now());
        PropietarioEntity actualizado = propietarioRepository.saveAndFlush(recuperado.get());
        assertThat(actualizado.getVerificadoEn()).isNotNull();
    }

    @Test
    @DisplayName("Persistencia de Cliente con @MapsId y campo JSONB datosFacturacion")
    void persistirYRecuperarClienteConJsonb() {
        UsuarioEntity usuario = new UsuarioEntity(
                UUID.randomUUID(),
                "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@obras.com",
                "$2a$10$hashedpasswordstring",
                UserRol.CLIENTE,
                "Ana",
                "Rojas",
                "3001239876",
                Instant.now(),
                Instant.now()
        );
        usuario = usuarioRepository.saveAndFlush(usuario);

        String jsonbFacturacion = "{\"nit\":\"901234567-8\",\"razon_social\":\"Obras y Diseños SAS\",\"ciudad\":\"Medellin\"}";
        ClienteEntity cliente = new ClienteEntity(usuario, "Carrera 43A # 1-50", jsonbFacturacion);
        cliente = clienteRepository.saveAndFlush(cliente);

        assertThat(cliente.getId()).isEqualTo(usuario.getId());

        // Recuperar y verificar
        Optional<ClienteEntity> recuperado = clienteRepository.findById(usuario.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getDireccionEntregaDefault()).isEqualTo("Carrera 43A # 1-50");
        assertThat(recuperado.get().getDatosFacturacion()).isEqualTo(jsonbFacturacion);
        assertThat(recuperado.get().getUsuario().getRol()).isEqualTo(UserRol.CLIENTE);

        // Actualizar JSONB
        String nuevoJsonb = "{\"nit\":\"901234567-8\",\"razon_social\":\"Obras y Diseños SAS\",\"ciudad\":\"Bogota\"}";
        recuperado.get().setDatosFacturacion(nuevoJsonb);
        ClienteEntity actualizado = clienteRepository.saveAndFlush(recuperado.get());
        assertThat(actualizado.getDatosFacturacion()).contains("Bogota");
    }

    @Test
    @DisplayName("Persistencia de Categoria y Herramienta con ENUM HERRAMIENTA_PUB_ESTADO")
    void persistirYRecuperarCategoriaYHerramienta() {
        // Usuario y propietario requeridos para FK
        UsuarioEntity usuario = new UsuarioEntity(
                UUID.randomUUID(),
                "prop-herr-" + UUID.randomUUID().toString().substring(0, 8) + "@maquinaria.com",
                "$2a$10$hashedpasswordstring",
                UserRol.PROPIETARIO,
                "Mario",
                "Giraldo",
                "3158889999",
                Instant.now(),
                Instant.now()
        );
        usuario = usuarioRepository.saveAndFlush(usuario);

        String nitUnico = "800" + (100000 + (int)(Math.random() * 800000)) + "-3";
        PropietarioEntity propietario = new PropietarioEntity(
                usuario, "Alquileres Giraldo", nitUnico,
                "Calle 12 # 34-56", "3158889999", "SIMPLIFICADO", "Cali", "Valle del Cauca"
        );
        propietario = propietarioRepository.saveAndFlush(propietario);

        // Categoría
        CategoriaEntity categoria = new CategoriaEntity(
                UUID.randomUUID(),
                "Compactación-" + UUID.randomUUID().toString().substring(0, 6),
                "Equipos para compactación de suelos",
                Instant.now()
        );
        categoria = categoriaRepository.saveAndFlush(categoria);

        // Herramienta
        HerramientaEntity herramienta = new HerramientaEntity(
                UUID.randomUUID(),
                categoria,
                propietario,
                "Compactador Tipo Rana",
                "Motor Honda 4 tiempos GX160",
                "Mikasa",
                "MTX-70",
                new BigDecimal("85000.00"),
                4,
                HerramientaPubEstado.PUBLICADA,
                new BigDecimal("19.00"),
                true,
                Instant.now(),
                Instant.now()
        );
        herramienta = herramientaRepository.saveAndFlush(herramienta);

        assertThat(herramienta.getId()).isNotNull();

        // Recuperar
        Optional<HerramientaEntity> recuperada = herramientaRepository.findById(herramienta.getId());
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getNombre()).isEqualTo("Compactador Tipo Rana");
        assertThat(recuperada.get().getEstadoPublicacion()).isEqualTo(HerramientaPubEstado.PUBLICADA);
        assertThat(recuperada.get().getPrecioDia()).isEqualByComparingTo(new BigDecimal("85000.00"));
        assertThat(recuperada.get().getCategoria().getId()).isEqualTo(categoria.getId());
        assertThat(recuperada.get().getPropietario().getUsuarioId()).isEqualTo(propietario.getUsuarioId());

        // Actualizar estado a PAUSADA
        recuperada.get().setEstadoPublicacion(HerramientaPubEstado.PAUSADA);
        recuperada.get().setPrecioDia(new BigDecimal("90000.00"));
        HerramientaEntity actualizada = herramientaRepository.saveAndFlush(recuperada.get());

        assertThat(actualizada.getEstadoPublicacion()).isEqualTo(HerramientaPubEstado.PAUSADA);
        assertThat(actualizada.getPrecioDia()).isEqualByComparingTo(new BigDecimal("90000.00"));
    }

    @Test
    @DisplayName("Persistencia de Pago y Factura con PostgreSQL ENUM (pago_estado, factura_tipo) y JSONB")
    void persistirYRecuperarPagoYFacturaConEnumsYJsonb() {
        UsuarioEntity usuarioCliente = new UsuarioEntity(
                UUID.randomUUID(),
                "cliente-pago-" + UUID.randomUUID().toString().substring(0, 8) + "@constructora.com",
                "$2a$10$hashedpasswordstring",
                UserRol.CLIENTE,
                "Laura",
                "Morales",
                "3101112233",
                Instant.now(),
                Instant.now()
        );
        usuarioCliente = usuarioRepository.saveAndFlush(usuarioCliente);
        ClienteEntity cliente = new ClienteEntity(usuarioCliente, "Calle 50 # 10-20", "{\"nit\":\"900888777-1\"}");
        cliente = clienteRepository.saveAndFlush(cliente);

        ReservaEntity reserva = new ReservaEntity(
                UUID.randomUUID(),
                cliente,
                ReservaEstado.CONFIRMADA,
                java.time.LocalDate.now().plusDays(1),
                java.time.LocalDate.now().plusDays(3),
                Instant.now(),
                null,
                Instant.now()
        );
        reserva = reservaRepository.saveAndFlush(reserva);

        PagoEntity pago = new PagoEntity(
                UUID.randomUUID(),
                reserva,
                null,
                PagoEstado.COMPLETADO,
                "SIMULADO",
                "TX-INT-100",
                new BigDecimal("238000.00"),
                "COP",
                "Pago total alquiler",
                Instant.now(),
                Instant.now()
        );
        pago = pagoRepository.saveAndFlush(pago);
        assertThat(pago.getId()).isNotNull();

        String emisorJson = "{\"nit\":\"900111222-3\",\"razon_social\":\"Equipos del Valle SAS\"}";
        String receptorJson = "{\"nit\":\"900888777-1\",\"razon_social\":\"Constructora Morales SAS\"}";

        FacturaEntity factura = new FacturaEntity(
                UUID.randomUUID(),
                reserva,
                "F-INT-" + UUID.randomUUID().toString().substring(0, 8),
                FacturaTipo.FACTURA,
                null,
                null,
                new BigDecimal("200000.00"),
                new BigDecimal("38000.00"),
                new BigDecimal("238000.00"),
                "COP",
                new BigDecimal("19.00"),
                true,
                emisorJson,
                receptorJson,
                Instant.now(),
                Instant.now()
        );
        factura = facturaRepository.saveAndFlush(factura);
        assertThat(factura.getId()).isNotNull();

        PagoEntity reembolso = new PagoEntity(
                UUID.randomUUID(),
                reserva,
                pago,
                PagoEstado.REEMBOLSADO,
                "SIMULADO",
                null,
                new BigDecimal("119000.00"),
                "COP",
                "Reembolso por devolución anticipada",
                Instant.now(),
                Instant.now()
        );
        reembolso = pagoRepository.saveAndFlush(reembolso);

        FacturaEntity notaCredito = new FacturaEntity(
                UUID.randomUUID(),
                reserva,
                "NC-INT-" + UUID.randomUUID().toString().substring(0, 8),
                FacturaTipo.NOTA_CREDITO,
                factura,
                reembolso,
                new BigDecimal("100000.00"),
                new BigDecimal("19000.00"),
                new BigDecimal("119000.00"),
                "COP",
                new BigDecimal("19.00"),
                true,
                emisorJson,
                receptorJson,
                Instant.now(),
                Instant.now()
        );
        notaCredito = facturaRepository.saveAndFlush(notaCredito);

        Optional<PagoEntity> pagoRecuperado = pagoRepository.findById(pago.getId());
        assertThat(pagoRecuperado).isPresent();
        assertThat(pagoRecuperado.get().getEstado()).isEqualTo(PagoEstado.COMPLETADO);
        assertThat(pagoRepository.existsCompletadoByReservaId(reserva.getId())).isTrue();
        assertThat(pagoRepository.existsByPagoOriginalIdAndEstado(pago.getId(), PagoEstado.REEMBOLSADO)).isTrue();

        Optional<FacturaEntity> facturaRecuperada = facturaRepository.findById(factura.getId());
        assertThat(facturaRecuperada).isPresent();
        assertThat(facturaRecuperada.get().getTipo()).isEqualTo(FacturaTipo.FACTURA);
        assertThat(facturaRecuperada.get().getDatosEmisor()).isEqualTo(emisorJson);
        assertThat(facturaRecuperada.get().getDatosReceptor()).isEqualTo(receptorJson);

        Optional<FacturaEntity> notaRecuperada = facturaRepository.findById(notaCredito.getId());
        assertThat(notaRecuperada).isPresent();
        assertThat(notaRecuperada.get().getTipo()).isEqualTo(FacturaTipo.NOTA_CREDITO);
        assertThat(notaRecuperada.get().getFacturaOriginal().getId()).isEqualTo(factura.getId());
        assertThat(notaRecuperada.get().getPagoReembolso().getId()).isEqualTo(reembolso.getId());
    }
}
