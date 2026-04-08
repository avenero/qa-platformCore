package com.qa.common.runtime;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cucumber EventListener que captura resultados de ejecucion en memoria.
 *
 * <p>A diferencia de los reporters basados en archivos (JSON, HTML), este collector
 * mantiene los resultados en memoria para que {@link CucumberRuntimeEngine} pueda
 * construir un {@link ExecutionResult} sin I/O.
 *
 * <p>Thread-safe gracias a {@link ConcurrentEventListener} y contadores atomicos.
 *
 * <p>Uso tipico:
 * <pre>
 * InMemoryResultCollector collector = new InMemoryResultCollector();
 * // ... registrar como plugin de Cucumber ...
 * // Despues de runtime.run():
 * ExecutionResult result = collector.buildResult();
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see CucumberRuntimeEngine
 * @see ExecutionResult
 */
public final class InMemoryResultCollector implements ConcurrentEventListener {

    private static final Logger log = LoggerFactory.getLogger(InMemoryResultCollector.class);

    private final AtomicInteger totalScenarios = new AtomicInteger(0);
    private final AtomicInteger passedScenarios = new AtomicInteger(0);
    private final AtomicInteger failedScenarios = new AtomicInteger(0);
    private final AtomicInteger skippedScenarios = new AtomicInteger(0);
    private final CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();

    private volatile Instant startTime;
    private volatile Instant endTime;
    private volatile boolean finished = false;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::onTestRunStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onTestRunFinished);
    }

    private void onTestRunStarted(TestRunStarted event) {
        startTime = event.getInstant();
        log.debug("Test run iniciado: {}", startTime);
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        totalScenarios.incrementAndGet();

        Result result = event.getResult();
        Status status = result.getStatus();

        if (status == Status.PASSED) {
            passedScenarios.incrementAndGet();
        } else if (status == Status.FAILED) {
            failedScenarios.incrementAndGet();
            Throwable error = result.getError();
            String scenarioName = event.getTestCase().getName();
            String errorMsg = error != null ? error.getMessage() : "Unknown error";
            errors.add(scenarioName + ": " + errorMsg);
            log.debug("Escenario fallido: {} - {}", scenarioName, errorMsg);
        } else {
            // SKIPPED, PENDING, UNDEFINED, AMBIGUOUS
            skippedScenarios.incrementAndGet();
        }
    }

    private void onTestRunFinished(TestRunFinished event) {
        endTime = event.getInstant();
        finished = true;
        log.debug("Test run finalizado: {} (total={}, passed={}, failed={})",
                endTime, totalScenarios.get(), passedScenarios.get(), failedScenarios.get());
    }

    /**
     * Construye el {@link ExecutionResult} con los datos capturados.
     *
     * <p>Debe invocarse despues de {@code runtime.run()}.
     *
     * @return resultado inmutable de la ejecucion
     */
    public ExecutionResult buildResult() {
        Instant start = startTime != null ? startTime : Instant.now();
        Instant end = endTime != null ? endTime : Instant.now();

        int total = totalScenarios.get();
        int passed = passedScenarios.get();
        int failed = failedScenarios.get();

        ExecutionResult.Status status;
        if (failed > 0) {
            status = ExecutionResult.Status.FAILED;
        } else if (total == 0) {
            status = ExecutionResult.Status.PASSED; // No scenarios = vacuous truth
        } else {
            status = ExecutionResult.Status.PASSED;
        }

        return new ExecutionResult.Builder()
                .status(status)
                .totalScenarios(total)
                .passedScenarios(passed)
                .failedScenarios(failed)
                .duration(Duration.between(start, end))
                .startTime(start)
                .endTime(end)
                .errors(List.copyOf(errors))
                .build();
    }

    /**
     * Indica si la ejecucion ha finalizado.
     * @return true si TestRunFinished fue recibido
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Total de escenarios ejecutados hasta el momento.
     * @return total
     */
    public int getTotalScenarios() {
        return totalScenarios.get();
    }

    /**
     * Escenarios pasados hasta el momento.
     * @return pasados
     */
    public int getPassedScenarios() {
        return passedScenarios.get();
    }

    /**
     * Escenarios fallidos hasta el momento.
     * @return fallidos
     */
    public int getFailedScenarios() {
        return failedScenarios.get();
    }

    /**
     * Lista de errores capturados.
     * @return lista inmutable de errores
     */
    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * Reinicia el collector para reutilizacion.
     */
    public void reset() {
        totalScenarios.set(0);
        passedScenarios.set(0);
        failedScenarios.set(0);
        skippedScenarios.set(0);
        errors.clear();
        startTime = null;
        endTime = null;
        finished = false;
    }

    @Override
    public String toString() {
        return "InMemoryResultCollector{total=" + totalScenarios.get()
                + ", passed=" + passedScenarios.get()
                + ", failed=" + failedScenarios.get()
                + ", finished=" + finished + "}";
    }
}

