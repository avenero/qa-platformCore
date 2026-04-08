package com.qa.common.reporting.manager.pipeline;

/**
 * Resultado de la ejecución de un step en el pipeline.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class StepResult {

    private final boolean success;
    private final String message;
    private final Throwable error;

    private StepResult(boolean success, String message, Throwable error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    /**
     * Crea un resultado exitoso.
     */
    public static StepResult success() {
        return new StepResult(true, null, null);
    }

    /**
     * Crea un resultado exitoso con mensaje.
     */
    public static StepResult success(String message) {
        return new StepResult(true, message, null);
    }

    /**
     * Crea un resultado fallido.
     */
    public static StepResult failure(String message) {
        return new StepResult(false, message, null);
    }

    /**
     * Crea un resultado fallido con excepción.
     */
    public static StepResult failure(String message, Throwable error) {
        return new StepResult(false, message, error);
    }

    // Getters

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
        return "StepResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error=" + (error != null ? error.getClass().getSimpleName() : "null") +
                '}';
    }
}

