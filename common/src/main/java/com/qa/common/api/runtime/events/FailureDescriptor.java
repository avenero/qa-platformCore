package com.qa.common.api.runtime.events;

import java.util.Objects;

/**
 * Descriptor de fallo seguro — desacopla la información técnica interna
 * del mensaje que puede mostrarse al usuario final.
 *
 * <p>Las implementaciones deben garantizar que {@code failureCode} sea estable
 * y no contenga detalles de implementación (nombres de clase, stack traces, etc.).
 *
 * <p>Ejemplos de {@code failureCode}:
 * <ul>
 *   <li>ASSERTION_FAILED</li>
 *   <li>TIMEOUT</li>
 *   <li>ELEMENT_NOT_AVAILABLE</li>
 *   <li>TEST_DATA_INVALID</li>
 *   <li>ENVIRONMENT_UNAVAILABLE</li>
 *   <li>INTEGRATION_SYNC_FAILED</li>
 *   <li>UNEXPECTED_EXECUTION_ERROR</li>
 * </ul>
 */
public final class FailureDescriptor {

    private final String failureCode;
    private final String userSummary;
    private final ActivitySeverity severity;
    private final boolean retryable;

    private FailureDescriptor(String failureCode, String userSummary,
                              ActivitySeverity severity, boolean retryable) {
        this.failureCode = Objects.requireNonNull(failureCode, "failureCode no puede ser null");
        this.userSummary = Objects.requireNonNull(userSummary, "userSummary no puede ser null");
        this.severity    = severity != null ? severity : ActivitySeverity.ERROR;
        this.retryable   = retryable;
    }

    public static FailureDescriptor of(String failureCode, String userSummary) {
        return new FailureDescriptor(failureCode, userSummary, ActivitySeverity.ERROR, false);
    }

    public static FailureDescriptor of(String failureCode, String userSummary,
                                       ActivitySeverity severity, boolean retryable) {
        return new FailureDescriptor(failureCode, userSummary, severity, retryable);
    }

    /** Clasifica un {@link Throwable} en un descriptor seguro sin exponer detalles internos. */
    public static FailureDescriptor fromThrowable(Throwable t) {
        if (t == null) {
            return of("UNEXPECTED_EXECUTION_ERROR", "Ocurrió un error inesperado durante la ejecución.");
        }
        String msg = t.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("timeout") || lower.contains("timed out")) {
                return of("TIMEOUT", "La operación superó el tiempo límite esperado.", ActivitySeverity.ERROR, true);
            }
            if (lower.contains("assertion") || lower.contains("expected") || lower.contains("actual")) {
                return of("ASSERTION_FAILED", "La verificación del resultado falló.", ActivitySeverity.ERROR, false);
            }
            if (lower.contains("element") || lower.contains("locator")
                    || lower.contains("no such element")) {
                return of(
                    "ELEMENT_NOT_AVAILABLE",
                    "No se pudo interactuar con el elemento esperado en la pantalla.",
                    ActivitySeverity.ERROR,
                    true);
            }
            if (lower.contains("connect") || lower.contains("connection refused")
                    || lower.contains("unreachable")) {
                return of(
                    "ENVIRONMENT_UNAVAILABLE",
                    "No fue posible conectarse al ambiente de prueba.",
                    ActivitySeverity.ERROR,
                    true);
            }
        }
        return of("UNEXPECTED_EXECUTION_ERROR", "Ocurrió un error inesperado durante la ejecución.");
    }

    public String getFailureCode()  { return failureCode; }
    public String getUserSummary()  { return userSummary; }
    public ActivitySeverity getSeverity() { return severity; }
    public boolean isRetryable()    { return retryable; }

    @Override
    public String toString() {
        return "FailureDescriptor{code=" + failureCode + ", retryable=" + retryable + "}";
    }
}
