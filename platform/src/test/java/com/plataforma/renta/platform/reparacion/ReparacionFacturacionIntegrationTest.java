package com.plataforma.renta.platform.reparacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataforma.renta.domain.model.*;
import com.plataforma.renta.domain.port.out.*;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.domain.value.Email;
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

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReparacionFacturacionIntegrationTest {

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
    @DisplayName("Flujo completo de facturacion de danos y reparaciones: autorizacion, idempotencia, consulta, IDOR y liquidacion")
    void flujoCompletoFacturacionDanosYReparaciones() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        String baseUrl = "http://localhost:" + puerto;
        HttpClient client = HttpClient.newHttpClient();

        // 1. Propietario Autorizado (Owner A)
        String propEmail = "prop.autorizado." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String propPass = "PropPass123!";
        UUID propId = UUID.randomUUID();
        Usuario propUser = new Usuario(
                propId, Email.of(propEmail), passwordEncoder.encode(propPass),
                UserRol.PROPIETARIO, "Carlos", "Propietario", "+57300111", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(propUser);
        propietarioRepository.save(new Propietario(
                propId, propUser.getEmail(), "Equipos Bogota SAS",
                "Calle 10 # 20", "+57300111", "COMUN", "Bogota", "Cundinamarca", null
        ));

        // 2. Propietario Ajeno No Autorizado (Owner B)
        String propBAliase = "prop.ajeno." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        UUID propBId = UUID.randomUUID();
        Usuario propBUser = new Usuario(
                propBId, Email.of(propBAliase), passwordEncoder.encode(propPass),
                UserRol.PROPIETARIO, "Rival", "Propietario", "+57300333", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(propBUser);
        propietarioRepository.save(new Propietario(
                propBId, propBUser.getEmail(), "Equipos Ajeno SAS",
                "Calle 99", "+57300333", "COMUN", "Bogota", "Cundinamarca", null
        ));

        // 3. Cliente Titular (Client A)
        String cliEmail = "cli.titular." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String cliPass = "CliPass123!";
        UUID cliId = UUID.randomUUID();
        Usuario cliUser = new Usuario(
                cliId, Email.of(cliEmail), passwordEncoder.encode(cliPass),
                UserRol.CLIENTE, "Juan", "Titular", "+57300222", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(cliUser);
        clienteRepository.save(new Cliente(
                cliId, cliUser.getEmail(), "Juan", "Titular", "+57300222", "Calle 50 # 10", "{}"
        ));

        // 4. Cliente Atacante/Ajeno (Client B)
        String cliBEmail = "cli.ajeno." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        UUID cliBId = UUID.randomUUID();
        Usuario cliBUser = new Usuario(
                cliBId, Email.of(cliBEmail), passwordEncoder.encode(cliPass),
                UserRol.CLIENTE, "Pedro", "Atacante", "+57300444", true, Instant.now(), Instant.now()
        );
        usuarioRepository.save(cliBUser);
        clienteRepository.save(new Cliente(
                cliBId, cliBUser.getEmail(), "Pedro", "Atacante", "+57300444", "Calle 80", "{}"
        ));

        // 5. Herramienta registrada por Propietario Autorizado
        Categoria cat = categoriaRepository.findAll().stream().findFirst().orElseThrow();
        UUID herramientaId = UUID.randomUUID();
        Herramienta herramienta = new Herramienta(
                herramientaId, cat.getId(), propId,
                "Demoledor Hidráulico 1500W", "Equipo para demolición pesada", "DeWalt", "D25901K",
                Dinero.cop(new BigDecimal("70000")), 5, HerramientaPubEstado.PUBLICADA,
                new BigDecimal("19.00"), true, Instant.now(), Instant.now()
        );
        herramientaRepository.save(herramienta);

        // 6. Reserva para Cliente Titular en estado EN_CURSO (equipo entregado)
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = new Reserva(
                reservaId, cliId, ReservaEstado.EN_CURSO,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1, Dinero.cop(new BigDecimal("70000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                new BigDecimal("19.00"), true
        ));
        reservaRepository.save(reserva);

        // Pago inicial del alquiler
        UUID pagoAlquilerId = UUID.randomUUID();
        Pago pagoAlquiler = new Pago(
                pagoAlquilerId, reservaId, null, PagoEstado.COMPLETADO,
                "SIMULADO", "TX-ALQUILER-1", Dinero.cop(new BigDecimal("166600")),
                "Pago alquiler inicial", Instant.now(), Instant.now()
        );
        pagoRepository.save(pagoAlquiler);

        // Factura normal inicial de alquiler
        Factura facturaAlquiler = Factura.original(
                reservaId, "F-001001", Dinero.cop(new BigDecimal("140000")),
                Dinero.cop(new BigDecimal("26600")), Dinero.cop(new BigDecimal("166600")),
                new BigDecimal("19.00"), false, "{}", "{}"
        );
        facturaRepository.save(facturaAlquiler);

        // Login tokens
        String propToken = login(client, baseUrl, propEmail, propPass);
        String propBToken = login(client, baseUrl, propBAliase, propPass);
        String cliToken = login(client, baseUrl, cliEmail, cliPass);
        String cliBToken = login(client, baseUrl, cliBEmail, cliPass);

        // Paso 1: Propietario no autorizado intenta registrar daño sobre reserva ajena -> 400 Bad Request
        HttpResponse<String> respPropB = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reservas/" + reservaId + "/devolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + propBToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "aceptada", true,
                                "presentaDanos", true,
                                "descripcionDano", "Cincel roto",
                                "costoReparacion", 95000,
                                "observaciones", "Intento no autorizado"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respPropB.statusCode()).isEqualTo(400);

        // Paso 2: Propietario autorizado confirma devolución con daño y costo de reparación -> 200 OK
        HttpResponse<String> respPropA = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reservas/" + reservaId + "/devolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + propToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "aceptada", true,
                                "presentaDanos", true,
                                "descripcionDano", "Mandril de sujeción fracturado y cable cortado",
                                "costoReparacion", 120000,
                                "observaciones", "Requiere sustitución de mandril original"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respPropA.statusCode()).isEqualTo(200);

        // Paso 3: Idempotencia - Repetir la devolución con daño no debe duplicar la factura
        HttpResponse<String> respPropARepeat = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reservas/" + reservaId + "/devolver"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + propToken)
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "aceptada", true,
                                "presentaDanos", true,
                                "descripcionDano", "Mandril de sujeción fracturado y cable cortado",
                                "costoReparacion", 120000
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respPropARepeat.statusCode()).isEqualTo(200);

        // Paso 4: Cliente Titular consulta "Mis facturas" -> ve la factura de reparación
        HttpResponse<String> respFacturasCli = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/facturas"))
                        .header("Authorization", "Bearer " + cliToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respFacturasCli.statusCode()).isEqualTo(200);

        JsonNode facturasCli = objectMapper.readTree(respFacturasCli.body());
        assertThat(facturasCli.isArray()).isTrue();
        JsonNode facReparacion = null;
        for (JsonNode f : facturasCli) {
            if ("REPARACION".equalsIgnoreCase(f.path("tipo").asText())) {
                facReparacion = f;
                break;
            }
        }
        assertThat(facReparacion).as("Debe existir la factura de reparación para el cliente").isNotNull();
        String facturaIdStr = facReparacion.get("id").asText();
        UUID facturaReparacionId = UUID.fromString(facturaIdStr);

        assertThat(facReparacion.get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(facReparacion.get("descripcionDano").asText()).isEqualTo("Mandril de sujeción fracturado y cable cortado");
        assertThat(facReparacion.get("herramientaNombre").asText()).isEqualTo("Demoledor Hidráulico 1500W");
        assertThat(facReparacion.get("concepto").asText()).contains("Costos de reparación de Demoledor Hidráulico 1500W");
        assertThat(facReparacion.get("total").asDouble()).isEqualTo(120000.0);

        // Paso 5: Cliente Ajeno consulta sus facturas -> NO ve la factura de reparación de Cliente A
        HttpResponse<String> respFacturasCliB = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/facturas"))
                        .header("Authorization", "Bearer " + cliBToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respFacturasCliB.statusCode()).isEqualTo(200);
        JsonNode facturasCliB = objectMapper.readTree(respFacturasCliB.body());
        for (JsonNode f : facturasCliB) {
            assertThat(f.get("id").asText()).isNotEqualTo(facturaIdStr);
        }

        // Paso 6: Cliente Ajeno intenta pagar la factura de Cliente A (Prevención IDOR) -> Rechazado 400
        HttpResponse<String> respPagarIDOR = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/pagos/procesar"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cliBToken)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "facturaId", facturaIdStr,
                                "metodo", "SIMULADO"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respPagarIDOR.statusCode()).isEqualTo(400);

        // Paso 7: Cliente Titular paga su factura de reparación -> 201 Created / 200 OK
        HttpResponse<String> respPagarOK = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/pagos/procesar"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cliToken)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "facturaId", facturaIdStr,
                                "metodo", "SIMULADO"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respPagarOK.statusCode()).isIn(200, 201);
        JsonNode respPagoBody = objectMapper.readTree(respPagarOK.body());
        JsonNode pagoRepJson = respPagoBody.get("pago");
        assertThat(pagoRepJson.get("estado").asText()).isEqualTo("COMPLETADO");
        assertThat(pagoRepJson.get("monto").asDouble()).isEqualTo(120000.0);
        assertThat(respPagoBody.get("factura").get("estado").asText()).isEqualTo("PAGADA");

        // Paso 8: Comprobar que la factura cambió de estado a PAGADA en /api/v1/facturas
        HttpResponse<String> respFacturasActualizadas = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/facturas"))
                        .header("Authorization", "Bearer " + cliToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        JsonNode facturasActualizadas = objectMapper.readTree(respFacturasActualizadas.body());
        JsonNode facRepActualizada = null;
        for (JsonNode f : facturasActualizadas) {
            if (f.get("id").asText().equals(facturaIdStr)) {
                facRepActualizada = f;
                break;
            }
        }
        assertThat(facRepActualizada).isNotNull();
        assertThat(facRepActualizada.get("estado").asText()).isEqualTo("PAGADA");

        // Paso 9: Intento de doble cobro / segundo pago de la misma factura -> Manejo idempotente sin duplicar cobro
        HttpResponse<String> respSegundoPago = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/pagos/procesar"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cliToken)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                                "facturaId", facturaIdStr,
                                "metodo", "SIMULADO"
                        ))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respSegundoPago.statusCode()).isEqualTo(200);
        JsonNode segundoPagoBody = objectMapper.readTree(respSegundoPago.body());
        assertThat(segundoPagoBody.get("mensaje").asText()).contains("previamente confirmado");
        assertThat(segundoPagoBody.get("pago").get("id").asText()).isEqualTo(pagoRepJson.get("id").asText());

        // Verificar en base de datos que solo existe 1 pago completado para esta factura de reparación
        long countPagosRep = pagoRepository.findByFacturaId(facturaReparacionId).stream()
                .filter(p -> p.getEstado() == PagoEstado.COMPLETADO)
                .count();
        assertThat(countPagosRep).isEqualTo(1L);

        // Paso 10: Descarga de PDF de la factura de reparación
        HttpResponse<byte[]> respPdf = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/facturas/" + facturaIdStr + "/pdf"))
                        .header("Authorization", "Bearer " + cliToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
        assertThat(respPdf.statusCode()).isEqualTo(200);
        assertThat(respPdf.headers().firstValue("Content-Type").orElse("")).contains("application/pdf");
        byte[] pdfBytes = respPdf.body();
        assertThat(pdfBytes).isNotEmpty();
        // Verificar firma mágica de archivo PDF (%PDF)
        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertThat(pdfHeader).isEqualTo("%PDF-");
    }

    private String login(HttpClient client, String baseUrl, String email, String password) throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).get("token").asText();
    }
}
