package com.qa.common.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioOutcomeTest {

    // ── isTerminal ────────────────────────────────────────────────────────────

    @Test
    void passed_isTerminal() {
        assertThat(ScenarioOutcome.PASSED.isTerminal()).isTrue();
    }

    @Test
    void failed_isTerminal() {
        assertThat(ScenarioOutcome.FAILED.isTerminal()).isTrue();
    }

    @Test
    void aborted_isTerminal() {
        assertThat(ScenarioOutcome.ABORTED.isTerminal()).isTrue();
    }

    @Test
    void skipped_isNotTerminal() {
        assertThat(ScenarioOutcome.SKIPPED.isTerminal()).isFalse();
    }

    // ── isSuccess ─────────────────────────────────────────────────────────────

    @Test
    void passed_isSuccess() {
        assertThat(ScenarioOutcome.PASSED.isSuccess()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioOutcome.class, names = {"FAILED", "SKIPPED", "ABORTED"})
    void nonPassed_isNotSuccess(ScenarioOutcome outcome) {
        assertThat(outcome.isSuccess()).isFalse();
    }

    // ── isFailureOrAbort ──────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = ScenarioOutcome.class, names = {"FAILED", "ABORTED"})
    void failedOrAborted_isFailureOrAbort(ScenarioOutcome outcome) {
        assertThat(outcome.isFailureOrAbort()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioOutcome.class, names = {"PASSED", "SKIPPED"})
    void passedOrSkipped_isNotFailureOrAbort(ScenarioOutcome outcome) {
        assertThat(outcome.isFailureOrAbort()).isFalse();
    }

    // ── enum coverage ─────────────────────────────────────────────────────────

    @Test
    void allValues_present() {
        assertThat(ScenarioOutcome.values())
            .containsExactlyInAnyOrder(
                ScenarioOutcome.PASSED,
                ScenarioOutcome.FAILED,
                ScenarioOutcome.SKIPPED,
                ScenarioOutcome.ABORTED
            );
    }

    @Test
    void valueOf_works() {
        assertThat(ScenarioOutcome.valueOf("PASSED")).isEqualTo(ScenarioOutcome.PASSED);
        assertThat(ScenarioOutcome.valueOf("FAILED")).isEqualTo(ScenarioOutcome.FAILED);
        assertThat(ScenarioOutcome.valueOf("SKIPPED")).isEqualTo(ScenarioOutcome.SKIPPED);
        assertThat(ScenarioOutcome.valueOf("ABORTED")).isEqualTo(ScenarioOutcome.ABORTED);
    }
}
