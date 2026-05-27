package com.qa.common.internal.reporting.manager.pipeline;
import com.qa.common.api.Internal;


/**
 * Resultado de la ejecución de un step del pipeline de reporting.
 *
 * <p>Encapsula el resultado de ejecutar un {@link ReportingStep} dentro del
 * {@link ReportingPipeline}: éxito o fallo con mensaje y excepción opcional.</p>
 *
 * <p>Distinto de {@code com.qa.common.internal.reporting.model.StepResult}, que
 * modela el resultado de un step Cucumber (keyword, duración, estado BDD).</p>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 */
@Internal

public class PipelineStepResult {

    private final boolean success;
    private final String message;
    private final Throwable error;

    /**
     * Constructor privado para forzar el uso de los factory methods.
     *
     * @param success {@code true} si el resultado es exitoso
     * @param message mensaje descriptivo, puede ser null
     * @param error   excepción causa, puede ser null
     */
    private PipelineStepResult(boolean success, String message, Throwable error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    /**
     * Crea un resultado exitoso sin mensaje.
     *
     * @return nueva instancia de resultado exitoso
     */
    public static PipelineStepResult success() {
        return new PipelineStepResult(true, null, null);
    }

    /**
     * Crea un resultado exitoso con mensaje descriptivo.
     *
     * @param message mensaje descriptivo del resultado
     * @return nueva instancia de resultado exitoso con mensaje
     */
    public static PipelineStepResult success(String message) {
        return new PipelineStepResult(true, message, null);
    }

    /**
     * Crea un resultado fallido con mensaje de error.
     *
     * @param message mensaje descriptivo del error
     * @return nueva instancia de resultado fallido
     */
    public static PipelineStepResult failure(String message) {
        return new PipelineStepResult(false, message, null);
    }

    /**
     * Crea un resultado fallido con mensaje de error y excepción causa.
     *
     * @param message mensaje descriptivo del error
     * @param error   excepción que originó el fallo
     * @return nueva instancia de resultado fallido con causa
     */
    public static PipelineStepResult failure(String message, Throwable error) {
        return new PipelineStepResult(false, message, error);
    }

    /**
     * Indica si el step del pipeline fue exitoso.
     *
     * @return {@code true} si el resultado es exitoso
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Obtiene el mensaje asociado al resultado.
     *
     * @return mensaje del resultado, puede ser null
     */
    public String getMessage() {
        return message;
    }

    /**
     * Obtiene la excepción que causó el fallo, si existe.
     *
     * @return excepción causante, o null si no hay error
     */
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
