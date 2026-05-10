package com.qa.mobileagent.service;

import com.qa.common.reporter.ScenarioOutcome;
import com.qa.common.runtime.ExecutionResult;
import com.qa.mobileagent.api.dto.AgentEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SseStepReporterTest {

    @Test
    void emitsScenarioAndStepEventsInOrder() {
        SseStepReporter r = new SseStepReporter("exec-1", 64);

        r.onScenarioStarted("exec-1", "Login OK");
        r.onStepStarted("exec-1", "Given user");
        r.onStepPassed("exec-1", "Given user", Duration.ofMillis(120));
        r.onScenarioCompleted("exec-1", ScenarioOutcome.PASSED, Duration.ofSeconds(1));

        assertThat(r.queue()).extracting(AgentEvent::type).containsExactly(
                "SCENARIO_STARTED", "STEP_STARTED", "STEP_PASSED", "SCENARIO_COMPLETED");
    }

    @Test
    void onStepFailedCarriesErrorClassAndMessage() {
        SseStepReporter r = new SseStepReporter("exec-2", 16);
        r.onStepFailed("exec-2", "When I click", new IllegalStateException("boom"), null);

        AgentEvent ev = r.queue().poll();
        assertThat(ev).isNotNull();
        assertThat(ev.type()).isEqualTo("STEP_FAILED");
        assertThat(ev.errorClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(ev.errorMessage()).isEqualTo("boom");
    }

    @Test
    void onExecutionCompletedSerializesGlobalStats() {
        SseStepReporter r = new SseStepReporter("exec-3", 16);
        ExecutionResult res = new ExecutionResult.Builder()
                .status(ExecutionResult.Status.PASSED)
                .totalScenarios(3)
                .passedScenarios(2)
                .failedScenarios(1)
                .duration(Duration.ofMillis(500))
                .startTime(Instant.now())
                .endTime(Instant.now())
                .build();
        r.onExecutionCompleted(res);

        AgentEvent ev = r.queue().poll();
        assertThat(ev).isNotNull();
        assertThat(ev.type()).isEqualTo("EXECUTION_COMPLETED");
        assertThat(ev.status()).isEqualTo("PASSED");
        assertThat(ev.total()).isEqualTo(3);
        assertThat(ev.passed()).isEqualTo(2);
        assertThat(ev.failed()).isEqualTo(1);
        assertThat(ev.durationMs()).isEqualTo(500L);
    }

    @Test
    void boundedQueueDropsOldestOnOverflow() {
        SseStepReporter r = new SseStepReporter("exec-4", 64); // capacidad mínima 64
        for (int i = 0; i < 80; i++) {
            r.onStepStarted("exec-4", "step-" + i);
        }
        // No debe haber bloqueo y el tamaño está acotado.
        assertThat(r.queue().size()).isLessThanOrEqualTo(64);
    }
}
