package com.qa.common.internal.concurrency;

import com.qa.common.api.Internal;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.logging.TestLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta un conjunto de tareas en paralelo con un pool de hilos configurable.
 *
 * <p>Uso típico en BDD steps:
 * <ol>
 *   <li>Construir una lista de {@link Callable} (cada uno representa una petición HTTP o acción)</li>
 *   <li>Llamar a {@link #execute(List)} y obtener los {@link ParallelResult}</li>
 *   <li>Validar los resultados con {@link #allSucceeded()} o {@link #getLastResults()}</li>
 * </ol>
 *
 * <p>Cada instancia está pensada para un escenario; no reutilizar entre escenarios.
 *
 * @since 2.1.0
 */
@Internal(reason = "internal — usar api/runtime + steps/parallel para ejecutar pasos en paralelo")
public class ParallelStepExecutor {

    private static final TestLogger.LoggerWrapper LOG =
            TestLogger.getLogger(ParallelStepExecutor.class);

    private final int threadCount;
    private final long timeoutSeconds;
    private final List<ParallelResult> lastResults = new CopyOnWriteArrayList<>();
    private long lastExecutionMs = -1;

    /**
     * @param threadCount    número de hilos del pool (>0)
     * @param timeoutSeconds tiempo máximo de espera total (>0)
     */
    public ParallelStepExecutor(int threadCount, long timeoutSeconds) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount debe ser > 0");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds debe ser > 0");
        }
        this.threadCount = threadCount;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Ejecuta las tareas en paralelo y almacena los resultados para inspección posterior.
     *
     * @param tasks lista de tareas a ejecutar
     * @return lista inmutable de resultados, en el mismo orden que las tareas
     */
    public List<ParallelResult> execute(List<Callable<Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(threadCount, tasks.size()));
        long startMs = System.currentTimeMillis();

        try {
            List<Future<Object>> futures = executor.invokeAll(tasks, timeoutSeconds, TimeUnit.SECONDS);
            List<ParallelResult> results = new ArrayList<>(futures.size());

            for (Future<Object> f : futures) {
                if (f.isCancelled()) {
                    results.add(ParallelResult.timeout());
                } else {
                    try {
                        results.add(ParallelResult.success(f.get()));
                    } catch (ExecutionException e) {
                        results.add(ParallelResult.failure(e.getCause()));
                    }
                }
            }

            lastExecutionMs = System.currentTimeMillis() - startMs;
            lastResults.clear();
            lastResults.addAll(results);

            long succeeded = results.stream().filter(r -> r.success()).count();
            long failed    = results.stream().filter(r -> r.failed()).count();
            long timedOut  = results.stream().filter(r -> r.timedOut()).count();
            LOG.info("Parallel execution finished in {}ms — success={}, failed={}, timeout={}",
                    lastExecutionMs, succeeded, failed, timedOut);

            return Collections.unmodifiableList(results);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel execution interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /** @return resultados de la última llamada a {@link #execute(List)} */
    public List<ParallelResult> getLastResults() {
        return Collections.unmodifiableList(lastResults);
    }

    /** @return duración en ms de la última ejecución, o -1 si aún no se ejecutó */
    public long getLastExecutionMs() {
        return lastExecutionMs;
    }

    /** @return true si todos los resultados fueron exitosos */
    public boolean allSucceeded() {
        return !lastResults.isEmpty() && lastResults.stream().allMatch(r -> r.success());
    }

    /** @return true si algún resultado falló (excepción, no timeout) */
    public boolean anyFailed() {
        return lastResults.stream().anyMatch(r -> r.failed());
    }

    /** @return true si alguna tarea fue cancelada por timeout */
    public boolean anyTimedOut() {
        return lastResults.stream().anyMatch(r -> r.timedOut());
    }

    /** @return número de hilos configurado */
    public int getThreadCount() { return threadCount; }

    /** @return timeout en segundos configurado */
    public long getTimeoutSeconds() { return timeoutSeconds; }
}
