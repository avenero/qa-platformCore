package com.scotia.qa.common.jiraXray.config;

import com.scotia.qa.common.logging.TestLogger;

/**
 * Configuración para actualización de test cases en Jira.
 * Soporta configuración mediante variables de entorno y archivos YAML.
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class ReportConfig {


    // Credenciales de Jira
    private String user;
    private String password;

    // Parámetros principales
    private Boolean uploadReport;
    private String testExecution;

    public ReportConfig() {
        // No cargar automáticamente para permitir configuración manual
    }

    /**
     * Carga la configuración desde variables de entorno.
     */
    private void loadFromEnvironment() {
        this.user = System.getenv("JIRA_USER");
        this.password = System.getenv("JIRA_PASSWORD");
        this.uploadReport = Boolean.valueOf(System.getenv("JIRA_UPLOAD_REPORT"));
        this.testExecution = System.getenv("JIRA_TEST_EXECUTION");

        TestLogger.logInfo("REPORT_CONFIG", "Configuración cargada desde variables de entorno", null);
        TestLogger.logInfo("REPORT_CONFIG", "Usuario configurado: " + (user != null ? "✓" : "✗"), null);
        TestLogger.logInfo("REPORT_CONFIG", "Password configurado: " + (password != null ? "✓" : "✗"), null);
        TestLogger.logInfo("REPORT_CONFIG", "Upload Report: " + uploadReport, null);
        TestLogger.logInfo("REPORT_CONFIG", "Test Execution: " + testExecution, null);
    }

    /**
     * Método estático para crear configuración desde variables de entorno
     */
    public static ReportConfig loadFromEnv() {
        ReportConfig config = new ReportConfig();
        config.loadFromEnvironment();
        return config;
    }

    // Getters y setters
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getUploadReport() {
        return uploadReport;
    }

    public void setUploadReport(Boolean uploadReport) {
        this.uploadReport = uploadReport;
    }

    public String getTestExecution() {
        return testExecution;
    }

    public void setTestExecution(String testExecution) {
        this.testExecution = testExecution;
    }
}
