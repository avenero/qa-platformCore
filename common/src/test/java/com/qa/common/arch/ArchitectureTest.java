package com.qa.common.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

/**
 * Tests de arquitectura para el módulo {@code common} (TASK-H01).
 *
 * <p>Reglas adicionales se añadirán en:
 * <ul>
 *   <li>TASK-H02 — módulos especializados no se cruzan entre sí.</li>
 *   <li>TASK-H03 — {@code common} no importa módulos especializados.</li>
 *   <li>TASK-H04 — el Backend sólo importa {@code common} del Core.</li>
 * </ul>
 *
 * @since TASK-H01
 */
@AnalyzeClasses(
        packages = "com.qa.common",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    /**
     * Estándar de logging del Core: SLF4J + {@code TestLogger}. Nada de
     * {@code java.util.logging} (JUL) — su API global es difícil de configurar
     * de forma consistente y no se integra con MDC.
     */
    @ArchTest
    static final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    /**
     * Producción nunca debe escribir a {@code System.out}/{@code System.err}.
     * Si necesitás imprimir, usa el logger.
     */
    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /**
     * TASK-H03 — {@code common} es la capa base. NUNCA puede depender de los módulos
     * especializados (eso invertiría el grafo y rompería la regla inviolable de
     * Parte 1 §2). Esta regla protege la frontera más importante del Core.
     */
    @ArchTest
    static final ArchRule common_does_not_depend_on_specialized_modules = noClasses()
            .that().resideInAPackage("com.qa.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.qa.httpcore..",
                    "com.qa.webcore..",
                    "com.qa.mobilecore..",
                    "com.qa.databasecore..")
            .as("common no debe depender de http-core, web-core, mobile-core ni database-core");

    /**
     * TASK-H03 — {@code common} no puede depender de frameworks de UI/automatización.
     * Estas dependencias pertenecen a los módulos especializados que las encapsulan
     * detrás de contratos en {@code common}.
     */
    @ArchTest
    static final ArchRule common_does_not_depend_on_automation_frameworks = noClasses()
            .that().resideInAPackage("com.qa.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.microsoft.playwright..",
                    "io.appium..",
                    "org.openqa.selenium..",
                    "org.apache.hc..")
            .as("common no debe depender de Playwright, Appium, Selenium ni Apache HttpClient");
}
