package com.qa.common.reporting.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo estándar para resultados de ejecución de tests.
 * Representa la información completa de una ejecución (Jira, Extent, etc.).
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 * @deprecated since 2.0.0 — use {@link com.qa.common.reporting.core.bridge.ExecutionData} with
 *             {@link com.qa.common.reporting.core.port.ReportGeneratorPort} instead.
 *             This class will be removed in a future version.
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class TestExecutionResult {

    // Identificación
    private String testExecutionKey;
    private String projectKey;

    // Información general de la ejecución (legacy - mantener compatibilidad)
    private LocalDateTime executionStart;
    private LocalDateTime executionEnd;
    private String environment;
    private String summary;

    // Resultados de scenarios individuales
    private List<ScenarioResult> scenarios;

    // Evidencias/Attachments
    private List<Attachment> attachments;

    // Información del entorno
    private EnvironmentInfo environmentInfo;

    /**
     * Constructor por defecto que inicializa las listas de attachments e información del entorno.
     */
    public TestExecutionResult() {
        this.attachments = new ArrayList<>();
        this.environmentInfo = new EnvironmentInfo();
    }

    /**
     * Agrega un attachment al resultado de la ejecución.
     *
     * @param attachment attachment a agregar
     */
    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }

    /**
     * Obtiene la clave del resultado de ejecución en Jira.
     *
     * @return clave del test execution (ej: "PROJ-123")
     */
    public String getTestExecutionKey() {
        return testExecutionKey;
    }

    /**
     * Establece la clave del resultado de ejecución en Jira.
     *
     * @param testExecutionKey clave del test execution
     */
    public void setTestExecutionKey(String testExecutionKey) {
        this.testExecutionKey = testExecutionKey;
    }

    /**
     * Obtiene la fecha y hora de inicio de la ejecución.
     *
     * @return fecha y hora de inicio
     */
    public LocalDateTime getExecutionStart() {
        return executionStart;
    }

    /**
     * Establece la fecha y hora de inicio de la ejecución.
     *
     * @param executionStart fecha y hora de inicio
     */
    public void setExecutionStart(LocalDateTime executionStart) {
        this.executionStart = executionStart;
    }

    /**
     * Obtiene la fecha y hora de fin de la ejecución.
     *
     * @return fecha y hora de fin
     */
    public LocalDateTime getExecutionEnd() {
        return executionEnd;
    }

    /**
     * Establece la fecha y hora de fin de la ejecución.
     *
     * @param executionEnd fecha y hora de fin
     */
    public void setExecutionEnd(LocalDateTime executionEnd) {
        this.executionEnd = executionEnd;
    }

    /**
     * Obtiene el ambiente de ejecución.
     *
     * @return nombre del ambiente (ej: "qa", "prod")
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Establece el ambiente de ejecución.
     *
     * @param environment nombre del ambiente
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Obtiene el resumen de la ejecución.
     *
     * @return texto de resumen
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Establece el resumen de la ejecución.
     *
     * @param summary texto de resumen
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Obtiene la lista de resultados de escenarios.
     *
     * @return lista de resultados de escenarios
     */
    public List<ScenarioResult> getScenarios() {
        return scenarios;
    }

    /**
     * Establece la lista de resultados de escenarios.
     *
     * @param scenarios lista de resultados
     */
    public void setScenarios(List<ScenarioResult> scenarios) {
        this.scenarios = scenarios;
    }

    /**
     * Obtiene la clave del proyecto Jira.
     *
     * @return clave del proyecto (ej: "PROJ")
     */
    public String getProjectKey() {
        return projectKey;
    }

    /**
     * Establece la clave del proyecto Jira.
     *
     * @param projectKey clave del proyecto
     */
    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    /**
     * Obtiene la lista de attachments de la ejecución.
     *
     * @return lista de attachments
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Establece la lista de attachments de la ejecución.
     *
     * @param attachments lista de attachments
     */
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Obtiene la información del entorno de la ejecución.
     *
     * @return información del entorno
     */
    public EnvironmentInfo getEnvironmentInfo() {
        return environmentInfo;
    }

    /**
     * Establece la información del entorno de la ejecución.
     *
     * @param environmentInfo información del entorno
     */
    public void setEnvironmentInfo(EnvironmentInfo environmentInfo) {
        this.environmentInfo = environmentInfo;
    }

    @Override
    public String toString() {
        return "TestExecutionResult{" +
                "testExecutionKey='" + testExecutionKey + '\'' +
                ", projectKey='" + projectKey + '\'' +
                ", scenarios=" + (scenarios != null ? scenarios.size() : 0) +
                ", attachments=" + (attachments != null ? attachments.size() : 0) +
                '}';
    }
}
