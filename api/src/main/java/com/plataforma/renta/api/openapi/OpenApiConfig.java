package com.plataforma.renta.api.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definicion del documento OpenAPI base de la plataforma.
 *
 * <p>springdoc expone {@code /v3/api-docs} y la UI de Swagger en {@code /swagger-ui.html}
 * automaticamente; esta configuracion aporta los metadatos del documento y la configuracion
 * del esquema de autorizacion JWT Bearer para Swagger UI.</p>
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI plataformaRentaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma de Renta de Herramientas y Equipos de Construccion")
                        .description("API REST de la plataforma de renta de herramientas y equipos de construccion")
                        .version("0.1.0-SNAPSHOT"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido en /api/v1/auth/login. Swagger antepone automáticamente 'Bearer '.")));
    }
}
