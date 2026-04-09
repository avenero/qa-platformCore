package com.qa.common.reporting.manager.pipeline;

import java.util.Map;

/**
 * Resultado final de la ejecución del pipeline completo.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class PipelineResult {

    private final boolean success;
    private final String failedStep;
    private final String errorMessage;
    private final PipelineContext context;

    private PipelineResult(boolean success, String failedStep, String errorMessage, PipelineContext context) {
        this.success = success;
        this.failedStep = failedStep;
        this.errorMessage = errorMessage;
        this.context = context;
    }

    /**
     * Crea un resultado exitoso.
     */
    public static PipelineResult success(PipelineContext context) {
        return new PipelineResult(true, null, null, context);
    }

    /**
     * Crea un resultado fallido.
     */
    public static PipelineResult failure(String failedStep, String errorMessage) {
        return new PipelineResult(false, failedStep, errorMessage, null);
    }

    /**
     * Crea un resultado fallido con contexto.
     */
    public static PipelineResult failure(String failedStep, String errorMessage, PipelineContext context) {
        return new PipelineResult(false, failedStep, errorMessage, context);
    }

    // Getters

    public boolean isSuccess() {
        return success;
    }

    public String getFailedStep() {
        return failedStep;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public PipelineContext getContext() {
        return context;
    }

    // Métodos de conveniencia

    public String getExtentReportPath() {
        return context != null ? context.getExtentReportPath() : null;
    }

    public Map<String, PipelineStepResult> getStepResults() {
        return context != null ? context.getStepResults() : Map.of();
    }

    @Override
    public String toString() {
        return "PipelineResult{" +
                "success=" + success +
                ", failedStep='" + failedStep + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", extentReport='" + getExtentReportPath() + '\'' +
                '}';
    }
}

