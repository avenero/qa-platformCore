package com.qa.common.api.runtime.events;

/**
 * Fase del ciclo de vida de una ejecución — vocabulario neutral de proveedor.
 * Usada en {@link ExecutionActivityEvent} para contexto de alto nivel.
 */
public enum ActivityPhase {
    PREPARING,
    EXECUTING,
    COLLECTING_EVIDENCE,
    GENERATING_REPORT,
    SYNCING_RESULTS,
    COMPLETED,
    FAILED
}
