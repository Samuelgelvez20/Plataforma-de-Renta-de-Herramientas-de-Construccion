package com.plataforma.renta.api.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definicion del documento OpenAPI base de la plataforma.
 *
 * <p>springdoc expone {@code /v3/api-docs} y la UI de Swagger en {@code /swagger-ui.html}
 * automaticamente; esta configuracion aporta los metadatos del documento.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI plataformaRentaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Plataforma de Renta de Herramientas y Equipos de Construccion")
                .description("API REST de la plataforma de renta de herramientas y equipos de construccion")
                .version("0.1.0-SNAPSHOT"));
    }
}
