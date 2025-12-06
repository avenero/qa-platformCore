package com.scotia.qa.common.reporting.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración específica para generación de reportes Extent.
 * Los módulos deben configurar estas propiedades en config-scotia.properties:
 *
 * <pre>
 * # Extent Reports Generation
 * extent.enabled=true
 * extent.outputPath=build/reports/extent/
 * extent.reportName=execution-report.html
 *
 * # Customization
 * extent.documentTitle=Scotia QA - Test Results
 * extent.reportTitle=Automated Test Execution Report
 * extent.theme=STANDARD
 * extent.includeScreenshots=true
 * extent.includeSystemInfo=true
 * extent.includeTimeline=true
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class ExtentConfig {

    // Generación
    private boolean enabled;
    private String outputPath;
    private String reportName;

    // Customización
    private String documentTitle;
    private String reportTitle;
    private String theme;
    private boolean includeScreenshots;
    private boolean includeSystemInfo;
    private boolean includeTimeline;

    // System Info custom (agregado por el módulo)
    private Map<String, String> systemInfo;

    // Reporters (HTML, JSON, PDF en el futuro)
    private String[] reporterOrder;

    public ExtentConfig() {
        // Defaults
        this.enabled = true;
        this.outputPath = "build/reports/extent/";
        this.reportName = "execution-report.html";
        this.documentTitle = "Test Execution Report";
        this.reportTitle = "Automated Tests";
        this.theme = "STANDARD";
        this.includeScreenshots = true;
        this.includeSystemInfo = true;
        this.includeTimeline = true;
        this.systemInfo = new HashMap<>();
        this.reporterOrder = new String[]{"HTML"};
    }

    /**
     * Temas disponibles para Extent Reports
     */
    public enum Theme {
        STANDARD,
        DARK
    }

    /**
     * Agrega información de sistema custom
     */
    public void addSystemInfo(String key, String value) {
        this.systemInfo.put(key, value);
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isIncludeScreenshots() {
        return includeScreenshots;
    }

    public void setIncludeScreenshots(boolean includeScreenshots) {
        this.includeScreenshots = includeScreenshots;
    }

    public boolean isIncludeSystemInfo() {
        return includeSystemInfo;
    }

    public void setIncludeSystemInfo(boolean includeSystemInfo) {
        this.includeSystemInfo = includeSystemInfo;
    }

    public boolean isIncludeTimeline() {
        return includeTimeline;
    }

    public void setIncludeTimeline(boolean includeTimeline) {
        this.includeTimeline = includeTimeline;
    }

    public Map<String, String> getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(Map<String, String> systemInfo) {
        this.systemInfo = systemInfo;
    }

    public String[] getReporterOrder() {
        return reporterOrder;
    }

    public void setReporterOrder(String[] reporterOrder) {
        this.reporterOrder = reporterOrder;
    }

    /**
     * Obtiene la ruta completa del reporte
     */
    public String getFullReportPath() {
        return outputPath + (outputPath.endsWith("/") ? "" : "/") + reportName;
    }

    @Override
    public String toString() {
        return "ExtentConfig{" +
                "enabled=" + enabled +
                ", outputPath='" + outputPath + '\'' +
                ", reportName='" + reportName + '\'' +
                ", theme='" + theme + '\'' +
                '}';
    }
}

