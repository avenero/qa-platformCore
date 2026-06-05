package com.qa.httpcore.bootstrap;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.qa.common.api.runtime.ApiContextHolder;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.HttpEngine;
import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.implementations.PlaywrightHttpEngine;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.model.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HttpEngineBootstrap — supplier holder-aware (FEC-API-SHIP-WEB-SHARE)")
class HttpEngineBootstrapTest {

    @AfterEach
    void cleanup() {
        ApiContextHolder.clear();
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
    }

    private static HttpClient createPlaywright() {
        return HttpClientFactory.create(
                new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build());
    }

    private static APIRequestContext stubContext(int status, String body) {
        APIRequestContext ctx = mock(APIRequestContext.class);
        APIResponse resp = mock(APIResponse.class);
        when(resp.status()).thenReturn(status);
        when(resp.text()).thenReturn(body);
        when(resp.headers()).thenReturn(new HashMap<>());
        when(ctx.get(anyString(), any(RequestOptions.class))).thenReturn(resp);
        return ctx;
    }

    @Test
    @DisplayName("con sesión de browser en el holder → el engine usa ESE contexto (sesión compartida)")
    void holderPresentEngineUsesSharedContext() throws Exception {
        APIRequestContext browserCtx = stubContext(200, "{\"ok\":true}");
        ApiContextHolder.set(browserCtx);
        HttpEngineBootstrap.register();

        HttpClient client = createPlaywright();
        assertThat(client).isInstanceOf(PlaywrightHttpEngine.class);
        client.setHost("https://api.example.com");
        HttpResponse resp = client.get("/me");

        assertThat(resp.getStatusCode()).isEqualTo(200);
        verify(browserCtx, times(1)).get(eq("https://api.example.com/me"), any(RequestOptions.class));
    }

    @Test
    @DisplayName("con holder vacío → engine PLAYWRIGHT (no fallback Apache); operaría standalone")
    void holderEmptyEngineIsPlaywrightStandalone() {
        ApiContextHolder.clear();
        HttpEngineBootstrap.register();

        HttpClient client = createPlaywright();
        // Es el motor Playwright (no fallback Apache). Sin holder operaría standalone
        // (self-provision); no se ejecuta request para no lanzar el binario nativo aquí.
        assertThat(client).isInstanceOf(PlaywrightHttpEngine.class);
    }

    @Test
    @DisplayName("el supplier resuelve el holder POR PETICIÓN: cambiar el holder cambia el contexto usado")
    void supplierResolvesHolderPerRequest() throws Exception {
        APIRequestContext ctxA = stubContext(201, "A");
        APIRequestContext ctxB = stubContext(202, "B");

        HttpEngineBootstrap.register();
        HttpClient client = createPlaywright();
        client.setHost("https://api.example.com");

        ApiContextHolder.set(ctxA);
        assertThat(client.get("/x").getStatusCode()).isEqualTo(201);

        ApiContextHolder.set(ctxB);
        assertThat(client.get("/x").getStatusCode()).isEqualTo(202);

        // La MISMA instancia de engine usó cada contexto según el holder vigente al ejecutar.
        verify(ctxA, times(1)).get(anyString(), any(RequestOptions.class));
        verify(ctxB, times(1)).get(anyString(), any(RequestOptions.class));
    }
}
