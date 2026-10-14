package com.plataforma.renta.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reglas arquitectonicas del proyecto ejecutadas con el API core de ArchUnit.
 *
 * <p>Se usa el API core (sin el engine JUnit5 de ArchUnit) porque el engine 1.5.1 depende
 * de junit-platform 1.14.x y Spring Boot 4.1 administra JUnit 6; el API core no tiene
 * esa dependencia y es estable respecto a la version de JUnit.</p>
 *
 * <p>Las reglas sobre {@code domain}, {@code application} y entidades JPA usan
 * {@code allowEmptyShould(true)} porque en el Sprint 0 esos modulos aun no contienen
 * clases de negocio: sin esa opcion, ArchUnit 1.5.1 fallaria toda regla que no
 * matchee ninguna clase. Las reglas son guardianes activos: se aplicaran a cada clase
 * en cuanto existan. La presencia de los modulos se verifica en {@link PresenciaModulosTest}
 * y la cobertura del importador en {@link #importaLasClasesRealesDelProyecto()}.</p>
 */
class ReglasArquitecturaTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.plataforma.renta");

    @Test
    @DisplayName("Las clases reales del proyecto se importan para las reglas")
    void importaLasClasesRealesDelProyecto() {
        assertThat(CLASES)
                .extracting(JavaClass::getSimpleName)
                .contains("PlataformaRentaApplication", "OpenApiConfig");
    }

    @Test
    @DisplayName("domain no depende de Spring, JPA, HTTP ni infraestructura")
    void domainSoloDependeDeJavaYDeSiMismo() {
        ArchRule regla = classes()
                .that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "java..", "javax..", "jdk..")
                .allowEmptyShould(true);
        regla.check(CLASES);
    }

    @Test
    @DisplayName("application solo depende de domain, jakarta.transaction, spring.stereotype y Java")
    void applicationSoloDependeDeDomain() {
        ArchRule regla = classes()
                .that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..application..", "..domain..", "jakarta.transaction..", "org.springframework.stereotype..", "java..", "javax..", "jdk..")
                .allowEmptyShould(true);
        regla.check(CLASES);
    }

    @Test
    @DisplayName("application no depende de api ni de infrastructure")
    void applicationNoDependeDeCapasExternas() {
        ArchRule regla = classes()
                .that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideOutsideOfPackages("..api..", "..infrastructure..")
                .allowEmptyShould(true);
        regla.check(CLASES);
    }

    @Test
    @DisplayName("api no depende de infrastructure")
    void apiNoDependeDeInfraestructura() {
        ArchRule regla = classes()
                .that().resideInAPackage("..api..")
                .should().onlyDependOnClassesThat()
                .resideOutsideOfPackages("..infrastructure..");
        regla.check(CLASES);
    }

    @Test
    @DisplayName("Las entidades JPA solo existen en infrastructure")
    void entidadesJpaSoloEnInfraestructura() {
        ArchRule regla = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..infrastructure..")
                .allowEmptyShould(true);
        regla.check(CLASES);
    }

    @Test
    @DisplayName("No hay dependencias ciclicas entre los modulos")
    void noHayCiclosEntreModulos() {
        slices()
                .matching("com.plataforma.renta.(*)..")
                .should().beFreeOfCycles()
                .check(CLASES);
    }
}
