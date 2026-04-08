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
 */
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

    public TestExecutionResult() {
        this.attachments = new ArrayList<>();
        this.environmentInfo = new EnvironmentInfo();
    }

    /**
     * Agrega un attachment al resultado
     */
    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }

    // Getters y setters
    public String getTestExecutionKey() {
        return testExecutionKey;
    }

    public void setTestExecutionKey(String testExecutionKey) {
        this.testExecutionKey = testExecutionKey;
    }

    public LocalDateTime getExecutionStart() {
        return executionStart;
    }

    public void setExecutionStart(LocalDateTime executionStart) {
        this.executionStart = executionStart;
    }

    public LocalDateTime getExecutionEnd() {
        return executionEnd;
    }

    public void setExecutionEnd(LocalDateTime executionEnd) {
        this.executionEnd = executionEnd;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ScenarioResult> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ScenarioResult> scenarios) {
        this.scenarios = scenarios;
    }

    // Getters y Setters para campos extendidos

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }


    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public EnvironmentInfo getEnvironmentInfo() {
        return environmentInfo;
    }

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
