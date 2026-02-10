package com.scotia.qa.common.reporting.core.config;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.logging.TestLogger;

import java.util.Properties;

/**
 * Configuración unificada para el sistema de reporting.
 * Combina configuraciones de Jira y Extent Reports.
 *
 * Los módulos configuran en config-scotia.properties:
 * <pre>
 * # Master switch
 * reporting.enabled=true
 * reporting.environment=QA
 *
 * # Ver JiraConfig y ExtentConfig para propiedades específicas
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class ReportingConfig {

    private boolean enabled;
    private String environment;

    private JiraConfig jira;
    private ExtentConfig extent;

    public ReportingConfig() {
        this.jira = new JiraConfig();
        this.extent = new ExtentConfig();
        this.enabled = true; // Por defecto habilitado
    }

    /**
     * Carga configuración desde ConfigManager (config-scotia.properties)
     */
    public static ReportingConfig fromConfigManager() {
        ConfigManager configManager = ConfigManager.getInstance();
        ReportingConfig config = new ReportingConfig();

        TestLogger.logInfo("REPORTING_CONFIG", "📄 Cargando configuración de reporting...", null);

        // Master settings
        config.setEnabled(Boolean.parseBoolean(
                configManager.get("reporting.enabled", "true")
        ));
        config.setEnvironment(configManager.get("reporting.environment", "local"));

        // Jira configuration
        config.getJira().setUrl(configManager.get("jira.url"));
        config.getJira().setUser(configManager.get("jira.user"));
        config.getJira().setPassword(configManager.get("jira.password"));
        config.getJira().setProjectKey(configManager.get("jira.projectKey"));
        config.getJira().setTestExecutionId(configManager.get("jira.testExecutionId"));
        config.getJira().setAutoCreateExecution(Boolean.parseBoolean(
                configManager.get("jira.autoCreateExecution", "false")
        ));
        config.getJira().setTestEnvironment(configManager.get("jira.testEnvironment", config.getEnvironment()));
        config.getJira().setUpdateStatus(Boolean.parseBoolean(
                configManager.get("jira.updateStatus", "false")
        ));
        config.getJira().setUploadReport(Boolean.parseBoolean(
                configManager.get("jira.uploadReport", "false")
        ));
        config.getJira().setIncludeEvidences(Boolean.parseBoolean(
                configManager.get("jira.includeEvidences", "true")
        ));
        config.getJira().setMaxAttachmentSizeMb(Integer.parseInt(
                configManager.get("jira.maxAttachmentSizeMb", "10")
        ));
        config.getJira().setFailOnError(Boolean.parseBoolean(
                configManager.get("jira.failOnError", "false")
        ));

        String updateMode = configManager.get("jira.updateMode", "BATCH");
        try {
            config.getJira().setUpdateMode(JiraConfig.UpdateMode.valueOf(updateMode.toUpperCase()));
        } catch (IllegalArgumentException e) {
            TestLogger.logWarning("REPORTING_CONFIG",
                "⚠️ Update mode inválido: " + updateMode + ", usando BATCH", null);
            config.getJira().setUpdateMode(JiraConfig.UpdateMode.BATCH);
        }

        // Extent configuration
        config.getExtent().setEnabled(Boolean.parseBoolean(
                configManager.get("extent.enabled", "true")
        ));
        config.getExtent().setOutputPath(
                configManager.get("extent.outputPath", "build/reports/extent/")
        );
        config.getExtent().setReportName(
                configManager.get("extent.reportName", "execution-report.html")
        );
        config.getExtent().setDocumentTitle(
                configManager.get("extent.documentTitle", "Test Execution Report")
        );

        // ReportTitle: Usar nombre del módulo si no está configurado
        String reportTitle = configManager.get("extent.reportTitle");
        if (reportTitle == null || reportTitle.isEmpty()) {
            // Detectar nombre del módulo automáticamente
            String moduleName = com.scotia.qa.common.logging.ModuleDetector.detectModuleName();
            reportTitle = "Automated Tests - " + moduleName;
        }
        config.getExtent().setReportTitle(reportTitle);

        config.getExtent().setTheme(
                configManager.get("extent.theme", "STANDARD")
        );
        config.getExtent().setIncludeScreenshots(Boolean.parseBoolean(
                configManager.get("extent.includeScreenshots", "true")
        ));
        config.getExtent().setIncludeSystemInfo(Boolean.parseBoolean(
                configManager.get("extent.includeSystemInfo", "true")
        ));
        config.getExtent().setIncludeTimeline(Boolean.parseBoolean(
                configManager.get("extent.includeTimeline", "true")
        ));

        // Cargar system info custom desde properties (extent.systemInfo.*)
        // Ejemplo: extent.systemInfo.Proyecto=Evaluador Auto
        //          extent.systemInfo.Módulo=Simulación de Préstamos
        //          extent.systemInfo.Ambiente=QA
        loadExtentSystemInfo(configManager, config.getExtent());

        TestLogger.logInfo("REPORTING_CONFIG", "✅ Configuración cargada", null);
        TestLogger.logInfo("REPORTING_CONFIG", "   - Reporting habilitado: " + config.isEnabled(), null);
        TestLogger.logInfo("REPORTING_CONFIG", "   - Jira update status: " + config.getJira().isUpdateStatus(), null);
        TestLogger.logInfo("REPORTING_CONFIG", "   - Jira upload report: " + config.getJira().isUploadReport(), null);
        TestLogger.logInfo("REPORTING_CONFIG", "   - Extent enabled: " + config.getExtent().isEnabled(), null);

        return config;
    }

    /**
     * Carga system info custom desde ConfigManager.
     * Busca todas las properties con prefijo "extent.systemInfo."
     *
     * Ejemplo en config-scotia.properties:
     * <pre>
     * extent.systemInfo.Proyecto=Evaluador Auto
     * extent.systemInfo.Módulo=Simulación de Préstamos
     * extent.systemInfo.Ambiente=QA
     * extent.systemInfo.Fecha=${fecha_actual}
     * </pre>
     *
     * @param configManager ConfigManager para leer properties
     * @param extentConfig ExtentConfig donde agregar el system info
     */
    private static void loadExtentSystemInfo(ConfigManager configManager, ExtentConfig extentConfig) {
        String prefix = "extent.systemInfo.";

        // Cargar desde System Properties
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith(prefix)) {
                String infoKey = keyStr.substring(prefix.length());
                extentConfig.addSystemInfo(infoKey, value.toString());
                TestLogger.logDebug("REPORTING_CONFIG",
                    String.format("System Info agregado: %s = %s", infoKey, value), null);
            }
        });

        // Cargar desde Environment Variables (con formato EXTENT_SYSTEMINFO_KEY)
        String envPrefix = "EXTENT_SYSTEMINFO_";
        System.getenv().forEach((key, value) -> {
            if (key.startsWith(envPrefix)) {
                String infoKey = key.substring(envPrefix.length())
                                    .replace("_", " ")  // Convertir underscores a espacios
                                    .toLowerCase();
                // Capitalizar primera letra de cada palabra
                infoKey = capitalizeWords(infoKey);
                extentConfig.addSystemInfo(infoKey, value);
                TestLogger.logDebug("REPORTING_CONFIG",
                    String.format("System Info agregado desde ENV: %s = %s", infoKey, value), null);
            }
        });

        // Agregar fecha actual si se configuró con placeholder
        String fechaKey = prefix + "Fecha";
        String fechaValue = configManager.get(fechaKey);
        if (fechaValue != null && (fechaValue.contains("${") || fechaValue.equalsIgnoreCase("auto"))) {
            extentConfig.addSystemInfo("Fecha",
                java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                ));
        }
    }

    /**
     * Capitaliza la primera letra de cada palabra.
     */
    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Carga configuración desde Properties
     */
    public static ReportingConfig fromProperties(Properties props) {
        ReportingConfig config = new ReportingConfig();

        config.setEnabled(Boolean.parseBoolean(
                props.getProperty("reporting.enabled", "true")
        ));
        config.setEnvironment(props.getProperty("reporting.environment", "local"));

        // Jira
        config.getJira().setUrl(props.getProperty("jira.url"));
        config.getJira().setUser(props.getProperty("jira.user"));
        config.getJira().setPassword(props.getProperty("jira.password"));
        config.getJira().setProjectKey(props.getProperty("jira.projectKey"));
        config.getJira().setTestExecutionId(props.getProperty("jira.testExecutionId"));
        config.getJira().setUpdateStatus(Boolean.parseBoolean(
                props.getProperty("jira.updateStatus", "false")
        ));
        config.getJira().setUploadReport(Boolean.parseBoolean(
                props.getProperty("jira.uploadReport", "false")
        ));

        // Extent
        config.getExtent().setEnabled(Boolean.parseBoolean(
                props.getProperty("extent.enabled", "true")
        ));
        config.getExtent().setOutputPath(
                props.getProperty("extent.outputPath", "build/reports/extent/")
        );

        return config;
    }


    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public JiraConfig getJira() {
        return jira;
    }

    public void setJira(JiraConfig jira) {
        this.jira = jira;
    }

    public ExtentConfig getExtent() {
        return extent;
    }

    public void setExtent(ExtentConfig extent) {
        this.extent = extent;
    }

    @Override
    public String toString() {
        return "ReportingConfig{" +
                "enabled=" + enabled +
                ", environment='" + environment + '\'' +
                ", jira=" + jira +
                ", extent=" + extent +
                '}';
    }
}

