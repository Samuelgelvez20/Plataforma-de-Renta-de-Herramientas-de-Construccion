package com.plataforma.renta.platform.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.platform.PlataformaRentaApplication;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlataformaRentaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReporteControllerIntegrationTest {

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
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private int getPort() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    @DisplayName("Endpoint /api/v1/reportes/dashboard requiere rol ADMIN y rechaza CLIENTE")
    void testDashboardSeguridadPorRol() throws Exception {
        int port = getPort();
        String baseUrl = "http://localhost:" + port;

        // 1. Acceso anónimo denegado
        HttpResponse<String> anonResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reportes/dashboard")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(anonResp.statusCode()).isIn(401, 403);

        // 2. Registrar cliente
        String cliEmail = "cli.rep." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String cliPass = "Pass123!";
        usuarioRepository.save(new Usuario(
                UUID.randomUUID(), Email.of(cliEmail), passwordEncoder.encode(cliPass),
                UserRol.CLIENTE, "Cli", "Rep", "+57300", true, Instant.now(), Instant.now()
        ));

        // Login cliente
        HttpResponse<String> cliLogin = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", cliEmail, "password", cliPass))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String cliToken = objectMapper.readTree(cliLogin.body()).get("token").asText();

        // Cliente intenta acceder a /api/v1/reportes/dashboard -> 403 FORBIDDEN
        HttpResponse<String> cliResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reportes/dashboard"))
                        .header("Authorization", "Bearer " + cliToken)
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(cliResp.statusCode()).isEqualTo(403);

        // 3. Crear ADMIN y acceder al dashboard -> 200 OK
        String adminEmail = "admin.rep." + UUID.randomUUID().toString().substring(0, 8) + "@renta.com";
        String adminPass = "AdminPass123!";
        usuarioRepository.save(new Usuario(
                UUID.randomUUID(), Email.of(adminEmail), passwordEncoder.encode(adminPass),
                UserRol.ADMIN, "Admin", "Rep", "+57300", true, Instant.now(), Instant.now()
        ));

        HttpResponse<String> adminLogin = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("email", adminEmail, "password", adminPass))))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String adminToken = objectMapper.readTree(adminLogin.body()).get("token").asText();

        HttpResponse<String> adminResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reportes/dashboard"))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(adminResp.statusCode()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(adminResp.body());
        assertThat(body.has("resumen")).isTrue();
        assertThat(body.has("disponibilidadInventario")).isTrue();
        assertThat(body.has("masAlquiladas")).isTrue();
        assertThat(body.has("clientesFrecuentes")).isTrue();
        assertThat(body.has("ingresosMensuales")).isTrue();
    }
}
