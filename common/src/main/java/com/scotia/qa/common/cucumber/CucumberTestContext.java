package com.scotia.qa.common.cucumber;

import com.scotia.qa.common.logging.EvidenceManager;
import com.scotia.qa.common.logging.TestLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contexto compartido para tests de Cucumber.
 * Permite el intercambio de datos entre steps y hooks de manera thread-safe.
 *
 * 📚 DOCUMENTACIÓN COMPLETA: Ver /common/README.md sección "🥒 Paquete Cucumber"
 *    Incluye:
 *    - Guía completa de uso del contexto
 *    - API completa con ejemplos
 *    - Explicación de thread-safety
 *    - Errores comunes y soluciones
 *
 * CARACTERÍSTICAS PRINCIPALES:
 * - ✅ Thread-safe: Usa ThreadLocal para aislamiento entre escenarios paralelos
 * - ✅ Auto-limpieza: Se limpia automáticamente después de cada escenario
 * - ✅ Fácil de usar: API simple con storeData/getData
 * - ✅ Debugging: Métodos para ver todo el contexto
 *
 * USO BÁSICO:
 * <pre>
 * // Guardar datos
 * CucumberTestContext.storeData("authToken", token);
 *
 * // Recuperar datos
 * String token = (String) CucumberTestContext.getData("authToken");
 *
 * // Verificar existencia
 * if (CucumberTestContext.hasData("authToken")) { ... }
 * </pre>
 *
 * @see BaseCucumberHooks - Gestiona el ciclo de vida y limpieza del contexto
 * @see ExampleFrameworkHooks - Ejemplo de uso en frameworks específicos
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class  CucumberTestContext {


    // Contexto por thread para soporte de ejecución paralela
    private static final ThreadLocal<Map<String, Object>> testData = new ThreadLocal<>();
    private static final ThreadLocal<String> currentScenario = new ThreadLocal<>();
    private static final ThreadLocal<String> currentFeature = new ThreadLocal<>();
    private static final ThreadLocal<String> framework = new ThreadLocal<>();
    private static final ThreadLocal<LocalDateTime> scenarioStartTime = new ThreadLocal<>();

    private CucumberTestContext() {
        // Utility class - no instances
    }

    // =================================================================================
    // GESTIÓN DE CONTEXTO
    // =================================================================================

    /**
     * Inicializa el contexto para un nuevo escenario.
     */
    public static void initializeScenario(String scenarioName, String featureName, String frameworkType) {
        testData.set(new ConcurrentHashMap<>());
        currentScenario.set(scenarioName);
        currentFeature.set(featureName);
        framework.set(frameworkType);
        scenarioStartTime.set(LocalDateTime.now());

        // Configurar contexto en otros componentes
        TestLogger.setFramework(frameworkType);
        TestLogger.setTestContext(scenarioName);
        EvidenceManager.setTestContext(frameworkType, featureName, scenarioName);

        TestLogger.logInfo("TEST_CONTEXT_INIT",
                          String.format("Initialized test context - Feature: %s, Scenario: %s, Framework: %s",
                                       featureName, scenarioName, framework), null);
    }

    /**
     * Limpia el contexto al finalizar el escenario.
     */
    public static void cleanupScenario() {
        String scenario = currentScenario.get();
        LocalDateTime startTime = scenarioStartTime.get();

        if (scenario != null && startTime != null) {
            long duration = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            TestLogger.logInfo("TEST_CONTEXT_DURATION",
                              String.format("Scenario '%s' completed in %dms", scenario, duration), null);
        }

        // Limpiar contextos
        TestLogger.clearTestContext();
        EvidenceManager.clearTestContext();

        // Limpiar thread locals
        testData.remove();
        currentScenario.remove();
        currentFeature.remove();
        framework.remove();
        scenarioStartTime.remove();
    }

    // =================================================================================
    // GESTIÓN DE DATOS
    // =================================================================================

    /**
     * Almacena un valor en el contexto del test.
     */
    public static void setValue(String key, Object value) {
        Map<String, Object> data = testData.get();
        if (data != null) {
            data.put(key, value);
            TestLogger.logDebug("TEST_CONTEXT",
                               String.format("Stored value in test context: %s = %s", key, value), null);
        } else {
            TestLogger.logWarning("TEST_CONTEXT",
                                 String.format("Attempted to store value '%s' but test context is not initialized", key), null);
        }
    }

    /**
     * Obtiene un valor del contexto del test.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(String key) {
        Map<String, Object> data = testData.get();
        if (data != null) {
            return (T) data.get(key);
        } else {
            TestLogger.logWarning("TEST_CONTEXT",
                                 String.format("Attempted to get value '%s' but test context is not initialized", key), null);
            return null;
        }
    }

    /**
     * Obtiene un valor del contexto con valor por defecto.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(String key, T defaultValue) {
        T value = (T) getValue(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Verifica si existe una clave en el contexto.
     */
    public static boolean hasValue(String key) {
        Map<String, Object> data = testData.get();
        return data != null && data.containsKey(key);
    }

    /**
     * Elimina un valor del contexto.
     */
    public static void removeValue(String key) {
        Map<String, Object> data = testData.get();
        if (data != null) {
            data.remove(key);
            TestLogger.logDebug("TEST_CONTEXT",
                               String.format("Removed value from test context: %s", key), null);
        }
    }

    /**
     * Obtiene todos los datos del contexto.
     */
    public static Map<String, Object> getAllData() {
        Map<String, Object> data = testData.get();
        return data != null ? new ConcurrentHashMap<>(data) : new ConcurrentHashMap<>();
    }

    // =================================================================================
    // INFORMACIÓN DEL CONTEXTO
    // =================================================================================

    /**
     * Obtiene el nombre del escenario actual.
     */
    public static String getCurrentScenario() {
        return currentScenario.get();
    }

    /**
     * Obtiene el nombre del feature actual.
     */
    public static String getCurrentFeature() {
        return currentFeature.get();
    }

    /**
     * Obtiene el framework actual.
     */
    public static String getCurrentFramework() {
        return framework.get();
    }

    /**
     * Obtiene el tiempo de inicio del escenario.
     */
    public static LocalDateTime getScenarioStartTime() {
        return scenarioStartTime.get();
    }

    /**
     * Obtiene la duración actual del escenario en milisegundos.
     */
    public static long getCurrentScenarioDuration() {
        LocalDateTime startTime = scenarioStartTime.get();
        if (startTime != null) {
            return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        }
        return 0L;
    }

    // =================================================================================
    // UTILIDADES ESPECÍFICAS
    // =================================================================================

    /**
     * Almacena información de la última request HTTP.
     */
    public static void setLastHttpRequest(String method, String url, String body) {
        setValue("last_http_method", method);
        setValue("last_http_url", url);
        setValue("last_http_request_body", body);
        setValue("last_http_timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * Almacena información de la última response HTTP.
     */
    public static void setLastHttpResponse(int statusCode, String body, Map<String, String> headers) {
        setValue("last_http_status_code", statusCode);
        setValue("last_http_response_body", body);
        setValue("last_http_response_headers", headers);
    }

    /**
     * Almacena información del último elemento de UI interactuado.
     */
    public static void setLastUiElement(String elementType, String locator, String action, String value) {
        setValue("last_ui_element_type", elementType);
        setValue("last_ui_locator", locator);
        setValue("last_ui_action", action);
        setValue("last_ui_value", value);
        setValue("last_ui_timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * Marca el escenario como fallido con razón.
     */
    public static void markScenarioAsFailed(String reason, Throwable exception) {
        setValue("scenario_failed", true);
        setValue("failure_reason", reason);
        setValue("failure_timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (exception != null) {
            setValue("failure_exception", exception.getClass().getSimpleName());
            setValue("failure_stack_trace", getStackTraceAsString(exception));
        }

        TestLogger.logError("SCENARIO_FAILURE", reason, getAllData());
    }

    /**
     * Verifica si el escenario ha fallado.
     */
    public static boolean isScenarioFailed() {
        return getValue("scenario_failed", false);
    }

    /**
     * Obtiene la razón del fallo del escenario.
     */
    public static String getFailureReason() {
        return getValue("failure_reason");
    }

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Convierte stack trace a string.
     */
    private static String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }
}
