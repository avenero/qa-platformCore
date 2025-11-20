package com.scotia.qa.common.jiraXray.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo estándar para resultados de ejecución de tests.
 * Representa la información relevante que se enviará a Jira.
 */
public class TestExecutionResult {

    // Información general de la ejecución
    private String testExecutionKey;
    private LocalDateTime executionStart;
    private LocalDateTime executionEnd;
    private String environment;
    private String summary;

    // Resultados de scenarios individuales
    private List<ScenarioResult> scenarios;

    // Estadísticas
    private TestStatistics statistics;

    public TestExecutionResult() {
        this.statistics = new TestStatistics();
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
        updateStatistics();
    }

    public TestStatistics getStatistics() {
        return statistics;
    }

    private void updateStatistics() {
        if (scenarios != null) {
            statistics.calculateFromScenarios(scenarios);
        }
    }
}
