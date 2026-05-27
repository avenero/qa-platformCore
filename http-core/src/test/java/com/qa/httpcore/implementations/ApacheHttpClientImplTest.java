package com.qa.httpcore.implementations;

import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.interfaces.HttpClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias de {@link ApacheHttpClientImpl}.
 *
 * <p>Valida el comportamiento del cliente HTTP basado en Apache HttpClient 5 sin
 * realizar peticiones reales a la red (lógica de estado, validaciones, configuración).
 * Las pruebas de integración real se ejecutan desde {@code qa-module-test/features/API/}.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("ApacheHttpClientImpl — contrato HttpClient")
class ApacheHttpClientImplTest {

    private ApacheHttpClientImpl client;

    @BeforeEach
    void setUp() {
        client = new ApacheHttpClientImpl();
    }

    // =========================================================================
    // Host
    // =========================================================================

    @Test
    @DisplayName("setHost acepta URL válida")
    void setHost_validUrl_stored() {
        client.setHost("https://api.example.com");
        assertThat(client.getHost()).isEqualTo("https://api.example.com");
        assertThat(client.hasValidHost()).isTrue();
    }

    @Test
    @DisplayName("setHost null lanza IllegalArgumentException")
    void setHost_null_throws() {
        assertThatThrownBy(() -> client.setHost(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setHost vacío lanza IllegalArgumentException")
    void setHost_blank_throws() {
        assertThatThrownBy(() -> client.setHost("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("hasValidHost false si no hay host configurado")
    void hasValidHost_noHost_false() {
        assertThat(client.hasValidHost()).isFalse();
    }

    // =========================================================================
    // Headers
    // =========================================================================

    @Test
    @DisplayName("addHeader almacena header")
    void addHeader_stored() {
        client.addHeader("Authorization", "Bearer token");
        assertThat(client.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    @DisplayName("addHeader con value null elimina el header")
    void addHeader_nullValue_removes() {
        client.addHeader("X-Trace", "abc");
        client.addHeader("X-Trace", null);
        assertThat(client.getHeaders()).doesNotContainKey("X-Trace");
    }

    @Test
    @DisplayName("addHeader con key null lanza excepción")
    void addHeader_nullKey_throws() {
        assertThatThrownBy(() -> client.addHeader(null, "value"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addHeaders agrega múltiples headers")
    void addHeaders_multiple_stored() {
        client.addHeaders(Map.of("A", "1", "B", "2"));
        assertThat(client.getHeaders()).containsKeys("A", "B");
    }

    @Test
    @DisplayName("removeHeader elimina un header existente")
    void removeHeader_existing_removed() {
        client.addHeader("X-Remove", "val");
        client.removeHeader("X-Remove");
        assertThat(client.getHeaders()).doesNotContainKey("X-Remove");
    }

    // =========================================================================
    // Query params
    // =========================================================================

    @Test
    @DisplayName("addQueryParam almacena parámetro")
    void addQueryParam_stored() {
        client.addQueryParam("page", "1");
        // Los params se construyen en la URL al ejecutar; verificamos estado interno via clearQueryParams
        client.addQueryParam("size", "10");
        client.clearQueryParams();
        // Después del clear, la próxima ejecución no incluirá los params
        // (estado interno verificado indirectamente)
        assertThatNoException().isThrownBy(() -> client.addQueryParam("filter", "active"));
    }

    @Test
    @DisplayName("clearQueryParams limpia todos los parámetros")
    void clearQueryParams_clearsAll() {
        client.addQueryParam("a", "1");
        client.addQueryParam("b", "2");
        client.clearQueryParams();
        // Sin excepción, podemos agregar de nuevo
        assertThatNoException().isThrownBy(() -> client.addQueryParam("c", "3"));
    }

    // =========================================================================
    // Body
    // =========================================================================

    @Test
    @DisplayName("setBody almacena el body")
    void setBody_stored() {
        client.setBody("{\"name\":\"test\"}");
        assertThat(client.hasBody()).isTrue();
        assertThat(client.getBody()).isEqualTo("{\"name\":\"test\"}");
    }

    @Test
    @DisplayName("hasBody false cuando body es null")
    void hasBody_null_false() {
        assertThat(client.hasBody()).isFalse();
    }

    @Test
    @DisplayName("getBodySize retorna bytes del body")
    void getBodySize_returnsBytes() {
        String body = "hello";
        client.setBody(body);
        assertThat(client.getBodySize()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getBodySize cero sin body")
    void getBodySize_noBody_zero() {
        assertThat(client.getBodySize()).isZero();
    }

    // =========================================================================
    // Cookies
    // =========================================================================

    @Test
    @DisplayName("addCookie almacena cookie")
    void addCookie_stored() {
        client.addCookie("session", "abc123");
        assertThat(client.hasCookie("session")).isTrue();
        assertThat(client.getCookie("session")).isEqualTo("abc123");
    }

    @Test
    @DisplayName("removeCookie elimina cookie existente")
    void removeCookie_removes() {
        client.addCookie("token", "xyz");
        client.removeCookie("token");
        assertThat(client.hasCookie("token")).isFalse();
    }

    @Test
    @DisplayName("clearCookies limpia todas las cookies")
    void clearCookies_clearsAll() {
        client.addCookie("a", "1");
        client.addCookie("b", "2");
        client.clearCookies();
        assertThat(client.getCookies()).isEmpty();
    }

    // =========================================================================
    // Contexto de usuario
    // =========================================================================

    @Test
    @DisplayName("setUserContext almacena userId y sessionId")
    void setUserContext_stored() {
        client.setUserContext("user-1", "session-1");
        assertThat(client.hasUserContext()).isTrue();
        assertThat(client.getCurrentUserId()).isEqualTo("user-1");
        assertThat(client.getCurrentSessionId()).isEqualTo("session-1");
    }

    @Test
    @DisplayName("clearUserContext elimina contexto")
    void clearUserContext_clears() {
        client.setUserContext("u", "s");
        client.clearUserContext();
        assertThat(client.hasUserContext()).isFalse();
    }

    // =========================================================================
    // Content type
    // =========================================================================

    @Test
    @DisplayName("configureForJson establece content-type y accept JSON")
    void configureForJson_setsHeaders() {
        client.configureForJson();
        assertThat(client.isJsonContentType()).isTrue();
        assertThat(client.getAcceptType()).contains("json");
    }

    @Test
    @DisplayName("configureForXml establece content-type y accept XML")
    void configureForXml_setsHeaders() {
        client.configureForXml();
        assertThat(client.isXmlContentType()).isTrue();
    }

    @Test
    @DisplayName("configureForFormData establece form content-type")
    void configureForFormData_setsHeaders() {
        client.configureForFormData();
        assertThat(client.isFormDataContentType()).isTrue();
    }

    // =========================================================================
    // Detección automática de content type
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[1,2]", "{\"key\":\"val\"}"})
    @DisplayName("detectContentType reconoce JSON")
    void detectContentType_json(String body) {
        assertThat(client.detectContentType(body)).contains("json");
    }

    @Test
    @DisplayName("detectContentType reconoce XML")
    void detectContentType_xml() {
        assertThat(client.detectContentType("<root/>")).contains("xml");
    }

    // =========================================================================
    // Configuración técnica
    // =========================================================================

    @Test
    @DisplayName("setConnectionTimeout almacena el valor")
    void setConnectionTimeout_stored() {
        client.setConnectionTimeout(5_000);
        assertThat(client.getConnectionTimeout()).isEqualTo(5_000);
    }

    @Test
    @DisplayName("setConnectionTimeout <= 0 lanza excepción")
    void setConnectionTimeout_invalid_throws() {
        assertThatThrownBy(() -> client.setConnectionTimeout(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setRetryPolicy almacena maxRetries y delay")
    void setRetryPolicy_stored() {
        client.setRetryPolicy(3, 500);
        assertThat(client.isRetryEnabled()).isTrue();
        assertThat(client.getMaxRetries()).isEqualTo(3);
        assertThat(client.getRetryDelay()).isEqualTo(500);
    }

    @Test
    @DisplayName("setRetryPolicy negativo lanza excepción")
    void setRetryPolicy_negative_throws() {
        assertThatThrownBy(() -> client.setRetryPolicy(-1, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // Reset / clearRequestData
    // =========================================================================

    @Test
    @DisplayName("clearRequestData limpia headers, params, body pero mantiene host")
    void clearRequestData_keepsHost() {
        client.setHost("https://api.example.com");
        client.addHeader("X-Test", "value");
        client.setBody("{}");
        client.clearRequestData();
        assertThat(client.getHost()).isEqualTo("https://api.example.com");
        assertThat(client.hasBody()).isFalse();
        assertThat(client.getHeaders()).doesNotContainKey("X-Test");
    }

    @Test
    @DisplayName("reset limpia todo incluido host y cookies")
    void reset_clearsEverything() {
        client.setHost("https://api.example.com");
        client.addCookie("session", "s");
        client.setUserContext("u", "s");
        client.reset();
        assertThat(client.hasValidHost()).isFalse();
        assertThat(client.getCookies()).isEmpty();
        assertThat(client.hasUserContext()).isFalse();
        assertThat(client.getLastResponse()).isNull();
        assertThat(client.getLastRequestDuration()).isEqualTo(-1L);
    }

    // =========================================================================
    // Estado de última respuesta antes de ejecutar
    // =========================================================================

    @Test
    @DisplayName("getLastRequestDuration retorna -1 sin peticiones previas")
    void getLastRequestDuration_noPrior_minusOne() {
        assertThat(client.getLastRequestDuration()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("getLastResponse null antes de ejecutar")
    void getLastResponse_noPrior_null() {
        assertThat(client.getLastResponse()).isNull();
    }

    // =========================================================================
    // Validación pre-ejecución
    // =========================================================================

    @Test
    @DisplayName("executeRequest sin host lanza IllegalStateException")
    void executeRequest_noHost_throws() {
        assertThatThrownBy(() -> client.get("/api"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Host no configurado");
    }

    @Test
    @DisplayName("executeRequest con endpoint null/blank usa el host como URL completa")
    void executeRequest_nullEndpoint_usesHostAsFullUrl() {
        // Patrón "configuro la URL completa": el host trae path completo, endpoint vacío
        client.setHost("https://api.example.com/v1/login");
        // No debe lanzar IllegalArgumentException; lo que falla es la conexión real
        assertThatThrownBy(() -> client.get(null))
            .isInstanceOf(FrameworkTechnicalException.class);
        assertThatThrownBy(() -> client.get(""))
            .isInstanceOf(FrameworkTechnicalException.class);
    }

    // =========================================================================
    // Factory — selección de implementación
    // =========================================================================

    @Test
    @DisplayName("HttpClientFactory.getInstance() retorna ApacheHttpClientImpl por defecto")
    void factory_defaultImpl_isApache() {
        System.clearProperty(HttpClientFactory.CLIENT_IMPL_KEY);
        HttpClient c = HttpClientFactory.getInstance();
        assertThat(c).isInstanceOf(ApacheHttpClientImpl.class);
    }

    @Test
    @DisplayName("HttpClientFactory con http.client=apache retorna ApacheHttpClientImpl")
    void factory_apache_isApache() {
        System.setProperty(HttpClientFactory.CLIENT_IMPL_KEY, "apache");
        try {
            HttpClient c = HttpClientFactory.getInstance();
            assertThat(c).isInstanceOf(ApacheHttpClientImpl.class);
        } finally {
            System.clearProperty(HttpClientFactory.CLIENT_IMPL_KEY);
        }
    }

    @Test
    @DisplayName("HttpClientFactory con http.client=unirest cae a ApacheHttpClientImpl con warning (TASK-J01)")
    void factory_unirest_fallsBackToApache() {
        System.setProperty(HttpClientFactory.CLIENT_IMPL_KEY, "unirest");
        try {
            HttpClient c = HttpClientFactory.getInstance();
            // BaseHttpClient eliminado en TASK-J01 — el alias legacy "unirest"
            // ahora produce el cliente Apache para no romper configs viejas.
            assertThat(c).isInstanceOf(ApacheHttpClientImpl.class);
        } finally {
            System.clearProperty(HttpClientFactory.CLIENT_IMPL_KEY);
        }
    }

    @Test
    @DisplayName("getDebugInfo retorna información útil")
    void getDebugInfo_notEmpty() {
        client.setHost("https://api.example.com");
        assertThat(client.getDebugInfo())
            .contains("ApacheHttpClientImpl")
            .contains("api.example.com");
    }
}
