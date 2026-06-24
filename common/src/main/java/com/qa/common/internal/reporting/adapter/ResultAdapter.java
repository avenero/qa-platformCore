package com.qa.common.internal.reporting.adapter;
import com.qa.common.api.Internal;

import com.qa.common.api.reporter.bridge.ExecutionData;

/**
 * Interfaz para adaptadores de diferentes formatos de resultados.
 *
 * <p>Permite agregar soporte para nuevos frameworks sin modificar el core.
 */
@Internal
public interface ResultAdapter {

    /**
     * Convierte resultados en formato específico al modelo puente.
     *
     * @param rawResults resultados en formato nativo (JSON, XML, etc.)
     * @return resultados proyectados al modelo puente {@link ExecutionData}
     */
    ExecutionData convert(String rawResults);

    /**
     * Indica si este adaptador puede manejar el formato dado.
     *
     * @param rawResults resultados a evaluar
     * @return true si puede procesar este formato
     */
    boolean canHandle(String rawResults);

    /**
     * Nombre del adaptador para logging.
     *
     * @return nombre del adaptador
     */
    String getName();
}
