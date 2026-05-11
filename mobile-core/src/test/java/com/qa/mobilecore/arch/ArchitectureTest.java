package com.qa.mobilecore.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

/**
 * Tests de arquitectura para {@code mobile-core} (TASK-H01).
 * Reglas reales en TASK-H02 / H03.
 *
 * @since TASK-H01
 */
@AnalyzeClasses(
        packages = "com.qa.mobilecore",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /**
     * TASK-H02 — {@code mobile-core} no puede importar a sus pares.
     */
    @ArchTest
    static final ArchRule mobilecore_does_not_depend_on_siblings = noClasses()
            .that().resideInAPackage("com.qa.mobilecore..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.qa.httpcore..",
                    "com.qa.webcore..",
                    "com.qa.databasecore..")
            .as("mobile-core no debe importar http-core, web-core ni database-core");

    /** TASK-K02 — whitelist de internals legítimos. */
    private static final Set<String> K02_INTERNAL_WHITELIST = Set.of(
            "com.qa.common.internal.config.ConfigManager"
    );

    @ArchTest
    static final ArchRule mobilecore_does_not_import_common_internal_except_whitelist = noClasses()
            .that().resideInAPackage("com.qa.mobilecore..")
            .should().dependOnClassesThat(new DescribedPredicate<JavaClass>(
                    "com.qa.common.internal.* (no whitelisted TASK-K02)") {
                @Override public boolean test(JavaClass jc) {
                    String n = jc.getFullName();
                    if (!n.startsWith("com.qa.common.internal.")) return false;
                    int d = n.indexOf("$");
                    String tl = d > 0 ? n.substring(0, d) : n;
                    return !K02_INTERNAL_WHITELIST.contains(tl);
                }
            })
            .as("mobile-core no debe importar com.qa.common.internal.* (R-K02-2). "
                + "Excepciones aprobadas en whitelist con plan de salida.");
}
