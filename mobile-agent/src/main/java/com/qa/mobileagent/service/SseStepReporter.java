package com.qa.mobileagent.service;

import com.qa.common.api.reporter.ScenarioOutcome;
import com.qa.common.api.reporter.StepReporter;
import com.qa.common.api.runtime.ExecutionResult;
import com.qa.mobileagent.api.dto.AgentEvent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link StepReporter} que traduce eventos a {@link AgentEvent} y los encola
 * para envío vía SSE.
 *
 * <p>Thread-safe: el reporter es invocado desde el hilo del engine; los
 * controladores HTTP drenan la cola desde otro hilo. La cola es bounded
 * (capacidad configurable) para evitar OOM si el cliente HTTP es lento.
 *
 * @since TASK-I04
 */
public class SseStepReporter implements StepReporter {

    private static final int MIN_BUFFER_CAPACITY = 64;

    private final String executionId;
    private final LinkedBlockingQueue<AgentEvent> queue;

    public SseStepReporter(String executionId, int bufferCapacity) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.queue = new LinkedBlockingQueue<>(Math.max(MIN_BUFFER_CAPACITY, bufferCapacity));
    }

    public LinkedBlockingQueue<AgentEvent> queue() {
        return queue;
    }

    @Override
    public void onScenarioStarted(String scenarioId, String name) {
        offer(AgentEvent.scenarioStarted(executionId, name));
    }

    @Override
    public void onStepStarted(String scenarioId, String stepText) {
        offer(AgentEvent.stepStarted(executionId, stepText));
    }

    @Override
    public void onStepPassed(String scenarioId, String stepText, Duration duration) {
        offer(AgentEvent.stepPassed(executionId, stepText, durationMs(duration)));
    }

    @Override
    public void onStepFailed(String scenarioId, String stepText, Throwable error, byte[] screenshot) {
        // R-5: NUNCA logueamos tokens; aquí sólo propagamos clase + mensaje del error.
        offer(AgentEvent.stepFailed(executionId, stepText, error));
    }

    @Override
    public void onStepSkipped(String scenarioId, String stepText) {
        offer(AgentEvent.stepSkipped(executionId, stepText));
    }

    @Override
    public void onScenarioCompleted(String scenarioId, ScenarioOutcome outcome, Duration totalDuration) {
        offer(AgentEvent.scenarioCompleted(executionId,
                outcome == null ? "ABORTED" : outcome.name(),
                durationMs(totalDuration)));
    }

    @Override
    public void onExecutionCompleted(ExecutionResult result) {
        if (result == null) {
            offer(AgentEvent.executionCompleted(executionId, "ERROR", 0, 0, 0, 0));
            return;
        }
        offer(AgentEvent.executionCompleted(
                executionId,
                result.getStatus() == null ? "ERROR" : result.getStatus().name(),
                result.getTotalScenarios(),
                result.getPassedScenarios(),
                result.getFailedScenarios(),
                durationMs(result.getDuration())));
    }

    private static long durationMs(Duration d) {
        return d == null ? 0L : d.toMillis();
    }

    private void offer(AgentEvent ev) {
        // offer (no put) — si la cola está llena, descartamos el evento más
        // antiguo para mantener el flujo. Es preferible perder un STEP_STARTED
        // que bloquear al engine. EXECUTION_COMPLETED es prioritario y por
        // construcción siempre cabe (la cola se drena al completar).
        if (!queue.offer(ev)) {
            queue.poll();          // libera el más viejo
            queue.offer(ev);
        }
    }
}
