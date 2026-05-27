package com.qa.common.api.reporter;

import com.qa.common.api.runtime.ExecutionResult;

import java.time.Duration;

/**
 * Contrato semántico para reportar el progreso de una ejecución BDD en tiempo real.
 *
 * <p>Todos los métodos tienen implementación vacía por defecto ({@code default}), permitiendo
 * que implementaciones parciales solo sobreescriban los eventos que les interesan.
 *
 * <h2>Flujo de llamadas esperado</h2>
 * <pre>
 * onScenarioStarted(...)
 *   onStepStarted(...)
 *   onStepPassed(...)  ─ o ─  onStepFailed(...)  ─ o ─  onStepSkipped(...)
 *   ...
 * onScenarioCompleted(...)
 * ...
 * onExecutionCompleted(...)
 * </pre>
 *
 * <h2>Implementaciones previstas</h2>
 * <ul>
 *   <li>{@link NoOpStepReporter} — no hace nada (standalone / tests internos)</li>
 *   <li>{@code BEStepReporter} (TASK-F03, en BE) — despacha via WebSocket al frontend</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>Las implementaciones deben ser thread-safe: el {@link EventBusStepReporterAdapter}
 * puede invocar métodos desde el hilo que publica al EventBus. Si el reporter escribe a
 * estructuras compartidas, debe sincronizarlas internamente.
 *
 * @see EventBusStepReporterAdapter
 * @see NoOpStepReporter
 * @see ScenarioOutcome
 * @since 3.1.0
 */
public interface StepReporter {

    /**
     * Un escenario está a punto de comenzar.
     *
     * @param scenarioId identificador único del escenario (ej: UUID de ejecución)
     * @param name       nombre legible del escenario tal como aparece en el archivo .feature
     */
    default void onScenarioStarted(String scenarioId, String name) { }

    /**
     * Un step está siendo ejecutado.
     *
     * @param scenarioId identificador del escenario padre
     * @param stepText   texto del step tal como aparece en el archivo .feature
     */
    default void onStepStarted(String scenarioId, String stepText) { }

    /**
     * Un step finalizó exitosamente.
     *
     * @param scenarioId identificador del escenario padre
     * @param stepText   texto del step
     * @param duration   tiempo que tardó en ejecutarse
     */
    default void onStepPassed(String scenarioId, String stepText, Duration duration) { }

    /**
     * Un step falló con una excepción.
     *
     * @param scenarioId identificador del escenario padre
     * @param stepText   texto del step
     * @param error      excepción capturada; nunca null
     * @param screenshot captura de pantalla tomada al fallar; puede ser null si no aplica
     */
    default void onStepFailed(String scenarioId, String stepText,
                              Throwable error, byte[] screenshot) { }

    /**
     * Un step fue saltado (normalmente por un step previo que falló).
     *
     * @param scenarioId identificador del escenario padre
     * @param stepText   texto del step saltado
     */
    default void onStepSkipped(String scenarioId, String stepText) { }

    /**
     * Un escenario finalizó (con cualquier outcome).
     *
     * @param scenarioId    identificador del escenario
     * @param outcome       resultado del escenario
     * @param totalDuration duración total del escenario incluyendo todos sus steps
     */
    default void onScenarioCompleted(String scenarioId, ScenarioOutcome outcome,
                                     Duration totalDuration) { }

    /**
     * Toda la ejecución finalizó.
     *
     * <p>Este es el último evento emitido. El resultado contiene estadísticas globales
     * (total, passed, failed, duration).
     *
     * @param result resultado global de la ejecución
     */
    default void onExecutionCompleted(ExecutionResult result) { }
}
