package com.qa.mobileagent.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit guards del módulo {@code mobile-agent} (TASK-I04, RFC-AGENT-01).
 *
 * <h2>Reglas activas</h2>
 * <ul>
 *   <li><b>R-MA-1:</b> los controllers HTTP no acceden directamente al transport;
 *       sólo a través del {@code AgentExecutionService}.</li>
 *   <li><b>R-MA-2:</b> el módulo no importa nada del BE (qa-platformBE).</li>
 *   <li><b>R-MA-3:</b> el módulo no importa entities JPA (no debe tener persistencia).</li>
 *   <li><b>R-MA-4:</b> el wire-protocol record {@code AgentEvent} vive sólo en
 *       {@code api.dto} — no se filtra a controllers ni service como API
 *       interna mezclada con tipos de Spring.</li>
 * </ul>
 */
@AnalyzeClasses(packages = "com.qa.mobileagent",
                importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule R_MA_1_controllersGoThroughService =
            noClasses().that().resideInAPackage("..api.controller..")
                    .should().dependOnClassesThat().haveFullyQualifiedName(
                            "com.qa.common.api.transport.ExecutionTransport")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(
                            "com.qa.common.internal.transport.InProcessTransport")
                    .because("Los controllers HTTP usan AgentExecutionService — no instancian"
                            + " ni invocan al transport directamente. Pueden referenciar tipos de"
                            + " resultado (ExecutionHandle) si los obtienen del service.");

    @ArchTest
    static final ArchRule R_MA_2_noBackendDeps =
            noClasses().should().dependOnClassesThat().resideInAnyPackage(
                            "com.qa.platform..", "com.qa.be..")
                    .because("mobile-agent es un módulo del Core; nada del BE debe filtrarse");

    @ArchTest
    static final ArchRule R_MA_3_noJpa =
            noClasses().should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "javax.persistence..", "org.hibernate..")
                    .because("mobile-agent no persiste estado en DB");

    @ArchTest
    static final ArchRule R_MA_4_dtosLiveInApiDto =
            classes().that().haveSimpleNameEndingWith("Event")
                    .or().haveSimpleNameEndingWith("Request")
                    .or().haveSimpleNameEndingWith("Response")
                    .and().resideInAPackage("com.qa.mobileagent..")
                    .should().resideInAPackage("..api.dto..")
                    .because("Wire-protocol records viven juntos para evolución controlada");

    /**
     * TASK-K02 — mobile-agent es Core-internal (wraps el InProcessTransport del Core
     * y lo expone via HTTP/SSE). Por diseño accede a `common.internal.transport.*`
     * (R-MA-1 ya documenta el patrón). El whitelist captura los FQNs aprobados.
     */
    private static final java.util.Set<String> K02_INTERNAL_WHITELIST = java.util.Set.of(
            "com.qa.common.internal.transport.InProcessTransport"
    );

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule R_K02_mobileagent_internal_whitelist =
            noClasses().that().resideInAPackage("com.qa.mobileagent..")
                    .should().dependOnClassesThat(
                            new com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>(
                                    "com.qa.common.internal.* (no whitelisted TASK-K02)") {
                                @Override
                                public boolean test(com.tngtech.archunit.core.domain.JavaClass jc) {
                                    String n = jc.getFullName();
                    if (!n.startsWith("com.qa.common.internal.")) return false;
                    int d = n.indexOf("$");
                    String tl = d > 0 ? n.substring(0, d) : n;
                    return !K02_INTERNAL_WHITELIST.contains(tl);
                                }
                            })
                    .because("mobile-agent solo accede a common.internal.transport.InProcessTransport "
                            + "(R-MA-1 + TASK-K02 whitelist). Cualquier otro acceso a internal es violación.");
}
