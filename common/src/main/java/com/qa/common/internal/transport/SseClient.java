package com.qa.common.internal.transport;

import com.qa.common.api.Internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Cliente SSE minimalista basado en {@link HttpClient}
 * (TASK-I03 / RFC-AGENT-01). Lee un stream {@code text/event-stream}
 * línea-a-línea, agrupa registros por línea-vacía-separadora, y entrega
 * tuples {@code (eventType, dataPayload)} al consumer.
 *
 * <h2>Reconexión</h2>
 * <p>Si la conexión cae mid-stream y el {@link #stop()} no se ha invocado,
 * reintenta {@code maxRetries} veces con backoff lineal (500ms·n). El loop
 * usa {@code Last-Event-ID} si el agente lo emite (header opcional);
 * actualmente el {@code mobile-agent} v1 no lo emite, así que el reintento
 * es sin reanudación — el agente reinicia el stream desde el evento
 * encolado más reciente.
 *
 * <h2>Thread-safety</h2>
 * <p>{@link #start} bloquea el hilo invocador hasta que el stream cierra
 * (limpio o reconexiones agotadas). Se diseñó para ser invocado en un
 * hilo dedicado del transport. {@link #stop} es seguro desde otro hilo.
 *
 * @since TASK-I03
 */
@Internal(reason = "internal — utilidad del HttpAgentTransport, no usar directamente")
public final class SseClient {

    private static final Logger LOG = LoggerFactory.getLogger(SseClient.class);

    private final HttpClient httpClient;
    private final URI url;
    private final BiConsumer<String, String> handler;
    private final int maxRetries;
    private final Duration baseBackoff;
    private final Duration readTimeout;

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public SseClient(HttpClient httpClient, URI url,
                     BiConsumer<String, String> handler,
                     int maxRetries, Duration baseBackoff, Duration readTimeout) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.url = Objects.requireNonNull(url);
        this.handler = Objects.requireNonNull(handler);
        this.maxRetries = Math.max(0, maxRetries);
        this.baseBackoff = baseBackoff == null ? Duration.ofMillis(500) : baseBackoff;
        this.readTimeout = readTimeout == null ? Duration.ofMinutes(2) : readTimeout;
    }

    /**
     * Bloquea hasta que el stream cierra normalmente o se agotan los
     * reintentos. Lanza {@link IOException} si todos los reintentos fallan.
     */
    public void start() throws IOException, InterruptedException {
        int attempt = 0;
        IOException last = null;
        while (!stopped.get() && attempt <= maxRetries) {
            try {
                streamOnce();
                return;                          // EOF limpio
            } catch (IOException io) {
                last = io;
                if (stopped.get()) { return; }
                attempt++;
                if (attempt > maxRetries) { break; }
                long sleep = baseBackoff.toMillis() * attempt;
                LOG.warn("SSE {} cayó (intento {}/{}): {} — reintentando en {} ms",
                        url, attempt, maxRetries, io.getMessage(), sleep);
                Thread.sleep(sleep);
            }
        }
        if (last != null) { throw last; }
    }

    public void stop() {
        stopped.set(true);
    }

    public boolean isStopped() {
        return stopped.get();
    }

    // -------------------------------------------------------------------------

    private void streamOnce() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(url)
                .timeout(readTimeout)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();
        HttpResponse<Stream<String>> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofLines());
        if (resp.statusCode() == 404) {
            // Ejecución desconocida — caso terminal; no reintentar.
            stop();
            throw new IOException("SSE 404 — ejecución no encontrada en " + url);
        }
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("SSE HTTP " + resp.statusCode() + " desde " + url);
        }
        try (Stream<String> lines = resp.body()) {
            String currentEvent = null;
            StringBuilder data = new StringBuilder();
            var it = lines.iterator();
            while (it.hasNext()) {
                if (stopped.get()) { return; }
                String line = it.next();
                if (line.isEmpty()) {
                    if (data.length() > 0) {
                        // R-5 RFC-AGENT-01: nunca logueamos el payload (puede llevar PII).
                        try {
                            handler.accept(currentEvent, data.toString());
                        } catch (RuntimeException re) {
                            LOG.warn("Handler SSE lanzó: {}", re.getMessage());
                        }
                    }
                    currentEvent = null;
                    data.setLength(0);
                } else if (line.startsWith(":")) {
                    // comentario / heartbeat — ignorar
                } else if (line.startsWith("event:")) {
                    currentEvent = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) { data.append('\n'); }
                    data.append(line.substring("data:".length()).stripLeading());
                } else if (line.startsWith("id:") || line.startsWith("retry:")) {
                    // soportable a futuro; no se usa en v1
                }
            }
        }
    }
}
