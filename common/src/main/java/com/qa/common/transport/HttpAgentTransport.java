package com.qa.common.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.common.driver.CapabilityReport;
import com.qa.common.reporter.ScenarioOutcome;
import com.qa.common.reporter.StepReporter;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionResult;
import com.qa.common.transport.wire.AgentEventDto;
import com.qa.common.transport.wire.SubmitRequestDto;
import com.qa.common.transport.wire.SubmitResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación de {@link ExecutionTransport} que delega la ejecución a un
 * agente remoto (TASK-I03 / RFC-AGENT-01). Habla el wire-protocol v1
 * expuesto por {@code mobile-agent} (TASK-I04).
 *
 * <h2>Flujo</h2>
 * <ol>
 *   <li>{@code POST baseUrl/v1/runs} con {@link SubmitRequestDto} (incluye
 *       contenidos UTF-8 de los {@code .feature} — el cliente NUNCA envía
 *       paths locales del filesystem; RFC-AGENT-01 §contrato featurePaths).</li>
 *   <li>Abre stream SSE en {@code GET baseUrl + eventsUrl} y traduce cada
 *       {@link AgentEventDto} a la llamada semántica del {@link StepReporter}.</li>
 *   <li>Al recibir {@code EXECUTION_COMPLETED} reconstruye un
 *       {@link ExecutionResult} y completa el future del handle.</li>
 *   <li>{@link ExecutionHandle#cancel()} → {@code POST baseUrl/v1/runs/&#123;id&#125;/cancel}
 *       (idempotente) y cancela el future + el SSE local.</li>
 * </ol>
 *
 * <h2>Reconexión SSE</h2>
 * <p>Si el stream SSE cae antes de {@code EXECUTION_COMPLETED}, el
 * {@link SseClient} reintenta {@code maxSseRetries} veces con backoff lineal.
 * Si todos fallan, el future completa excepcionalmente.
 *
 * <h2>Reglas inviolables (RFC-AGENT-01 §16)</h2>
 * <ul>
 *   <li><b>R-1.</b> El BE en producción usa esta clase (no
 *       {@code InProcessTransport}).</li>
 *   <li><b>R-2.</b> Los DTOs en {@code transport.wire} sólo evolucionan de
 *       forma aditiva.</li>
 *   <li><b>R-5.</b> Auth bearer (cuando llegue) NUNCA en logs. El método
 *       {@link #log(String)} ya redacta el header.</li>
 * </ul>
 *
 * @author Abel Venero
 * @since TASK-I03
 */
public class HttpAgentTransport implements ExecutionTransport {

    private static final Logger LOG = LoggerFactory.getLogger(HttpAgentTransport.class);
    private static final AtomicLong THREAD_SEQ = new AtomicLong();

    private final URI baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final ExecutorService sseExecutor;
    private final boolean ownsExecutor;
    private final Duration requestTimeout;
    private final int maxSseRetries;
    private final Duration sseRetryBackoff;
    private final Optional<String> bearerToken;

    public static Builder builder(URI baseUrl) {
        return new Builder(baseUrl);
    }

    HttpAgentTransport(Builder b) {
        this.baseUrl = Objects.requireNonNull(b.baseUrl, "baseUrl");
        this.httpClient = b.httpClient != null ? b.httpClient
                : HttpClient.newBuilder().connectTimeout(b.connectTimeout).build();
        this.mapper = b.mapper != null ? b.mapper
                : new ObjectMapper().configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (b.sseExecutor != null) {
            this.sseExecutor = b.sseExecutor;
            this.ownsExecutor = false;
        } else {
            this.sseExecutor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "http-agent-sse-" + THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            });
            this.ownsExecutor = true;
        }
        this.requestTimeout = b.requestTimeout;
        this.maxSseRetries = b.maxSseRetries;
        this.sseRetryBackoff = b.sseRetryBackoff;
        this.bearerToken = Optional.ofNullable(b.bearerToken);
    }

    // =========================================================================
    // ExecutionTransport
    // =========================================================================

    @Override
    public ExecutionHandle submit(ExecutionConfig config,
                                  List<String> featurePaths,
                                  StepReporter reporter) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(featurePaths, "featurePaths");
        Objects.requireNonNull(reporter, "reporter");

        SubmitRequestDto body;
        try {
            body = toRequest(config, featurePaths);
        } catch (IOException io) {
            return failedHandle("error leyendo featurePaths: " + io.getMessage(), io);
        }

        SubmitResponseDto submission;
        try {
            submission = postSubmit(body);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return failedHandle("submit a " + baseUrl + " falló: " + ex.getMessage(), ex);
        }

        Instant t0 = Instant.now();
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();
        URI eventsUri = baseUrl.resolve(submission.eventsUrl());

        SseClient sse = new SseClient(httpClient, eventsUri,
                (eventType, data) -> dispatchEvent(eventType, data, reporter, future, t0),
                maxSseRetries, sseRetryBackoff, requestTimeout);

        sseExecutor.execute(() -> {
            try {
                sse.start();
                if (!future.isDone()) {
                    // Stream cerró sin EXECUTION_COMPLETED — usual cuando el
                    // agente cae mid-execution. Completamos con ERROR.
                    future.completeExceptionally(new IOException(
                            "SSE cerró sin EXECUTION_COMPLETED para " + submission.executionId()));
                }
            } catch (IOException io) {
                future.completeExceptionally(io);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(ie);
            } finally {
                // onExecutionCompleted del reporter ya lo emite el dispatcher
                // si vimos EXECUTION_COMPLETED. Si no, sintetizamos uno con
                // los contadores en cero para no dejar huérfano al reporter.
                if (future.isCompletedExceptionally()) {
                    safeCompleted(reporter, errorResult(t0));
                }
            }
        });

        Runnable cancel = () -> {
            try {
                postCancel(submission.executionId());
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                LOG.warn("cancel remoto falló para {}: {}", submission.executionId(), ex.getMessage());
            } finally {
                sse.stop();
                future.cancel(true);
            }
        };

        return new ExecutionHandle(submission.executionId(), future, cancel);
    }

    @Override
    public List<CapabilityReport> describeCapabilities() {
        try {
            HttpRequest req = baseRequest(baseUrl.resolve("/v1/capabilities")).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + resp.statusCode() + " desde /v1/capabilities");
            }
            return mapper.readValue(resp.body(), new TypeReference<List<CapabilityReport>>() { });
        } catch (IOException io) {
            LOG.warn("describeCapabilities falló contra {}: {}", baseUrl, io.getMessage());
            return List.of();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    public void close() {
        if (ownsExecutor) {
            sseExecutor.shutdown();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SubmitResponseDto postSubmit(SubmitRequestDto body) throws IOException, InterruptedException {
        HttpRequest req = baseRequest(baseUrl.resolve("/v1/runs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " desde /v1/runs: " + resp.body());
        }
        return mapper.readValue(resp.body(), SubmitResponseDto.class);
    }

    private void postCancel(String executionId) throws IOException, InterruptedException {
        HttpRequest req = baseRequest(baseUrl.resolve("/v1/runs/" + executionId + "/cancel"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        if (resp.statusCode() != 202 && resp.statusCode() != 404 && resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " desde /v1/runs/{id}/cancel");
        }
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri).timeout(requestTimeout);
        bearerToken.ifPresent(t -> b.header("Authorization", "Bearer " + t));
        return b;
    }

    static SubmitRequestDto toRequest(ExecutionConfig config, List<String> featurePaths) throws IOException {
        Map<String, String> contents = new LinkedHashMap<>();
        for (String p : featurePaths) {
            Path path = Paths.get(p);
            String basename = path.getFileName().toString();
            // Si hay colisiones de basename, prefijamos con el directorio inmediato.
            if (contents.containsKey(basename) && path.getParent() != null) {
                basename = path.getParent().getFileName() + "/" + basename;
            }
            contents.put(basename, Files.readString(path, StandardCharsets.UTF_8));
        }
        // Resolución del httpEngine de forma defensiva (puede ser null en el config).
        String httpEngineName = config.getHttpEngine() == null ? null : config.getHttpEngine().name();
        return new SubmitRequestDto(
                config.getEnvironment(),
                config.getBrowser() == null ? "" : config.getBrowser(),
                config.getTags() == null ? "" : config.getTags(),
                config.isParallelEnabled(),
                config.getThreadCount(),
                httpEngineName,
                config.isTrustAllSsl(),
                new HashMap<>(config.getProperties()),
                contents);
    }

    private void dispatchEvent(String eventType, String dataJson,
                               StepReporter reporter,
                               CompletableFuture<ExecutionResult> future,
                               Instant t0) {
        AgentEventDto ev;
        try {
            ev = mapper.readValue(dataJson, AgentEventDto.class);
        } catch (IOException io) {
            LOG.warn("Evento SSE no parseable ({}): {}", eventType, io.getMessage());
            return;
        }
        String execId = ev.executionId();
        String type = ev.type() != null ? ev.type() : eventType;
        if (type == null) return;
        switch (type) {
            case "SCENARIO_STARTED" -> reporter.onScenarioStarted(execId, nullToEmpty(ev.name()));
            case "STEP_STARTED"     -> reporter.onStepStarted(execId, nullToEmpty(ev.stepText()));
            case "STEP_PASSED"      -> reporter.onStepPassed(execId, nullToEmpty(ev.stepText()),
                                            Duration.ofMillis(ev.durationMs() == null ? 0 : ev.durationMs()));
            case "STEP_FAILED"      -> reporter.onStepFailed(execId, nullToEmpty(ev.stepText()),
                                            new RemoteAgentException(
                                                    ev.errorClass(),
                                                    ev.errorMessage() == null ? "step failed" : ev.errorMessage()),
                                            null);
            case "STEP_SKIPPED"     -> reporter.onStepSkipped(execId, nullToEmpty(ev.stepText()));
            case "SCENARIO_COMPLETED" -> reporter.onScenarioCompleted(execId,
                                            mapOutcome(ev.outcome()),
                                            Duration.ofMillis(ev.durationMs() == null ? 0 : ev.durationMs()));
            case "EXECUTION_COMPLETED" -> {
                ExecutionResult res = new ExecutionResult.Builder()
                        .status(parseStatus(ev.status()))
                        .totalScenarios(opt(ev.total()))
                        .passedScenarios(opt(ev.passed()))
                        .failedScenarios(opt(ev.failed()))
                        .duration(Duration.ofMillis(ev.durationMs() == null ? 0 : ev.durationMs()))
                        .startTime(t0)
                        .endTime(Instant.now())
                        .build();
                safeCompleted(reporter, res);
                future.complete(res);
            }
            case "EXECUTION_ERROR" -> {
                RemoteAgentException ex = new RemoteAgentException(
                        ev.errorClass(),
                        ev.errorMessage() == null ? "agent error" : ev.errorMessage());
                future.completeExceptionally(ex);
            }
            default -> { /* tipos futuros — ignorar para mantener forward-compat (R-2) */ }
        }
    }

    private static void safeCompleted(StepReporter r, ExecutionResult res) {
        try { r.onExecutionCompleted(res); } catch (RuntimeException re) {
            LOG.warn("reporter.onExecutionCompleted falló: {}", re.getMessage());
        }
    }

    private static int opt(Integer v) { return v == null ? 0 : v; }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static ScenarioOutcome mapOutcome(String s) {
        if (s == null) return ScenarioOutcome.ABORTED;
        try { return ScenarioOutcome.valueOf(s); } catch (IllegalArgumentException ex) {
            return ScenarioOutcome.ABORTED;
        }
    }

    private static ExecutionResult.Status parseStatus(String s) {
        if (s == null) return ExecutionResult.Status.ERROR;
        try { return ExecutionResult.Status.valueOf(s); } catch (IllegalArgumentException ex) {
            return ExecutionResult.Status.ERROR;
        }
    }

    private ExecutionResult errorResult(Instant t0) {
        return new ExecutionResult.Builder()
                .status(ExecutionResult.Status.ERROR)
                .duration(Duration.between(t0, Instant.now()))
                .startTime(t0)
                .endTime(Instant.now())
                .build();
    }

    private static ExecutionHandle failedHandle(String msg, Throwable cause) {
        CompletableFuture<ExecutionResult> f = new CompletableFuture<>();
        f.completeExceptionally(new IOException(msg, cause));
        return new ExecutionHandle("failed-" + System.nanoTime(), f, () -> { });
    }

    // ---------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------

    /**
     * Builder para configurar timeouts, retries y autenticación. Los defaults
     * son sanos para un agente local.
     */
    public static final class Builder {
        private final URI baseUrl;
        private HttpClient httpClient;
        private ObjectMapper mapper;
        private ExecutorService sseExecutor;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofMinutes(2);
        private int maxSseRetries = 3;
        private Duration sseRetryBackoff = Duration.ofMillis(500);
        private String bearerToken;

        Builder(URI baseUrl) { this.baseUrl = baseUrl; }

        public Builder httpClient(HttpClient c)    { this.httpClient = c; return this; }
        public Builder objectMapper(ObjectMapper m){ this.mapper = m; return this; }
        public Builder sseExecutor(ExecutorService e){ this.sseExecutor = e; return this; }
        public Builder connectTimeout(Duration d)  { this.connectTimeout = d; return this; }
        public Builder requestTimeout(Duration d)  { this.requestTimeout = d; return this; }
        public Builder maxSseRetries(int n)        { this.maxSseRetries = n; return this; }
        public Builder sseRetryBackoff(Duration d) { this.sseRetryBackoff = d; return this; }
        public Builder bearerToken(String t)       { this.bearerToken = t; return this; }

        public HttpAgentTransport build() { return new HttpAgentTransport(this); }
    }

    /**
     * Excepción tipada que el reporter recibe cuando un STEP falla en el
     * agente remoto: preserva la clase original como string + el mensaje.
     */
    public static final class RemoteAgentException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String remoteClass;

        public RemoteAgentException(String remoteClass, String message) {
            super(message);
            this.remoteClass = remoteClass == null ? "RemoteAgentException" : remoteClass;
        }

        public String getRemoteClass() { return remoteClass; }

        @Override public String toString() {
            return remoteClass + ": " + getMessage();
        }
    }
}
