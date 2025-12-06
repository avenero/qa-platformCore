package com.scotia.qa.common.reporting.core.adapter;

import com.scotia.qa.common.reporting.core.model.TestExecutionResult;

/**
 * Interfaz para adaptadores de diferentes formatos de resultados.
 * Permite agregar soporte para nuevos frameworks sin modificar el core.
 */
public interface ResultAdapter {

    /**
     * Convierte resultados en formato específico al modelo estándar
     *
     * @param rawResults resultados en formato nativo (JSON, XML, etc.)
     * @return resultados en modelo estándar para enviar a Jira
     */
    TestExecutionResult convert(String rawResults);

    /**
     * Indica si este adaptador puede manejar el formato dado
     *
     * @param rawResults resultados a evaluar
     * @return true si puede procesar este formato
     */
    boolean canHandle(String rawResults);

    /**
     * Nombre del adaptador para logging
     */
    String getName();
}
