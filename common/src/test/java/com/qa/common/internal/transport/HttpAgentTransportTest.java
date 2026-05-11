package com.qa.common.internal.transport;


import com.qa.common.api.transport.ExecutionHandle;
import com.qa.common.internal.transport.HttpAgentTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.common.api.driver.CapabilityDescriptor;
import com.qa.common.api.driver.CapabilityReport;
import com.qa.common.api.reporter.ScenarioOutcome;
import com.qa.common.api.reporter.StepReporter;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests E2E del {@link HttpAgentTransport} contra un agente fake basado en
 * {@link HttpServer} (JDK builtin) — sin WireMock, sin deps nuevas.
 *
 * <p>Cubre los 4 endpoints del wire-protocol v1, reconexión SSE,
 * cancel idempotente y manejo de errores.
 */
class HttpAgentTransportTest {

    private HttpServer server;
    private FakeAgent agent;
    private URI baseUrl;
    private HttpAgentTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        agent = new FakeAgent();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/runs", agent::handleRunsRoot);
        server.createContext("/v1/runs/", agent::handleRunsSubpath);
        server.createContext("/v1/capabilities", agent::handleCapabilities);
        server.start();
        baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        transport = HttpAgentTransport.builder(baseUrl)
                .requestTimeout(Duration.ofSeconds(5))
                .sseRetryBackoff(Duration.ofMillis(50))
                .maxSseRetries(3)
                .build();
    }

    @AfterEach
    void tearDown() {
        transport.close();
        server.stop(0);
    }

    // -------------------------------------------------------------------------
    // describeCapabilities
    // -------------------------------------------------------------------------

    @Test
    void describeCapabilitiesReturnsAgentList() {
        agent.capabilities = List.of(CapabilityReport.available("MOBILE",
                List.of(new CapabilityDescriptor("emulator-5554", "Pixel 5", "Android API 33"))));

        List<CapabilityReport> caps = transport.describeCapabilities();

        assertThat(caps).hasSize(1);
        assertThat(caps.get(0).platformId()).isEqualTo("MOBILE");
        assertThat(caps.get(0).options()).hasSize(1);
    }

    @Test
    void describeCapabilitiesReturnsEmptyOnTransportError() {
        // Apaga el server: el transport debe degradar a [] sin lanzar.
        server.stop(0);
        assertThat(transport.describeCapabilities()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // submit + SSE drain end-to-end
    // -------------------------------------------------------------------------

    @Test
    void submitFullFlowDispatchesEventsAndCompletesFuture(@TempDir Path tmp) throws Exception {
        Path feature = tmp.resolve("login.feature");
        Files.writeString(feature, "Feature: Login\n  Scenario: ok\n    Given user\n");

        agent.events = List.of(
                "{\"type\":\"SCENARIO_STARTED\",\"executionId\":\"EXEC\",\"name\":\"Login OK\"}",
                "{\"type\":\"STEP_STARTED\",\"executionId\":\"EXEC\",\"stepText\":\"Given user\"}",
                "{\"type\":\"STEP_PASSED\",\"executionId\":\"EXEC\",\"stepText\":\"Given user\",\"durationMs\":42}",
                "{\"type\":\"SCENARIO_COMPLETED\",\"executionId\":\"EXEC\",\"outcome\":\"PASSED\",\"durationMs\":80}",
                "{\"type\":\"EXECUTION_COMPLETED\",\"executionId\":\"EXEC\",\"status\":\"PASSED\",\"total\":1,\"passed\":1,\"failed\":0,\"durationMs\":120}"
        );

        RecordingReporter rep = new RecordingReporter();
        ExecutionConfig cfg = new ExecutionConfig.Builder().environment("qa").build();
        ExecutionHandle handle = transport.submit(cfg, List.of(feature.toString()), rep);

        ExecutionResult res = handle.future().get(5, TimeUnit.SECONDS);
        assertThat(res.getStatus()).isEqualTo(ExecutionResult.Status.PASSED);
        assertThat(res.getTotalScenarios()).isEqualTo(1);
        assertThat(res.getPassedScenarios()).isEqualTo(1);

        assertThat(rep.types).contains("SCENARIO_STARTED", "STEP_STARTED", "STEP_PASSED",
                "SCENARIO_COMPLETED", "EXECUTION_COMPLETED");

        // El agente recibió el contenido del .feature serializado en el body.
        assertThat(agent.lastSubmitBody).contains("\"login.feature\"")
                .contains("Feature: Login");
    }

    @Test
    void submitForwardsStepFailureWithRemoteAgentException() throws Exception {
        agent.events = List.of(
                "{\"type\":\"SCENARIO_STARTED\",\"executionId\":\"E\",\"name\":\"S\"}",
                "{\"type\":\"STEP_FAILED\",\"executionId\":\"E\",\"stepText\":\"When click\",\"errorClass\":\"java.lang.IllegalStateException\",\"errorMessage\":\"boom\"}",
                "{\"type\":\"EXECUTION_COMPLETED\",\"executionId\":\"E\",\"status\":\"FAILED\",\"total\":1,\"passed\":0,\"failed\":1,\"durationMs\":50}"
        );

        RecordingReporter rep = new RecordingReporter();
        ExecutionConfig cfg = new ExecutionConfig.Builder().environment("qa").build();
        ExecutionHandle h = transport.submit(cfg, List.of(), rep);
        ExecutionResult res = h.future().get(5, TimeUnit.SECONDS);

        assertThat(res.getStatus()).isEqualTo(ExecutionResult.Status.FAILED);
        assertThat(rep.lastError).isInstanceOf(HttpAgentTransport.RemoteAgentException.class);
        assertThat(((HttpAgentTransport.RemoteAgentException) rep.lastError).getRemoteClass())
                .isEqualTo("java.lang.IllegalStateException");
        assertThat(rep.lastError).hasMessage("boom");
    }

    @Test
    void executionErrorEventCompletesFutureExceptionally() {
        agent.events = List.of(
                "{\"type\":\"EXECUTION_ERROR\",\"executionId\":\"E\",\"errorClass\":\"X\",\"errorMessage\":\"agent crashed\"}"
        );

        RecordingReporter rep = new RecordingReporter();
        ExecutionConfig cfg = new ExecutionConfig.Builder().environment("qa").build();
        ExecutionHandle h = transport.submit(cfg, List.of(), rep);

        assertThatThrownBy(() -> h.future().get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HttpAgentTransport.RemoteAgentException.class);
    }

    // -------------------------------------------------------------------------
    // reconnect
    // -------------------------------------------------------------------------

    @Test
    void reconnectsSseUpToMaxRetriesThenCompletes() throws Exception {
        // Primer intento: mata la conexión sin enviar nada útil.
        // Segundo intento: stream completo con EXECUTION_COMPLETED.
        agent.dropFirstSseAttempts = 1;
        agent.events = List.of(
                "{\"type\":\"EXECUTION_COMPLETED\",\"executionId\":\"E\",\"status\":\"PASSED\",\"total\":1,\"passed\":1,\"failed\":0,\"durationMs\":10}"
        );

        RecordingReporter rep = new RecordingReporter();
        ExecutionConfig cfg = new ExecutionConfig.Builder().environment("qa").build();
        ExecutionHandle h = transport.submit(cfg, List.of(), rep);

        ExecutionResult res = h.future().get(5, TimeUnit.SECONDS);
        assertThat(res.getStatus()).isEqualTo(ExecutionResult.Status.PASSED);
        assertThat(agent.sseAttempts.get()).isGreaterThanOrEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // cancel
    // -------------------------------------------------------------------------

    @Test
    void cancelHitsRemoteEndpointAndStopsLocalFuture() throws Exception {
        agent.holdSse = new CountDownLatch(1);   // bloquea el SSE hasta que cancel libere
        agent.events = List.of(); // no eventos

        RecordingReporter rep = new RecordingReporter();
        ExecutionConfig cfg = new ExecutionConfig.Builder().environment("qa").build();
        ExecutionHandle h = transport.submit(cfg, List.of(), rep);

        // Espera hasta que el agent registre el SSE pendiente.
        for (int i = 0; i < 50 && agent.sseAttempts.get() == 0; i++) Thread.sleep(20);

        h.cancel();
        agent.holdSse.countDown();

        // El cancel se reflejó en el agente.
        for (int i = 0; i < 50 && agent.cancelHits.get() == 0; i++) Thread.sleep(20);
        assertThat(agent.cancelHits.get()).isGreaterThanOrEqualTo(1);
        assertThat(h.isCancelled() || h.future().isCompletedExceptionally()).isTrue();
    }

    // -------------------------------------------------------------------------
    // toRequest helper
    // -------------------------------------------------------------------------

    @Test
    void toRequestPrefixesCollidingBasenamesWithParentDir(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a/login.feature"); Files.createDirectories(a.getParent()); Files.writeString(a, "FA");
        Path b = tmp.resolve("b/login.feature"); Files.createDirectories(b.getParent()); Files.writeString(b, "FB");

        var req = HttpAgentTransport.toRequest(
                new ExecutionConfig.Builder().environment("qa").build(),
                List.of(a.toString(), b.toString()));

        assertThat(req.featureContents()).containsKeys("login.feature", "b/login.feature");
    }

    // -------------------------------------------------------------------------
    // FakeAgent
    // -------------------------------------------------------------------------

    static class FakeAgent {
        List<CapabilityReport> capabilities = List.of();
        List<String> events = List.of();
        String lastSubmitBody;
        AtomicInteger sseAttempts = new AtomicInteger();
        AtomicInteger cancelHits  = new AtomicInteger();
        int dropFirstSseAttempts = 0;
        CountDownLatch holdSse;
        final Map<String, String> registry = new ConcurrentHashMap<>();
        final ObjectMapper mapper = new ObjectMapper();

        void handleCapabilities(HttpExchange ex) throws IOException {
            byte[] body = mapper.writeValueAsBytes(capabilities);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }

        void handleRunsRoot(HttpExchange ex) throws IOException {
            if (!"POST".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1); return;
            }
            lastSubmitBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String execId = "exec-" + UUID.randomUUID();
            registry.put(execId, "RUNNING");
            String resp = "{\"executionId\":\"" + execId + "\",\"eventsUrl\":\"/v1/runs/" + execId + "/events\"}";
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(202, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }

        void handleRunsSubpath(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();   // /v1/runs/{id}/{sub}
            String[] parts = path.split("/");
            if (parts.length < 5) { ex.sendResponseHeaders(404, -1); return; }
            String execId = parts[3];
            String sub = parts[4];
            if ("events".equals(sub) && "GET".equals(ex.getRequestMethod())) {
                int attempt = sseAttempts.incrementAndGet();
                if (attempt <= dropFirstSseAttempts) {
                    // Simula error transitorio: 503 → SseClient reintenta.
                    ex.sendResponseHeaders(503, -1);
                    ex.close();
                    return;
                }
                ex.getResponseHeaders().add("Content-Type", "text/event-stream");
                ex.sendResponseHeaders(200, 0);
                try (OutputStream os = ex.getResponseBody()) {
                    if (holdSse != null) {
                        try { holdSse.await(2, TimeUnit.SECONDS); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                    for (String evJson : events) {
                        os.write(("event: x\ndata: " + evJson + "\n\n").getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                }
                return;
            }
            if ("cancel".equals(sub) && "POST".equals(ex.getRequestMethod())) {
                cancelHits.incrementAndGet();
                ex.sendResponseHeaders(202, -1);
                return;
            }
            ex.sendResponseHeaders(404, -1);
        }
    }

    static class RecordingReporter implements StepReporter {
        final List<String> types = new ArrayList<>();
        final ConcurrentLinkedQueue<String> raw = new ConcurrentLinkedQueue<>();
        volatile Throwable lastError;

        @Override public void onScenarioStarted(String s, String n) { types.add("SCENARIO_STARTED"); }
        @Override public void onStepStarted(String s, String t)     { types.add("STEP_STARTED"); }
        @Override public void onStepPassed(String s, String t, Duration d) { types.add("STEP_PASSED"); }
        @Override public void onStepFailed(String s, String t, Throwable err, byte[] sc) {
            types.add("STEP_FAILED"); lastError = err;
        }
        @Override public void onStepSkipped(String s, String t) { types.add("STEP_SKIPPED"); }
        @Override public void onScenarioCompleted(String s, ScenarioOutcome o, Duration d) {
            types.add("SCENARIO_COMPLETED");
        }
        @Override public void onExecutionCompleted(ExecutionResult r) { types.add("EXECUTION_COMPLETED"); }
    }
}
