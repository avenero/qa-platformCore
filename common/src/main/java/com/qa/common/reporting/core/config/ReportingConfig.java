package com.qa.common.reporting.core.config;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;

import java.util.Properties;

/**
 * Configuración unificada para el sistema de reporting (Extent Reports).
 *
 * <p>Las integraciones externas como Jira/Xray se gestionan desde el Backend,
 * no desde el Core. El Core sólo genera reportes locales (Extent).
 *
 * Los módulos configuran en config-socorro.properties:
 * <pre>
 * reporting.enabled=true
 * reporting.environment=QA
 * reporting.title=Automated Tests
 * </pre>
 *
 * @author QA Frameworks Core
 * @version 2.0.0
 * @since 1.0.0
 */
public class ReportingConfig {

    private boolean enabled;
    private String environment;
    private ExtentConfig extent;

    public ReportingConfig() {
        this.extent = new ExtentConfig();
        this.enabled = true;
    }

    /** Carga configuración desde ConfigManager (config-*.properties) */
    public static ReportingConfig fromConfigManager() {
        ConfigManager configManager = ConfigManager.getInstance();
        ReportingConfig config = new ReportingConfig();

        TestLogger.logInfo("REPORTING_CONFIG", "📄 Cargando configuración de reporting...", null);

        config.setEnabled(Boolean.parseBoolean(configManager.get("reporting.enabled", "true")));
        config.setEnvironment(configManager.get("reporting.environment", "local"));

        config.getExtent().setEnabled(Boolean.parseBoolean(configManager.get("extent.enabled", "true")));
        config.getExtent().setOutputPath(configManager.get("extent.outputPath", "build/reports/extent/"));
        config.getExtent().setReportName(configManager.get("extent.reportName", "execution-report.html"));
        config.getExtent().setDocumentTitle(configManager.get("extent.documentTitle", "Test Execution Report"));

        String reportTitle = configManager.get("extent.reportTitle");
        if (reportTitle == null || reportTitle.isEmpty()) {
            String moduleName = configManager.get("framework.module.name", "PLATFORM");
            reportTitle = "Automated Tests - " + moduleName;
        }
        config.getExtent().setReportTitle(reportTitle);
        config.getExtent().setTheme(configManager.get("extent.theme", "STANDARD"));
        config.getExtent().setIncludeScreenshots(Boolean.parseBoolean(configManager.get("extent.includeScreenshots", "true")));
        config.getExtent().setIncludeSystemInfo(Boolean.parseBoolean(configManager.get("extent.includeSystemInfo", "true")));
        config.getExtent().setIncludeTimeline(Boolean.parseBoolean(configManager.get("extent.includeTimeline", "true")));

        loadExtentSystemInfo(configManager, config.getExtent());

        TestLogger.logInfo("REPORTING_CONFIG", "✅ Configuración cargada — Reporting: " + config.isEnabled()
            + " | Extent: " + config.getExtent().isEnabled(), null);
        return config;
    }

    /** Carga configuración desde Properties */
    public static ReportingConfig fromProperties(Properties props) {
        ReportingConfig config = new ReportingConfig();
        config.setEnabled(Boolean.parseBoolean(props.getProperty("reporting.enabled", "true")));
        config.setEnvironment(props.getProperty("reporting.environment", "local"));
        config.getExtent().setEnabled(Boolean.parseBoolean(props.getProperty("extent.enabled", "true")));
        config.getExtent().setOutputPath(props.getProperty("extent.outputPath", "build/reports/extent/"));
        return config;
    }

    private static void loadExtentSystemInfo(ConfigManager configManager, ExtentConfig extentConfig) {
        String prefix = "extent.systemInfo.";
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith(prefix)) {
                extentConfig.addSystemInfo(keyStr.substring(prefix.length()), value.toString());
            }
        });
        String envPrefix = "EXTENT_SYSTEMINFO_";
        System.getenv().forEach((key, value) -> {
            if (key.startsWith(envPrefix)) {
                String infoKey = capitalizeWords(key.substring(envPrefix.length()).replace("_", " ").toLowerCase());
                extentConfig.addSystemInfo(infoKey, value);
            }
        });
        String fechaValue = configManager.get(prefix + "Fecha");
        if (fechaValue != null && (fechaValue.contains("${") || fechaValue.equalsIgnoreCase("auto"))) {
            extentConfig.addSystemInfo("Fecha",
                java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return result.toString().trim();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public ExtentConfig getExtent() { return extent; }
    public void setExtent(ExtentConfig extent) { this.extent = extent; }

    @Override
    public String toString() {
        return "ReportingConfig{enabled=" + enabled + ", environment='" + environment + "', extent=" + extent + '}';
    }
}
