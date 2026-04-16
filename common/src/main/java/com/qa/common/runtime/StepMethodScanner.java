package com.qa.common.runtime;

import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Escáner de reflexión que descubre los steps BDD individuales de una clase de steps Cucumber.
 *
 * <p>Opera sobre la clase retornada por {@link StepComponent#getStepDefinitionClass()} y
 * detecta todos los métodos anotados con {@code @Given}, {@code @When} o {@code @Then},
 * construyendo un {@link StepDefinitionInfo} por cada uno.
 *
 * <h2>Algoritmo de escaneo</h2>
 * <ol>
 *   <li>Obtiene todos los métodos públicos de la clase via {@code getMethods()} (incluye
 *       métodos heredados).</li>
 *   <li>Filtra los que tengan {@code @Given}, {@code @When} o {@code @Then}.</li>
 *   <li>Extrae el patrón Cucumber Expression del valor de la anotación.</li>
 *   <li>Extrae los tokens del patrón con regex ({@code {string}}, {@code {int}}, etc.).</li>
 *   <li>Construye la lista de {@link ParamInfo} matcheando tokens con parámetros del método
 *       por posición (el parámetro en posición {@code i} corresponde al token {@code i}).</li>
 *   <li>Resuelve el {@code stepDefId} desde {@link StepDef} si está presente, o lo deriva
 *       como {@code {componentId}#{methodName}}.</li>
 *   <li>Propaga los mapas i18n del componente padre al {@link StepDefinitionInfo} (v2.3.0).</li>
 * </ol>
 *
 * <h2>Comportamiento ante clases nulas o sin steps</h2>
 * <p>Si {@link StepComponent#getStepDefinitionClass()} retorna {@code null} (componente
 * aún no implementado), se emite un log DEBUG y se retorna lista vacía sin lanzar excepción.
 * Si la reflexión falla por cualquier motivo, se emite un log WARN y se retorna lista vacía.
 *
 * <h2>Parámetros especiales (DataTable / DocString)</h2>
 * <p>Cucumber puede inyectar {@code DataTable} o {@code DocString} como último parámetro
 * de un método sin que aparezcan como tokens {@code {}} en el patrón. El scanner detecta
 * este caso: si hay más parámetros Java que tokens en el patrón, los parámetros restantes
 * reciben {@code cucumberToken = null} en su {@link ParamInfo}, y su {@link ParamSchema}
 * reflejará el tipo estructurado correspondiente ({@code "table"}, {@code "json"},
 * {@code "docstring"}).
 *
 * <h2>I18n propagada desde el componente (v2.3.0)</h2>
 * <p>Los mapas {@code displayNameByLocale} y {@code descriptionByLocale} del
 * {@link StepComponent} se propagan a todos sus {@link StepDefinitionInfo}. En este
 * corte inicial, todos los steps de un componente comparten los mismos mapas. El soporte
 * de i18n por step individual se planifica para una versión futura vía
 * {@link com.qa.common.runtime.annotation.StepDef}.
 *
 * <h2>Limitaciones conocidas</h2>
 * <ul>
 *   <li>Solo procesa anotaciones en inglés ({@code io.cucumber.java.en.*}).
 *       Las variantes en español ({@code io.cucumber.java.es.*}) se agregarán
 *       en una versión futura.</li>
 *   <li>{@code @And} y {@code @But} no son procesados: su fase BDD es ambigua estáticamente
 *       (depende del contexto en el feature file). Ver ADR-003.</li>
 *   <li>Los nombres de parámetros son correctos solo si se compiló con {@code -parameters};
 *       sin ese flag, el fallback es {@code "arg0"}, {@code "arg1"}, etc.</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see StepDefinitionInfo
 * @see ParamInfo
 * @see ParamSchema
 * @see StepDiscoveryService#discoverAllStepDefs()
 * @see com.qa.common.runtime.annotation.StepDef
 */
public final class StepMethodScanner {

    private static final Logger log = LoggerFactory.getLogger(StepMethodScanner.class);

    /**
     * Regex para extraer tokens Cucumber del patrón:
     * {@code {string}}, {@code {int}}, {@code {word}}, {@code {float}}, etc.
     */
    private static final Pattern TOKEN_REGEX = Pattern.compile("\\{([^}]+)\\}");

    /**
     * Escanea todos los componentes de una lista y retorna la colección completa de
     * steps individuales descubiertos.
     *
     * @param components lista de componentes a escanear, no null
     * @return lista inmutable de {@link StepDefinitionInfo}; nunca null, puede estar vacía
     */
    public List<StepDefinitionInfo> scanAll(List<StepDiscoveryService.ComponentInfo> components) {
        Objects.requireNonNull(components, "components no puede ser null");
        return components.stream()
                .flatMap(info -> scan(info).stream())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Escanea un componente y retorna la lista de sus steps individuales.
     *
     * <p>Si {@link StepComponent#getStepDefinitionClass()} retorna {@code null}, el componente
     * aún no está implementado y se retorna lista vacía sin error.
     *
     * @param componentInfo componente a escanear, no null
     * @return lista inmutable de {@link StepDefinitionInfo}; nunca null, puede estar vacía
     */
    public List<StepDefinitionInfo> scan(StepDiscoveryService.ComponentInfo componentInfo) {
        Objects.requireNonNull(componentInfo, "componentInfo no puede ser null");

        Class<?> stepClass = componentInfo.component().getStepDefinitionClass();
        if (stepClass == null) {
            log.debug("Componente '{}' no tiene stepDefinitionClass (aún no implementado) — se omite.",
                    componentInfo.component().getId());
            return List.of();
        }

        try {
            return scanClass(
                    stepClass,
                    componentInfo.pluginName(),
                    componentInfo.component().getId(),
                    componentInfo.component().getDisplayNameByLocale(),
                    componentInfo.component().getDescriptionByLocale());
        } catch (Exception e) {
            log.warn("Error escaneando la clase de steps '{}' del componente '{}': {}",
                    stepClass.getName(), componentInfo.component().getId(), e.getMessage());
            return List.of();
        }
    }

    // =========================================================================
    // Private — lógica central de escaneo
    // =========================================================================

    /**
     * Escanea todos los métodos de una clase de steps y construye la lista de
     * {@link StepDefinitionInfo}.
     *
     * @param stepClass           clase con anotaciones Cucumber
     * @param layer               nombre del plugin (ej: "api", "web")
     * @param componentId         ID del componente padre
     * @param displayNameByLocale mapas i18n del componente padre (propagados a cada step)
     * @param descriptionByLocale mapas i18n del componente padre (propagados a cada step)
     */
    private List<StepDefinitionInfo> scanClass(Class<?> stepClass,
                                               String layer,
                                               String componentId,
                                               Map<String, String> displayNameByLocale,
                                               Map<String, String> descriptionByLocale) {
        List<StepDefinitionInfo> result = new ArrayList<>();

        for (Method method : stepClass.getMethods()) {
            PhaseAndPattern pp = extractPhaseAndPattern(method);
            if (pp == null) {
                continue; // no es un método de step Cucumber
            }

            List<ParamInfo> params = buildParamInfoList(method, pp.pattern());

            StepDef stepDefAnn    = method.getAnnotation(StepDef.class);
            String  stepDefId     = resolveStepDefId(stepDefAnn, componentId, method.getName());
            String  displayName   = resolveDisplayName(stepDefAnn, pp.pattern());
            boolean deprecated    = (stepDefAnn != null && stepDefAnn.deprecated());
            String  replacementId = resolveReplacementId(stepDefAnn);

            if (deprecated) {
                log.warn("StepDef '{}' está marcado como DEPRECATED. Reemplazo: {}.",
                        stepDefId, replacementId != null ? "'" + replacementId + "'" : "(sin declarar)");
            }

            result.add(new StepDefinitionInfo(
                    stepDefId,
                    pp.pattern(),
                    params,
                    pp.phase(),
                    layer,
                    componentId,
                    displayName,
                    deprecated,
                    replacementId,
                    displayNameByLocale != null ? displayNameByLocale : Map.of(),
                    descriptionByLocale != null ? descriptionByLocale : Map.of()
            ));

            log.debug("Step descubierto: [{}] '{}' → stepDefId='{}'",
                    pp.phase().name(), pp.pattern(), stepDefId);
        }

        log.debug("Escaneados {} steps en '{}' (componente: '{}', capa: '{}')",
                result.size(), stepClass.getSimpleName(), componentId, layer);
        return List.copyOf(result);
    }

    /**
     * Extrae la fase BDD y el patrón Cucumber de un método, si está anotado con
     * {@code @Given}, {@code @When} o {@code @Then}.
     *
     * @param method método a inspeccionar
     * @return carrier con fase y patrón, o {@code null} si el método no es un step Cucumber
     */
    private PhaseAndPattern extractPhaseAndPattern(Method method) {
        Given given = method.getAnnotation(Given.class);
        if (given != null) {
            return new PhaseAndPattern(BddPhase.GIVEN, given.value());
        }
        When when = method.getAnnotation(When.class);
        if (when != null) {
            return new PhaseAndPattern(BddPhase.WHEN, when.value());
        }
        Then then = method.getAnnotation(Then.class);
        if (then != null) {
            return new PhaseAndPattern(BddPhase.THEN, then.value());
        }
        return null;
    }

    /**
     * Construye la lista de {@link ParamInfo} para un método, matcheando tokens del patrón
     * con parámetros Java por posición.
     *
     * <p>Si hay más parámetros Java que tokens (caso DataTable/DocString), los parámetros
     * excedentes reciben {@code cucumberToken = null}.
     */
    private List<ParamInfo> buildParamInfoList(Method method, String pattern) {
        Parameter[] javaParams = method.getParameters();
        if (javaParams.length == 0) {
            return List.of();
        }

        List<String> tokens = extractTokens(pattern);
        List<ParamInfo> paramInfos = new ArrayList<>(javaParams.length);

        for (int i = 0; i < javaParams.length; i++) {
            Parameter p       = javaParams[i];
            String paramName  = p.isNamePresent() ? p.getName() : "arg" + i;
            String javaType   = p.getType().getSimpleName();
            String token      = (i < tokens.size()) ? tokens.get(i) : null;
            paramInfos.add(new ParamInfo(i, paramName, javaType, token));
        }
        return paramInfos;
    }

    /**
     * Extrae todos los tokens {@code {…}} del patrón Cucumber en orden de aparición.
     * Ejemplo: {@code "reintento {string} al endpoint {string} hasta que el código sea {int}"}
     * → {@code ["{string}", "{string}", "{int}"]}.
     */
    private List<String> extractTokens(String pattern) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_REGEX.matcher(pattern);
        while (matcher.find()) {
            tokens.add(matcher.group(0)); // incluye las llaves: "{string}", "{int}"
        }
        return tokens;
    }

    /**
     * Resuelve el {@code stepDefId}: usa el valor de {@link StepDef#value()} si está presente;
     * de lo contrario, deriva el ID como {@code {componentId}#{methodName}}.
     */
    private String resolveStepDefId(StepDef ann, String componentId, String methodName) {
        if (ann != null && !ann.value().isBlank()) {
            return ann.value();
        }
        return componentId + "#" + methodName;
    }

    /**
     * Resuelve el nombre de display: usa {@link StepDef#displayName()} si está presente;
     * de lo contrario, retorna el patrón Cucumber (el record normaliza null/blank).
     */
    private String resolveDisplayName(StepDef ann, String pattern) {
        if (ann != null && !ann.displayName().isBlank()) {
            return ann.displayName();
        }
        return pattern;
    }

    /**
     * Resuelve el ID de reemplazo desde {@link StepDef#replacedBy()}.
     * Retorna {@code null} si la anotación no existe o {@code replacedBy} está vacío.
     */
    private String resolveReplacementId(StepDef ann) {
        if (ann != null && !ann.replacedBy().isBlank()) {
            return ann.replacedBy();
        }
        return null;
    }

    // =========================================================================
    // Private record — carrier interno
    // =========================================================================

    /**
     * Par inmutable de (fase BDD, patrón Cucumber) extraído de una anotación
     * {@code @Given/@When/@Then}. Solo se usa internamente dentro del escáner.
     */
    private record PhaseAndPattern(BddPhase phase, String pattern) {}
}
