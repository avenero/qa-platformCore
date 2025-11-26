package com.scotia.qa.common.jiraXray;

import com.scotia.qa.common.jiraXray.config.ReportConfig;
import com.scotia.qa.common.jiraXray.service.JiraTestCaseUpdaterService;
import com.scotia.qa.common.logging.TestLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase principal para gestionar la actualización de test cases en Jira.
 * Permite integración sencilla con diferentes frameworks de testing.
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class JiraTestCaseManager {

    private static JiraTestCaseUpdaterService serviceInstance;
    private static boolean initialized = false;

    private JiraTestCaseManager() {}

    /**
     * Inicializa el servicio de actualización de test cases
     */
    public static synchronized void initialize() {
        if (initialized) {
            TestLogger.logInfo("JIRA_MANAGER", "JiraTestCaseManager ya inicializado", null);
            return;
        }

        TestLogger.logInfo("JIRA_MANAGER", "🚀 Inicializando JiraTestCaseManager", null);

        ReportConfig config = loadConfig();
        serviceInstance = new JiraTestCaseUpdaterService(config);
        initialized = true;

        TestLogger.logInfo("JIRA_MANAGER", "✅ JiraTestCaseManager inicializado correctamente", null);
        logConfiguration(config);
    }

    /**
     * Procesa y envía resultados de tests a Jira
     *
     * @param testResults resultados en formato nativo (Cucumber JSON, JUnit XML, etc.)
     */
    public static void sendTestResults(String testResults) {
        ensureInitialized();
        serviceInstance.processAndSendResults(testResults);
    }

    /**
     * Método de compatibilidad (deprecated)
     *
     * @deprecated Usar sendTestResults(String testResults) en su lugar
     */
    @Deprecated
    public static void updateTestCases(String tagCode, String projectCode) {
        ensureInitialized();
        TestLogger.logWarning("JIRA_MANAGER", "⚠️ updateTestCases está deprecated. Usar sendTestResults(String) en su lugar", null);
        serviceInstance.updateTestCasesFromFeatures(tagCode, projectCode);
    }

    /**
     * Obtiene la instancia del servicio (para uso avanzado)
     */
    public static JiraTestCaseUpdaterService getService() {
        ensureInitialized();
        return serviceInstance;
    }

    /**
     * Cierra el manager y libera recursos
     */
    public static synchronized void shutdown() {
        if (initialized) {
            TestLogger.logInfo("JIRA_MANAGER", "🔄 Cerrando JiraTestCaseManager", null);
            serviceInstance = null;
            initialized = false;
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * Carga la configuración desde YAML o variables de entorno
     */
    private static ReportConfig loadConfig() {
        ReportConfig config;

        try (InputStream in = JiraTestCaseManager.class.getClassLoader().getResourceAsStream("reporting-config.yml")) {
            if (in == null) {
                TestLogger.logWarning("JIRA_MANAGER", "No se encontró reporting-config.yml, usando variables de entorno", null);
                config = ReportConfig.loadFromEnv();
            } else {
                TestLogger.logInfo("JIRA_MANAGER", "📄 Cargando configuración desde reporting-config.yml", null);
                config = loadFromYaml(in);
            }
        } catch (Exception e) {
            TestLogger.logException("JIRA_MANAGER", "Error al cargar configuración, usando variables de entorno", e);
            config = ReportConfig.loadFromEnv();
        }

        return config;
    }

    /**
     * Carga configuración desde archivo YAML
     */
    private static ReportConfig loadFromYaml(InputStream in) {
        Yaml yaml = new Yaml();
        Map<String, Object> rawConfig = yaml.load(in);
        rawConfig = (Map<String, Object>) resolveEnvVars(rawConfig);

        ReportConfig config = new ReportConfig();

        // Cargar credenciales principales
        if (rawConfig.containsKey("user")) {
            config.setUser(String.valueOf(rawConfig.get("user")));
        }
        if (rawConfig.containsKey("password")) {
            config.setPassword(String.valueOf(rawConfig.get("password")));
        }

        // Cargar parámetros
        if (rawConfig.containsKey("params") && rawConfig.get("params") instanceof Map) {
            Map<String, Object> params = (Map<String, Object>) rawConfig.get("params");

            if (params.containsKey("uploadReport")) {
                config.setUploadReport(Boolean.valueOf(String.valueOf(params.get("uploadReport"))));
            }
            if (params.containsKey("testExecution")) {
                config.setTestExecution(String.valueOf(params.get("testExecution")));
            }
        }

        return config;
    }

    /**
     * Resuelve variables de entorno en la configuración
     */
    private static Object resolveEnvVars(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            Pattern pattern = Pattern.compile("\\$\\{([^:}]+):?([^}]*)}");
            Matcher matcher = pattern.matcher(str);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String envVar = matcher.group(1);
                String defaultValue = matcher.group(2);
                String resolved = System.getenv(envVar);
                if (resolved == null || resolved.isEmpty()) {
                    resolved = defaultValue;
                }
                matcher.appendReplacement(sb, resolved != null ? Matcher.quoteReplacement(resolved) : "");
            }
            matcher.appendTail(sb);
            return sb.toString();
        } else if (value instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                entry.setValue(resolveEnvVars(entry.getValue()));
            }
            return map;
        } else if (value instanceof java.util.List) {
            java.util.List<Object> list = (java.util.List<Object>) value;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, resolveEnvVars(list.get(i)));
            }
            return list;
        }
        return value;
    }

    /**
     * Log de la configuración cargada
     */
    private static void logConfiguration(ReportConfig config) {
        TestLogger.logInfo("JIRA_MANAGER", "=== Configuración Cargada ===", null);
        TestLogger.logInfo("JIRA_MANAGER", "Usuario: " + (config.getUser() != null ? "✓ Configurado" : "✗ No configurado"), null);
        TestLogger.logInfo("JIRA_MANAGER", "Password: " + (config.getPassword() != null ? "✓ Configurado" : "✗ No configurado"), null);
        TestLogger.logInfo("JIRA_MANAGER", "Upload Report: " + config.getUploadReport(), null);
        TestLogger.logInfo("JIRA_MANAGER", "Test Execution: " + config.getTestExecution(), null);
        TestLogger.logInfo("JIRA_MANAGER", "============================", null);
    }
}
