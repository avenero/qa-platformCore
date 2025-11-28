package com.scotia.qa.common.logging;

import com.scotia.qa.common.config.ConfigManager;

/**
 * Detector automático del nombre del módulo consumidor del framework.
 *
 * <p>Esta clase implementa una estrategia de detección multi-nivel para identificar
 * el nombre del módulo que está ejecutando los tests. Intenta múltiples fuentes
 * en orden de prioridad para garantizar que siempre se obtenga un nombre válido.</p>
 *
 * <p><b>Estrategia de Detección (en orden de prioridad):</b></p>
 * <ol>
 *   <li><b>System Property:</b> {@code -Dframework.module.name=BANKING}</li>
 *   <li><b>Variable de entorno:</b> {@code FRAMEWORK_MODULE_NAME=BANKING}</li>
 *   <li><b>ConfigManager:</b> Propiedad {@code framework.module.name} en config-{env}.properties</li>
 *   <li><b>Gradle project.name:</b> {@code -Dproject.name=qa-banking} → BANKING</li>
 *   <li><b>Package detection:</b> Detecta del package de las clases de steps</li>
 *   <li><b>Fallback:</b> Retorna "TEST" como valor por defecto</li>
 * </ol>
 *
 * <p><b>Configuración recomendada en el módulo:</b></p>
 *
 * <p><b>Opción 1: En config-{env}.properties (Recomendado):</b></p>
 * <pre>
 * # src/test/resources/config-qa.properties
 * framework.module.name=BANKING
 * framework.module.type=HYBRID  # WEB+API
 * </pre>
 *
 * <p><b>Opción 2: En build.gradle:</b></p>
 * <pre>
 * test {
 *     systemProperty 'framework.module.name', 'BANKING'
 *     systemProperty 'framework.module.type', 'HYBRID'
 * }
 * </pre>
 *
 * <p><b>Opción 3: Variables de entorno:</b></p>
 * <pre>
 * export FRAMEWORK_MODULE_NAME=BANKING
 * export FRAMEWORK_MODULE_TYPE=HYBRID
 * ./gradlew test
 * </pre>
 *
 * <p><b>Uso interno del framework:</b></p>
 * <pre>
 * // En hooks de Cucumber
 * String moduleName = ModuleDetector.detectModuleName();
 * TestLogger.setFramework(moduleName);  // Logs: [BANKING] en lugar de [API]
 * </pre>
 *
 * <p><b>Detección automática sin configuración:</b></p>
 * Si no se configura explícitamente, intenta detectar del nombre del proyecto Gradle:
 * <ul>
 *   <li>{@code qa-banking} → {@code BANKING}</li>
 *   <li>{@code qa-mobile-payments} → {@code MOBILE_PAYMENTS}</li>
 *   <li>{@code banking-automation} → {@code BANKING_AUTOMATION}</li>
 * </ul>
 *
 * @author Abnel Venero
 * @version 1.0.0
 * @since 2025-11-27
 */
public class ModuleDetector {

    private static final String PROP_MODULE_NAME = "framework.module.name";
    private static final String PROP_MODULE_TYPE = "framework.module.type";
    private static final String ENV_MODULE_NAME = "FRAMEWORK_MODULE_NAME";
    private static final String ENV_MODULE_TYPE = "FRAMEWORK_MODULE_TYPE";
    private static final String PROP_PROJECT_NAME = "project.name";

    private static String cachedModuleName = null;
    private static String cachedModuleType = null;

    /**
     * Constructor privado - clase de utilidad.
     */
    private ModuleDetector() {
        throw new UnsupportedOperationException("ModuleDetector es una clase de utilidad");
    }

    /**
     * Detecta automáticamente el nombre del módulo consumidor.
     *
     * <p>Usa caché para evitar re-detección en cada llamada.</p>
     *
     * @return Nombre del módulo (ej: "BANKING", "MOBILE", "AUTOS")
     */
    public static String detectModuleName() {
        if (cachedModuleName != null) {
            return cachedModuleName;
        }

        // Estrategia 1: System Property explícita
        String moduleName = System.getProperty(PROP_MODULE_NAME);
        if (isValid(moduleName)) {
            cachedModuleName = moduleName.trim().toUpperCase();
            logDetection("System Property", cachedModuleName);
            return cachedModuleName;
        }

        // Estrategia 2: Variable de entorno
        moduleName = System.getenv(ENV_MODULE_NAME);
        if (isValid(moduleName)) {
            cachedModuleName = moduleName.trim().toUpperCase();
            logDetection("Environment Variable", cachedModuleName);
            return cachedModuleName;
        }

        // Estrategia 3: ConfigManager (config-{env}.properties)
        try {
            ConfigManager config = ConfigManager.getInstance();
            moduleName = config.get(PROP_MODULE_NAME);
            if (isValid(moduleName)) {
                cachedModuleName = moduleName.trim().toUpperCase();
                logDetection("ConfigManager", cachedModuleName);
                return cachedModuleName;
            }
        } catch (Exception e) {
            // ConfigManager puede fallar si no hay archivo de config, continuar
        }

        // Estrategia 4: Gradle project.name
        String projectName = System.getProperty(PROP_PROJECT_NAME);
        if (isValid(projectName)) {
            cachedModuleName = extractModuleFromProjectName(projectName);
            logDetection("Gradle Project Name", cachedModuleName);
            return cachedModuleName;
        }

        // Estrategia 5: Detección del package (más complejo, puede fallar)
        try {
            moduleName = detectFromStackTrace();
            if (isValid(moduleName)) {
                cachedModuleName = moduleName.toUpperCase();
                logDetection("Package Detection", cachedModuleName);
                return cachedModuleName;
            }
        } catch (Exception e) {
            // Ignorar errores de detección por package
        }

        // Fallback: Valor por defecto
        cachedModuleName = "TEST";
        logDetection("Fallback", cachedModuleName);
        return cachedModuleName;
    }

