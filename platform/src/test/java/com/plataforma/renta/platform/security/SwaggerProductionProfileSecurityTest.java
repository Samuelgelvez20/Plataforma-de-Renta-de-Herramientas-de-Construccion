package com.plataforma.renta.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

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
@ActiveProfiles({"test", "prod"})
class SwaggerProductionProfileSecurityTest {

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
    @DisplayName("SEC-03 (PROD): Las propiedades de springdoc estan deshabilitadas en perfil prod")
    void propiedadesSpringDocEstanDeshabilitadasEnProd() {
        boolean apiDocsEnabled = environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true);
        boolean swaggerUiEnabled = environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true);

        assertThat(apiDocsEnabled).isFalse();
        assertThat(swaggerUiEnabled).isFalse();
    }

    @Test
    @DisplayName("SEC-03 (PROD): /v3/api-docs y variantes responden 404 en produccion")
    void openApiNoExpuestoEnProduccion() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpClient client = HttpClient.newHttpClient();

        // 1. /v3/api-docs (especificación OpenAPI principal) - Bloqueado por Security (403/401) o no encontrado (404)
        HttpResponse<String> respApiDocs = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respApiDocs.statusCode()).isIn(401, 403, 404);

        // 2. /v3/api-docs/swagger-config (configuración de Swagger)
        HttpResponse<String> respSwaggerConfig = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/v3/api-docs/swagger-config")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respSwaggerConfig.statusCode()).isIn(401, 403, 404);

        // 3. /swagger-ui/index.html (interfaz de usuario de Swagger)
        HttpResponse<String> respSwaggerUi = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/swagger-ui/index.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respSwaggerUi.statusCode()).isIn(401, 403, 404);

        // 4. /swagger-ui.html (acceso directo a Swagger)
        HttpResponse<String> respSwaggerHtml = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/swagger-ui.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respSwaggerHtml.statusCode()).isIn(401, 403, 404);

        // 5. Las rutas de negocio continúan protegidas por autenticación
        HttpResponse<String> respNegocio = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/reservas")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(respNegocio.statusCode()).isIn(401, 403);
    }
}
