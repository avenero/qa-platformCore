package com.qa.httpcore.integration;

import com.qa.httpcore.implementations.ApacheHttpClientImpl;
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
 * Integration test (FEC-API-PW-ENDPOINT / ADR-FECPW-01): repro del GAP-2 de G13.
 *
 * <p>Patrón canónico multi-step de los steps de ejecución: la URL completa vive en el
 * campo {@code host} (ApiHelper.setHost / configureEndpoint) y el step ejecuta con
 * endpoint vacío ({@code HttpExecutionSteps.ejecutarPeticionHttp → get("")}). Antes del
 * fix, PLAYWRIGHT rechazaba ese endpoint vacío ({@code IllegalArgumentException}) que
 * APACHE tolera — paridad de motor rota.</p>
 *
 * <h2>Por qué está gateado</h2>
 * <p>Requiere el binario nativo de Playwright (driver Node). Se habilita sólo con
 * {@code -Dplaywright.it=true} (mismo gate que {@link PlaywrightHttpEngineIT}). Sin la
 * property, JUnit lo marca como skipped (verde), no como fallo.</p>
 *
 * <p>El destino es un {@link HttpServer} local efímero (sin red externa) → hermético.</p>
 */
@EnabledIfSystemProperty(
        named = "playwright.it",
        matches = "true",
        disabledReason = "Requiere binario nativo de Playwright; habilitar con -Dplaywright.it=true")
@DisplayName("PlaywrightHttpEngineEndpointIT — endpoint vacío (URL en host) → 2xx + paridad PW/Apache")
class PlaywrightHttpEngineEndpointIT {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startLocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/health", exchange -> {
            byte[] body = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
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
    }

    /** Escenario repro G13: URL completa en host + ejecución con endpoint vacío. */
    private HttpResponse ejecutarEscenarioRepro(HttpClient client) throws Exception {
        client.setHost(baseUrl + "/api/v1/health");
        return client.get("");
    }

    @Test
    @DisplayName("PLAYWRIGHT: GET con endpoint vacío y URL completa en host → 200 (no IllegalArgumentException)")
    void playwrightEndpointVacioRetorna2xx() throws Exception {
        try (PlaywrightHttpEngine engine = new PlaywrightHttpEngine()) {
            HttpResponse resp = ejecutarEscenarioRepro(engine);

            assertThat(resp.getStatusCode()).isEqualTo(200);
            assertThat(resp.getBody()).contains("UP");
        }
    }

    @Test
    @DisplayName("Paridad de motor (guard E.4): el MISMO escenario por PLAYWRIGHT y APACHE → mismo resultado")
    void paridadPlaywrightApacheMismoEscenario() throws Exception {
        HttpResponse apache = ejecutarEscenarioRepro(new ApacheHttpClientImpl());

        try (PlaywrightHttpEngine engine = new PlaywrightHttpEngine()) {
            HttpResponse playwright = ejecutarEscenarioRepro(engine);

            assertThat(playwright.getStatusCode()).isEqualTo(apache.getStatusCode());
            assertThat(playwright.getStatusCode()).isEqualTo(200);
            assertThat(playwright.getBody()).isEqualTo(apache.getBody());
        }
    }
}
