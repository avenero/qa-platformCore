package com.qa.common.reporting.manager.pipeline;

/**
 * Resultado de la ejecución de un step del pipeline de reporting.
 *
 * <p>Encapsula el resultado de ejecutar un {@link ReportingStep} dentro del
 * {@link ReportingPipeline}: éxito o fallo con mensaje y excepción opcional.</p>
 *
 * <p>Distinto de {@code com.qa.common.reporting.core.model.StepResult}, que
 * modela el resultado de un step Cucumber (keyword, duración, estado BDD).</p>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 */
public class PipelineStepResult {

    private final boolean success;
    private final String message;
    private final Throwable error;

    private PipelineStepResult(boolean success, String message, Throwable error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    /** Resultado exitoso sin mensaje. */
    public static PipelineStepResult success() {
        return new PipelineStepResult(true, null, null);
    }

    /** Resultado exitoso con mensaje descriptivo. */
    public static PipelineStepResult success(String message) {
        return new PipelineStepResult(true, message, null);
    }

    /** Resultado fallido con mensaje de error. */
    public static PipelineStepResult failure(String message) {
        return new PipelineStepResult(false, message, null);
    }

    /** Resultado fallido con mensaje de error y excepción causa. */
    public static PipelineStepResult failure(String message, Throwable error) {
        return new PipelineStepResult(false, message, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public String toString() {
        return "PipelineStepResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error=" + (error != null ? error.getClass().getSimpleName() : "null") +
                '}';
    }
}
