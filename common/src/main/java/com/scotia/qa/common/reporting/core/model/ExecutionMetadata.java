package com.scotia.qa.common.reporting.core.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata de la ejecución de tests (tiempos, CI/CD info, etc.).
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class ExecutionMetadata {

    private String executionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;

    // CI/CD info
    private String buildNumber;
    private String buildUrl;
    private String branch;
    private String commit;

    // Test info
    private String testSuite;
    private String testEnvironment;
    private String triggeredBy;

    // Custom metadata
    private Map<String, String> customData;

    public ExecutionMetadata() {
        this.customData = new HashMap<>();
    }

    /**
     * Calcula la duración basada en start y end time
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * Agrega metadata custom
     */
    public void addCustomData(String key, String value) {
        this.customData.put(key, value);
    }

    // Getters and Setters

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(String testSuite) {
        this.testSuite = testSuite;
    }

    public String getTestEnvironment() {
        return testEnvironment;
    }

    public void setTestEnvironment(String testEnvironment) {
        this.testEnvironment = testEnvironment;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, String> customData) {
        this.customData = customData;
    }

    @Override
    public String toString() {
        return "ExecutionMetadata{" +
                "executionId='" + executionId + '\'' +
                ", testEnvironment='" + testEnvironment + '\'' +
                ", durationMs=" + durationMs +
                ", buildNumber='" + buildNumber + '\'' +
                '}';
    }
}

