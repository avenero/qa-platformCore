package com.qa.httpcore.implementations;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.qa.httpcore.model.HttpMethod;
import com.qa.httpcore.model.HttpResponse;
import com.qa.common.api.exception.FrameworkTechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PlaywrightHttpEngine — implementación Playwright de HttpClient")
class PlaywrightHttpEngineTest {

    private APIRequestContext apiCtx;
    private PlaywrightHttpEngine engine;
    private AtomicInteger supplierCalls;

    @BeforeEach
    void setUp() {
        apiCtx = mock(APIRequestContext.class);
        supplierCalls = new AtomicInteger(0);
        Supplier<APIRequestContext> supplier = () -> {
            supplierCalls.incrementAndGet();
            return apiCtx;
        };
        engine = new PlaywrightHttpEngine(supplier);
    }

    private APIResponse stubResponse(int status, String body, Map<String, String> headers) {
        APIResponse pw = mock(APIResponse.class);
        when(pw.status()).thenReturn(status);
        when(pw.text()).thenReturn(body);
        when(pw.headers()).thenReturn(headers != null ? headers : new HashMap<>());
        return pw;
    }

    @Test
    @DisplayName("constructor rechaza supplier null")
    void constructorRechazaSupplierNull() {
        assertThatThrownBy(() -> new PlaywrightHttpEngine(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apiContextSupplier");
    }

    @Test
    @DisplayName("GET delega en APIRequestContext.get y retorna HttpResponse mapeado")
    void getDelegaYMapeaRespuesta() throws Exception {
        APIResponse pw = stubResponse(200, "{\"ok\":true}", Map.of("Content-Type", "application/json"));
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com");
        engine.addHeader("X-Trace", "abc");

        HttpResponse resp = engine.get("/users");

        assertThat(resp.getStatusCode()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ok\":true}");
        assertThat(resp.getHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(engine.getLastRequestUrl()).isEqualTo("https://api.example.com/users");
        assertThat(engine.getLastRequestMethod()).isEqualTo("GET");
        assertThat(engine.getLastResponse()).isNotNull();
        verify(apiCtx, times(1)).get(eq("https://api.example.com/users"), any(RequestOptions.class));
        verify(pw, times(1)).dispose();
    }

    @Test
    @DisplayName("Soporta los 5 métodos principales (GET/POST/PUT/DELETE/PATCH)")
    void soportaMetodosPrincipales() throws Exception {
        APIResponse pw = stubResponse(204, "", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);
        when(apiCtx.post(anyString(), any(RequestOptions.class))).thenReturn(pw);
        when(apiCtx.put(anyString(), any(RequestOptions.class))).thenReturn(pw);
        when(apiCtx.delete(anyString(), any(RequestOptions.class))).thenReturn(pw);
        when(apiCtx.patch(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com");
        engine.setBody("{\"k\":1}");

        engine.get("/r");
        engine.post("/r");
        engine.put("/r");
        engine.delete("/r");
        engine.patch("/r");

        verify(apiCtx).get(eq("https://api.example.com/r"), any(RequestOptions.class));
        verify(apiCtx).post(eq("https://api.example.com/r"), any(RequestOptions.class));
        verify(apiCtx).put(eq("https://api.example.com/r"), any(RequestOptions.class));
        verify(apiCtx).delete(eq("https://api.example.com/r"), any(RequestOptions.class));
        verify(apiCtx).patch(eq("https://api.example.com/r"), any(RequestOptions.class));
    }

    @Test
    @DisplayName("query params se anexan a la URL final")
    void queryParamsSeAnexan() throws Exception {
        APIResponse pw = stubResponse(200, "ok", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);
        engine.setHost("https://api.example.com");
        engine.addQueryParam("page", "2");
        engine.addQueryParam("size", "10");

        engine.get("/items");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(apiCtx).get(urlCaptor.capture(), any(RequestOptions.class));
        String url = urlCaptor.getValue();
        assertThat(url).startsWith("https://api.example.com/items?");
        assertThat(url).contains("page=2");
        assertThat(url).contains("size=10");
    }

    @Test
    @DisplayName("ejecutar sin host configurado lanza IllegalStateException")
    void sinHostFalla() {
        assertThatThrownBy(() -> engine.get("/x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Host no configurado");
        // Nunca se invocó el supplier ni el contexto.
        assertThat(supplierCalls.get()).isEqualTo(0);
        verify(apiCtx, never()).get(anyString(), any(RequestOptions.class));
    }

    @Test
    @DisplayName("endpoint vacío con host válido ejecuta petición host-only (paridad Apache, ADR-FECPW-01)")
    void endpointVacioEjecutaHostOnly() throws Exception {
        // Patrón canónico de los steps: la URL completa vive en host y el step
        // ejecuta con endpoint "" (HttpExecutionSteps.ejecutarPeticionHttp → get("")).
        APIResponse pw = stubResponse(200, "ok", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com/v1/login");
        HttpResponse resp = engine.executeRequest(HttpMethod.GET, "");

        assertThat(resp.getStatusCode()).isEqualTo(200);
        verify(apiCtx).get(eq("https://api.example.com/v1/login"), any(RequestOptions.class));
    }

    @Test
    @DisplayName("endpoint null con host válido ejecuta petición host-only (paridad Apache, ADR-FECPW-01)")
    void endpointNullEjecutaHostOnly() throws Exception {
        APIResponse pw = stubResponse(200, "ok", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com/v1/login");
        HttpResponse resp = engine.executeRequest(HttpMethod.GET, null);

        assertThat(resp.getStatusCode()).isEqualTo(200);
        verify(apiCtx).get(eq("https://api.example.com/v1/login"), any(RequestOptions.class));
    }

    @Test
    @DisplayName("endpoint sin slash inicial se normaliza con '/' (paridad Apache)")
    void endpointSinSlashSeNormaliza() throws Exception {
        APIResponse pw = stubResponse(200, "ok", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com");
        engine.executeRequest(HttpMethod.GET, "users");

        verify(apiCtx).get(eq("https://api.example.com/users"), any(RequestOptions.class));
    }

    @Test
    @DisplayName("retry policy reintenta hasta agotar intentos y luego envuelve la excepción")
    void retryPolicyAgotaIntentos() {
        when(apiCtx.get(anyString(), any(RequestOptions.class)))
                .thenThrow(new RuntimeException("network"));

        engine.setHost("https://api.example.com");
        engine.setRetryPolicy(2, 0); // 1 intento original + 2 reintentos = 3

        assertThatThrownBy(() -> engine.get("/x"))
                .isInstanceOf(FrameworkTechnicalException.class)
                .hasMessageContaining("falló después de 3 intento(s)");

        verify(apiCtx, times(3)).get(anyString(), any(RequestOptions.class));
    }

    @Test
    @DisplayName("supplier null → auto-provisiona contexto standalone y ejecuta la petición (FEC-API-SHIP-CORE)")
    void supplierNullAutoProvisionaStandalone() throws Exception {
        APIRequestContext standalone = mock(APIRequestContext.class);
        APIResponse pw = stubResponse(200, "{\"ok\":true}", Map.of("Content-Type", "application/json"));
        when(standalone.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        // Supplier retorna null (p.ej. @api puro sin browser). createStandaloneContext()
        // se stubea para no lanzar el binario nativo de Playwright en el test unitario.
        PlaywrightHttpEngine eng = new PlaywrightHttpEngine(() -> null) {
            @Override
            APIRequestContext createStandaloneContext() {
                return standalone;
            }
        };
        eng.setHost("https://api.example.com");

        HttpResponse resp = eng.get("/x");

        assertThat(resp.getStatusCode()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo("{\"ok\":true}");
        verify(standalone, times(1)).get(eq("https://api.example.com/x"), any(RequestOptions.class));
    }

    @Test
    @DisplayName("constructor sin args (standalone) reusa el contexto entre peticiones; close() lo dispone (idempotente)")
    void noArgConstructorReusaContextoYClose() throws Exception {
        APIRequestContext standalone = mock(APIRequestContext.class);
        APIResponse pw = stubResponse(200, "ok", Map.of());
        when(standalone.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        AtomicInteger created = new AtomicInteger(0);
        PlaywrightHttpEngine eng = new PlaywrightHttpEngine() {
            @Override
            APIRequestContext createStandaloneContext() {
                created.incrementAndGet();
                return standalone;
            }
        };
        eng.setHost("https://api.example.com");
        eng.get("/a");
        eng.get("/b");

        // El contexto standalone se crea una sola vez y se reusa entre peticiones.
        assertThat(created.get()).isEqualTo(1);

        eng.close();
        verify(standalone, times(1)).dispose();
        // close() es idempotente: una segunda llamada no vuelve a disponer.
        eng.close();
        verify(standalone, times(1)).dispose();
    }

    @Test
    @DisplayName("reset() limpia host, headers, cookies, body, lastResponse")
    void resetLimpiaTodo() throws Exception {
        APIResponse pw = stubResponse(200, "ok", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com");
        engine.addHeader("h", "v");
        engine.addCookie("c", "v");
        engine.setBody("{}");
        engine.get("/u");

        engine.reset();

        assertThat(engine.getHost()).isNull();
        assertThat(engine.getHeaders()).isEmpty();
        assertThat(engine.getCookies()).isEmpty();
        assertThat(engine.getBody()).isNull();
        assertThat(engine.getLastResponse()).isNull();
        assertThat(engine.getLastRequestUrl()).isNull();
        assertThat(engine.getLastRequestDuration()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("clearRequestData() preserva host y cookies pero limpia headers/body/queryParams")
    void clearRequestDataPreservaHostYCookies() {
        engine.setHost("https://api.example.com");
        engine.addHeader("h", "v");
        engine.addCookie("session", "abc");
        engine.setBody("{}");
        engine.addQueryParam("p", "1");

        engine.clearRequestData();

        assertThat(engine.getHost()).isEqualTo("https://api.example.com");
        assertThat(engine.getCookies()).containsEntry("session", "abc");
        assertThat(engine.getHeaders()).isEmpty();
        assertThat(engine.getBody()).isNull();
    }

    @Test
    @DisplayName("cookies configuradas se serializan al header Cookie")
    void cookiesSeSerializanAlHeader() throws Exception {
        APIResponse pw = stubResponse(200, "", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com");
        engine.addCookie("a", "1");
        engine.addCookie("b", "2");
        engine.get("/x");

        // El snapshot expuesto debe contener la cookie serializada.
        Map<String, String> snapshot = engine.getLastRequestHeadersSnapshot();
        assertThat(snapshot).containsKey("Cookie");
        assertThat(snapshot.get("Cookie")).contains("a=1").contains("b=2");
    }

    @Test
    @DisplayName("autoApplyUserContext inyecta X-User-Id y X-Session-Id")
    void autoApplyUserContextInyectaHeaders() throws Exception {
        APIResponse pw = stubResponse(200, "", Map.of());
        when(apiCtx.get(anyString(), any(RequestOptions.class))).thenReturn(pw);

        engine.setHost("https://api.example.com");
        engine.setUserContext("u-1", "s-1");
        engine.setAutoApplyUserContext(true);
        engine.get("/x");

        Map<String, String> snapshot = engine.getLastRequestHeadersSnapshot();
        assertThat(snapshot).containsEntry("X-User-Id", "u-1");
        assertThat(snapshot).containsEntry("X-Session-Id", "s-1");
    }

    @Test
    @DisplayName("detectContentType identifica JSON, XML, form, octet-stream")
    void detectContentTypeFunciona() {
        assertThat(engine.detectContentType("{\"a\":1}")).isEqualTo("application/json");
        assertThat(engine.detectContentType("[1,2,3]")).isEqualTo("application/json");
        assertThat(engine.detectContentType("<root/>")).isEqualTo("application/xml");
        assertThat(engine.detectContentType("a=1&b=2")).isEqualTo("application/x-www-form-urlencoded");
        assertThat(engine.detectContentType("xyz")).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("configureSsl es no-op (Playwright maneja TLS nativo)")
    void configureSslEsNoOp() {
        // No debe lanzar excepción ni modificar comportamiento observable.
        engine.configureSsl(null, true);
        assertThat(engine.getDebugInfo()).contains("PlaywrightHttpEngine{");
    }

    @Test
    @DisplayName("getDebugInfo no expone valores de cookies ni bodies")
    void debugInfoNoExponeSecretos() {
        engine.setHost("https://api.example.com");
        engine.addCookie("session", "supersecret");
        engine.setBody("{\"password\":\"x\"}");

        String info = engine.getDebugInfo();

        assertThat(info).doesNotContain("supersecret");
        assertThat(info).doesNotContain("password");
        // Sí expone el conteo y tamaños.
        assertThat(info).contains("cookies=1");
        assertThat(info).contains("bytes");
    }

}
