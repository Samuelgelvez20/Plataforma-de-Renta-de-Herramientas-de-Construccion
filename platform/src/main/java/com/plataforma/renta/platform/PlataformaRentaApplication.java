package com.plataforma.renta.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Punto de entrada y composicion de la aplicacion.
 *
 * <p>El escaneo de componentes cubre el paquete raiz {@code com.plataforma.renta} para que
 * los modulos {@code api} e {@code infrastructure} aporten sus clases de configuracion sin
 * que {@code domain} ni {@code application} dependan de Spring.</p>
 */
@SpringBootApplication(scanBasePackages = "com.plataforma.renta")
@EntityScan(basePackages = "com.plataforma.renta.infrastructure.persistence.entity")
@ComponentScan(basePackages = {
    "com.plataforma.renta.application",
    "com.plataforma.renta.api",
    "com.plataforma.renta.infrastructure.persistence.adapter",
    "com.plataforma.renta.infrastructure.persistence.mapper",
    "com.plataforma.renta.infrastructure.config",
    "com.plataforma.renta.infrastructure.pdf",
    "com.plataforma.renta.platform.security"
})
public class PlataformaRentaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlataformaRentaApplication.class, args);
    }
}
