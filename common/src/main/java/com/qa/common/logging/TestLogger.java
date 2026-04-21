package com.qa.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
 *   <li>Contexto por thread (ThreadLocal) para ejecución paralela segura</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class TestLogger {

    // Inicializar configuración de logging para suprimir mensajes de SLF4J
    static {
        LoggingInitializer.initialize();
    }

    private static final Logger LOG = LoggerFactory.getLogger(TestLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Longitud máxima para valores en logs. */
    private static final int MAX_LOG_VALUE_LEN = 200;

    // Contexto por thread (thread-safe para ejecución paralela de tests)
    private static final ThreadLocal<Map<String, Object>> THREAD_CONTEXT =
            ThreadLocal.withInitial(HashMap::new);

    private TestLogger() {
        // Utility class - no instances
    }

    // =================================================================================
    // CONFIGURACIÓN DE CONTEXTO (Delegado a LoggingInitializer)
    // =================================================================================

    /**
     * Establece el contexto del test actual.
     * Delega a LoggingInitializer para usar MDC.
     *
     * @param testName nombre del test que se está ejecutando
     */
    public static void setTestContext(String testName) {
        LoggingInitializer.setTestContext(testName);
        logInfo("TEST_START", "Iniciando test: " + testName, null);
    }

    /**
     * Establece el framework actual (API, Web, Mobile).
     * Delega a LoggingInitializer para usar MDC y almacena en contexto por thread.
     *
     * @param framework nombre del framework activo (ej: "API", "WEB", "MOBILE")
     */
    public static void setFramework(String framework) {
        LoggingInitializer.initModuleContext(framework);
        THREAD_CONTEXT.get().put("framework", framework);
    }

    /**
     * Limpia el contexto del test actual.
     * Delega a LoggingInitializer para limpiar MDC y limpia el contexto del thread.
     */
    public static void clearTestContext() {
        String testName = LoggingInitializer.getContextValue("testName");
        if (testName != null) {
            logInfo("TEST_END", "Finalizando test: " + testName, null);
        }
        LoggingInitializer.clearTestContext();
        THREAD_CONTEXT.remove();
    }

    // =================================================================================
    // LOGGING DE STEPS Y ACCIONES
    // =================================================================================

    /**
     * Registra un step de testing.
     *
     * @param stepType    tipo de step (GIVEN, WHEN, THEN, etc.)
     * @param description descripción del step
     */
    public static void logStep(String stepType, String description) {
        logStep(stepType, description, null);
    }

    /**
     * Registra un step de testing con datos adicionales.
     *
     * @param stepType    tipo de step (GIVEN, WHEN, THEN, etc.)
     * @param description descripción del step
     * @param data        datos de contexto adicionales
     */
    public static void logStep(String stepType, String description, Map<String, Object> data) {
        String message = String.format("[STEP][%s] %s", stepType.toUpperCase(), description);
        logInfo("STEP", message, data);
    }

    /**
     * Registra una acción HTTP.
     *
     * @param method     método HTTP (GET, POST, PUT, DELETE, etc.)
     * @param url        URL de la petición
     * @param statusCode código de estado HTTP de la respuesta
     * @param duration   duración de la petición en milisegundos
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
     *
     * @param action  acción realizada (click, type, etc.)
     * @param element identificador del elemento de interfaz
     * @param value   valor introducido o seleccionado, puede ser null
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
     *
     * @param assertion nombre o descripción de la assertion
     * @param expected  valor esperado
     * @param actual    valor actual obtenido
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
     *
     * @param assertion nombre o descripción de la assertion
     * @param expected  valor esperado
     * @param actual    valor actual obtenido
     * @param reason    razón del fallo
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
     *
     * @param validationType tipo de validación realizada
     * @param description    descripción de la validación
     * @param passed         {@code true} si la validación fue exitosa
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
     *
     * @param category categoría del error (ej: "HTTP", "DB", "ASSERTION")
     * @param message  mensaje descriptivo del error
     * @param context  datos de contexto adicionales, puede ser null
     */
    public static void logError(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage(category, message);
        LOG.error(formattedMessage);

        if (context != null && !context.isEmpty()) {
            LOG.error("Error context: {}", sanitizeContext(context));
        }
    }

    /**
     * Registra una excepción.
     *
     * @param category  categoría del error
     * @param message   mensaje descriptivo
     * @param throwable excepción a registrar
     */
    public static void logException(String category, String message, Throwable throwable) {
        String formattedMessage = formatMessage(category, message);
        LOG.error(formattedMessage, throwable);
    }

    /**
     * Registra un warning.
     *
     * @param category categoría del warning
     * @param message  mensaje descriptivo
     * @param context  datos de contexto adicionales, puede ser null
     */
    public static void logWarning(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage(category, message);
        LOG.warn(formattedMessage);

        if (context != null && !context.isEmpty()) {
            LOG.warn("Warning context: {}", sanitizeContext(context));
        }
    }

    // =================================================================================
    // LOGGING GENERAL
    // =================================================================================

    /**
     * Registra información general.
     *
     * @param category categoría del mensaje
     * @param message  mensaje informativo
     * @param context  datos de contexto adicionales, puede ser null
     */
    public static void logInfo(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage(category, message);
        LOG.info(formattedMessage);

        if (context != null && !context.isEmpty()) {
            LOG.info("Context: {}", sanitizeContext(context));
        }
    }

    /**
     * Registra información de debug.
     *
     * @param category categoría del mensaje
     * @param message  mensaje de debug
     * @param context  datos de contexto adicionales, puede ser null
     */
    public static void logDebug(String category, String message, Map<String, Object> context) {
        String formattedMessage = formatMessage(category, message);
        LOG.debug(formattedMessage);

        if (context != null && !context.isEmpty()) {
            LOG.debug("Debug context: {}", sanitizeContext(context));
        }
    }

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Formatea el mensaje con contexto actual.
     *
     * <p>NOTA: No se agrega módulo ni test aquí porque logback ya los muestra en el pattern:
     * Pattern: [%X{module}] [%X{testName}] LOGGER - mensaje.
     * Solo agregamos la categoría para contexto adicional.
     *
     * @param category categoría del mensaje
     * @param message  texto del mensaje
     * @return mensaje formateado con categoría
     */
    private static String formatMessage(String category, String message) {
        // Solo agregar categoría - módulo y test ya están en el pattern de logback
        return String.format("[%s] %s", category, message);
    }

    /**
     * Sanitiza valores sensibles para logs.
     *
     * @param value valor a sanitizar
     * @return valor sanitizado o {@code "***HIDDEN***"} si es sensible
     */
    private static String sanitizeValue(String value) {
        if (value == null) {
            return "null";
        }

        String lowerValue = value.toLowerCase();
        if (lowerValue.contains("password") || lowerValue.contains("token") ||
            lowerValue.contains("secret") || lowerValue.contains("key")) {
            return "***HIDDEN***";
        }

        // Truncar valores muy largos
        return value.length() > MAX_LOG_VALUE_LEN ? value.substring(0, MAX_LOG_VALUE_LEN) + "..." : value;
    }

    /**
     * Sanitiza URLs para logs eliminando query parameters sensibles.
     *
     * @param url URL a sanitizar
     * @return URL con parámetros sensibles enmascarados
     */
    private static String sanitizeUrl(String url) {
        if (url == null) {
            return "null";
        }

        // Remover query parameters sensibles
        return url.replaceAll("([?&])(password|token|secret|key)=[^&]*", "$1$2=***HIDDEN***");
    }

    /**
     * Sanitiza el contexto para logs enmascarando valores sensibles.
     *
     * @param context mapa de contexto a sanitizar
     * @return nuevo mapa con los valores sanitizados
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
     * Crea un LOGGER específico para una clase, devuelve wrapper con API tradicional.
     *
     * @param clazz clase para la cual se crea el LOGGER
     * @return instancia de {@link LoggerWrapper} configurada para la clase
     */
    public static LoggerWrapper getLogger(Class<?> clazz) {
        // Auto-inicializar el framework si no está configurado en MDC
        String currentModule = LoggingInitializer.getContextValue("module");
        if (currentModule == null) {
            autoDetectFramework(clazz);
        }
        return new LoggerWrapper(clazz.getSimpleName());
    }

    /**
     * Auto-detecta el framework basado en el package de la clase y lo configura en MDC.
     *
     * @param clazz clase cuyo package se usa para inferir el módulo
     */
    private static void autoDetectFramework(Class<?> clazz) {
        String packageName = clazz.getPackage().getName();
        String moduleName;

        if (packageName.contains(".apicore.")) {
            moduleName = LoggingInitializer.MODULE_API;
        } else if (packageName.contains(".webcore.")) {
            moduleName = LoggingInitializer.MODULE_WEB;
        } else if (packageName.contains(".mobilecore.")) {
            moduleName = LoggingInitializer.MODULE_MOBILE;
        } else if (packageName.contains(".common.")) {
            moduleName = LoggingInitializer.MODULE_COMMON;
        } else {
            moduleName = "UNKNOWN";
        }

        LoggingInitializer.initModuleContext(moduleName);
    }

    /**
     * Wrapper que proporciona API de logging tradicional compatible con SLF4J.
     */
    public static class LoggerWrapper {

        private final String className;

        /**
         * Constructor privado; usar {@link TestLogger#getLogger(Class)}.
         *
         * @param className nombre simple de la clase para el contexto del LOG
         */
        private LoggerWrapper(String className) {
            this.className = className;
        }

        /**
         * Registra un mensaje a nivel DEBUG.
         *
         * @param message mensaje a registrar
         */
        public void debug(String message) {
            logDebug(className, message, null);
        }

        /**
         * Registra un mensaje a nivel DEBUG con parámetros.
         *
         * @param message mensaje con placeholders {@code {}}
         * @param args    argumentos para los placeholders
         */
        public void debug(String message, Object... args) {
            logDebug(className, String.format(message.replace("{}", "%s"), args), null);
        }

        /**
         * Registra un mensaje a nivel INFO.
         *
         * @param message mensaje a registrar
         */
        public void info(String message) {
            logInfo(className, message, null);
        }

        /**
         * Registra un mensaje a nivel INFO con parámetros.
         *
         * @param message mensaje con placeholders {@code {}}
         * @param args    argumentos para los placeholders
         */
        public void info(String message, Object... args) {
            logInfo(className, String.format(message.replace("{}", "%s"), args), null);
        }

        /**
         * Registra un mensaje a nivel WARN.
         *
         * @param message mensaje a registrar
         */
        public void warn(String message) {
            logWarning(className, message, null);
        }

        /**
         * Registra un mensaje a nivel WARN con parámetros.
         *
         * @param message mensaje con placeholders {@code {}}
         * @param args    argumentos para los placeholders
         */
        public void warn(String message, Object... args) {
            logWarning(className, String.format(message.replace("{}", "%s"), args), null);
        }

        /**
         * Registra un mensaje a nivel ERROR.
         *
         * @param message mensaje a registrar
         */
        public void error(String message) {
            logError(className, message, null);
        }

        /**
         * Registra un mensaje a nivel ERROR con parámetros.
         *
         * @param message mensaje con placeholders {@code {}}
         * @param args    argumentos para los placeholders
         */
        public void error(String message, Object... args) {
            logError(className, String.format(message.replace("{}", "%s"), args), null);
        }

        /**
         * Registra un mensaje a nivel ERROR con excepción asociada.
         *
         * @param message   mensaje a registrar
         * @param throwable excepción a registrar
         */
        public void error(String message, Throwable throwable) {
            logException(className, message, throwable);
        }
    }
}