    /**
     * Detecta el tipo de módulo (WEB, API, MOBILE, HYBRID).
     *
     * @return Tipo del módulo o null si no está configurado
     */
    public static String detectModuleType() {
        if (cachedModuleType != null) {
            return cachedModuleType;
        }

        // System Property
        String moduleType = System.getProperty(PROP_MODULE_TYPE);
        if (isValid(moduleType)) {
            cachedModuleType = moduleType.trim().toUpperCase();
            return cachedModuleType;
        }

        // Environment variable
        moduleType = System.getenv(ENV_MODULE_TYPE);
        if (isValid(moduleType)) {
            cachedModuleType = moduleType.trim().toUpperCase();
            return cachedModuleType;
        }

        // ConfigManager
        try {
            ConfigManager config = ConfigManager.getInstance();
            moduleType = config.get(PROP_MODULE_TYPE);
            if (isValid(moduleType)) {
                cachedModuleType = moduleType.trim().toUpperCase();
                return cachedModuleType;
            }
        } catch (Exception e) {
            // Ignorar
        }

        return null;  // No configurado
    }

    /**
     * Limpia el caché de detección.
     * Útil para tests o cuando el módulo cambia dinámicamente.
     */
    public static void clearCache() {
        cachedModuleName = null;
        cachedModuleType = null;
    }

    /**
     * Extrae el nombre del módulo del nombre del proyecto Gradle.
     *
     * <p>Ejemplos:</p>
     * <ul>
     *   <li>{@code qa-banking} → {@code BANKING}</li>
     *   <li>{@code qa-mobile-payments} → {@code MOBILE_PAYMENTS}</li>
     *   <li>{@code banking-automation} → {@code BANKING_AUTOMATION}</li>
     * </ul>
     *
     * @param projectName Nombre del proyecto Gradle
     * @return Nombre del módulo extraído
     */
    private static String extractModuleFromProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return "TEST";
        }

        String name = projectName.trim();

        // Remover prefijos comunes
        if (name.startsWith("qa-")) {
            name = name.substring(3);
        } else if (name.startsWith("test-")) {
            name = name.substring(5);
        } else if (name.startsWith("automation-")) {
            name = name.substring(11);
        }

        // Remover sufijos comunes
        if (name.endsWith("-automation")) {
            name = name.substring(0, name.length() - 11);
        } else if (name.endsWith("-tests")) {
            name = name.substring(0, name.length() - 6);
        } else if (name.endsWith("-test")) {
            name = name.substring(0, name.length() - 5);
        }

        // Convertir guiones a underscores y uppercase
        return name.replace("-", "_").toUpperCase();
    }

    /**
     * Intenta detectar el módulo analizando el stack trace.
     * Busca packages que no sean del framework (com.scotia.qa.*).
     *
     * @return Nombre del módulo detectado o null
     */
    private static String detectFromStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();

            // Ignorar clases del framework
            if (className.startsWith("com.scotia.qa.")) {
                continue;
            }

            // Ignorar clases de Java, JUnit, Cucumber, etc.
            if (className.startsWith("java.") ||
                className.startsWith("jdk.") ||
                className.startsWith("org.junit.") ||
                className.startsWith("io.cucumber.") ||
                className.startsWith("sun.")) {
                continue;
            }

            // Extraer el nombre del módulo del package
            // Ej: com.banking.steps.LoginSteps → BANKING
            String[] parts = className.split("\\.");
            if (parts.length >= 2) {
                return parts[1].toUpperCase();
            }
        }

        return null;
    }

    /**
     * Valida que un string no sea null ni vacío.
     */
    private static boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Loguea la detección del módulo (solo en modo debug).
     */
    private static void logDetection(String source, String moduleName) {
        TestLogger.logDebug("MODULE_DETECTOR",
            String.format("Módulo detectado desde %s: %s", source, moduleName),
            null);
    }

    /**
     * Información de debugging sobre el módulo detectado.
     *
     * @return Mapa con información de detección
     */
    public static java.util.Map<String, String> getDetectionInfo() {
        java.util.Map<String, String> info = new java.util.HashMap<>();
        info.put("moduleName", detectModuleName());
        info.put("moduleType", detectModuleType() != null ? detectModuleType() : "NOT_CONFIGURED");
        info.put("isCached", cachedModuleName != null ? "true" : "false");
        return info;
    }
}

