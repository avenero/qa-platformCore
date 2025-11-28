package com.scotia.qa.common.cucumber.validators;

import com.scotia.qa.common.logging.TestLogger;
import io.cucumber.java.Scenario;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validador de consistencia entre tags de Cucumber y componentes del framework.
 *
 * <p>Este validador garantiza que los scenarios tengan los tags apropiados según
 * los steps que utilizan, ayudando a prevenir errores de inicialización.</p>
 *
 * <p><b>Estrategia de Validación:</b></p>
 * <ul>
 *   <li>Verifica que scenarios con steps web tengan tags web (@web, @ui, etc.)</li>
 *   <li>Verifica que scenarios con steps api tengan tags api (@api, @rest, etc.)</li>
 *   <li>Verifica que scenarios con steps mobile tengan tags mobile (@mobile, etc.)</li>
 *   <li>Loguea WARNINGS si detecta inconsistencias (no falla el test)</li>
 * </ul>
 *
 * <p><b>Modo de Operación:</b></p>
 * <ul>
 *   <li><b>WARNING Mode (Default):</b> Loguea advertencia pero permite continuar</li>
 *   <li><b>STRICT Mode:</b> Lanza excepción si hay inconsistencia (configurable)</li>
 * </ul>
 *
 * <p><b>Ejemplo de Uso:</b></p>
 * <pre>
 * // En WebSteps @Before hook
 * HookValidator.validateWebScenario(scenario);
 *
 * // En ApiSteps @Before hook
 * HookValidator.validateApiScenario(scenario);
 * </pre>
 *
 * <p><b>Configuración (opcional):</b></p>
 * <pre>
 * # En config-{env}.properties
 * framework.hooks.validation.mode=STRICT  # WARNING (default) o STRICT
 * </pre>
 *
 * @author Abnel Venero
 * @version 1.0.0
 * @since 2025-11-27
 */
public class HookValidator {

    // Tags soportados por capa
    private static final Set<String> WEB_TAGS = Set.of("@web", "@ui", "@selenium", "@browser");
    private static final Set<String> API_TAGS = Set.of("@api", "@rest", "@http", "@service");
    private static final Set<String> MOBILE_TAGS = Set.of("@mobile", "@android", "@ios", "@appium");
    private static final Set<String> DATABASE_TAGS = Set.of("@database", "@db", "@sql");

    // Modo de validación
    private static ValidationMode mode = ValidationMode.WARNING;

    /**
     * Modo de validación.
     */
    public enum ValidationMode {
        /** Loguea warning pero permite continuar */
        WARNING,
        /** Lanza excepción si hay inconsistencia */
        STRICT
    }

    /**
     * Constructor privado - clase de utilidad.
     */
    private HookValidator() {
        throw new UnsupportedOperationException("HookValidator es una clase de utilidad");
    }

    /**
     * Configura el modo de validación.
     *
     * @param validationMode modo de validación (WARNING o STRICT)
     */
    public static void setValidationMode(ValidationMode validationMode) {
        mode = validationMode;
        TestLogger.logDebug("HOOK_VALIDATOR",
            String.format("Modo de validación configurado: %s", mode),
            null);
    }

    /**
     * Valida que un scenario web tenga los tags apropiados.
     *
     * <p>Se ejecuta cuando el hook de WebSteps se activa. Verifica que el
     * scenario tenga al menos uno de los tags web esperados.</p>
     *
     * @param scenario Scenario de Cucumber
     */
    public static void validateWebScenario(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();

        if (!hasAnyTag(tags, WEB_TAGS)) {
            String message = String.format(
                "⚠️  Scenario '%s' usa WebDriver pero NO tiene tag web. " +
                "Agregar uno de: %s para mejor claridad.",
                scenario.getName(), WEB_TAGS
            );

            handleValidationIssue("WEB_VALIDATION", message);
        } else {
            TestLogger.logDebug("HOOK_VALIDATOR",
                String.format("✅ Scenario '%s' tiene tags web válidos", scenario.getName()),
                null);
        }
    }

    /**
     * Valida que un scenario api tenga los tags apropiados.
     *
     * @param scenario Scenario de Cucumber
     */
    public static void validateApiScenario(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();

        if (!hasAnyTag(tags, API_TAGS)) {
            String message = String.format(
                "⚠️  Scenario '%s' usa HttpClient pero NO tiene tag api. " +
                "Agregar uno de: %s para mejor claridad.",
                scenario.getName(), API_TAGS
            );

            handleValidationIssue("API_VALIDATION", message);
        } else {
            TestLogger.logDebug("HOOK_VALIDATOR",
                String.format("✅ Scenario '%s' tiene tags api válidos", scenario.getName()),
                null);
        }
    }

