package com.qa.common.runtime;

import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para InMemoryResultCollector.
 *
 * <p>Estrategia: se capturan los handlers registrados en {@link EventPublisher}
 * mediante un stub funcional, y se disparan directamente con eventos mockeados
 * (Mockito). De esta forma se prueba toda la logica sin un runtime Cucumber real
 * ni I/O.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("InMemoryResultCollector")
class InMemoryResultCollectorTest {

    // =========================================================================
    // Infraestructura de stub para EventPublisher
    // =========================================================================

    private final Map<Class<?>, EventHandler<?>> handlers = new HashMap<>();

    private EventPublisher buildPublisher() {
        return new EventPublisher() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> void registerHandlerFor(Class<T> eventType, EventHandler<T> handler) {
                handlers.put(eventType, handler);
            }

            @Override
            public <T> void removeHandlerFor(Class<T> eventType, EventHandler<T> handler) {
                handlers.remove(eventType);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> void fire(Class<T> type, T event) {
        EventHandler<T> handler = (EventHandler<T>) handlers.get(type);
        if (handler != null) {
            handler.receive(event);
        }
    }

    // =========================================================================
    // Helpers para construir eventos mockeados
    // =========================================================================

    private TestRunStarted mockStartEvent() {
        TestRunStarted e = mock(TestRunStarted.class);
        when(e.getInstant()).thenReturn(Instant.now());
        return e;
    }

    private TestRunFinished mockEndEvent() {
        TestRunFinished e = mock(TestRunFinished.class);
        when(e.getInstant()).thenReturn(Instant.now().plusSeconds(1));
        return e;
    }

    private TestCaseFinished mockScenario(String name, Status status, Throwable error) {
        Result result = mock(Result.class);
        when(result.getStatus()).thenReturn(status);
        when(result.getError()).thenReturn(error);

        TestCase testCase = mock(TestCase.class);
        when(testCase.getName()).thenReturn(name);

        TestCaseFinished event = mock(TestCaseFinished.class);
        when(event.getResult()).thenReturn(result);
        when(event.getTestCase()).thenReturn(testCase);
        return event;
    }

    // =========================================================================

    private InMemoryResultCollector collector;

    @BeforeEach
    void setUp() {
        handlers.clear();
        collector = new InMemoryResultCollector();
        collector.setEventPublisher(buildPublisher());
    }

    // =========================================================================

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicialTest {

        @Test
        @DisplayName("totalScenarios es 0 antes de cualquier evento")
        void totalEsCeroInicialmente() {
            assertThat(collector.getTotalScenarios()).isZero();
        }

        @Test
        @DisplayName("passedScenarios es 0 inicialmente")
        void passedEsCeroInicialmente() {
            assertThat(collector.getPassedScenarios()).isZero();
        }

        @Test
        @DisplayName("failedScenarios es 0 inicialmente")
        void failedEsCeroInicialmente() {
            assertThat(collector.getFailedScenarios()).isZero();
        }

        @Test
        @DisplayName("isFinished() es false antes de TestRunFinished")
        void noEstaFinalizadoInicialmente() {
            assertThat(collector.isFinished()).isFalse();
        }

        @Test
        @DisplayName("getErrors() retorna lista vacia inicialmente")
        void erroresVaciosInicialmente() {
            assertThat(collector.getErrors()).isEmpty();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("buildResult() — sin escenarios")
    class BuildResultVacioTest {

        @Test
        @DisplayName("Sin eventos retorna status PASSED (vacuous truth)")
        void sinEscenariosPassed() {
            ExecutionResult result = collector.buildResult();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.PASSED);
        }

        @Test
        @DisplayName("Sin eventos: total=0, passed=0, failed=0")
        void sinEscenariosContadoresCero() {
            ExecutionResult result = collector.buildResult();
            assertThat(result.getTotalScenarios()).isZero();
            assertThat(result.getPassedScenarios()).isZero();
            assertThat(result.getFailedScenarios()).isZero();
        }

        @Test
        @DisplayName("Sin eventos: errors es lista vacia")
        void sinEscenariosErroresVacios() {
            assertThat(collector.buildResult().getErrors()).isEmpty();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Procesamiento de TestRunStarted")
    class TestRunStartedTest {

        @Test
        @DisplayName("isFinished() sigue en false despues de TestRunStarted")
        void noFinalizaDespuesDeStart() {
            fire(TestRunStarted.class, mockStartEvent());
            assertThat(collector.isFinished()).isFalse();
        }

        @Test
        @DisplayName("buildResult() retorna startTime no nulo despues de TestRunStarted")
        void startTimePresenteDespuesDeStart() {
            fire(TestRunStarted.class, mockStartEvent());
            assertThat(collector.buildResult().getStartTime()).isNotNull();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Procesamiento de TestCaseFinished — escenarios PASSED")
    class EscenariosPassedTest {

        @Test
        @DisplayName("Un escenario PASSED incrementa total a 1 y passed a 1")
        void unEscenarioPassed() {
            fire(TestCaseFinished.class, mockScenario("Login test", Status.PASSED, null));

            assertThat(collector.getTotalScenarios()).isEqualTo(1);
            assertThat(collector.getPassedScenarios()).isEqualTo(1);
            assertThat(collector.getFailedScenarios()).isZero();
        }

        @Test
        @DisplayName("Tres escenarios PASSED: total=3, passed=3, failed=0")
        void tresEscenariosPassed() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            fire(TestCaseFinished.class, mockScenario("T2", Status.PASSED, null));
            fire(TestCaseFinished.class, mockScenario("T3", Status.PASSED, null));

            assertThat(collector.getTotalScenarios()).isEqualTo(3);
            assertThat(collector.getPassedScenarios()).isEqualTo(3);
            assertThat(collector.getFailedScenarios()).isZero();
        }

        @Test
        @DisplayName("buildResult() con solo escenarios PASSED retorna status PASSED")
        void buildResultConPassedEsSuccess() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            assertThat(collector.buildResult().getStatus())
                .isEqualTo(ExecutionResult.Status.PASSED);
        }

        @Test
        @DisplayName("Escenario PASSED no agrega entradas a errors")
        void passedNoAgregaErrores() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            assertThat(collector.getErrors()).isEmpty();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Procesamiento de TestCaseFinished — escenarios FAILED")
    class EscenariosFailedTest {

        @Test
        @DisplayName("Un escenario FAILED incrementa total a 1 y failed a 1")
        void unEscenarioFailed() {
            fire(TestCaseFinished.class,
                mockScenario("Login test", Status.FAILED, new RuntimeException("Assertion error")));

            assertThat(collector.getTotalScenarios()).isEqualTo(1);
            assertThat(collector.getFailedScenarios()).isEqualTo(1);
            assertThat(collector.getPassedScenarios()).isZero();
        }

        @Test
        @DisplayName("buildResult() con escenario FAILED retorna status FAILED")
        void buildResultConFailedEsFailed() {
            fire(TestCaseFinished.class,
                mockScenario("T1", Status.FAILED, new RuntimeException("Boom")));

            assertThat(collector.buildResult().getStatus())
                .isEqualTo(ExecutionResult.Status.FAILED);
        }

        @Test
        @DisplayName("Escenario FAILED agrega mensaje de error que contiene nombre del escenario")
        void failedAgregaMensajeDeError() {
            fire(TestCaseFinished.class,
                mockScenario("Login test", Status.FAILED, new RuntimeException("Expected true but was false")));

            assertThat(collector.getErrors())
                .hasSize(1)
                .first().asString()
                .contains("Login test");
        }

        @Test
        @DisplayName("Escenario FAILED con error nulo usa 'Unknown error'")
        void failedConErrorNuloUsaUnknown() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.FAILED, null));

            assertThat(collector.getErrors())
                .hasSize(1)
                .first().asString()
                .contains("Unknown error");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Procesamiento de TestCaseFinished — escenarios SKIPPED")
    class EscenariosSkippedTest {

        @Test
        @DisplayName("Escenario SKIPPED incrementa total pero no passed ni failed")
        void skippedIncrementaSoloTotal() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.SKIPPED, null));

            assertThat(collector.getTotalScenarios()).isEqualTo(1);
            assertThat(collector.getPassedScenarios()).isZero();
            assertThat(collector.getFailedScenarios()).isZero();
        }

        @Test
        @DisplayName("Escenario SKIPPED no agrega errores")
        void skippedNoAgregaErrores() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.SKIPPED, null));
            assertThat(collector.getErrors()).isEmpty();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Mix de escenarios passed/failed/skipped")
    class MixEscenariosTest {

        @Test
        @DisplayName("Mix 2 passed + 1 failed + 1 skipped: total=4, passed=2, failed=1")
        void mixDeEscenarios() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            fire(TestCaseFinished.class, mockScenario("T2", Status.PASSED, null));
            fire(TestCaseFinished.class,
                mockScenario("T3", Status.FAILED, new AssertionError("Expected 200 got 500")));
            fire(TestCaseFinished.class, mockScenario("T4", Status.SKIPPED, null));

            assertThat(collector.getTotalScenarios()).isEqualTo(4);
            assertThat(collector.getPassedScenarios()).isEqualTo(2);
            assertThat(collector.getFailedScenarios()).isEqualTo(1);
        }

        @Test
        @DisplayName("buildResult() con al menos 1 failed tiene status FAILED")
        void buildResultConFalladoEsFailed() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            fire(TestCaseFinished.class,
                mockScenario("T2", Status.FAILED, new RuntimeException("Timeout")));

            ExecutionResult result = collector.buildResult();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.FAILED);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("buildResult() con todos PASSED tiene isSuccess() == true")
        void buildResultTodosPassedEsSuccess() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            fire(TestCaseFinished.class, mockScenario("T2", Status.PASSED, null));

