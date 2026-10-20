package com.plataforma.renta.platform.reembolso;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataforma.renta.domain.model.*;
import com.plataforma.renta.domain.port.out.*;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.domain.value.Nit;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReembolsoResolucionIntegrationTest {

    private static final DockerImageName IMAGEN_POSTGRES = DockerImageName.parse("postgres:16-alpine");
    private static PostgreSQLContainer postgres;

    @BeforeAll
    static void levantarPostgres() {
        postgres = new PostgreSQLContainer(IMAGEN_POSTGRES);
        postgres.start();
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void detenerPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "clave-secreta-de-prueba-de-32-bytes-minimo-para-testing");
    }

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PropietarioRepository propietarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private HerramientaRepository herramientaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SEC-02: Flujo completo de rechazo de reembolso persiste estado FALLIDO y valida autorizaciones")
    void flujoCompletoRechazoReembolsoPersisteEstado() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        String baseUrl = "http://localhost:" + puerto;
        HttpClient client = HttpClient.newHttpClient();

        // 1. Crear propietario titular
        String propEmail = "prop.reemb." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String propPass = "PropPass123!";
        UUID propId = UUID.randomUUID();
        Usuario propUser = new Usuario(
                propId, Email.of(propEmail), passwordEncoder.encode(propPass),
                UserRol.PROPIETARIO, "Carlos", "Propietario", "+57300111", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(propUser);
        propietarioRepository.save(new Propietario(
                propId, propUser.getEmail(), Nit.of("900123450-4"), "Maquinaria SAS",
                "Calle 1", "+57300111", "COMUN", "Bogota", "Cundinamarca", null
        ));

        // 2. Obtener categoría sembrada por Flyway
        Categoria cat = categoriaRepository.findAll().stream().findFirst().orElseThrow();

        UUID herramientaId = UUID.randomUUID();
        Herramienta herramienta = new Herramienta(
                herramientaId, cat.getId(), propId,
                "Rotomartillo Industrial", "Desc", "Bosch", "GBH-2000",
                Dinero.cop(new BigDecimal("50000")), 5, HerramientaPubEstado.PUBLICADA,
                new BigDecimal("19.00"), true, Instant.now(), Instant.now()
        );
        herramientaRepository.save(herramienta);

        // 3. Crear cliente
        String cliEmail = "cli.reemb." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String cliPass = "CliPass123!";
        UUID cliId = UUID.randomUUID();
        Usuario cliUser = new Usuario(
                cliId, Email.of(cliEmail), passwordEncoder.encode(cliPass),
                UserRol.CLIENTE, "Andres", "Cliente", "+57300222", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(cliUser);
        clienteRepository.save(new Cliente(
                cliId, cliUser.getEmail(), "Andres", "Cliente", "+57300222", "Calle 2", "{}"
        ));

        // 4. Crear reserva confirmada
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = new Reserva(
                reservaId, cliId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1, Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                new BigDecimal("19.00"), true
        ));
        reservaRepository.save(reserva);

        // 5. Crear pago original completado
        UUID pagoOriginalId = UUID.randomUUID();
        Pago pagoOriginal = new Pago(
                pagoOriginalId, reservaId, null, PagoEstado.COMPLETADO,
                "SIMULADO", "TX-123456", Dinero.cop(new BigDecimal("119000")),
                "Pago original", Instant.now(), Instant.now()
        );
        pagoRepository.save(pagoOriginal);

        // 6. Crear solicitud de reembolso en estado PENDIENTE
        UUID reembolsoId = UUID.randomUUID();
        Pago reembolso = new Pago(
                reembolsoId, reservaId, pagoOriginalId, PagoEstado.PENDIENTE,
                "SIMULADO", null, Dinero.cop(new BigDecimal("119000")),
                "Solicitud de reembolso por cliente", Instant.now(), Instant.now()
        );
        pagoRepository.save(reembolso);

        // Iniciar sesión como propietario
        HttpResponse<String> propLoginResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", propEmail, "password", propPass))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String propToken = objectMapper.readTree(propLoginResp.body()).get("token").asText();

        // Iniciar sesión como cliente
        HttpResponse<String> cliLoginResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", cliEmail, "password", cliPass))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String cliToken = objectMapper.readTree(cliLoginResp.body()).get("token").asText();

        // Intento 1: Cliente intenta resolver su propio reembolso -> DEBE SER RECHAZADO (400)
        HttpResponse<String> cliResolverResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + reembolsoId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cliToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", false, "resolucion", "Auto-rechazo"))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(cliResolverResp.statusCode()).isGreaterThanOrEqualTo(400);

        // Intento 2: Propietario titular rechaza la solicitud -> DEBE SER EXITOSO (200) y persistir
        HttpResponse<String> propResolverResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + reembolsoId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + propToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "aprobar", false,
                                "resolucion", "Herramienta inspeccionada y en perfecto estado"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(propResolverResp.statusCode()).isEqualTo(200);

        JsonNode responseJson = objectMapper.readTree(propResolverResp.body());
        assertThat(responseJson.get("estado").asText()).isEqualTo("RECHAZADO");
        assertThat(responseJson.get("motivo").asText()).contains("Herramienta inspeccionada y en perfecto estado");
        assertThat(responseJson.get("resolucion").asText()).isEqualTo("Herramienta inspeccionada y en perfecto estado");

        // 7. VERIFICACIÓN DE PERSISTENCIA REAL EN BASE DE DATOS
        Pago reembolsoPersistido = pagoRepository.findById(reembolsoId).orElseThrow();
        assertThat(reembolsoPersistido.getEstado()).isEqualTo(PagoEstado.FALLIDO);
        assertThat(reembolsoPersistido.getMotivo()).contains("Herramienta inspeccionada y en perfecto estado");

        // 8. VERIFICAR QUE EL PAGO ORIGINAL SIGUE INTACTO (COMPLETADO)
        Pago pagoOriginalPersistido = pagoRepository.findById(pagoOriginalId).orElseThrow();
        assertThat(pagoOriginalPersistido.getEstado()).isEqualTo(PagoEstado.COMPLETADO);
        assertThat(pagoOriginalPersistido.getMonto().getMonto()).isEqualByComparingTo(new BigDecimal("119000"));

        // 9. SEC-02A: DEMOSTRAR QUE EL REEMBOLSO RECHAZADO NO CONSUME SALDO DISPONIBLE
        // El cliente solicita un nuevo reembolso por el importe total y debe ser aceptado
        HttpResponse<String> cliNuevoReembolsoResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cliToken)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "reservaId", reservaId.toString(),
                                "montoConIva", 119000,
                                "motivo", "Nueva solicitud tras subsanar observaciones"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(cliNuevoReembolsoResp.statusCode()).isEqualTo(200);
        JsonNode nuevoReembolsoJson = objectMapper.readTree(cliNuevoReembolsoResp.body());
        assertThat(nuevoReembolsoJson.get("estado").asText()).isEqualTo("PENDIENTE");

        // Intento 3: Intentar rechazar o aprobar un reembolso que ya fue resuelto -> DEBE FALLAR
        HttpResponse<String> reintentoResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + reembolsoId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + propToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", true, "resolucion", "Reintento de aprobar"))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(reintentoResp.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