    /**
     * Valida que un scenario mobile tenga los tags apropiados.
     *
     * @param scenario Scenario de Cucumber
     */
    public static void validateMobileScenario(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();

        if (!hasAnyTag(tags, MOBILE_TAGS)) {
            String message = String.format(
                "⚠️  Scenario '%s' usa AppiumDriver pero NO tiene tag mobile. " +
                "Agregar uno de: %s para mejor claridad.",
                scenario.getName(), MOBILE_TAGS
            );

            handleValidationIssue("MOBILE_VALIDATION", message);
        } else {
            TestLogger.logDebug("HOOK_VALIDATOR",
                String.format("✅ Scenario '%s' tiene tags mobile válidos", scenario.getName()),
                null);
        }
    }

    /**
     * Valida consistencia general del scenario.
     *
     * <p>Puede ser llamado desde hooks genéricos para validar múltiples aspectos.</p>
     *
     * @param scenario Scenario de Cucumber
     */
    public static void validateScenario(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();

        // Validar que tenga al menos un tag de capa
        if (!hasAnyLayerTag(tags)) {
            String message = String.format(
                "ℹ️  Scenario '%s' no tiene tags de capa (web/api/mobile/database). " +
                "Considera agregar tags para mejor organización.",
                scenario.getName()
            );

            TestLogger.logInfo("HOOK_VALIDATOR", message, null);
        }

        // Validar combinaciones válidas
        validateTagCombinations(scenario, tags);
    }

    /**
     * Valida que las combinaciones de tags sean coherentes.
     *
     * @param scenario Scenario de Cucumber
     * @param tags Tags del scenario
     */
    private static void validateTagCombinations(Scenario scenario, Collection<String> tags) {
        boolean hasWeb = hasAnyTag(tags, WEB_TAGS);
        boolean hasApi = hasAnyTag(tags, API_TAGS);
        boolean hasMobile = hasAnyTag(tags, MOBILE_TAGS);
        boolean hasDatabase = hasAnyTag(tags, DATABASE_TAGS);

        // Contar capas activas
        int layerCount = (hasWeb ? 1 : 0) + (hasApi ? 1 : 0) +
                        (hasMobile ? 1 : 0) + (hasDatabase ? 1 : 0);

        if (layerCount > 1) {
            // Es un test híbrido - validar que sea intencional
            TestLogger.logDebug("HOOK_VALIDATOR",
                String.format("ℹ️  Scenario '%s' es híbrido: web=%s, api=%s, mobile=%s, db=%s",
                    scenario.getName(), hasWeb, hasApi, hasMobile, hasDatabase),
                null);
        }

        // Validación: No se recomienda Web + Mobile en el mismo scenario
        if (hasWeb && hasMobile) {
            String message = String.format(
                "⚠️  Scenario '%s' combina tags @web y @mobile. " +
                "Esto es inusual - verifica que sea intencional.",
                scenario.getName()
            );

            TestLogger.logWarning("HOOK_VALIDATOR", message, null);
        }
    }

    /**
     * Verifica si una colección de tags contiene al menos uno de los tags esperados.
     *
     * @param actualTags Tags del scenario
     * @param expectedTags Tags esperados
     * @return true si al menos uno coincide
     */
    private static boolean hasAnyTag(Collection<String> actualTags, Set<String> expectedTags) {
        if (actualTags == null || actualTags.isEmpty()) {
            return false;
        }

        for (String tag : actualTags) {
            if (expectedTags.contains(tag.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica si el scenario tiene al menos un tag de capa (web/api/mobile/database).
     *
     * @param tags Tags del scenario
     * @return true si tiene al menos un tag de capa
     */
    private static boolean hasAnyLayerTag(Collection<String> tags) {
        return hasAnyTag(tags, WEB_TAGS) ||
               hasAnyTag(tags, API_TAGS) ||
               hasAnyTag(tags, MOBILE_TAGS) ||
               hasAnyTag(tags, DATABASE_TAGS);
    }

    /**
     * Maneja un problema de validación según el modo configurado.
     *
     * @param component Componente que detectó el problema
     * @param message Mensaje descriptivo
     */
    private static void handleValidationIssue(String component, String message) {
        if (mode == ValidationMode.STRICT) {
            TestLogger.logError(component, message, null);
            throw new IllegalStateException(message);
        } else {
            // WARNING mode - solo loguear
            TestLogger.logWarning(component, message, null);
        }
    }

    /**
     * Obtiene información de debugging sobre las validaciones.
     *
     * @param scenario Scenario a analizar
     * @return String con información detallada
     */
    public static String getValidationInfo(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();

        return String.format(
            "Validation Info for '%s':%n" +
            "  Tags: %s%n" +
            "  Has Web tags: %s%n" +
            "  Has API tags: %s%n" +
            "  Has Mobile tags: %s%n" +
            "  Has Database tags: %s%n" +
            "  Validation Mode: %s",
            scenario.getName(),
            tags,
            hasAnyTag(tags, WEB_TAGS),
            hasAnyTag(tags, API_TAGS),
            hasAnyTag(tags, MOBILE_TAGS),
            hasAnyTag(tags, DATABASE_TAGS),
            mode
        );
    }
}


