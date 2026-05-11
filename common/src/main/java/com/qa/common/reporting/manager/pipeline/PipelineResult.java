package com.qa.common.reporting.manager.pipeline;
import com.qa.common.utils.security.SecurityUtilities;

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

    /**
     * Constructor privado; usar los factory methods.
     *
     * @param success      {@code true} si el pipeline fue exitoso
     * @param failedStep   nombre del step que falló, o null si fue exitoso
     * @param errorMessage mensaje de error, o null si fue exitoso
     * @param context      contexto del pipeline, puede ser null
     */
    private PipelineResult(boolean success, String failedStep, String errorMessage, PipelineContext context) {
        this.success = success;
        this.failedStep = failedStep;
        this.errorMessage = errorMessage;
        this.context = context;
    }

    /**
     * Crea un resultado exitoso con el contexto del pipeline.
     *
     * @param context contexto final del pipeline
     * @return nueva instancia de resultado exitoso
     */
    public static PipelineResult success(PipelineContext context) {
        return new PipelineResult(true, null, null, context);
    }

    /**
     * Crea un resultado fallido sin contexto de pipeline.
     *
     * @param failedStep   nombre del step que falló
     * @param errorMessage mensaje de error
     * @return nueva instancia de resultado fallido
     */
    public static PipelineResult failure(String failedStep, String errorMessage) {
        return new PipelineResult(false, failedStep, errorMessage, null);
    }

    /**
     * Crea un resultado fallido con contexto de pipeline.
     *
     * @param failedStep   nombre del step que falló
     * @param errorMessage mensaje de error
     * @param context      contexto del pipeline al momento del fallo
     * @return nueva instancia de resultado fallido con contexto
     */
    public static PipelineResult failure(String failedStep, String errorMessage, PipelineContext context) {
        return new PipelineResult(false, failedStep, errorMessage, context);
    }

    /**
     * Indica si el pipeline fue ejecutado con éxito.
     *
     * @return {@code true} si todos los steps completaron sin errores
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Obtiene el nombre del step que causó el fallo.
     *
     * @return nombre del step fallido, o null si el resultado fue exitoso
     */
    public String getFailedStep() {
        return failedStep;
    }

    /**
     * Obtiene el mensaje de error del pipeline.
     *
     * @return mensaje de error, o null si el resultado fue exitoso
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Obtiene el contexto del pipeline.
     *
     * @return contexto del pipeline, puede ser null
     */
    public PipelineContext getContext() {
        return context;
    }

    /**
     * Obtiene la ruta al reporte Extent generado.
     *
     * @return ruta al archivo de reporte, o null si no hay contexto
     */
    public String getExtentReportPath() {
        return context != null ? context.getExtentReportPath() : null;
    }

    /**
     * Obtiene los resultados individuales de cada step del pipeline.
     *
     * @return mapa de nombre-de-step a resultado, o mapa vacío si no hay contexto
     */
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

