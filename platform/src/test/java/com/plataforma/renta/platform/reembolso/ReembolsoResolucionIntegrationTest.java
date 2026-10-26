package com.plataforma.renta.platform.reembolso;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataforma.renta.domain.model.*;
import com.plataforma.renta.domain.port.out.*;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.domain.value.Email;
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
    private FacturaRepository facturaRepository;

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
                propId, propUser.getEmail(), "Maquinaria SAS",
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

        // Intento 1: Cliente intenta resolver su propio reembolso -> DEBE SER RECHAZADO CON 403 FORBIDDEN POR SEGURIDAD HTTP
        HttpResponse<String> cliResolverResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + reembolsoId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cliToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", false, "resolucion", "Auto-rechazo"))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(cliResolverResp.statusCode()).isEqualTo(403);

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

    @Test
    @DisplayName("SEC-02B: Resolución de reembolsos sin token o con token inválido retorna 401 Unauthorized")
    void resolucionSinTokenOConTokenInvalidoRetorna401() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        String baseUrl = "http://localhost:" + puerto;
        HttpClient client = HttpClient.newHttpClient();
        UUID fakeId = UUID.randomUUID();

        // 1. Sin cabecera Authorization -> 401 Unauthorized
        HttpResponse<String> sinAuthResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + fakeId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", true))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(sinAuthResp.statusCode()).isEqualTo(401);

        // 2. Con token malformado/inválido -> 401 Unauthorized
        HttpResponse<String> invalidTokenResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + fakeId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer token-invalido-o-expirado")
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", true))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(invalidTokenResp.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-02C: Propietario ajeno recibe 400 Bad Request y Administrador aprueba legítimamente (200 OK)")
    void propietarioAjenoRecibe400YAdminPuedeAprobar() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        String baseUrl = "http://localhost:" + puerto;
        HttpClient client = HttpClient.newHttpClient();

        // 1. Crear Propietario A (titular de la herramienta de la reserva)
        String propAEmail = "prop.a." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String propAPass = "PropPass123!";
        UUID propAId = UUID.randomUUID();
        Usuario propAUser = new Usuario(
                propAId, Email.of(propAEmail), passwordEncoder.encode(propAPass),
                UserRol.PROPIETARIO, "Carlos", "PropietarioA", "+57300111", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(propAUser);
        propietarioRepository.save(new Propietario(
                propAId, propAUser.getEmail(), "Maquinaria A",
                "Calle 1", "+57300111", "COMUN", "Bogota", "Cundinamarca", null
        ));

        // 2. Crear Propietario B (ajeno: posee otra herramienta no vinculada a la reserva)
        String propBEmail = "prop.b." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String propBPass = "PropPass123!";
        UUID propBId = UUID.randomUUID();
        Usuario propBUser = new Usuario(
                propBId, Email.of(propBEmail), passwordEncoder.encode(propBPass),
                UserRol.PROPIETARIO, "Bernardo", "PropietarioB", "+57300333", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(propBUser);
        propietarioRepository.save(new Propietario(
                propBId, propBUser.getEmail(), "Maquinaria B",
                "Calle 3", "+57300333", "COMUN", "Bogota", "Cundinamarca", null
        ));

        // 3. Crear Administrador
        String adminEmail = "admin.reemb." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String adminPass = "AdminPass123!";
        UUID adminId = UUID.randomUUID();
        Usuario adminUser = new Usuario(
                adminId, Email.of(adminEmail), passwordEncoder.encode(adminPass),
                UserRol.ADMIN, "Super", "Admin", "+57300999", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(adminUser);

        // 4. Crear Cliente
        String cliEmail = "cli.test." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String cliPass = "CliPass123!";
        UUID cliId = UUID.randomUUID();
        Usuario cliUser = new Usuario(
                cliId, Email.of(cliEmail), passwordEncoder.encode(cliPass),
                UserRol.CLIENTE, "Diego", "Cliente", "+57300444", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(cliUser);
        clienteRepository.save(new Cliente(
                cliId, cliUser.getEmail(), "Diego", "Cliente", "+57300444", "Calle 4", "{}"
        ));

        // 5. Herramienta de Propietario A
        Categoria cat = categoriaRepository.findAll().stream().findFirst().orElseThrow();
        UUID herrAId = UUID.randomUUID();
        herramientaRepository.save(new Herramienta(
                herrAId, cat.getId(), propAId,
                "Andamio Tubular A", "Desc", "Layher", "AND-01",
                Dinero.cop(new BigDecimal("70000")), 3, HerramientaPubEstado.PUBLICADA,
                new BigDecimal("19.00"), true, Instant.now(), Instant.now()
        ));

        // 6. Reserva confirmada y pagada para Herramienta de Propietario A
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = new Reserva(
                reservaId, cliId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herrAId, 1, Dinero.cop(new BigDecimal("70000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));
        reservaRepository.save(reserva);

        UUID pagoOriginalId = UUID.randomUUID();
        pagoRepository.save(new Pago(
                pagoOriginalId, reservaId, null, PagoEstado.COMPLETADO,
                "SIMULADO", "TX-AJENO-123", Dinero.cop(new BigDecimal("83300")),
                "Pago alquiler andamio", Instant.now(), Instant.now()
        ));

        facturaRepository.save(Factura.original(
                reservaId, "F-TEST-AJENO-01",
                Dinero.cop(new BigDecimal("70000")),
                Dinero.cop(new BigDecimal("13300")),
                Dinero.cop(new BigDecimal("83300")),
                new BigDecimal("19.00"), true,
                "{\"emisor\":\"Propietario A\"}", "{\"receptor\":\"Diego Cliente\"}"
        ));

        UUID reembolsoId = UUID.randomUUID();
        pagoRepository.save(new Pago(
                reembolsoId, reservaId, pagoOriginalId, PagoEstado.PENDIENTE,
                "SIMULADO", null, Dinero.cop(new BigDecimal("83300")),
                "Solicitud de reembolso por cancelación", Instant.now(), Instant.now()
        ));

        // Logins
        HttpResponse<String> propBLoginResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", propBEmail, "password", propBPass))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String propBToken = objectMapper.readTree(propBLoginResp.body()).get("token").asText();

        HttpResponse<String> adminLoginResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", adminEmail, "password", adminPass))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String adminToken = objectMapper.readTree(adminLoginResp.body()).get("token").asText();

        // 7. Intento Propietario B (ajeno): Pasa HTTP @PreAuthorize pero falla en servicio -> 400 Bad Request
        HttpResponse<String> propBAjenoResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + reembolsoId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + propBToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", true))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(propBAjenoResp.statusCode()).isEqualTo(400);
        assertThat(propBAjenoResp.body()).contains("No autorizado: solo el propietario relacionado o un administrador");

        // El reembolso debe seguir PENDIENTE
        assertThat(pagoRepository.findById(reembolsoId).orElseThrow().getEstado()).isEqualTo(PagoEstado.PENDIENTE);

        // 8. Intento Administrador: Pasa HTTP @PreAuthorize y autorización de servicio -> 200 OK
        HttpResponse<String> adminResolverResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reembolsos/" + reembolsoId + "/resolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + adminToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("aprobar", true))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(adminResolverResp.statusCode()).isEqualTo(200);

        JsonNode adminRespJson = objectMapper.readTree(adminResolverResp.body());
        assertThat(adminRespJson.get("estado").asText()).isEqualTo("APROBADO");

        // 9. Verificar persistencia y nota de crédito emitida
        Pago reembolsoAprobado = pagoRepository.findById(reembolsoId).orElseThrow();
        assertThat(reembolsoAprobado.getEstado()).isEqualTo(PagoEstado.REEMBOLSADO);

        List<Factura> facturasReserva = facturaRepository.findByReservaId(reservaId);
        boolean notaCreditoExiste = facturasReserva.stream()
                .anyMatch(f -> f.getTipo() == FacturaTipo.NOTA_CREDITO);
        assertThat(notaCreditoExiste).isTrue();
    }
}