            assertThat(collector.buildResult().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("getErrors() retorna lista inmutable")
        void erroresEsInmutable() {
            fire(TestCaseFinished.class,
                mockScenario("T1", Status.FAILED, new RuntimeException("err")));

            assertThat(collector.getErrors())
                .isNotNull()
                .hasSize(1);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Procesamiento de TestRunFinished")
    class TestRunFinishedTest {

        @Test
        @DisplayName("isFinished() es true despues de TestRunFinished")
        void finalizadoDespuesDeEnd() {
            fire(TestRunFinished.class, mockEndEvent());
            assertThat(collector.isFinished()).isTrue();
        }

        @Test
        @DisplayName("buildResult() tiene endTime no nulo despues de TestRunFinished")
        void endTimePresenteDespuesDeEnd() {
            fire(TestRunStarted.class, mockStartEvent());
            fire(TestRunFinished.class, mockEndEvent());

            assertThat(collector.buildResult().getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("duration es positiva si start < end")
        void durationEsPositiva() {
            fire(TestRunStarted.class, mockStartEvent());

            // Simular que fin ocurre 1 segundo despues
            TestRunFinished endEvent = mock(TestRunFinished.class);
            when(endEvent.getInstant()).thenReturn(Instant.now().plusSeconds(2));
            fire(TestRunFinished.class, endEvent);

            assertThat(collector.buildResult().getDuration()).isPositive();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("reset()")
    class ResetTest {

        @Test
        @DisplayName("reset() pone todos los contadores a 0")
        void resetLimpiaContadores() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            fire(TestCaseFinished.class,
                mockScenario("T2", Status.FAILED, new RuntimeException("err")));

            collector.reset();

            assertThat(collector.getTotalScenarios()).isZero();
            assertThat(collector.getPassedScenarios()).isZero();
            assertThat(collector.getFailedScenarios()).isZero();
        }

        @Test
        @DisplayName("reset() limpia la lista de errores")
        void resetLimpiaErrores() {
            fire(TestCaseFinished.class,
                mockScenario("T1", Status.FAILED, new RuntimeException("boom")));
            collector.reset();
            assertThat(collector.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("reset() pone isFinished() a false")
        void resetLimpiaFinished() {
            fire(TestRunFinished.class, mockEndEvent());
            assertThat(collector.isFinished()).isTrue();

            collector.reset();
            assertThat(collector.isFinished()).isFalse();
        }

        @Test
        @DisplayName("Despues de reset() se puede volver a acumular resultados")
        void despuesDeResetAcumulaNuevamente() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));
            collector.reset();

            fire(TestCaseFinished.class, mockScenario("T2", Status.PASSED, null));
            fire(TestCaseFinished.class, mockScenario("T3", Status.PASSED, null));

            assertThat(collector.getTotalScenarios()).isEqualTo(2);
            assertThat(collector.getPassedScenarios()).isEqualTo(2);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("toString()")
    class ToStringTest {

        @Test
        @DisplayName("toString() contiene total, passed, failed y finished")
        void toStringContieneCamposClave() {
            fire(TestCaseFinished.class, mockScenario("T1", Status.PASSED, null));

            String str = collector.toString();
            assertThat(str)
                .contains("total=")
                .contains("passed=")
                .contains("failed=")
                .contains("finished=");
        }
    }
}
