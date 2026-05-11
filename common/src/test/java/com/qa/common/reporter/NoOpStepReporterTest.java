package com.qa.common.reporter;


import com.qa.common.api.reporter.NoOpStepReporter;
import com.qa.common.api.reporter.ScenarioOutcome;
import com.qa.common.api.reporter.StepReporter;
import com.qa.common.api.runtime.ExecutionResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NoOpStepReporterTest {

    private final StepReporter noOp = NoOpStepReporter.INSTANCE;

    // ── Singleton ─────────────────────────────────────────────────────────────

    @Test
    void instance_isSingleton() {
        assertThat(NoOpStepReporter.INSTANCE).isSameAs(NoOpStepReporter.INSTANCE);
    }

    @Test
    void instance_implementsStepReporter() {
        assertThat(noOp).isInstanceOf(StepReporter.class);
    }

    // ── Todos los métodos deben ser no-op (no lanzar excepción) ───────────────

    @Test
    void onScenarioStarted_doesNotThrow() {
        assertThatCode(() -> noOp.onScenarioStarted("s1", "Login scenario"))
            .doesNotThrowAnyException();
    }

    @Test
    void onStepStarted_doesNotThrow() {
        assertThatCode(() -> noOp.onStepStarted("s1", "Given the user is logged in"))
            .doesNotThrowAnyException();
    }

    @Test
    void onStepPassed_doesNotThrow() {
        assertThatCode(() -> noOp.onStepPassed("s1", "Given step", Duration.ofMillis(50)))
            .doesNotThrowAnyException();
    }

    @Test
    void onStepFailed_doesNotThrow() {
        assertThatCode(() ->
            noOp.onStepFailed("s1", "When step", new RuntimeException("fail"), null)
        ).doesNotThrowAnyException();
    }

    @Test
    void onStepFailed_withScreenshot_doesNotThrow() {
        assertThatCode(() ->
            noOp.onStepFailed("s1", "When step", new RuntimeException("fail"), new byte[]{1, 2, 3})
        ).doesNotThrowAnyException();
    }

    @Test
    void onStepSkipped_doesNotThrow() {
        assertThatCode(() -> noOp.onStepSkipped("s1", "Then step"))
            .doesNotThrowAnyException();
    }

    @Test
    void onScenarioCompleted_doesNotThrow() {
        assertThatCode(() ->
            noOp.onScenarioCompleted("s1", ScenarioOutcome.PASSED, Duration.ofSeconds(2))
        ).doesNotThrowAnyException();
    }

    @Test
    void onExecutionCompleted_doesNotThrow() {
        ExecutionResult result = new ExecutionResult.Builder()
            .status(ExecutionResult.Status.PASSED)
            .totalScenarios(3)
            .passedScenarios(3)
            .build();

        assertThatCode(() -> noOp.onExecutionCompleted(result))
            .doesNotThrowAnyException();
    }

    @Test
    void allMethodsWithNullArgs_doesNotThrow() {
        // Los default methods son no-op, no deben validar nada
        assertThatCode(() -> {
            noOp.onScenarioStarted(null, null);
            noOp.onStepStarted(null, null);
            noOp.onStepPassed(null, null, null);
            noOp.onStepFailed(null, null, null, null);
            noOp.onStepSkipped(null, null);
            noOp.onScenarioCompleted(null, null, null);
            noOp.onExecutionCompleted(null);
        }).doesNotThrowAnyException();
    }
}
