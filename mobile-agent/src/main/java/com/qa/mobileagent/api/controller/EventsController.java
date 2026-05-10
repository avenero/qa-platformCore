package com.qa.mobileagent.api.controller;

import com.qa.mobileagent.api.dto.AgentEvent;
import com.qa.mobileagent.service.AgentExecutionService;
import com.qa.mobileagent.service.AgentExecutionService.RunSession;
import com.qa.mobileagent.service.SseStepReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Wire-protocol v1 — {@code GET /v1/runs/&#123;id&#125;/events}: stream SSE
 * con el progreso de la ejecución.
 *
 * @since TASK-I04
 */
@RestController
@RequestMapping("/v1/runs")
public class EventsController {

    private static final Logger LOG = LoggerFactory.getLogger(EventsController.class);
    private static final long EMITTER_TIMEOUT_MS = TimeUnit.HOURS.toMillis(2);

    private final AgentExecutionService service;

    public EventsController(AgentExecutionService service) {
        this.service = service;
    }

    @GetMapping(path = "/{executionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> events(@PathVariable String executionId) {
        RunSession session = service.session(executionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        SseStepReporter reporter = session.reporter();

        // Hilo dedicado por suscriptor — drena la cola hasta que la ejecución
        // complete y el flag terminal se observa. Podría reusarse un pool.
        Thread t = Executors.defaultThreadFactory().newThread(() -> drain(executionId, emitter, reporter, session));
        t.setName("sse-drain-" + executionId);
        t.setDaemon(true);
        t.start();

        emitter.onCompletion(() -> service.release(executionId));
        emitter.onTimeout(() -> {
            LOG.warn("SSE emitter timeout para {}", executionId);
            emitter.complete();
        });
        emitter.onError(err -> {
            LOG.warn("SSE emitter error para {}: {}", executionId, err.getMessage());
            emitter.complete();
        });

        return ResponseEntity.ok(emitter);
    }

    private void drain(String executionId, SseEmitter emitter, SseStepReporter reporter, RunSession session) {
        try {
            while (true) {
                AgentEvent ev = reporter.queue().poll(500, TimeUnit.MILLISECONDS);
                if (ev != null) {
                    emitter.send(SseEmitter.event().name(ev.type()).data(ev));
                    if ("EXECUTION_COMPLETED".equals(ev.type()) || "EXECUTION_ERROR".equals(ev.type())) {
                        // Drain remanente y cierra.
                        AgentEvent tail;
                        while ((tail = reporter.queue().poll()) != null) {
                            emitter.send(SseEmitter.event().name(tail.type()).data(tail));
                        }
                        emitter.complete();
                        return;
                    }
                } else if (session.handle().isDone() && reporter.queue().isEmpty()) {
                    // Future done sin EXECUTION_COMPLETED (raro): cerramos limpio.
                    emitter.complete();
                    return;
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            emitter.complete();
        } catch (IOException io) {
            LOG.debug("Cliente SSE desconectado para {}: {}", executionId, io.getMessage());
            emitter.complete();
        } catch (RuntimeException ex) {
            LOG.warn("Error drenando SSE {}: {}", executionId, ex.getMessage());
            emitter.completeWithError(ex);
        }
    }
}
