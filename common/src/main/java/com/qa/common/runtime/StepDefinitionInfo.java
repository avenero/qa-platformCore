package com.qa.common.runtime;

import java.util.List;
import java.util.Objects;

/**
 * DTO de metadata de un step BDD individual (nivel método).
 *
 * <p>Representa la vista "plana" de un método de step Cucumber con su contexto completo:
 * la capa origen ({@link #layer()}), el componente padre ({@link #componentId()}),
 * el patrón Cucumber ({@link #cucumberPattern()}) y los parámetros ({@link #params()}).
 *
 * <h2>Modelo de dos niveles</h2>
 * <p>El catálogo de steps del framework opera en dos niveles de granularidad:
 * <ol>
 *   <li><strong>Nivel componente</strong> — {@link StepInfo}: agrupa steps por responsabilidad.
 *       Ejemplo: el componente {@code "api.authentication"} agrupa todos los steps de
 *       autenticación HTTP (Bearer, Basic, OAuth2, JWT, API Key).</li>
 *   <li><strong>Nivel step (este DTO)</strong>: representa un step individual.
 *       Ejemplo: el step {@code "api.authentication.bearer.rut"} corresponde al método
 *       {@code @Given("agrego autenticación Bearer para RUT {string}")}.</li>
 * </ol>
 *
 * <h2>Usos previstos</h2>
 * <ul>
 *   <li><strong>Scenario Builder (Frontend):</strong> autocompletar patrones al escribir
 *       steps en el editor BDD. El {@link #cucumberPattern()} es el texto a mostrar;
 *       {@link #params()} define qué slots de entrada tiene el step.</li>
 *   <li><strong>Lint BDD (Backend):</strong> validar que cada step escrito en un
 *       feature file existe en el catálogo y que el número y tipo de sus parámetros
 *       es correcto antes de lanzar una ejecución.</li>
 *   <li><strong>Documentación generada:</strong> construir catálogos en ES/EN/FR a partir
 *       del {@link #cucumberPattern()} y el {@link #displayName()} del step.</li>
 *   <li><strong>Import Swagger/Postman:</strong> sugerir el step más afín al endpoint
 *       siendo importado, basándose en fase ({@link #phase()}) y capa ({@link #layer()}).</li>
 * </ul>
 *
 * <h2>Identificadores</h2>
 * <p>El {@link #stepDefId()} puede ser:
 * <ul>
 *   <li><strong>Explícito:</strong> declarado en la anotación
 *       {@link com.qa.common.runtime.annotation.StepDef} puesta en el método de la clase
 *       de steps. Formato recomendado: {@code {componentId}.{sub-identificador}}.
 *       Ejemplo: {@code "api.authentication.bearer.rut"}.</li>
 *   <li><strong>Derivado:</strong> generado por {@link StepMethodScanner} cuando no hay
 *       {@code @StepDef}, con el formato {@code {componentId}#{methodName}}.
 *       Ejemplo: {@code "api.authentication#agregoAutenticacionBearerParaRUT"}.
 *       Este ID es menos estable porque depende del nombre del método Java.</li>
 * </ul>
 * <p>Para IDs duraderos que soporten refactorizaciones del código fuente, se recomienda
 * siempre declarar {@code @StepDef} en los métodos de step.
 *
 * @param stepDefId             ID del step; explícito (via {@code @StepDef}) o derivado
 * @param cucumberPattern       patrón Cucumber Expression (texto de {@code @Given/@When/@Then})
 * @param params                lista inmutable de parámetros del step; vacía si sin parámetros
 * @param phase                 fase BDD del step (GIVEN, WHEN, THEN)
 * @param layer                 nombre del plugin origen (ej: {@code "api"}, {@code "web"})
 * @param componentId           ID del componente padre (valor de su {@code @StepId})
 * @param displayName           nombre legible; si vacío/null, se deriva del {@code cucumberPattern}
 * @param deprecated            {@code true} si el step está marcado para migración
 * @param replacementStepDefId  ID del step sucesor; {@code null} si no aplica o no declarado
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see StepInfo
 * @see ParamInfo
 * @see StepMethodScanner
 * @see com.qa.common.runtime.annotation.StepDef
 */
public record StepDefinitionInfo(
        String stepDefId,
        String cucumberPattern,
        List<ParamInfo> params,
        BddPhase phase,
        String layer,
        String componentId,
        String displayName,
        boolean deprecated,
        String replacementStepDefId
) {

    /**
     * Compact constructor: valida campos requeridos e impone inmutabilidad en la lista
     * de parámetros.
     *
     * <p>Comportamiento de normalización:
     * <ul>
     *   <li>{@link #params()} — si {@code null}, se normaliza a lista vacía inmutable.</li>
     *   <li>{@link #displayName()} — si {@code null} o blank, se deriva automáticamente
     *       del {@link #cucumberPattern()}. Esto evita que el Frontend tenga que implementar
     *       esa lógica de fallback.</li>
     *   <li>{@link #replacementStepDefId()} — nullable intencionalmente: {@code null} cuando
     *       {@link #deprecated()} es {@code false} o cuando no hay sucesor conocido.</li>
     * </ul>
     */
    public StepDefinitionInfo {
        Objects.requireNonNull(stepDefId,       "stepDefId no puede ser null");
        Objects.requireNonNull(cucumberPattern, "cucumberPattern no puede ser null");
        Objects.requireNonNull(phase,           "phase no puede ser null");
        Objects.requireNonNull(layer,           "layer no puede ser null");
        Objects.requireNonNull(componentId,     "componentId no puede ser null");
        params = (params != null) ? List.copyOf(params) : List.of();
        if (displayName == null || displayName.isBlank()) {
            displayName = cucumberPattern;
        }
        // replacementStepDefId: nullable — null cuando no hay sucesor declarado
    }

    // =========================================================================
    // Convenience helpers
    // =========================================================================

    /**
     * Indica si este step tiene al menos un parámetro.
     *
     * <p>Conveniencia para el Frontend: un step sin parámetros puede insertarse
     * directamente sin abrir un diálogo de configuración de valores.
     *
     * @return {@code true} si {@link #params()} no está vacío
     */
    public boolean hasParams() {
        return !params.isEmpty();
    }

    /**
     * Indica si este step tiene un sucesor conocido declarado.
     *
     * <p>Conveniencia para evitar comparar {@link #replacementStepDefId()} contra
     * {@code null} en los consumers (Backend / Frontend).
     *
     * @return {@code true} si {@link #replacementStepDefId()} no es null ni blank
     */
    public boolean hasReplacement() {
        return replacementStepDefId != null && !replacementStepDefId.isBlank();
    }
}
