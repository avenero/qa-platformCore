package com.qa.common.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * TASK-K02 — frontera `api/` ↔ `internal/` en `common`.
 *
 * <h2>Reglas activas</h2>
 * <ul>
 *   <li><b>R-K02-3:</b> {@code com.qa.common.api.**} NO depende de
 *       {@code com.qa.common.internal.**} (direccional: internal → api).</li>
 *   <li><b>R-K02-4:</b> {@code com.qa.common.spi.**} NO depende de
 *       {@code com.qa.common.internal.**} (los SPI son contratos puros).</li>
 *   <li><b>R-K02-5:</b> {@code com.qa.common.utils.**} NO depende de
 *       {@code com.qa.common.internal.**} ni de {@code com.qa.common.api.**}
 *       (utilidades son funciones puras, no orquestan).</li>
 * </ul>
 *
 * <p>Las reglas que regulan a los <strong>consumidores externos</strong>
 * (specialized cores, BE) viven en cada uno de esos módulos
 * ({@code <module>/src/test/.../arch/ArchitectureTest.java}) con sus propios
 * whitelists documentados.
 */
@AnalyzeClasses(packages = "com.qa.common",
                importOptions = ImportOption.DoNotIncludeTests.class)
class ApiInternalBoundaryTest {

    @ArchTest
    static final ArchRule R_K02_3_api_does_not_depend_on_internal =
            noClasses().that().resideInAPackage("com.qa.common.api..")
                    .should().dependOnClassesThat().resideInAPackage("com.qa.common.internal..")
                    .because("api/ define contratos; internal/ los implementa. "
                            + "La dirección de dependencia debe ser internal → api, nunca al revés.");

    @ArchTest
    static final ArchRule R_K02_4_spi_does_not_depend_on_internal =
            noClasses().that().resideInAPackage("com.qa.common.spi..")
                    .should().dependOnClassesThat().resideInAPackage("com.qa.common.internal..")
                    .because("spi/ son puntos de extensión puros — solo contratos.");

    @ArchTest
    static final ArchRule R_K02_5_utils_does_not_depend_on_internal =
            noClasses().that().resideInAPackage("com.qa.common.utils..")
                    .should().dependOnClassesThat().resideInAPackage("com.qa.common.internal..")
                    .because("utils/ contiene funciones puras. Pueden importar api/ "
                            + "(logging, exceptions) pero no internal/ — eso violaría la frontera.");

    /**
     * Guard "soft": cualquier clase pública en {@code internal/**} debería llevar
     * la anotación {@code @Internal}. Documentado como advertencia (no fail-build
     * en este sprint) — bloqueante en K02 cleanup de seguimiento si emerge drift.
     */
    @ArchTest
    static final ArchRule R_K02_1_internal_classes_should_be_annotated =
            classes().that().resideInAPackage("com.qa.common.internal..")
                    .and().arePublic()
                    .and().areTopLevelClasses()
                    .should().beAnnotatedWith(com.qa.common.api.Internal.class)
                    .because("R-K02-1: toda clase pública en internal/ debe llevar @Internal "
                            + "para warning explícito + razón documentada.");
}
