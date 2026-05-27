package com.qa.common.api.reporter;


/**
 * Resultado final de la ejecución de un escenario BDD.
 *
 * <p>El outcome se determina cuando todos los steps del escenario finalizaron
 * o cuando el escenario fue interrumpido ({@link #ABORTED}).
 *
 * <h2>Reglas de transición</h2>
 * <ul>
 *   <li>{@link #PASSED} — todos los steps ejecutados resultaron PASSED</li>
 *   <li>{@link #FAILED} — al menos un step resultó FAILED (los siguientes quedan SKIPPED)</li>
 *   <li>{@link #SKIPPED} — todos los steps fueron saltados (ej: tag de exclusión)</li>
 *   <li>{@link #ABORTED} — la ejecución fue interrumpida por el usuario o por timeout</li>
 * </ul>
 *
 * @see StepReporter#onScenarioCompleted(String, ScenarioOutcome, java.time.Duration)
 * @since 3.1.0
 */
public enum ScenarioOutcome {

    /** Todos los steps ejecutados resultaron PASSED. */
    PASSED,

    /** Al menos un step resultó FAILED. */
    FAILED,

    /** Todos los steps fueron saltados sin ejecutarse. */
    SKIPPED,

    /** La ejecución fue interrumpida antes de completarse. */
    ABORTED;

    /**
     * Indica si el escenario alcanzó un estado terminal definitivo
     * (no puede continuar evolucionando).
     *
     * @return {@code true} para PASSED, FAILED y ABORTED; {@code false} para SKIPPED
     */
    public boolean isTerminal() {
        return this == PASSED || this == FAILED || this == ABORTED;
    }

    /**
     * Indica si el escenario completó exitosamente.
     *
     * @return {@code true} solo para PASSED
     */
    public boolean isSuccess() {
        return this == PASSED;
    }

    /**
     * Indica si el escenario terminó con algún tipo de fallo o interrupción.
     *
     * @return {@code true} para FAILED y ABORTED
     */
    public boolean isFailureOrAbort() {
        return this == FAILED || this == ABORTED;
    }
}
