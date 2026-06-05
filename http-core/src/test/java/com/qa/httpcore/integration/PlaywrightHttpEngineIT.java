package com.qa.httpcore.integration;

import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.HttpEngine;
import com.qa.httpcore.bootstrap.HttpEngineBootstrap;
import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.implementations.PlaywrightHttpEngine;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.model.HttpResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test (FEC-API-SHIP-CORE): verifica que un escenario {@code @api} puro
 * (sin browser) con {@code http.engine=PLAYWRIGHT} ejecuta {@link PlaywrightHttpEngine}
 * de verdad —auto-proveyendo su contexto standalone— y obtiene una respuesta real 200,
 * sin caer al fallback Apache.
 *
 * <h2>Por qué está gateado</h2>
 * <p>Requiere el binario nativo de Playwright (driver Node). Para no romper la suite
 * unitaria en entornos sin ese binario, se habilita sólo con {@code -Dplaywright.it=true}
 * (lo setea el job/imagen con runtime Playwright — DO-02). Sin la property, JUnit lo marca
 * como skipped (verde), no como fallo.</p>
 *
 * <p>El endpoint es un {@link HttpServer} local efímero (sin red externa) → hermético.</p>
 */
@EnabledIfSystemProperty(
        named = "playwright.it",
        matches = "true",
        disabledReason = "Requiere binario nativo de Playwright; habilitar con -Dplaywright.it=true")
@DisplayName("PlaywrightHttpEngineIT — @api puro con PLAYWRIGHT self-provision → 200 (sin fallback)")
class PlaywrightHttpEngineIT {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startLocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", exchange -> {
            byte[] body = "{\"pong\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopLocalServer() {
        if (server != null) {
            server.stop(0);
        }
        // No dejar PLAYWRIGHT registrado para el resto de la suite.
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
    }

    @Test
    @DisplayName("create(PLAYWRIGHT) tras el bootstrap → PlaywrightHttpEngine que resuelve 200 standalone")
    void apiPuroStandaloneRetorna200SinFallback() throws Exception {
        // Wiring real: el bootstrap registra el motor; el factory lo resuelve (sin fallback).
        HttpEngineBootstrap.register();
        ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build();

        HttpClient client = HttpClientFactory.create(cfg);
        assertThat(client).isInstanceOf(PlaywrightHttpEngine.class);

        // try-with-resources: close() libera el contexto standalone auto-proveído.
        try (PlaywrightHttpEngine engine = (PlaywrightHttpEngine) client) {
            engine.setHost(baseUrl);
            HttpResponse resp = engine.get("/ping");

            assertThat(resp.getStatusCode()).isEqualTo(200);
            assertThat(resp.getBody()).contains("pong");
        }
    }
}
