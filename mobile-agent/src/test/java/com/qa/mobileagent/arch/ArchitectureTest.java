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
                            "com.qa.common.transport.ExecutionTransport")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(
                            "com.qa.common.transport.InProcessTransport")
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
}
