package com.qa.common.reporting.core.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de un scenario individual.
 * Contiene información completa para Jira, Extent y otros reportes.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
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
    private long durationMs;       // NUEVO: duración en milisegundos (más fácil de usar)

    // Detalles de fallo (si aplica)
    private String errorMessage;        // Mensaje técnico completo (para developers)
    private String businessMessage;     // Mensaje amigable (para PO/PM)
    private String stackTrace;

    // Información de steps (resumen)
    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int skippedSteps;

    // Detalle de cada step
    private List<StepResult> steps;

    // Metadata adicional
    private List<String> tags;
    private boolean isOutline;      // Si es Scenario Outline
    private String outlineExample;  // Valores del example si es outline

    // NUEVO: Evidencias
    private List<Attachment> screenshots;
    private List<String> logs;

    // NUEVO: Metadata custom
    private Map<String, Object> metadata;

    public ScenarioResult() {
        this.screenshots = new ArrayList<>();
        this.logs = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.steps = new ArrayList<>();
    }

    /**
     * Agrega un step al scenario.
     *
     * @param step resultado del step a agregar
     */
    public void addStep(StepResult step) {
        this.steps.add(step);
    }

    /**
     * Agrega un screenshot al scenario.
     *
     * @param screenshot attachment de tipo screenshot a adjuntar
     */
    public void addScreenshot(Attachment screenshot) {
        this.screenshots.add(screenshot);
    }

    /**
     * Agrega un log al scenario.
     *
     * @param log mensaje de log a agregar
     */
    public void addLog(String log) {
        this.logs.add(log);
    }

    /**
     * Agrega metadata personalizada al scenario.
     *
     * @param key   clave de la metadata
     * @param value valor de la metadata
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

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

    public String getBusinessMessage() {
        return businessMessage;
    }

    public void setBusinessMessage(String businessMessage) {
        this.businessMessage = businessMessage;
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

    public List<StepResult> getSteps() {
        return steps;
    }

    public void setSteps(List<StepResult> steps) {
        this.steps = steps;
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

    // NUEVO: Getters y Setters para campos extendidos

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public List<Attachment> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<Attachment> screenshots) {
        this.screenshots = screenshots;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "ScenarioResult{" +
                "testKey='" + testKey + '\'' +
                ", scenarioName='" + scenarioName + '\'' +
                ", status=" + status +
                ", durationMs=" + durationMs +
                ", screenshots=" + (screenshots != null ? screenshots.size() : 0) +
                '}';
    }
}
