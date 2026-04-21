package com.qa.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Inicializador de configuración de logging para el QA Automation Framework.
 *
 * <p>Gestiona el contexto de logging mediante MDC (Mapped Diagnostic Context) para
 * proporcionar información contextual en todos los logs: módulo, test, framework, etc.
 *
 * <p><b>Características:</b>
 * <ul>
 *   <li>Configura MDC para tracking de módulos (API, WEB, MOBILE, COMMON)</li>
 *   <li>Gestiona contexto de tests en ejecución</li>
 *   <li>Suprime mensajes informativos innecesarios de librerías</li>
 *   <li>Thread-safe para ejecución paralela de tests</li>
 * </ul>
 *
 * <p><b>Uso básico:</b>
 * <pre>{@code
 * // Al inicio de tu test suite o clase
 * LoggingInitializer.initModuleContext("API");
 *
 * // Al inicio de cada test
 * LoggingInitializer.setTestContext("testLogin");
 *
 * // Al finalizar el test
 * LoggingInitializer.clearTestContext();
 *
 * // Al finalizar la suite
 * LoggingInitializer.clearAllContext();
 * }</pre>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 */
public class LoggingInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInitializer.class);

    // Módulos soportados
    public static final String MODULE_API = "API";
    public static final String MODULE_WEB = "WEB";
    public static final String MODULE_MOBILE = "MOBILE";
    public static final String MODULE_COMMON = "COMMON";

    // Keys para MDC
    private static final String MDC_MODULE = "module";
    private static final String MDC_TEST_NAME = "testName";
    private static final String MDC_FRAMEWORK = "framework";
    private static final String MDC_ENVIRONMENT = "environment";
    private static final String MDC_THREAD = "threadName";

    static {
        // Suprimir mensajes informativos de SLF4J y librerías
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("slf4j.internal.verbosity", "WARN");
        System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener");

        // Suprimir logs de wire protocol de Apache HttpClient
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }

    /**
     * Constructor privado para prevenir instanciación.
     */
    private LoggingInitializer() {
        throw new UnsupportedOperationException(
            "LoggingInitializer es una clase de utilidad y no debe ser instanciada");
    }

    /**
     * Inicializa el contexto de logging con el nombre del módulo.
     * Configura el MDC para que todos los logs subsecuentes incluyan el módulo.
     *
     * @param moduleName Nombre del módulo (API, WEB, MOBILE, COMMON)
     */
    public static void initModuleContext(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            moduleName = MODULE_COMMON;
        }

        MDC.put(MDC_MODULE, moduleName.toUpperCase());
        MDC.put(MDC_FRAMEWORK, "QA-Framework");
        MDC.put(MDC_THREAD, Thread.currentThread().getName());

        LOGGER.info("Contexto de logging inicializado para el módulo: {}", moduleName);
    }

    /**
     * Establece el contexto del test actual en el MDC.
     * Permite identificar qué test está generando cada LOG.
     *
     * @param testName Nombre del test en ejecución
     */
    public static void setTestContext(String testName) {
        if (testName != null && !testName.trim().isEmpty()) {
            MDC.put(MDC_TEST_NAME, testName);
            LOGGER.debug("Contexto de test establecido: {}", testName);
        }
    }

    /**
     * Establece el entorno de ejecución (dev, qa, prod).
     *
     * @param environment Entorno de ejecución
     */
    public static void setEnvironment(String environment) {
        if (environment != null && !environment.trim().isEmpty()) {
            MDC.put(MDC_ENVIRONMENT, environment.toLowerCase());
            LOGGER.debug("Entorno establecido: {}", environment);
        }
    }

    /**
     * Establece múltiples valores de contexto de una sola vez.
     *
     * @param contextMap Mapa con pares clave-valor para el contexto
     */
    public static void setContext(Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            contextMap.forEach(MDC::put);
            LOGGER.debug("Contexto múltiple establecido: {}", contextMap);
        }
    }

    /**
     * Obtiene el valor actual de una clave del MDC.
     *
     * @param key Clave a consultar
     * @return Valor asociado o null si no existe
     */
    public static String getContextValue(String key) {
        return MDC.get(key);
    }

    /**
     * Obtiene todos los valores actuales del MDC.
     *
     * @return Mapa con todos los valores de contexto
     */
    public static Map<String, String> getAllContext() {
        Map<String, String> contextCopy = MDC.getCopyOfContextMap();
        return contextCopy != null ? contextCopy : new HashMap<>();
    }

    /**
     * Limpia solo el contexto del test actual.
     * Mantiene el módulo y otros contextos globales.
     */
    public static void clearTestContext() {
        String testName = MDC.get(MDC_TEST_NAME);
        if (testName != null) {
            MDC.remove(MDC_TEST_NAME);
            LOGGER.debug("Contexto de test limpiado: {}", testName);
        }
    }

    /**
     * Limpia completamente el MDC.
     * Útil al finalizar una suite de tests o al cambiar de módulo.
     */
    public static void clearAllContext() {
        String module = MDC.get(MDC_MODULE);
        MDC.clear();
        LOGGER.info("Contexto MDC completamente limpiado para módulo: {}", module);
    }

    /**
     * Re-inicializa el contexto preservando solo el módulo.
     * Útil entre tests para limpiar contexto específico del test anterior.
     */
    public static void resetContext() {
        String module = MDC.get(MDC_MODULE);
        String framework = MDC.get(MDC_FRAMEWORK);
        String environment = MDC.get(MDC_ENVIRONMENT);

        MDC.clear();

        if (module != null) {
            MDC.put(MDC_MODULE, module);
        }
        if (framework != null) {
            MDC.put(MDC_FRAMEWORK, framework);
        }
        if (environment != null) {
            MDC.put(MDC_ENVIRONMENT, environment);
        }

        MDC.put(MDC_THREAD, Thread.currentThread().getName());

        LOGGER.debug("Contexto MDC reiniciado manteniendo: module={}, framework={}", module, framework);
    }

    /**
     * Método de conveniencia para inicialización completa.
     *
     * @param moduleName Nombre del módulo
     * @param environment Entorno de ejecución
     */
    public static void initializeComplete(String moduleName, String environment) {
        initModuleContext(moduleName);
        setEnvironment(environment);
        LOGGER.info("Logging inicializado completamente - Módulo: {}, Entorno: {}", moduleName, environment);
    }

    /**
     * Método público para forzar la carga de la clase y ejecutar el bloque static.
     * Útil para asegurar que la configuración se aplique antes de cualquier logging.
     */
    public static void initialize() {
        // Fuerza la carga de la clase y ejecución del bloque static
        LOGGER.debug("LoggingInitializer cargado y configurado");
    }
}

