package com.qa.httpcore.integration;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.qa.common.api.runtime.ApiContextHolder;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test (FEC-API-SHIP-WEB-SHARE, el diferenciador de UC-2): un escenario
 * híbrido {@code @web+@api} donde el browser hace login y obtiene una cookie de sesión,
 * y un step {@code @api} posterior con {@code http.engine=PLAYWRIGHT} <b>reutiliza esa
 * sesión</b> (200 autenticado) gracias a {@link ApiContextHolder}.
 *
 * <p>El "browser" se construye con un {@link BrowserContext} de Playwright directamente
 * (http-core tiene Playwright en classpath). En producción {@code web-core.WebPlugin} es
 * quien publica {@code browserContext.request()} en el holder; aquí se simula ese paso
 * para no acoplar http-core↔web-core (siblings). El mecanismo probado es idéntico:
 * {@code browserContext.request()} comparte el cookie-jar con las páginas del navegador.</p>
 *
 * <p>Gateado por {@code -Dplaywright.it=true} (requiere binario nativo + browser). Sin la
 * property, JUnit lo marca skipped (verde). Endpoint = {@link HttpServer} local → hermético.</p>
 */
@EnabledIfSystemProperty(
        named = "playwright.it",
        matches = "true",
        disabledReason = "Requiere binario nativo de Playwright (browser); habilitar con -Dplaywright.it=true")
@DisplayName("HybridWebApiSessionSharingIT — @api por PLAYWRIGHT reutiliza la sesión del browser (UC-2)")
class HybridWebApiSessionSharingIT {

    private static final String SESSION_COOKIE = "SESSIONID";
    private static final String SESSION_VALUE = "hybrid-shared-abc";

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // /login: setea la cookie de sesión (como un login web).
        server.createContext("/login", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=" + SESSION_VALUE + "; Path=/");
            byte[] body = "<html>logged-in</html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        // /protected: 200 sólo si llega la cookie de sesión; 401 si no.
        server.createContext("/protected", exchange -> {
            List<String> cookies = exchange.getRequestHeaders().getOrDefault("Cookie", List.of());
            boolean authed = cookies.stream().anyMatch(c -> c.contains(SESSION_COOKIE + "=" + SESSION_VALUE));
            byte[] body = (authed ? "{\"authenticated\":true}" : "{\"error\":\"no-session\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(authed ? 200 : 401, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
        ApiContextHolder.clear();
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
    }

    @Test
    @DisplayName("login en browser → step @api ve la cookie de sesión (200 autenticado)")
    void apiReusesBrowserSessionCookie() throws Exception {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch();
            BrowserContext browserContext = browser.newContext();
            try {
                // 1) "Escenario @web": el browser hace login y obtiene la cookie de sesión.
                Page page = browserContext.newPage();
                page.navigate(baseUrl + "/login");

                // 2) Lo que hace WebPlugin: publica browserContext.request() en el holder neutral.
                ApiContextHolder.set(browserContext.request());

                // 3) Step @api con motor PLAYWRIGHT: reutiliza la sesión (cookie) del browser.
                HttpEngineBootstrap.register();
                HttpClient client = HttpClientFactory.create(
                        new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build());
                assertThat(client).isInstanceOf(PlaywrightHttpEngine.class);
                client.setHost(baseUrl);

                HttpResponse resp = client.get("/protected");

                assertThat(resp.getStatusCode()).isEqualTo(200);
                assertThat(resp.getBody()).contains("authenticated");
            } finally {
                browser.close();
            }
        }
    }

    @Test
    @DisplayName("sin sesión de browser (holder vacío) → @api standalone funciona pero NO autenticado (401)")
    void pureApiWithoutBrowserIsNotAuthenticated() throws Exception {
        // Regresión FEC-API-SHIP-CORE: @api puro sigue standalone (el motor funciona),
        // y NO comparte una sesión que no existe (401 = sin cookie, pero respuesta real).
        ApiContextHolder.clear();
        HttpEngineBootstrap.register();
        try (PlaywrightHttpEngine engine = (PlaywrightHttpEngine) HttpClientFactory.create(
                new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build())) {
            engine.setHost(baseUrl);
            HttpResponse resp = engine.get("/protected");
            assertThat(resp.getStatusCode()).isEqualTo(401);
        }
    }
}
