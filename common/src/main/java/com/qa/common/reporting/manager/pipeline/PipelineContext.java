package com.qa.common.reporting.manager.pipeline;

import com.qa.common.reporting.core.config.ReportingConfig;
import com.qa.common.reporting.core.model.TestExecutionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Contexto compartido entre steps del pipeline.
 * Almacena datos intermedios y resultados de cada step.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class PipelineContext {

    private final String rawResults;
    private final ReportingConfig config;
    private final Map<String, Object> data;
    private final Map<String, StepResult> stepResults;

    // Datos principales
    private TestExecutionResult testExecutionResult;
    private String extentReportPath;

    public PipelineContext(String rawResults, ReportingConfig config) {
        this.rawResults = rawResults;
        this.config = config;
        this.data = new HashMap<>();
        this.stepResults = new HashMap<>();
    }

    /**
     * Almacena un dato custom en el contexto.
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Obtiene un dato custom del contexto.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * Almacena el resultado de un step.
     */
    public void addStepResult(String stepName, StepResult result) {
        stepResults.put(stepName, result);
    }

    /**
     * Obtiene el resultado de un step específico.
     */
    public StepResult getStepResult(String stepName) {
        return stepResults.get(stepName);
    }

    // Getters y Setters

    public String getRawResults() {
        return rawResults;
    }

    public ReportingConfig getConfig() {
        return config;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, StepResult> getStepResults() {
        return stepResults;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public void setTestExecutionResult(TestExecutionResult testExecutionResult) {
        this.testExecutionResult = testExecutionResult;
    }

    public String getExtentReportPath() {
        return extentReportPath;
    }

    public void setExtentReportPath(String extentReportPath) {
        this.extentReportPath = extentReportPath;
    }
}

