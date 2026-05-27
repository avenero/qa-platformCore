package com.qa.common.internal.reporting.manager.pipeline;
import com.qa.common.api.Internal;

import com.qa.common.internal.reporting.config.ReportingConfig;

/**
 * Representa un paso individual en el pipeline de reporting.
 * Cada step ejecuta una operación específica (conversión, generación, upload, etc.).
 *
 * Implementa Chain of Responsibility pattern.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
@Internal

public interface ReportingStep {

    /**
     * Nombre del step para logging y debugging.
     *
     * @return nombre descriptivo del step
     */
    String getName();

    /**
     * Indica si este step está habilitado según la configuración.
     *
     * @param config configuración de reporting
     * @return true si debe ejecutarse
     */
    boolean isEnabled(ReportingConfig config);

    /**
     * Indica si este step es requerido (falla pipeline si falla).
     *
     * @return true si es obligatorio
     */
    boolean isRequired();

    /**
     * Ejecuta el step.
     *
     * @param context contexto compartido del pipeline
     * @return resultado de la ejecución
     */
    PipelineStepResult execute(PipelineContext context);
}

