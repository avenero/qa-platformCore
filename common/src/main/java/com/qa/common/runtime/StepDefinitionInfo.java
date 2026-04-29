package com.qa.common.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DTO canónico de metadata de un step BDD individual (nivel método).
 *
 * <p>Representa la vista "plana" de un método de step Cucumber con su contexto completo:
 * la capa origen ({@link #layer()}), el componente padre ({@link #componentId()}),
 * el patrón Cucumber ({@link #cucumberPattern()}), los parámetros ({@link #params()})
 * y, desde la versión 2.3.0, mapas i18n ({@link #displayNameByLocale()},
 * {@link #descriptionByLocale()}) heredados del componente padre.
 *
 * <h2>Modelo de dos niveles</h2>
 * <p>El catálogo de steps del framework opera en dos niveles de granularidad:
 * <ol>
 *   <li><strong>Nivel componente</strong> — {@link StepInfo}: agrupa steps por responsabilidad.
 *       Ejemplo: el componente {@code "api.authentication"} agrupa todos los steps de
 *       autenticación HTTP (Bearer, Basic, OAuth2, JWT, API Key).</li>
 *   <li><strong>Nivel step (este DTO)</strong>: representa un step individual.
 *       Ejemplo: el step {@code "api.authentication.bearer.identifier"} corresponde al método
 *       {@code @Given("agrego autenticación Bearer con identificador {string}")}.</li>
 * </ol>
 *
 * <h2>Parámetros — dos vistas complementarias</h2>
 * <p>Los parámetros se exponen en dos representaciones:
 * <ul>
 *   <li>{@link #params()} — vista de reflexión Java ({@link ParamInfo}): posición,
 *       nombre en código, tipo Java, token Cucumber. Usada por el Core internamente.</li>
 *   <li>{@link #paramSchemas()} — vista semántica ({@link ParamSchema}): tipo lógico
 *       ({@code "string"}, {@code "number"}, {@code "boolean"}, {@code "json"}, etc.),
 *       required, descripción. Usada por el Backend, el FE y el lint BDD.</li>
 * </ul>
 * <p>{@link #paramSchemas()} se deriva automáticamente de {@link #params()} vía
 * {@link ParamSchema#fromParamInfo(ParamInfo)}; no requiere costo extra de construcción.
 *
 * <h2>Metadata i18n (desde v2.3.0)</h2>
 * <p>Los mapas {@link #displayNameByLocale()} y {@link #descriptionByLocale()} son
 * propagados desde el componente padre al construirse vía {@link StepMethodScanner}.
 * En este primer corte, todos los steps de un mismo componente comparten los mismos
 * mapas i18n. El Backend puede enriquecer con traducciones por step si lo requiere en
 * el futuro mediante {@link com.qa.common.runtime.annotation.StepDef#displayName()}.
 *
 * <h2>Usos previstos</h2>
 * <ul>
 *   <li><strong>Macros / CustomSteps (BE):</strong> expandir un macro a una secuencia
 *       de steps atómicos, cada uno identificado por su {@link #stepDefId()},
 *       con sus parámetros validados via {@link #paramSchemas()}.</li>
 *   <li><strong>Scenario Builder (FE):</strong> autocompletar patrones al escribir
 *       steps en el editor BDD.</li>
 *   <li><strong>Lint BDD (BE):</strong> validar que cada step escrito en un feature
 *       existe en el catálogo y que sus parámetros son correctos.</li>
 *   <li><strong>Sugerencias IA (BE):</strong> proporcionar contexto semántico rico
 *       (tipo lógico, descripción) al modelo de IA para generar valores de prueba.</li>
 * </ul>
 *
 * <h2>Identificadores</h2>
 * <p>El {@link #stepDefId()} puede ser:
 * <ul>
 *   <li><strong>Explícito:</strong> declarado en {@link com.qa.common.runtime.annotation.StepDef}
 *       en el método de la clase de steps. Formato: {@code {componentId}.{sub-id}}.
 *       Ejemplo: {@code "api.authentication.bearer.identifier"}.</li>
 *   <li><strong>Derivado:</strong> generado por {@link StepMethodScanner} con el formato
 *       {@code {componentId}#{methodName}}. Menos estable frente a refactorizaciones.</li>
 * </ul>
 * <p>Para IDs duraderos se recomienda siempre declarar
 * {@link com.qa.common.runtime.annotation.StepDef} en los métodos de step.
 *
 * <h2>Compatibilidad de constructores</h2>
 * <p>El constructor canónico (11 args) es el principal desde v2.3.0. Para compatibilidad
 * con código existente se mantiene el constructor de 9 args que usa mapas i18n vacíos.
 *
 * @param stepDefId              ID del step; explícito (via {@code @StepDef}) o derivado
 * @param cucumberPattern        patrón Cucumber Expression (texto de {@code @Given/@When/@Then})
 * @param params                 lista inmutable de parámetros del step; vacía si sin parámetros
 * @param phase                  fase BDD del step (GIVEN, WHEN, THEN)
 * @param layer                  nombre del plugin origen (ej: {@code "api"}, {@code "web"})
 * @param componentId            ID del componente padre (valor de su {@code @StepId})
 * @param displayName            nombre legible; si vacío/null, se deriva del {@code cucumberPattern}
 * @param deprecated             {@code true} si el step está marcado para migración
 * @param replacementStepDefId   ID del step sucesor; {@code null} si no aplica o no declarado
 * @param displayNameByLocale    mapa locale → nombre de display, heredado del componente padre;
 *                               nunca null (vacío si el componente no declara i18n)
 * @param descriptionByLocale    mapa locale → descripción, heredado del componente padre;
 *                               nunca null (vacío si el componente no declara i18n)
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see StepInfo
 * @see ParamInfo
 * @see ParamSchema
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
        String replacementStepDefId,
        Map<String, String> displayNameByLocale,
        Map<String, String> descriptionByLocale,
        /** Read from {@code @StepMetadata} — true if the step can change the HTTP base URL. */
        boolean canOverrideBaseUrl,
        /** Read from {@code @StepMetadata} — true if the URL is hardcoded in the pattern. */
        boolean usesLiteralUrl
) {

    // =========================================================================
    // Canonical compact constructor (v2.4.0 — 13 fields)
    // =========================================================================

    /**
     * Compact constructor: valida campos requeridos, normaliza colecciones e impone
     * inmutabilidad.
     *
     * <p>Comportamiento de normalización:
     * <ul>
     *   <li>{@link #params()} — si {@code null}, se normaliza a lista vacía inmutable.</li>
     *   <li>{@link #displayName()} — si {@code null} o blank, se deriva del
     *       {@link #cucumberPattern()}.</li>
     *   <li>{@link #displayNameByLocale()} y {@link #descriptionByLocale()} — si {@code null},
     *       se normalizan a mapa vacío inmutable.</li>
     *   <li>{@link #replacementStepDefId()} — nullable intencionalmente.</li>
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
        displayNameByLocale = (displayNameByLocale != null)
                ? Map.copyOf(displayNameByLocale) : Map.of();
        descriptionByLocale = (descriptionByLocale != null)
                ? Map.copyOf(descriptionByLocale) : Map.of();
        // replacementStepDefId, canOverrideBaseUrl, usesLiteralUrl: no normalization needed
    }

    // =========================================================================
    // Backward-compatible constructor (v2.2.0 — 9 args, sin i18n)
    // =========================================================================

    /**
     * Constructor de compatibilidad con v2.3.0 (11 args, sin flags de override).
     *
     * @deprecated desde v2.4.0 — usar el constructor de 13 argumentos que incluye
     *             {@code canOverrideBaseUrl} y {@code usesLiteralUrl}.
     */
    @Deprecated(since = "2.4.0", forRemoval = false)
    //CHECKSTYLE:OFF: ParameterNumber
    public StepDefinitionInfo(
            String stepDefId,
            String cucumberPattern,
            List<ParamInfo> params,
            BddPhase phase,
            String layer,
            String componentId,
            String displayName,
            boolean deprecated,
            String replacementStepDefId,
            Map<String, String> displayNameByLocale,
            Map<String, String> descriptionByLocale) {
        this(stepDefId, cucumberPattern, params, phase, layer, componentId,
             displayName, deprecated, replacementStepDefId,
             displayNameByLocale, descriptionByLocale,
             false, false);
    }

    /**
     * Constructor de compatibilidad con código anterior a v2.3.0 (9 args, sin i18n ni flags).
     *
     * @deprecated desde v2.3.0 — preferir el constructor completo de 13 argumentos.
     */
    @Deprecated(since = "2.3.0", forRemoval = false)
    public StepDefinitionInfo(
            String stepDefId,
            String cucumberPattern,
            List<ParamInfo> params,
            BddPhase phase,
            String layer,
            String componentId,
            String displayName,
            boolean deprecated,
            String replacementStepDefId) {
        this(stepDefId, cucumberPattern, params, phase, layer, componentId,
             displayName, deprecated, replacementStepDefId,
             Map.of(), Map.of(), false, false);
    }
    //CHECKSTYLE:ON: ParameterNumber

    // =========================================================================
    // Derived — ParamSchema (vista semántica de parámetros)
    // =========================================================================

    /**
     * Vista semántica de los parámetros del step como {@link ParamSchema}.
     *
     * <p>Se deriva automáticamente de {@link #params()} vía
     * {@link ParamSchema#fromParamInfo(ParamInfo)} en cada invocación.
     * El resultado no se cachea (la lista es pequeña y la derivación es O(n) barata).
     *
     * <p>Ejemplo de uso desde el Backend:
     * <pre>
     *   StepDefinitionInfo sdi = discovery.findById("api.authentication.bearer.identifier")
     *       .orElseThrow();
     *   sdi.paramSchemas().forEach(schema ->
     *       log.info("  param={} type={} required={}",
     *           schema.name(), schema.type(), schema.required()));
     * </pre>
     *
     * @return lista inmutable de {@link ParamSchema}; nunca null, puede estar vacía
     * @since 2.3.0
     */
    public List<ParamSchema> paramSchemas() {
        return params.stream().map(ParamSchema::fromParamInfo).collect(Collectors.toUnmodifiableList());
    }

    // =========================================================================
    // Locale helpers (i18n)
    // =========================================================================

    /**
     * Nombre de display del step en el locale solicitado.
     *
     * <p>Busca en {@link #displayNameByLocale()}; si el locale no está o el mapa está
     * vacío, retorna {@link #displayName()} como fallback.
     *
     * @param locale locale corto ("es", "en", "fr"), no null
     * @return nombre localizado; nunca null
     * @since 2.3.0
     */
    public String getDisplayNameForLocale(String locale) {
        Objects.requireNonNull(locale, "locale no puede ser null");
        String localized = displayNameByLocale.get(locale);
        return (localized != null) ? localized : displayName;
    }

    /**
     * Descripción del step en el locale solicitado.
     *
     * <p>Busca en {@link #descriptionByLocale()}; si el locale no está o el mapa está
     * vacío, retorna {@link #cucumberPattern()} como fallback legible.
     *
     * @param locale locale corto ("es", "en", "fr"), no null
     * @return descripción localizada; nunca null
     * @since 2.3.0
     */
    public String getDescriptionForLocale(String locale) {
        Objects.requireNonNull(locale, "locale no puede ser null");
        String localized = descriptionByLocale.get(locale);
        return (localized != null) ? localized : cucumberPattern;
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
     * @return {@code true} si {@link #replacementStepDefId()} no es null ni blank
     */
    public boolean hasReplacement() {
        return replacementStepDefId != null && !replacementStepDefId.isBlank();
    }

    /**
     * Indica si este step tiene traducciones i18n disponibles.
     *
     * @return {@code true} si al menos uno de los mapas i18n tiene entradas
     * @since 2.3.0
     */
    public boolean hasI18n() {
        return !displayNameByLocale.isEmpty() || !descriptionByLocale.isEmpty();
    }
}
