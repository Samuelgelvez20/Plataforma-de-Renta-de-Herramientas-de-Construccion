package com.plataforma.renta.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica que los cinco modulos Maven existen fisicamente y que la configuracion
 * de Flyway apunta a la ubicacion canonica acordada.
 *
 * <p>El directorio de trabajo de Surefire es la raiz del modulo {@code platform},
 * de ahi la referencia relativa {@code ..} hacia los modulos hermanos.</p>
 */
class PresenciaModulosTest {

    @Test
    @DisplayName("Existen los cinco modulos Maven con su pom")
    void existenLosCincoModulos() {
        for (String modulo : new String[] {"domain", "application", "infrastructure", "api", "platform"}) {
            Path pom = Path.of("..", modulo, "pom.xml");
            assertThat(pom).as("pom.xml del modulo %s", modulo).exists();
        }
    }

    @Test
    @DisplayName("La configuracion Flyway apunta a classpath:db/migration")
    void laConfiguracionFlywayApuntaALaUbicacionCanonica() throws Exception {
        Path yml = Path.of("src", "main", "resources", "application.yml");
        assertThat(yml).exists();
        assertThat(Files.readString(yml)).contains("classpath:db/migration");
    }
}
