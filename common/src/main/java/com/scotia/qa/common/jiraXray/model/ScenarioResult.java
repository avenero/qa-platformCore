package com.scotia.qa.common.jiraXray.model;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

/**
 * Resultado de un scenario individual.
 * Contiene solo la información relevante para reportar a Jira.
 */
public class ScenarioResult {

    // Identificación
    private String testKey;        // @QAAUY-123
    private String scenarioName;
    private String featureFile;

    // Estado de ejecución
    private TestStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;

    // Detalles de fallo (si aplica)
    private String errorMessage;
    private String stackTrace;

    // Información de steps (resumen)
    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int skippedSteps;

    // Metadata adicional
    private List<String> tags;
    private boolean isOutline;      // Si es Scenario Outline
    private String outlineExample;  // Valores del example si es outline

    // Getters y setters
    public String getTestKey() {
        return testKey;
    }

    public void setTestKey(String testKey) {
        this.testKey = testKey;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getFeatureFile() {
        return featureFile;
    }

    public void setFeatureFile(String featureFile) {
        this.featureFile = featureFile;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.duration = Duration.between(startTime, endTime);
        }
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getPassedSteps() {
        return passedSteps;
    }

    public void setPassedSteps(int passedSteps) {
        this.passedSteps = passedSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public int getSkippedSteps() {
        return skippedSteps;
    }

    public void setSkippedSteps(int skippedSteps) {
        this.skippedSteps = skippedSteps;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isOutline() {
        return isOutline;
    }

    public void setOutline(boolean outline) {
        isOutline = outline;
    }

    public String getOutlineExample() {
        return outlineExample;
    }

    public void setOutlineExample(String outlineExample) {
        this.outlineExample = outlineExample;
    }
}
