package com.scotia.qa.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de logging unificado para todos los frameworks de testing.
 * Proporciona una interfaz consistente para logging estructurado y contextual.
 * Clase estática sin dependencias de Spring.
 *
 * <p>Características principales:
 * <ul>
 *   <li>Logging estructurado con contexto</li>
 *   <li>Diferentes niveles para diferentes tipos de eventos</li>
 *   <li>Sanitización automática de datos sensibles</li>
 *   <li>Formato consistente entre frameworks</li>
 *   <li>Soporte para testing steps y assertions</li>
 * </ul>
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class TestLogger {

    // Inicializar configuración de logging para suprimir mensajes de SLF4J
    static {
        LoggingInitializer.initialize();
    }

    private static final Logger log = LoggerFactory.getLogger(TestLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Contexto actual del test
    private static final ThreadLocal<String> currentTestContext = new ThreadLocal<>();
    private static final ThreadLocal<String> currentFramework = new ThreadLocal<>();
    private static final Map<String, Object> globalContext = new ConcurrentHashMap<>();

    private TestLogger() {
        // Utility class - no instances
    }

    // =================================================================================
    // CONFIGURACIÓN DE CONTEXTO
    // =================================================================================

    /**
     * Establece el contexto del test actual.
     */
    public static void setTestContext(String testName) {
        currentTestContext.set(testName);
        logInfo("TEST_START", "Iniciando test: " + testName, null);
    }

    /**
     * Establece el framework actual (API, Web, Mobile).
     */
    public static void setFramework(String framework) {
        currentFramework.set(framework);
        globalContext.put("framework", framework);
    }

    /**
     * Limpia el contexto del test actual.
     */
    public static void clearTestContext() {
        String testName = currentTestContext.get();
        if (testName != null) {
            logInfo("TEST_END", "Finalizando test: " + testName, null);
        }
        currentTestContext.remove();
    }

    // =================================================================================
    // LOGGING DE STEPS Y ACCIONES
    // =================================================================================

    /**
     * Registra un step de testing.
     */
    public static void logStep(String stepType, String description) {
        logStep(stepType, description, null);
    }

    /**
     * Registra un step de testing con datos adicionales.
     */
    public static void logStep(String stepType, String description, Map<String, Object> data) {
        String message = String.format("[STEP][%s] %s", stepType.toUpperCase(), description);
        logInfo("STEP", message, data);
    }

    /**
     * Registra una acción HTTP.
     */
    public static void logHttpAction(String method, String url, int statusCode, long duration) {
        Map<String, Object> data = Map.of(
            "method", method,
            "url", sanitizeUrl(url),
            "statusCode", statusCode,
            "duration", duration + "ms"
        );
        String message = String.format("[HTTP] %s %s -> %d (%dms)", method, sanitizeUrl(url), statusCode, duration);
        logInfo("HTTP", message, data);
    }

    /**
     * Registra una interacción de UI.
     */
    public static void logUiAction(String action, String element, String value) {
        Map<String, Object> data = Map.of(
            "action", action,
            "element", element,
            "value", value != null ? sanitizeValue(value) : "N/A"
        );
        String message = String.format("[UI] %s en '%s'", action, element);
        logInfo("UI", message, data);
    }

    // =================================================================================
    // LOGGING DE VALIDACIONES
    // =================================================================================

    /**
     * Registra una assertion exitosa.
     */
    public static void logAssertionSuccess(String assertion, String expected, String actual) {
        Map<String, Object> data = Map.of(
            "assertion", assertion,
            "expected", sanitizeValue(expected),
            "actual", sanitizeValue(actual),
            "status", "PASS"
        );
        String message = String.format("[ASSERTION PASS] %s", assertion);
        logInfo("ASSERTION", message, data);
    }

    /**
     * Registra una assertion fallida.
     */
    public static void logAssertionFailure(String assertion, String expected, String actual, String reason) {
        Map<String, Object> data = Map.of(
            "assertion", assertion,
            "expected", sanitizeValue(expected),
            "actual", sanitizeValue(actual),
            "reason", reason,
            "status", "FAIL"
        );
        String message = String.format("[ASSERTION FAIL] %s - %s", assertion, reason);
        logError("ASSERTION", message, data);
    }

    /**
     * Registra una validación personalizada.
     */
    public static void logValidation(String validationType, String description, boolean passed) {
        Map<String, Object> data = Map.of(
            "type", validationType,
            "description", description,
            "status", passed ? "PASS" : "FAIL"
        );
        String message = String.format("[VALIDATION %s] %s", passed ? "PASS" : "FAIL", description);

        if (passed) {
            logInfo("VALIDATION", message, data);
        } else {
            logError("VALIDATION", message, data);
        }
    }

    // =================================================================================
    // LOGGING DE ERRORES Y EXCEPCIONES
    // =================================================================================

    /**
     * Registra un error con contexto.
     */
    public static void logError(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage("ERROR", category, message);
        log.error(formattedMessage);

        if (context != null && !context.isEmpty()) {
            log.error("Error context: {}", sanitizeContext(context));
        }
    }

    /**
     * Registra una excepción.
     */
    public static void logException(String category, String message, Throwable throwable) {
        String formattedMessage = formatMessage("ERROR", category, message);
        log.error(formattedMessage, throwable);
    }

    /**
     * Registra un warning.
     */
    public static void logWarning(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage("WARN", category, message);
        log.warn(formattedMessage);

        if (context != null && !context.isEmpty()) {
            log.warn("Warning context: {}", sanitizeContext(context));
        }
    }

    // =================================================================================
    // LOGGING GENERAL
    // =================================================================================

    /**
     * Registra información general.
     */
    public static void logInfo(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage("INFO", category, message);
        log.info(formattedMessage);

        if (context != null && !context.isEmpty()) {
            log.info("Context: {}", sanitizeContext(context));
        }
    }

    /**
     * Registra debug information.
     */
    public static void logDebug(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage("DEBUG", category, message);
        log.debug(formattedMessage);

        if (context != null && !context.isEmpty()) {
            log.debug("Debug context: {}", sanitizeContext(context));
        }
    }

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Formatea el mensaje de log con contexto.
     */
    private static String formatMessage(String level, String category, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String framework = currentFramework.get() != null ? currentFramework.get() : "UNKNOWN";
        String testContext = currentTestContext.get() != null ? currentTestContext.get() : "NO_TEST";

        return String.format("[%s][%s][%s][%s] %s",
                           timestamp, framework, testContext, category, message);
    }

    /**
     * Sanitiza valores sensibles para logs.
     */
    private static String sanitizeValue(String value) {
        if (value == null) return "null";

        String lowerValue = value.toLowerCase();
        if (lowerValue.contains("password") || lowerValue.contains("token") ||
            lowerValue.contains("secret") || lowerValue.contains("key")) {
            return "***HIDDEN***";
        }

        // Truncar valores muy largos
        return value.length() > 200 ? value.substring(0, 200) + "..." : value;
    }

    /**
     * Sanitiza URLs para logs.
     */
    private static String sanitizeUrl(String url) {
        if (url == null) return "null";

        // Remover query parameters sensibles
        return url.replaceAll("([?&])(password|token|secret|key)=[^&]*", "$1$2=***HIDDEN***");
    }

    /**
     * Sanitiza el contexto para logs.
     */
    private static Map<String, Object> sanitizeContext(Map<String, Object> context) {
        Map<String, Object> sanitized = new ConcurrentHashMap<>();

        context.forEach((key, value) -> {
            String valueStr = value != null ? value.toString() : "null";
            sanitized.put(key, sanitizeValue(valueStr));
        });

        return sanitized;
    }

    // =================================================================================
    // MÉTODOS DE CONVENIENCIA PARA COMPATIBILIDAD
    // =================================================================================

    /**
     * Crea un logger específico para una clase - devuelve wrapper con API tradicional.
     */
    public static LoggerWrapper getLogger(Class<?> clazz) {
        // Auto-inicializar el framework si no está configurado
        if (currentFramework.get() == null) {
            autoDetectFramework(clazz);
        }
        return new LoggerWrapper(clazz.getSimpleName());
    }

    /**
     * Auto-detecta el framework basado en el package de la clase.
     */
    private static void autoDetectFramework(Class<?> clazz) {
        String packageName = clazz.getPackage().getName();

        if (packageName.contains(".apicore.")) {
            setFramework("API");
        } else if (packageName.contains(".webcore.")) {
            setFramework("WEB");
        } else if (packageName.contains(".mobilecore.")) {
            setFramework("MOBILE");
        } else if (packageName.contains(".common.")) {
            setFramework("COMMON");
        } else {
            setFramework("UNKNOWN");
        }
    }

    /**
     * Wrapper que proporciona API de logging tradicional.
     */
    public static class LoggerWrapper {
        private final String className;

        private LoggerWrapper(String className) {
            this.className = className;
        }

        public void debug(String message) {
            logDebug(className, message, null);
        }

        public void debug(String message, Object... args) {
            logDebug(className, String.format(message.replace("{}", "%s"), args), null);
        }

        public void info(String message) {
            logInfo(className, message, null);
        }

        public void info(String message, Object... args) {
            logInfo(className, String.format(message.replace("{}", "%s"), args), null);
        }

        public void warn(String message) {
            logWarning(className, message, null);
        }

        public void warn(String message, Object... args) {
            logWarning(className, String.format(message.replace("{}", "%s"), args), null);
        }

        public void error(String message) {
            logError(className, message, null);
        }

        public void error(String message, Object... args) {
            logError(className, String.format(message.replace("{}", "%s"), args), null);
        }

        public void error(String message, Throwable throwable) {
            logException(className, message, throwable);
        }
    }
}
