package com.qa.common.api.reporter;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.runtime.ExecutionResult;
import com.qa.common.api.runtime.events.EventSubscriber;
import com.qa.common.api.runtime.events.ExecutionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * Traductor entre el {@link com.qa.common.api.runtime.events.EventBus} interno del Core
 * y el contrato semántico {@link StepReporter}.
 *
 * <p>Se registra como {@link EventSubscriber} en el EventBus y convierte cada
 * {@link ExecutionEvent} en la llamada semántica correspondiente del reporter.
 * De esta forma el BE (u otro consumidor) implementa {@link StepReporter} sin
 * conocer los detalles del bus de eventos interno del Core.
 *
 * <h2>Uso típico</h2>
 * <pre>
 * StepReporter myReporter = new MyBEStepReporter(wsSession);
 * EventBusStepReporterAdapter adapter = new EventBusStepReporterAdapter(myReporter);
 * eventBus.subscribe(adapter);
 * try {
 *     engine.execute(request);
 * } finally {
 *     eventBus.unsubscribe(adapter);   // SIEMPRE desregistrar en finally
 * }
 * </pre>
 *
 * <h2>Extracción de datos del evento</h2>
 * <p>Los datos viajan en el mapa {@code data} del evento. Las claves están definidas en
 * {@link EventDataKeys}. Si una clave no está presente en el mapa, se usa un valor
 * seguro por defecto (nunca lanza excepción).
 *
 * <h2>Invariante de thread-safety</h2>
 * <p>Este adapter es stateless: no guarda estado entre llamadas. Es seguro compartirlo
 * entre hilos siempre que el {@link StepReporter} inyectado también sea thread-safe.
 *
 * @see StepReporter
 * @see EventDataKeys
 * @see com.qa.common.api.runtime.events.EventBus
 * @since 3.1.0
 */
public final class EventBusStepReporterAdapter implements EventSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(EventBusStepReporterAdapter.class);

    private final StepReporter reporter;

    /**
     * Crea el adapter con el reporter al que se traducirán los eventos.
     *
     * @param reporter reporter destino; no null. Usar {@link NoOpStepReporter#INSTANCE}
     *                 cuando no haya consumidor activo.
     */
    public EventBusStepReporterAdapter(StepReporter reporter) {
        this.reporter = Objects.requireNonNull(reporter, "reporter no puede ser null");
    }

    /**
     * Recibe un evento del EventBus y lo traduce a la llamada semántica correspondiente.
     *
     * <p>Eventos sin mapeo (ej: {@code EXECUTION_START}, {@code CUSTOM}) son ignorados
     * silenciosamente. Errores en el reporter se capturan y logean para que no interrumpan
     * la publicación del evento hacia otros suscriptores.
     *
     * @param event evento del ciclo de vida BDD; no null
     */
    @Override
    public void onEvent(ExecutionEvent event) {
        Objects.requireNonNull(event, "event no puede ser null");
        try {
            dispatch(event);
        } catch (Exception e) {
            LOG.error("Error en StepReporter al procesar evento [{}]: {}",
                event.getType(), e.getMessage(), e);
        }
    }

    private void dispatch(ExecutionEvent event) {
        switch (event.getType()) {

            case STEP_START -> {
                String scenarioId = str(event, EventDataKeys.SCENARIO_ID);
                String stepText   = str(event, EventDataKeys.STEP_TEXT);
                reporter.onStepStarted(scenarioId, stepText);
            }

            case STEP_END -> {
                String scenarioId = str(event, EventDataKeys.SCENARIO_ID);
                String stepText   = str(event, EventDataKeys.STEP_TEXT);
                String status     = str(event, EventDataKeys.STEP_STATUS);

                switch (status) {
                    case EventDataKeys.STATUS_PASSED -> {
                        Duration duration = durationMs(event, EventDataKeys.STEP_DURATION_MS);
                        reporter.onStepPassed(scenarioId, stepText, duration);
                    }
                    case EventDataKeys.STATUS_FAILED -> {
                        Throwable error      = event.getData(EventDataKeys.STEP_ERROR, Throwable.class);
                        byte[]    screenshot = event.getData(EventDataKeys.STEP_SCREENSHOT, byte[].class);
                        reporter.onStepFailed(scenarioId, stepText, error, screenshot);
                    }
                    case EventDataKeys.STATUS_SKIPPED ->
                        reporter.onStepSkipped(scenarioId, stepText);
                    default ->
                        LOG.debug("STEP_END con status desconocido '{}' — ignorado", status);
                }
            }

            case SCENARIO_START -> {
                String scenarioId   = str(event, EventDataKeys.SCENARIO_ID);
                String scenarioName = str(event, EventDataKeys.SCENARIO_NAME);
                reporter.onScenarioStarted(scenarioId, scenarioName);
            }

            case SCENARIO_END -> {
                String          scenarioId = str(event, EventDataKeys.SCENARIO_ID);
                ScenarioOutcome outcome    = event.getData(EventDataKeys.SCENARIO_OUTCOME,
                                                           ScenarioOutcome.class);
                Duration        duration   = durationMs(event, EventDataKeys.SCENARIO_DURATION_MS);

                if (outcome == null) {
                    LOG.warn("SCENARIO_END sin ScenarioOutcome en data — usando ABORTED por defecto");
                    outcome = ScenarioOutcome.ABORTED;
                }
                reporter.onScenarioCompleted(scenarioId, outcome, duration);
            }

            case EXECUTION_END -> {
                ExecutionResult result = event.getData(EventDataKeys.EXECUTION_RESULT,
                                                        ExecutionResult.class);
                if (result == null) {
                    LOG.warn("EXECUTION_END sin ExecutionResult en data — ignorado");
                    return;
                }
                reporter.onExecutionCompleted(result);
            }

            default ->
                LOG.trace("Evento ignorado por el adapter: {}", event.getType());
        }
    }

    // ── helpers de extracción segura ──────────────────────────────────────────

    /** Extrae un String del data map; retorna cadena vacía si no está presente. */
    private static String str(ExecutionEvent event, String key) {
        String val = event.getData(key, String.class);
        return val != null ? val : "";
    }

    /**
     * Extrae una duración en milisegundos del data map.
     * Retorna {@link Duration#ZERO} si la clave no está o el valor es null.
     */
    private static Duration durationMs(ExecutionEvent event, String key) {
        Long ms = event.getData(key, Long.class);
        return ms != null ? Duration.ofMillis(ms) : Duration.ZERO;
    }

    /** Retorna el reporter configurado (útil para tests de integración). */
    public StepReporter getReporter() {
        return reporter;
    }
}
