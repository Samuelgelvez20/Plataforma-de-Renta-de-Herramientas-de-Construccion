package com.plataforma.renta.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.plataforma.renta.platform.PlataformaRentaApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LegacyAuthRouteRemovedSecurityTest {

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

    @Test
    @DisplayName("SEC-01: Rutas legacy /api/auth/** no estan expuestas publicamente ni registradas")
    void rutaLegacyNoEstaExpuestaPublicamente() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpClient client = HttpClient.newHttpClient();

        // Intento de llamar a endpoint legacy /api/auth/login
        HttpRequest legacyLoginReq = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"test@test.com\",\"password\":\"123456\"}"))
                .build();

        HttpResponse<String> respLegacyLogin = client.send(legacyLoginReq, HttpResponse.BodyHandlers.ofString());
        // Al no estar en permitAll y no haber controlador, Spring Security lo bloquea (401/403)
        // o si pasara daria 404, pero NUNCA 200
        assertThat(respLegacyLogin.statusCode()).isIn(401, 403, 404);

        // Intento de llamar a endpoint legacy /api/auth/register
        HttpRequest legacyRegReq = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"test@test.com\",\"password\":\"123456\",\"rol\":\"CLIENTE\"}"))
                .build();

        HttpResponse<String> respLegacyReg = client.send(legacyRegReq, HttpResponse.BodyHandlers.ofString());
        assertThat(respLegacyReg.statusCode()).isIn(401, 403, 404);

        // Ruta oficial /api/v1/auth/login sí está registrada y responde públicamente
        HttpRequest officialLoginReq = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"noexiste@test.com\",\"password\":\"123456\"}"))
                .build();

        HttpResponse<String> respOfficialLogin = client.send(officialLoginReq, HttpResponse.BodyHandlers.ofString());
        // Responde del controlador oficial (400 Bad Request o 401 por credenciales invalidas, pero NO 404 ni 403 de ruta protegida)
        assertThat(respOfficialLogin.statusCode()).isIn(400, 401);
    }
}
