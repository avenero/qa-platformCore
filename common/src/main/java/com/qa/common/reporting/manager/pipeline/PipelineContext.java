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
    private final Map<String, PipelineStepResult> stepResults;

    // Datos principales
    private TestExecutionResult testExecutionResult;
    private String extentReportPath;

    /**
     * Crea un nuevo contexto de pipeline.
     *
     * @param rawResults resultados JSON sin procesar del reporte Cucumber
     * @param config     configuración de reporting para esta ejecución
     */
    public PipelineContext(String rawResults, ReportingConfig config) {
        this.rawResults = rawResults;
        this.config = config;
        this.data = new HashMap<>();
        this.stepResults = new HashMap<>();
    }

    /**
     * Almacena un dato custom en el contexto del pipeline.
     *
     * @param key   clave del dato
     * @param value valor a almacenar
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Obtiene un dato custom del contexto del pipeline.
     *
     * @param <T> tipo del valor esperado
     * @param key clave del dato
     * @return valor del tipo indicado, o null si no existe
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * Almacena el resultado de un step del pipeline.
     *
     * @param stepName nombre del step
     * @param result   resultado de la ejecución del step
     */
    public void addStepResult(String stepName, PipelineStepResult result) {
        stepResults.put(stepName, result);
    }

    /**
     * Obtiene el resultado de un step específico del pipeline.
     *
     * @param stepName nombre del step
     * @return resultado del step, o null si no existe
     */
    public PipelineStepResult getStepResult(String stepName) {
        return stepResults.get(stepName);
    }

    /**
     * Obtiene los resultados JSON sin procesar del reporte Cucumber.
     *
     * @return resultados crudos del reporte
     */
    public String getRawResults() {
        return rawResults;
    }

    /**
     * Obtiene la configuración de reporting para esta ejecución.
     *
     * @return configuración de reporting
     */
    public ReportingConfig getConfig() {
        return config;
    }

    /**
     * Obtiene el mapa de datos custom del pipeline.
     *
     * @return mapa de datos del contexto
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Obtiene todos los resultados de los steps del pipeline.
     *
     * @return mapa de nombre-de-step a resultado
     */
    public Map<String, PipelineStepResult> getStepResults() {
        return stepResults;
    }

    /**
     * Obtiene el resultado de ejecución de los tests.
     *
     * @return resultado agregado de la ejecución de tests
     */
    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    /**
     * Establece el resultado de ejecución de los tests.
     *
     * @param testExecutionResult resultado a almacenar
     */
    public void setTestExecutionResult(TestExecutionResult testExecutionResult) {
        this.testExecutionResult = testExecutionResult;
    }

    /**
     * Obtiene la ruta al reporte Extent generado.
     *
     * @return ruta al archivo de reporte HTML
     */
    public String getExtentReportPath() {
        return extentReportPath;
    }

    /**
     * Establece la ruta al reporte Extent generado.
     *
     * @param extentReportPath ruta al archivo de reporte HTML
     */
    public void setExtentReportPath(String extentReportPath) {
        this.extentReportPath = extentReportPath;
    }
}

