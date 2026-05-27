package com.qa.common.api.reporter;


/**
 * Implementación inerte de {@link StepReporter} — no realiza ninguna acción.
 *
 * <p>Útil en tres escenarios:
 * <ul>
 *   <li><b>Ejecuciones standalone</b> — cuando no hay backend conectado que consuma el reporting.</li>
 *   <li><b>Tests internos del Core</b> — que validan la ejecución BDD pero no el reporting.</li>
 *   <li><b>Default antes de registrar un reporter real</b> — evita el patrón "null check" en el adapter.</li>
 * </ul>
 *
 * <h2>Patrón Null Object</h2>
 * <p>Usar {@link #INSTANCE} en vez de {@code null} como reporter. El adapter nunca recibe
 * {@code null} y los consumidores no necesitan defensas extra.
 *
 * <pre>
 * // En lugar de:
 * StepReporter reporter = null;
 * if (reporter != null) reporter.onStepPassed(...);
 *
 * // Usar:
 * StepReporter reporter = NoOpStepReporter.INSTANCE;
 * reporter.onStepPassed(...);  // seguro, no-op
 * </pre>
 *
 * @see StepReporter
 * @since 3.1.0
 */
public final class NoOpStepReporter implements StepReporter {

    /** Instancia singleton inmutable. Usar esta en vez de crear nuevas instancias. */
    public static final NoOpStepReporter INSTANCE = new NoOpStepReporter();

    private NoOpStepReporter() { }

    // Todos los métodos heredan los default vacíos de StepReporter.
    // No se sobreescribe nada — eso es exactamente el contrato NoOp.
}
