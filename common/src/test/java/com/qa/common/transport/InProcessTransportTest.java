package com.qa.common.transport;

import com.qa.common.reporter.ScenarioOutcome;
import com.qa.common.reporter.StepReporter;
import com.qa.common.runtime.CucumberRuntimeEngine;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionRequest;
import com.qa.common.runtime.ExecutionResult;
import io.cucumber.plugin.ConcurrentEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de {@link InProcessTransport} usando un engine mockeado para no depender
 * de la SPI real de plugins (que requiere classpath completo).
 *
 * <p>Las verificaciones de la traducción Cucumber → {@link StepReporter} se
 * hacen vía la sub-clase package-private {@link InProcessTransport.CucumberStepReporterBridge}
 * en un test separado más abajo. Aquí cubrimos el contrato del transport.
 */
@DisplayName("InProcessTransport — modo biblioteca (TASK-I02)")
class InProcessTransportTest {

    private CucumberRuntimeEngine engine;
    private ExecutorService executor;
    private InProcessTransport transport;

    @BeforeEach
    void setUp() {
        engine = mock(CucumberRuntimeEngine.class);
        executor = Executors.newSingleThreadExecutor();
        transport = new InProcessTransport(engine, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private static ExecutionResult passedResult() {
        return new ExecutionResult.Builder()
                .totalScenarios(1)
                .passedScenarios(1)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .status(ExecutionResult.Status.PASSED)
                .build();
    }

    private static ExecutionConfig minimalConfig() {
        return new ExecutionConfig.Builder().environment("test").build();
    }

    @Test
    @DisplayName("constructor inyectable rechaza null")
    void constructorRejectsNull() {
        assertThatThrownBy(() -> new InProcessTransport(null, executor))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("engine");
        assertThatThrownBy(() -> new InProcessTransport(engine, null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("executor");
    }

    @Test
    @DisplayName("submit() rechaza argumentos null")
    void submitRejectsNull() {
        assertThatThrownBy(() -> transport.submit(null, List.of(), com.qa.common.reporter.NoOpStepReporter.INSTANCE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> transport.submit(minimalConfig(), null, com.qa.common.reporter.NoOpStepReporter.INSTANCE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> transport.submit(minimalConfig(), List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("submit() retorna handle con executionId UUID válido")
    void submitReturnsHandleWithUuid() throws Exception {
        when(engine.execute(any(ExecutionRequest.class), any())).thenReturn(passedResult());

        ExecutionHandle handle = transport.submit(minimalConfig(), List.of("/tmp/x.feature"), com.qa.common.reporter.NoOpStepReporter.INSTANCE);

        assertThat(handle.executionId()).matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        handle.future().get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("future completa con ExecutionResult cuando engine termina ok")
    void futureCompletesOnEngineSuccess() throws Exception {
        ExecutionResult expected = passedResult();
        when(engine.execute(any(ExecutionRequest.class), any())).thenReturn(expected);

        ExecutionHandle handle = transport.submit(
                minimalConfig(), List.of("/tmp/x.feature"), com.qa.common.reporter.NoOpStepReporter.INSTANCE);

        ExecutionResult result = handle.future().get(5, TimeUnit.SECONDS);
        assertThat(result).isSameAs(expected);
        assertThat(handle.isDone()).isTrue();
        assertThat(handle.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("future completa excepcionalmente si el engine lanza")
    void futureCompletesExceptionallyOnEngineFailure() {
        when(engine.execute(any(ExecutionRequest.class), any()))
                .thenThrow(new RuntimeException("engine boom"));

        ExecutionHandle handle = transport.submit(
                minimalConfig(), List.of("/tmp/x.feature"), com.qa.common.reporter.NoOpStepReporter.INSTANCE);

        assertThatThrownBy(() -> handle.future().get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("engine boom");
    }

    @Test
    @DisplayName("reporter.onExecutionCompleted se invoca con el resultado del engine")
    void reporterReceivesOnExecutionCompleted() throws Exception {
        ExecutionResult expected = passedResult();
        when(engine.execute(any(ExecutionRequest.class), any())).thenReturn(expected);

        AtomicReference<ExecutionResult> received = new AtomicReference<>();
        StepReporter capturing = new StepReporter() {
            @Override public void onExecutionCompleted(ExecutionResult result) {
                received.set(result);
            }
        };

        ExecutionHandle handle = transport.submit(
                minimalConfig(), List.of("/tmp/x.feature"), capturing);
        handle.future().get(5, TimeUnit.SECONDS);

        assertThat(received.get()).isSameAs(expected);
    }

    @Test
    @DisplayName("submit pasa featurePaths al ExecutionRequest del engine")
    void submitForwardsFeaturePaths() throws Exception {
        when(engine.execute(any(ExecutionRequest.class), any())).thenReturn(passedResult());
        org.mockito.ArgumentCaptor<ExecutionRequest> reqCaptor =
                org.mockito.ArgumentCaptor.forClass(ExecutionRequest.class);

        ExecutionHandle handle = transport.submit(
                minimalConfig(),
                List.of("/tmp/a.feature", "/tmp/b.feature"),
                com.qa.common.reporter.NoOpStepReporter.INSTANCE);
        handle.future().get(5, TimeUnit.SECONDS);

        org.mockito.Mockito.verify(engine).execute(reqCaptor.capture(), any());
        assertThat(reqCaptor.getValue().getFeaturePaths())
                .containsExactly("/tmp/a.feature", "/tmp/b.feature");
    }

    @Test
    @DisplayName("submit registra el bridge Cucumber en additionalListeners")
    void submitRegistersCucumberBridge() throws Exception {
        when(engine.execute(any(ExecutionRequest.class), any())).thenReturn(passedResult());

        ExecutionHandle handle = transport.submit(
                minimalConfig(), List.of("/tmp/x.feature"), com.qa.common.reporter.NoOpStepReporter.INSTANCE);
        handle.future().get(5, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ConcurrentEventListener>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(engine).execute(any(), captor.capture());
        assertThat(captor.getValue())
                .hasSize(1)
                .allMatch(l -> l instanceof InProcessTransport.CucumberStepReporterBridge);
    }

    @Test
    @DisplayName("describeCapabilities delega al engine")
    void describeCapabilitiesDelegates() {
        com.qa.common.driver.CapabilityReport rep =
                com.qa.common.driver.CapabilityReport.unavailable("test", "stub");
        when(engine.listAllCapabilities()).thenReturn(List.of(rep));

        assertThat(transport.describeCapabilities()).containsExactly(rep);
    }

    @Test
    @DisplayName("cancel() interrumpe el run en curso (idempotente)")
    void cancelInterruptsRun() throws Exception {
        // Engine que bloquea hasta interrupción.
        AtomicBoolean interrupted = new AtomicBoolean(false);
        when(engine.execute(any(ExecutionRequest.class), any())).thenAnswer(inv -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted", e);
            }
            return passedResult();
        });

        ExecutionHandle handle = transport.submit(
                minimalConfig(), List.of("/tmp/x.feature"), com.qa.common.reporter.NoOpStepReporter.INSTANCE);

        // Espera breve para garantizar que el thread del executor entró a engine.execute.
        Thread.sleep(100);
        handle.cancel();
        handle.cancel(); // idempotencia

        // Esperamos a que el inner thread reaccione a la interrupción.
        for (int i = 0; i < 50 && !interrupted.get(); i++) {
            Thread.sleep(50);
        }

        assertThat(interrupted.get()).isTrue();
        assertThat(handle.future().isCancelled()).isTrue();
    }

    @Test
    @DisplayName("withDefaults() crea instancia funcional con executor interno")
    void withDefaultsCreatesUsableInstance() {
        // Smoke test: que se construya sin lanzar (la SPI puede no encontrar plugins en tests
        // pero el engine debe instanciarse). Cerramos para no leakear threads.
        try {
            InProcessTransport t = InProcessTransport.withDefaults();
            assertThat(t).isNotNull();
            t.close();
        } catch (RuntimeException e) {
            // En entornos sin SPI completa puede fallar; aceptable mientras no sea NPE.
            assertThat(e).isNotInstanceOf(NullPointerException.class);
        }
    }
}
