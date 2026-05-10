package com.qa.mobileagent.service;

import com.qa.common.driver.CapabilityReport;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.HttpEngine;
import com.qa.common.transport.ExecutionHandle;
import com.qa.common.transport.ExecutionTransport;
import com.qa.mobileagent.api.dto.AgentEvent;
import com.qa.mobileagent.api.dto.SubmitRequest;
import com.qa.mobileagent.api.dto.SubmitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Núcleo del agente: traduce {@link SubmitRequest} a una invocación de
 * {@link ExecutionTransport}, registra el {@link SseStepReporter}, y mantiene
 * el handle indexado por {@code executionId} para que los controllers de
 * {@code /events/&#123;id&#125;} y {@code /cancel/&#123;id&#125;} puedan
 * resolverlo.
 *
 * <p>El transport es inyectado para permitir tests (mock) y para soportar en
 * el futuro variantes (pool, locking por device, etc.) sin tocar este service.
 *
 * @since TASK-I04
 */
@Service
public class AgentExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentExecutionService.class);

    private final ExecutionTransport transport;
    private final FeatureMaterializer materializer;
    private final int sseBufferCapacity;

    private final Map<String, RunSession> sessions = new ConcurrentHashMap<>();

    public AgentExecutionService(ExecutionTransport transport,
                                 FeatureMaterializer materializer,
                                 @Value("${agent.sse.buffer-capacity:1024}") int sseBufferCapacity) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.sseBufferCapacity = sseBufferCapacity;
    }

    /**
     * Lanza una ejecución y devuelve {@code executionId} + URL de eventos.
     */
    public SubmitResponse submit(SubmitRequest req) throws IOException {
        String executionId = UUID.randomUUID().toString();
        Path execDir = materializer.execDir(executionId);
        List<String> featurePaths = materializer.materialize(executionId, req.featureContents());

        ExecutionConfig config = toConfig(req);
        SseStepReporter reporter = new SseStepReporter(executionId, sseBufferCapacity);
        ExecutionHandle handle = transport.submit(config, featurePaths, reporter);

        RunSession session = new RunSession(handle, reporter, execDir);
        // executionId del handle es el verdadero — usamos siempre el del transport.
        sessions.put(handle.executionId(), session);

        handle.future().whenComplete((res, err) -> {
            if (err != null) {
                reporter.queue().offer(AgentEvent.executionError(handle.executionId(), err));
            }
            // Cleanup workspace tras un breve grace para que el cliente termine de drenar.
            // El TTL real lo maneja el EventsController al cerrar el SseEmitter.
            materializer.cleanup(execDir);
        });

        LOG.info("Run iniciado: executionId={} features={}", handle.executionId(), featurePaths.size());
        return new SubmitResponse(handle.executionId(),
                "/v1/runs/" + handle.executionId() + "/events");
    }

    public RunSession session(String executionId) {
        return sessions.get(executionId);
    }

    public boolean cancel(String executionId) {
        RunSession s = sessions.get(executionId);
        if (s == null) return false;
        s.handle().cancel();
        return true;
    }

    public void release(String executionId) {
        sessions.remove(executionId);
    }

    public List<CapabilityReport> capabilities() {
        return transport.describeCapabilities();
    }

    @PreDestroy
    void shutdown() {
        sessions.values().forEach(s -> {
            try { s.handle().cancel(); } catch (RuntimeException ignored) { /* best-effort */ }
        });
        sessions.clear();
    }

    // -------------------------------------------------------------------------

    private static ExecutionConfig toConfig(SubmitRequest req) {
        ExecutionConfig.Builder b = new ExecutionConfig.Builder()
                .environment(req.environment())
                .browser(req.browser() == null ? "" : req.browser())
                .tags(req.tags() == null ? "" : req.tags())
                .parallelEnabled(req.parallelEnabled())
                .threadCount(Math.max(1, req.threadCount()))
                .trustAllSsl(req.trustAllSsl());

        if (req.properties() != null) {
            req.properties().forEach(b::property);
        }
        if (req.httpEngine() != null && !req.httpEngine().isBlank()) {
            try {
                b.httpEngine(HttpEngine.valueOf(req.httpEngine().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                LOG.warn("httpEngine desconocido '{}', usando default", req.httpEngine());
            }
        }
        return b.build();
    }

    /** Estado por ejecución activa. */
    public record RunSession(ExecutionHandle handle, SseStepReporter reporter, Path workspace) { }
}
