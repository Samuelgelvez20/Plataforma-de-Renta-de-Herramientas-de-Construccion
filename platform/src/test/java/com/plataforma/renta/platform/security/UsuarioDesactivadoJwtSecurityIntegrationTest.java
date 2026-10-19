package com.plataforma.renta.platform.security;

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
public class UsuarioDesactivadoJwtSecurityIntegrationTest {

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
    @DisplayName("Flujo completo: JWT de usuario desactivado es revocado de inmediato y login es rechazado")
    void testFlujoUsuarioDesactivadoJwtRevocado() throws Exception {
        int port = getPort();
        String baseUrl = "http://localhost:" + port;

        // 0. Crear un Administrador persistido en base de datos real
        UUID adminId = UUID.randomUUID();
        String adminEmail = "admin.sec." + UUID.randomUUID().toString().substring(0, 8) + "@renta.com";
        String adminPassword = "AdminPassword123!";
        Usuario admin = new Usuario(
                adminId,
                Email.of(adminEmail),
                passwordEncoder.encode(adminPassword),
                UserRol.ADMIN,
                "Admin",
                "Seguridad",
                "+573001112233",
                true,
                Instant.now(),
                Instant.now()
        );
        usuarioRepository.save(admin);

        // Login del administrador para obtener token de admin
        String adminLoginBody = objectMapper.writeValueAsString(Map.of(
                "email", adminEmail,
                "password", adminPassword
        ));
        HttpResponse<String> adminLoginResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(adminLoginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(adminLoginResp.statusCode()).isEqualTo(200);
        JsonNode adminJson = objectMapper.readTree(adminLoginResp.body());
        String adminToken = adminJson.get("token").asText();
        assertThat(adminToken).isNotBlank();

        // 1. Crear o registrar un usuario cliente activo
        String userEmail = "cliente.jwt." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String userPassword = "Password123!";
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", userEmail,
                "password", userPassword,
                "nombre", "Cliente",
                "apellido", "Prueba",
                "rol", "CLIENTE",
                "telefono", "+573001234567"
        ));

        HttpResponse<String> registerResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(registerResp.statusCode()).isEqualTo(200);
        JsonNode regJson = objectMapper.readTree(registerResp.body());
        UUID clienteId = UUID.fromString(regJson.get("usuario").get("id").asText());

        // 2. Obtener un JWT válido para el usuario cliente
        String userLoginBody = objectMapper.writeValueAsString(Map.of(
                "email", userEmail,
                "password", userPassword
        ));
        HttpResponse<String> userLoginResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(userLoginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(userLoginResp.statusCode()).isEqualTo(200);
        JsonNode userJson = objectMapper.readTree(userLoginResp.body());
        String userToken = userJson.get("token").asText();
        assertThat(userToken).isNotBlank();

        // 3. Acceder con ese token a un endpoint protegido (/api/v1/reservas) y comprobar que funciona (200 OK)
        HttpResponse<String> protectedResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reservas"))
                        .header("Authorization", "Bearer " + userToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(protectedResp.statusCode()).isEqualTo(200);

        // 4. Desactivar al usuario mediante el endpoint administrativo de admin
        HttpResponse<String> toggleResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/admin/usuarios/" + clienteId + "/toggle-activo"))
                        .header("Authorization", "Bearer " + adminToken)
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(toggleResp.statusCode()).isEqualTo(200);
        JsonNode toggleJson = objectMapper.readTree(toggleResp.body());
        assertThat(toggleJson.get("activo").asBoolean()).isFalse();

        // Comprobar persistencia real en PostgreSQL
        Usuario clienteEnDb = usuarioRepository.findById(clienteId).orElseThrow();
        assertThat(clienteEnDb.isActivo()).isFalse();

        // 5 & 6. Reutilizar el MISMO token, todavía no expirado, y verificar que ya no obtiene acceso autenticado (401 Unauthorized)
        HttpResponse<String> rejectedResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reservas"))
                        .header("Authorization", "Bearer " + userToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(rejectedResp.statusCode()).isEqualTo(401);
        assertThat(rejectedResp.body()).contains("CUENTA_DESHABILITADA");

        // 7. Verificar que el administrador activo conserva el acceso
        HttpResponse<String> adminAccessResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/admin/usuarios"))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(adminAccessResp.statusCode()).isEqualTo(200);

        // 8. Verificar que el inicio de sesión de un usuario desactivado continúa rechazándose
        HttpResponse<String> disabledLoginResp = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(userLoginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        // Debe ser rechazado (401 o 400) y no permitir obtener un token
        assertThat(disabledLoginResp.statusCode()).isIn(401, 400);
        assertThat(disabledLoginResp.body()).contains("suspendida o inactiva");
    }
}
