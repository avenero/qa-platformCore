package com.qa.httpcore.arch;

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
 * Tests de arquitectura para el módulo {@code http-core} (TASK-H01).
 *
 * <p>Reglas reales en TASK-H02 (no cruza con web/mobile/database) y TASK-H03
 * (sólo importa {@code common}).
 *
 * @since TASK-H01
 */
@AnalyzeClasses(
        packages = "com.qa.httpcore",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /**
     * TASK-H02 — {@code http-core} no puede importar a sus pares (web/mobile/database).
     * <p>Solo se permite depender de {@code common}. Si en el futuro se necesita
     * sesión compartida con el browser (ya cubierto por TASK-E02 via
     * {@code Supplier<APIRequestContext>}), se mantiene la inversión de dependencia
     * — el supplier se inyecta desde la capa wiring (BE), nunca como import directo.
     */
    @ArchTest
    static final ArchRule httpcore_does_not_depend_on_siblings = noClasses()
            .that().resideInAPackage("com.qa.httpcore..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.qa.webcore..",
                    "com.qa.mobilecore..",
                    "com.qa.databasecore..")
            .as("http-core no debe importar web-core, mobile-core ni database-core");

    /**
     * TASK-K02 — http-core NO importa {@code com.qa.common.internal.*} EXCEPTO el
     * whitelist documentado de FQNs que aún son contratos legítimos de impl.
     *
     * <p>Cada FQN del whitelist tiene plan de salida documentado en
     * {@code docs/K01_PHASE_B_SUBTASKS.md} §K02 whitelist.
     */
    private static final Set<String> K02_INTERNAL_WHITELIST = Set.of(
            // TASK-K03 reemplazará con TypedConfig (api/config/)
            "com.qa.common.internal.config.ConfigManager",
            // TASK-K07 expondrá un wrapper api/concurrency cuando RetryPolicy llegue
            "com.qa.common.internal.concurrency.ParallelStepExecutor",
            "com.qa.common.internal.concurrency.ParallelResult",
            // TASK-K05/K06 expondrá api/ssl/SslContextPort cuando externalicemos
            "com.qa.common.internal.ssl.SSLContextFactory"
    );

    @ArchTest
    static final ArchRule httpcore_does_not_import_common_internal_except_whitelist = noClasses()
            .that().resideInAPackage("com.qa.httpcore..")
            .should().dependOnClassesThat(internalNotWhitelisted(K02_INTERNAL_WHITELIST))
            .as("http-core no debe importar com.qa.common.internal.* "
                + "(R-K02-2 TASK-K02). Excepciones aprobadas en el whitelist con plan de salida.");

    static DescribedPredicate<JavaClass> internalNotWhitelisted(Set<String> whitelist) {
        return new DescribedPredicate<>("internal de common no whitelisted (TASK-K02)") {
            @Override
            public boolean test(JavaClass jc) {
                String n = jc.getFullName();
                if (!n.startsWith("com.qa.common.internal.")) return false;
                return !whitelist.contains(n);
            }
        };
    }
}
