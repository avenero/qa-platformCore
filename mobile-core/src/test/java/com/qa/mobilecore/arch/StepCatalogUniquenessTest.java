package com.qa.mobilecore.arch;

import com.qa.common.api.runtime.annotation.StepDef;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard de build (W0-S1): ningún par de anotaciones {@link StepDef} dentro de
 * {@code mobile-core} puede compartir el mismo id canónico.
 *
 * <p>La doble-registración de un id está prohibida (ver javadoc de {@link StepDef}
 * y regla de Core §5.1): el {@code value()} de {@code @StepDef} es un contrato
 * público que el Backend persiste y el Frontend usa como clave en el Scenario
 * Builder, por lo que dos métodos con el mismo id colisionan en el catálogo. Este
 * test previene regresiones de la clase de duplicados corregida en W1-T2-A12.
 *
 * <p>Usa ArchUnit para leer el bytecode del paquete de steps: nunca inicializa las
 * clases, evitando cargar drivers nativos (Playwright/Appium) o dependencias de IO.
 *
 * @since W0-S1
 * @see StepDef
 */
class StepCatalogUniquenessTest {

    /** Raíz de paquete de los steps de este módulo (sólo {@code src/main}). */
    private static final String STEPS_PACKAGE = "com.qa.mobilecore.steps";

    @Test
    @DisplayName("ningún id @StepDef se declara dos veces en el paquete de steps")
    void stepCatalogIdsAreUnique() {
        JavaClasses stepClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(STEPS_PACKAGE);

        Map<String, JavaMethod> seen = new HashMap<>();
        List<String> duplicates = new ArrayList<>();

        for (JavaClass stepClass : stepClasses) {
            for (JavaMethod method : stepClass.getMethods()) {
                if (!method.isAnnotatedWith(StepDef.class)) {
                    continue;
                }
                String id = method.getAnnotationOfType(StepDef.class).value();
                JavaMethod previous = seen.putIfAbsent(id, method);
                if (previous != null) {
                    duplicates.add(String.format(
                            "Duplicate @StepDef(\"%s\") at %s and %s",
                            id, method.getFullName(), previous.getFullName()));
                }
            }
        }

        assertThat(duplicates)
                .as("Cada id @StepDef debe declararse exactamente una vez en %s "
                        + "(doble-registración prohibida; ver javadoc de StepDef).", STEPS_PACKAGE)
                .isEmpty();
    }
}
