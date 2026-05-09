package com.qa.common.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
}
