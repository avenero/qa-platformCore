package com.qa.common.reporter;

import com.qa.common.runtime.ExecutionResult;
import com.qa.common.runtime.events.EventBus;
import com.qa.common.runtime.events.ExecutionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBusStepReporterAdapterTest {

    @Mock
    private StepReporter reporter;

    private EventBusStepReporterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EventBusStepReporterAdapter(reporter);
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    @Test
    void constructor_nullReporter_throwsNullPointerException() {
        assertThatNullPointerException().isThrownBy(
            () -> new EventBusStepReporterAdapter(null)
        );
    }

    @Test
    void getReporter_returnsConfiguredReporter() {
        assertThat(adapter.getReporter()).isSameAs(reporter);
    }

    // =========================================================================
    // STEP_START
    // =========================================================================

    @Nested
    class StepStart {

        @Test
        void onEvent_stepStart_callsOnStepStarted() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "step-start", Map.of(
                EventDataKeys.SCENARIO_ID, "scen-1",
                EventDataKeys.STEP_TEXT, "Given the user is authenticated"
            ));

            adapter.onEvent(event);

            verify(reporter).onStepStarted("scen-1", "Given the user is authenticated");
            verifyNoMoreInteractions(reporter);
        }

        @Test
        void onEvent_stepStart_missingData_usesEmptyStrings() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "no-data");

            adapter.onEvent(event);

            verify(reporter).onStepStarted("", "");
        }
    }

    // =========================================================================
    // STEP_END — PASSED
    // =========================================================================

    @Nested
    class StepEndPassed {

        @Test
        void onEvent_stepEndPassed_callsOnStepPassed() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step-end", Map.of(
                EventDataKeys.SCENARIO_ID,      "scen-1",
                EventDataKeys.STEP_TEXT,         "Then the response status is 200",
                EventDataKeys.STEP_STATUS,       EventDataKeys.STATUS_PASSED,
                EventDataKeys.STEP_DURATION_MS,  150L
            ));

            adapter.onEvent(event);

            verify(reporter).onStepPassed("scen-1", "Then the response status is 200",
                Duration.ofMillis(150));
            verifyNoMoreInteractions(reporter);
        }

        @Test
        void onEvent_stepEndPassed_missingDuration_usesZero() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step-end", Map.of(
                EventDataKeys.SCENARIO_ID,  "scen-1",
                EventDataKeys.STEP_TEXT,    "step",
                EventDataKeys.STEP_STATUS,  EventDataKeys.STATUS_PASSED
            ));

            adapter.onEvent(event);

            var captor = ArgumentCaptor.forClass(Duration.class);
            verify(reporter).onStepPassed(eq("scen-1"), eq("step"), captor.capture());
            assertThat(captor.getValue()).isEqualTo(Duration.ZERO);
        }
    }

    // =========================================================================
    // STEP_END — FAILED
    // =========================================================================

    @Nested
    class StepEndFailed {

        @Test
        void onEvent_stepEndFailed_callsOnStepFailed() {
            var error = new AssertionError("Expected 200 but got 404");
            var screenshot = new byte[]{1, 2, 3};

            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step-end", Map.of(
                EventDataKeys.SCENARIO_ID,    "scen-2",
                EventDataKeys.STEP_TEXT,      "Then status is 200",
                EventDataKeys.STEP_STATUS,    EventDataKeys.STATUS_FAILED,
                EventDataKeys.STEP_ERROR,     error,
                EventDataKeys.STEP_SCREENSHOT, screenshot
            ));

            adapter.onEvent(event);

            verify(reporter).onStepFailed("scen-2", "Then status is 200", error, screenshot);
            verifyNoMoreInteractions(reporter);
        }

        @Test
        void onEvent_stepEndFailed_withoutScreenshot_passesNull() {
            var error = new RuntimeException("timeout");

            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step-end", Map.of(
                EventDataKeys.SCENARIO_ID, "scen-2",
                EventDataKeys.STEP_TEXT,   "When timeout",
                EventDataKeys.STEP_STATUS, EventDataKeys.STATUS_FAILED,
                EventDataKeys.STEP_ERROR,  error
            ));

            adapter.onEvent(event);

            verify(reporter).onStepFailed("scen-2", "When timeout", error, null);
        }
    }

    // =========================================================================
    // STEP_END — SKIPPED
    // =========================================================================

    @Nested
    class StepEndSkipped {

        @Test
        void onEvent_stepEndSkipped_callsOnStepSkipped() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step-end", Map.of(
                EventDataKeys.SCENARIO_ID, "scen-3",
                EventDataKeys.STEP_TEXT,   "And the cart is updated",
                EventDataKeys.STEP_STATUS, EventDataKeys.STATUS_SKIPPED
            ));

            adapter.onEvent(event);

            verify(reporter).onStepSkipped("scen-3", "And the cart is updated");
            verifyNoMoreInteractions(reporter);
        }
    }

    // =========================================================================
    // STEP_END — status desconocido
    // =========================================================================

    @Test
    void onEvent_stepEnd_unknownStatus_noReporterCallMade() {
        var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step-end", Map.of(
            EventDataKeys.SCENARIO_ID, "scen-x",
            EventDataKeys.STEP_TEXT,   "step",
            EventDataKeys.STEP_STATUS, "PENDING"
        ));

        adapter.onEvent(event);

        verifyNoInteractions(reporter);
    }

    // =========================================================================
    // SCENARIO_START
    // =========================================================================

    @Nested
    class ScenarioStart {

        @Test
        void onEvent_scenarioStart_callsOnScenarioStarted() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.SCENARIO_START, "scen-start", Map.of(
                EventDataKeys.SCENARIO_ID,   "scen-99",
                EventDataKeys.SCENARIO_NAME, "User logs in with valid credentials"
            ));

            adapter.onEvent(event);

            verify(reporter).onScenarioStarted("scen-99", "User logs in with valid credentials");
            verifyNoMoreInteractions(reporter);
        }
    }

    // =========================================================================
    // SCENARIO_END
    // =========================================================================

    @Nested
    class ScenarioEnd {

        @Test
        void onEvent_scenarioEnd_passed_callsOnScenarioCompleted() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.SCENARIO_END, "scen-end", Map.of(
                EventDataKeys.SCENARIO_ID,          "scen-7",
                EventDataKeys.SCENARIO_OUTCOME,     ScenarioOutcome.PASSED,
                EventDataKeys.SCENARIO_DURATION_MS, 1200L
            ));

            adapter.onEvent(event);

            verify(reporter).onScenarioCompleted(
                "scen-7", ScenarioOutcome.PASSED, Duration.ofMillis(1200));
            verifyNoMoreInteractions(reporter);
        }

        @Test
        void onEvent_scenarioEnd_failed_callsOnScenarioCompleted() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.SCENARIO_END, "scen-end", Map.of(
                EventDataKeys.SCENARIO_ID,          "scen-8",
                EventDataKeys.SCENARIO_OUTCOME,     ScenarioOutcome.FAILED,
                EventDataKeys.SCENARIO_DURATION_MS, 800L
            ));

            adapter.onEvent(event);

            verify(reporter).onScenarioCompleted(
                "scen-8", ScenarioOutcome.FAILED, Duration.ofMillis(800));
        }

        @Test
        void onEvent_scenarioEnd_missingOutcome_usesAbortedDefault() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.SCENARIO_END, "scen-end", Map.of(
                EventDataKeys.SCENARIO_ID, "scen-9"
            ));

            adapter.onEvent(event);

            var captor = ArgumentCaptor.forClass(ScenarioOutcome.class);
            verify(reporter).onScenarioCompleted(eq("scen-9"), captor.capture(), any());
            assertThat(captor.getValue()).isEqualTo(ScenarioOutcome.ABORTED);
        }
    }

    // =========================================================================
    // EXECUTION_END
    // =========================================================================

    @Nested
    class ExecutionEnd {

        @Test
        void onEvent_executionEnd_callsOnExecutionCompleted() {
            ExecutionResult result = new ExecutionResult.Builder()
                .status(ExecutionResult.Status.PASSED)
                .totalScenarios(5)
                .passedScenarios(5)
                .build();

            var event = ExecutionEvent.of(ExecutionEvent.Type.EXECUTION_END, "exec-end", Map.of(
                EventDataKeys.EXECUTION_RESULT, result
            ));

            adapter.onEvent(event);

            verify(reporter).onExecutionCompleted(result);
            verifyNoMoreInteractions(reporter);
        }

        @Test
        void onEvent_executionEnd_missingResult_doesNotCallReporter() {
            var event = ExecutionEvent.of(ExecutionEvent.Type.EXECUTION_END, "exec-end");

            adapter.onEvent(event);

            verifyNoInteractions(reporter);
        }
    }

    // =========================================================================
    // Eventos ignorados
    // =========================================================================

    @Test
    void onEvent_executionStart_isIgnored() {
        var event = ExecutionEvent.of(ExecutionEvent.Type.EXECUTION_START, "exec-start");
        adapter.onEvent(event);
        verifyNoInteractions(reporter);
    }

    @Test
    void onEvent_customEvent_isIgnored() {
        var event = ExecutionEvent.of(ExecutionEvent.Type.CUSTOM, "some-custom");
        adapter.onEvent(event);
        verifyNoInteractions(reporter);
    }

    // =========================================================================
    // Resiliencia: error en reporter no lanza excepción
    // =========================================================================

    @Test
    void onEvent_reporterThrows_exceptionAbsorbed() {
        doThrow(new RuntimeException("reporter crash"))
            .when(reporter).onStepStarted(any(), any());

        var event = ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "s", Map.of(
            EventDataKeys.SCENARIO_ID, "sc",
            EventDataKeys.STEP_TEXT,   "step"
        ));

        assertThatCode(() -> adapter.onEvent(event)).doesNotThrowAnyException();
    }

    @Test
    void onEvent_nullEvent_throwsNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> adapter.onEvent(null));
    }

    // =========================================================================
    // TEST DE INTEGRACIÓN — EventBus → adapter → reporter
    // =========================================================================

    @Nested
    class IntegrationWithEventBus {

        @Test
        void publish_stepStart_throughBus_reachesReporter() {
            var bus = new EventBus();
            bus.subscribe(adapter);

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "s", Map.of(
                EventDataKeys.SCENARIO_ID, "sc-int",
                EventDataKeys.STEP_TEXT,   "Given integration test"
            )));

            verify(reporter).onStepStarted("sc-int", "Given integration test");
        }

        @Test
        void publish_fullScenarioLifecycle_allMethodsCalledInOrder() {
            var bus = new EventBus();
            bus.subscribe(adapter);

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.SCENARIO_START, "scen", Map.of(
                EventDataKeys.SCENARIO_ID,   "sc-flow",
                EventDataKeys.SCENARIO_NAME, "Full flow test"
            )));

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "step1", Map.of(
                EventDataKeys.SCENARIO_ID, "sc-flow",
                EventDataKeys.STEP_TEXT,   "Given a precondition"
            )));

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_END, "step1-end", Map.of(
                EventDataKeys.SCENARIO_ID,     "sc-flow",
                EventDataKeys.STEP_TEXT,       "Given a precondition",
                EventDataKeys.STEP_STATUS,     EventDataKeys.STATUS_PASSED,
                EventDataKeys.STEP_DURATION_MS, 100L
            )));

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.SCENARIO_END, "scen-end", Map.of(
                EventDataKeys.SCENARIO_ID,          "sc-flow",
                EventDataKeys.SCENARIO_OUTCOME,     ScenarioOutcome.PASSED,
                EventDataKeys.SCENARIO_DURATION_MS, 300L
            )));

            var inOrder = inOrder(reporter);
            inOrder.verify(reporter).onScenarioStarted("sc-flow", "Full flow test");
            inOrder.verify(reporter).onStepStarted("sc-flow", "Given a precondition");
            inOrder.verify(reporter).onStepPassed("sc-flow", "Given a precondition",
                Duration.ofMillis(100));
            inOrder.verify(reporter).onScenarioCompleted("sc-flow", ScenarioOutcome.PASSED,
                Duration.ofMillis(300));
        }

        @Test
        void unsubscribe_afterExecution_noMoreCalls() {
            var bus = new EventBus();
            bus.subscribe(adapter);
            bus.unsubscribe(adapter);

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "step", Map.of(
                EventDataKeys.SCENARIO_ID, "sc",
                EventDataKeys.STEP_TEXT,   "step"
            )));

            verifyNoInteractions(reporter);
        }

        @Test
        void errorInReporter_doesNotBlockOtherSubscribers() {
            var bus = new EventBus();
            bus.subscribe(adapter);

            var otherReceived = new java.util.concurrent.atomic.AtomicBoolean(false);
            bus.subscribe(e -> otherReceived.set(true));

            doThrow(new RuntimeException("boom")).when(reporter).onStepStarted(any(), any());

            bus.publish(ExecutionEvent.of(ExecutionEvent.Type.STEP_START, "s", Map.of(
                EventDataKeys.SCENARIO_ID, "sc",
                EventDataKeys.STEP_TEXT,   "step"
            )));

            assertThat(otherReceived).isTrue();
        }
    }
}
