package com.qa.common.reporter;

/**
 * Constantes de las claves del mapa {@code data} de {@link com.qa.common.runtime.events.ExecutionEvent}.
 *
 * <p>Uso de estas constantes garantiza coherencia entre los productores de eventos
 * (plugins, ScenarioLifecycleBridge, CucumberRuntimeEngine) y el consumidor
 * ({@link EventBusStepReporterAdapter}).
 *
 * <h2>Contrato de tipos esperados por clave</h2>
 * <pre>
 * SCENARIO_ID          → String
 * SCENARIO_NAME        → String
 * STEP_TEXT            → String
 * STEP_STATUS          → String ("PASSED" | "FAILED" | "SKIPPED")
 * STEP_DURATION_MS     → Long   (milisegundos)
 * STEP_ERROR           → Throwable
 * STEP_SCREENSHOT      → byte[]
 * SCENARIO_OUTCOME     → ScenarioOutcome
 * SCENARIO_DURATION_MS → Long   (milisegundos)
 * EXECUTION_RESULT     → com.qa.common.runtime.ExecutionResult
 * </pre>
 *
 * <h2>Regla de uso</h2>
 * <p>Siempre usar estas constantes en vez de strings literales cuando se produce o consume
 * datos del mapa de un {@code ExecutionEvent}. Nunca referenciar la clave como string directo
 * — un typo silencioso haría que el adapter ignorara el dato sin error en compilación.
 *
 * @since 3.1.0
 */
public final class EventDataKeys {

    private EventDataKeys() { }

    // ── Escenario ────────────────────────────────────────────────────────────

    /** Identificador único del escenario en curso. Tipo: {@code String}. */
    public static final String SCENARIO_ID = "scenario.id";

    /** Nombre legible del escenario. Tipo: {@code String}. */
    public static final String SCENARIO_NAME = "scenario.name";

    /** Resultado final del escenario. Tipo: {@link ScenarioOutcome}. */
    public static final String SCENARIO_OUTCOME = "scenario.outcome";

    /** Duración total del escenario en milisegundos. Tipo: {@code Long}. */
    public static final String SCENARIO_DURATION_MS = "scenario.duration.ms";

    // ── Step ─────────────────────────────────────────────────────────────────

    /** Texto del step tal como aparece en el archivo .feature. Tipo: {@code String}. */
    public static final String STEP_TEXT = "step.text";

    /**
     * Estado del step al finalizar. Tipo: {@code String}.
     * Valores: {@code "PASSED"}, {@code "FAILED"}, {@code "SKIPPED"}.
     */
    public static final String STEP_STATUS = "step.status";

    /** Duración de ejecución del step en milisegundos. Tipo: {@code Long}. */
    public static final String STEP_DURATION_MS = "step.duration.ms";

    /** Excepción capturada cuando el step falló. Tipo: {@code Throwable}. */
    public static final String STEP_ERROR = "step.error";

    /** Captura de pantalla tomada al fallar el step (puede ser null). Tipo: {@code byte[]}. */
    public static final String STEP_SCREENSHOT = "step.screenshot";

    // ── Ejecución ────────────────────────────────────────────────────────────

    /** Resultado global de la ejecución. Tipo: {@link com.qa.common.runtime.ExecutionResult}. */
    public static final String EXECUTION_RESULT = "execution.result";

    // ── Valores de STEP_STATUS ────────────────────────────────────────────────

    /** Valor de {@link #STEP_STATUS} cuando el step pasó. */
    public static final String STATUS_PASSED = "PASSED";

    /** Valor de {@link #STEP_STATUS} cuando el step falló. */
    public static final String STATUS_FAILED = "FAILED";

    /** Valor de {@link #STEP_STATUS} cuando el step fue saltado. */
    public static final String STATUS_SKIPPED = "SKIPPED";
}
