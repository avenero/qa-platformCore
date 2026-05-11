package com.qa.common.api.runtime.events;

/**
 * Severidad de una actividad de ejecución en el stream seguro.
 * Usada en {@link ExecutionActivityEvent} y {@link FailureDescriptor}.
 */
public enum ActivitySeverity {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}
