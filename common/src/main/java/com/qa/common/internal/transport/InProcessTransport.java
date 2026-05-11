package com.qa.common.internal.transport;


import com.qa.common.api.transport.ExecutionHandle;
import com.qa.common.api.transport.ExecutionTransport;
import com.qa.common.api.driver.CapabilityReport;
import com.qa.common.api.reporter.ScenarioOutcome;
import com.qa.common.api.reporter.StepReporter;
import com.qa.common.internal.runtime.CucumberRuntimeEngine;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionRequest;
import com.qa.common.api.runtime.ExecutionResult;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestStepStarted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementación de {@link ExecutionTransport} que invoca al
 * {@link CucumberRuntimeEngine} en la **misma JVM** del caller (TASK-I02,
 * sub-sprint S2 de RFC-AGENT-01).
 *
 * <h2>Modo de operación</h2>
 * <p>Default para CI / tests / single-host. Cero overhead de red, cero
 * serialización. Comparte el classpath del proceso que lo instancia.
 *
 * <h2>Cableado del reporter</h2>
 * <p>El {@link StepReporter} recibido por {@link #submit} se cablea como
 * {@link ConcurrentEventListener} de Cucumber (vía {@code additionalListeners}
 * del engine) a través de {@link CucumberStepReporterBridge} (clase interna).
 * Eso garantiza que el reporter reciba eventos en tiempo real, NO al final.
 *
 * <h2>Cancel</h2>
 * <p>Idempotente: invocar {@code cancel()} dispara {@link Future#cancel} con
 * {@code mayInterruptIfRunning=true}. El engine puede ignorar la interrupción
 * según la fase en la que se encuentre (Cucumber a veces no responde a
 * {@code Thread.interrupt()} mid-step). Para cancelación garantizada, las
 * implementaciones remotas (TASK-I03) usan {@code POST /v1/runs/{id}/cancel}.
 *
 * <h2>Thread-safety</h2>
 * <p>Una instancia es segura para múltiples llamadas concurrentes a
 * {@link #submit}. Cada llamada construye su propio handle independiente.
 * El executor por defecto es {@code newCachedThreadPool} con thread factory
 * que nombra los hilos {@code in-process-transport-N}.
 *
 * <h2>Reglas inviolables (RFC-AGENT-01 §16)</h2>
 * <ul>
 *   <li><b>R-1.</b> El BE en producción NO instancia esta clase. Lo usa sólo en
 *       CI / tests integrados. Producción siempre {@code HttpAgentTransport}.</li>
 * </ul>
 *
 * @author Abel Venero
 * @since TASK-I02
 * @see ExecutionTransport
 * @see CucumberRuntimeEngine
 */
public class InProcessTransport implements ExecutionTransport {

    private static final Logger LOG = LoggerFactory.getLogger(InProcessTransport.class);
    private static final java.util.concurrent.atomic.AtomicLong THREAD_SEQ = new java.util.concurrent.atomic.AtomicLong();

    private final CucumberRuntimeEngine engine;
    private final ExecutorService executor;
    private final boolean ownsExecutor;

    /**
     * Constructor inyectable — recomendado para tests y para callers que
     * gestionan su propio pool.
     *
     * @param engine   engine ya construido. Si quieres uno con SPI-discovery,
     *                 usa {@code CucumberRuntimeEngine.withServiceLoader()}.
     * @param executor executor donde correr los runs async. NO se cierra al
     *                 cerrar el transport (ownership externo).
     */
    public InProcessTransport(CucumberRuntimeEngine engine, ExecutorService executor) {
        this.engine = Objects.requireNonNull(engine, "engine no puede ser null");
        this.executor = Objects.requireNonNull(executor, "executor no puede ser null");
        this.ownsExecutor = false;
    }

    private InProcessTransport(CucumberRuntimeEngine engine, ExecutorService executor, boolean ownsExecutor) {
        this.engine = engine;
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
    }

    /**
     * Factory que reutiliza un engine ya construido externamente y crea un executor
     * cached propio.  Recomendado cuando el caller gestiona el ciclo de vida del
     * engine (p.ej. Spring Bean con SPI-discovery en {@code withServiceLoader()}).
     *
     * @param engine engine ya inicializado (con plugins y glue paths cargados)
     * @since TASK-I02-BE
     */
    public static InProcessTransport withEngine(CucumberRuntimeEngine engine) {
        Objects.requireNonNull(engine, "engine no puede ser null");
        ExecutorService exec = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "in-process-transport-" + THREAD_SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        return new InProcessTransport(engine, exec, true);
    }

    /**
     * Factory que descubre plugins via SPI y crea un executor cached interno.
     * Recomendado para callers single-host (CI / tests integrados / agent).
     */
    public static InProcessTransport withDefaults() {
        ExecutorService exec = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "in-process-transport-" + THREAD_SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        return new InProcessTransport(CucumberRuntimeEngine.withServiceLoader(), exec, true);
    }

    @Override
    public ExecutionHandle submit(ExecutionConfig config,
                                  List<String> featurePaths,
                                  StepReporter reporter) {
        Objects.requireNonNull(config,       "config no puede ser null");
        Objects.requireNonNull(featurePaths, "featurePaths no puede ser null");
        Objects.requireNonNull(reporter,     "reporter no puede ser null");

        String executionId = UUID.randomUUID().toString();
        ExecutionRequest request = ExecutionRequest.of(featurePaths, config);

        CucumberStepReporterBridge bridge = new CucumberStepReporterBridge(executionId, reporter);

        // submit retorna un Future<ExecutionResult>; lo envolvemos en CompletableFuture
        // para exponer el contrato del handle. Usamos un wrapper que respeta cancel().
        AtomicReference<Future<?>> innerFutureRef = new AtomicReference<>();
        CompletableFuture<ExecutionResult> external = new CompletableFuture<>();

        Future<?> innerFuture = executor.submit(() -> {
            try {
                LOG.debug("InProcessTransport iniciando run {} (features={})", executionId, featurePaths.size());
                ExecutionResult result = engine.execute(request, List.of(bridge));
                // Emite el evento terminal antes de completar el future, para garantizar
                // que cualquier listener del reporter procese onExecutionCompleted antes
                // de que el caller observe future.isDone().
                safe(() -> reporter.onExecutionCompleted(result));
                external.complete(result);
            } catch (Throwable t) {
                LOG.error("InProcessTransport run {} falló: {}", executionId, t.getMessage(), t);
                external.completeExceptionally(t);
            }
        });
        innerFutureRef.set(innerFuture);

        Runnable cancelHook = () -> {
            Future<?> f = innerFutureRef.get();
            if (f != null) {
                f.cancel(true);
            }
            // Marcamos el future externo como cancelado si aún no completó.
            external.cancel(true);
        };

        return new ExecutionHandle(executionId, external, cancelHook);
    }

    @Override
    public List<CapabilityReport> describeCapabilities() {
        return engine.listAllCapabilities();
    }

    /**
     * Cierra el executor SI lo creó el factory {@link #withDefaults()}.
     * Para instancias creadas con el constructor inyectable, el caller mantiene
     * el ownership del executor.
     */
    public void close() {
        if (ownsExecutor) {
            executor.shutdown();
        }
    }

    private static void safe(Runnable r) {
        try { r.run(); } catch (RuntimeException e) {
            LOG.warn("StepReporter lanzó excepción en onExecutionCompleted: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Cucumber listener → StepReporter
    // =========================================================================

    /**
     * Adapter Cucumber → {@link StepReporter}. Se registra como
     * {@link ConcurrentEventListener} en el engine y traduce eventos Cucumber
     * (TestCaseStarted/Finished, TestStepStarted/Finished) a llamadas
     * semánticas del reporter.
     *
     * <p>Si el reporter lanza, el adapter lo loguea y CONTINÚA — no debe
     * bloquear la ejecución del engine.
     */
    static final class CucumberStepReporterBridge implements ConcurrentEventListener {

        private final String executionId;
        private final StepReporter reporter;

        CucumberStepReporterBridge(String executionId, StepReporter reporter) {
            this.executionId = executionId;
            this.reporter = reporter;
        }

        @Override
        public void setEventPublisher(EventPublisher publisher) {
            publisher.registerHandlerFor(TestCaseStarted.class, this::onTestCaseStarted);
            publisher.registerHandlerFor(TestStepStarted.class, this::onTestStepStarted);
            publisher.registerHandlerFor(io.cucumber.plugin.event.TestStepFinished.class, this::onTestStepFinished);
            publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        }

        private void onTestCaseStarted(TestCaseStarted ev) {
            safeCall(() -> reporter.onScenarioStarted(executionId, ev.getTestCase().getName()));
        }

        private void onTestStepStarted(TestStepStarted ev) {
            String stepText = stepText(ev.getTestStep());
            if (stepText != null) {
                safeCall(() -> reporter.onStepStarted(executionId, stepText));
            }
        }

        private void onTestStepFinished(io.cucumber.plugin.event.TestStepFinished ev) {
            String stepText = stepText(ev.getTestStep());
            if (stepText == null) {
                return;
            }
            Status status = ev.getResult().getStatus();
            Duration dur = ev.getResult().getDuration();
            switch (status) {
                case PASSED -> safeCall(() -> reporter.onStepPassed(executionId, stepText, dur));
                case FAILED, AMBIGUOUS, UNDEFINED ->
                        safeCall(() -> reporter.onStepFailed(
                                executionId, stepText,
                                ev.getResult().getError() != null
                                        ? ev.getResult().getError()
                                        : new RuntimeException("Step " + status + " sin excepción asociada"),
                                null));
                case SKIPPED, PENDING ->
                        safeCall(() -> reporter.onStepSkipped(executionId, stepText));
                default -> { /* no-op */ }
            }
        }

        private void onTestCaseFinished(TestCaseFinished ev) {
            ScenarioOutcome outcome = mapOutcome(ev.getResult().getStatus());
            Duration dur = ev.getResult().getDuration();
            safeCall(() -> reporter.onScenarioCompleted(executionId, outcome, dur));
        }

        private static String stepText(io.cucumber.plugin.event.TestStep step) {
            if (step instanceof io.cucumber.plugin.event.PickleStepTestStep pst) {
                return pst.getStep().getKeyword() + pst.getStep().getText();
            }
            return null; // hooks no tienen step text
        }

        private static ScenarioOutcome mapOutcome(Status status) {
            return switch (status) {
                case PASSED -> ScenarioOutcome.PASSED;
                case FAILED, AMBIGUOUS -> ScenarioOutcome.FAILED;
                case SKIPPED -> ScenarioOutcome.SKIPPED;
                case PENDING, UNDEFINED -> ScenarioOutcome.ABORTED;
                default -> ScenarioOutcome.ABORTED;
            };
        }

        private static void safeCall(Runnable r) {
            try { r.run(); } catch (RuntimeException e) {
                LOG.warn("StepReporter lanzó excepción procesando evento Cucumber: {}", e.getMessage(), e);
            }
        }
    }

    // Suprimido para evitar import warnings sobre el record `TestStepFinished` de cucumber-messages.
    @SuppressWarnings("unused")
    private static void _keepImports(TestStepFinished unused) { /* solo evita unused-import */ }
}
