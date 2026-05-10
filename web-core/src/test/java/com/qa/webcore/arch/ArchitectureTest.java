package com.qa.webcore.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

/**
 * Tests de arquitectura para {@code web-core} (TASK-H01).
 * Reglas reales en TASK-H02 / H03.
 *
 * @since TASK-H01
 */
@AnalyzeClasses(
        packages = "com.qa.webcore",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /**
     * TASK-H02 — {@code web-core} no puede importar a sus pares.
     */
    @ArchTest
    static final ArchRule webcore_does_not_depend_on_siblings = noClasses()
            .that().resideInAPackage("com.qa.webcore..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.qa.httpcore..",
                    "com.qa.mobilecore..",
                    "com.qa.databasecore..")
            .as("web-core no debe importar http-core, mobile-core ni database-core");
}
