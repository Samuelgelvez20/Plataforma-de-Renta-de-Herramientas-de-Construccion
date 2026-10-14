package com.plataforma.renta.platform;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Prueba de integracion del arranque real de la aplicacion contra PostgreSQL (Testcontainers).
 *
 * <p>Verifica: composicion del contexto Spring, lectura de la configuracion, conexion a
 * PostgreSQL real y que la seguridad por defecto protege los endpoints.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicacionContextoTest {

    private static final DockerImageName IMAGEN_POSTGRES = DockerImageName.parse("postgres:16-alpine");

    static PostgreSQLContainer postgres;

    @BeforeAll
    static void levantarPostgres() {
        postgres = new PostgreSQLContainer(IMAGEN_POSTGRES);
        postgres.start();
    }

    @BeforeAll
    static void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void detenerPostgres() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("El contexto Spring arranca con una base PostgreSQL real")
    void elContextoSeIniciaConPostgresqlReal() {
        assertThat(context.isActive()).isTrue();
        assertThat(context.getBean(PlataformaRentaApplication.class)).isNotNull();
    }

    @Test
    @DisplayName("El bean OpenAPI definido en el modulo api se compone en la aplicacion")
    void elBeanOpenApiDelModuloApiSeCompone() {
        assertThat(context.getBeansOfType(OpenAPI.class)).hasSize(1);
    }

    @Test
    @DisplayName("Los endpoints quedan protegidos por la seguridad por defecto hasta definir SecurityConfig (Sprint 1)")
    void losEndpointsEstanProtegidosPorDefecto() throws Exception {
        int puerto = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        // Spring Security devuelve 403 (Forbidden) para acceso no autenticado a endpoints protegidos
        // 401 (Unauthorized) se usa cuando hay challenge de autenticación (ej. WWW-Authenticate header)
        assertThat(respuesta.statusCode()).isIn(401, 403);
    }
}
