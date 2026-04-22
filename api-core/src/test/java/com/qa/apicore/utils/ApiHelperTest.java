package com.qa.apicore.utils;

import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.enums.HttpMethod;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.http.model.HttpResponse;
import com.qa.common.runtime.ExecutionContext;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para ApiHelper.
 *
 * <p><b>Estrategia:</b> Stub manual del HttpClient para no requerir red real.</p>
 * <p><b>Cobertura:</b> Configuración de headers, validación de respuestas y autenticación.</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("ApiHelper")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiHelperTest {

    // =========================================================================
    // Stub de HttpClient
    // =========================================================================

    /**
     * Implementación stub de HttpClient para tests.
     * Implementa todos los métodos del interface; solo los relevantes tienen lógica.
     */
    static class StubHttpClient implements HttpClient {
        HttpResponse stubbedResponse;
        String capturedHost;
        final Map<String, String> capturedHeaders = new HashMap<>();
        final Map<String, String> capturedQueryParams = new HashMap<>();
        String capturedBody;

        // --- métodos usados por ApiHelper ---
        @Override public void setHost(String host) { this.capturedHost = host; }
        @Override public String getHost() { return capturedHost; }
        @Override public boolean hasValidHost() { return capturedHost != null; }
        @Override public void addHeader(String k, String v) { capturedHeaders.put(k, v); }
        @Override public void addHeaders(Map<String, String> h) { if (h != null) capturedHeaders.putAll(h); }
        @Override public void addQueryParam(String k, String v) { capturedQueryParams.put(k, v); }
        @Override public void addField(String k, String v) {}
        @Override public void setBody(String body) { this.capturedBody = body; }
        @Override public HttpResponse getLastResponse() { return stubbedResponse; }

        // --- métodos no usados por ApiHelper: stub vacío ---
        @Override public void addQueryParams(Map<String, Object> p) {}
        @Override public void addFields(Map<String, String> f) {}
        @Override public boolean hasBody() { return capturedBody != null; }
        @Override public long getBodySize() { return 0; }
        @Override public void setConnectionTimeout(int ms) {}
        @Override public void setReadTimeout(int ms) {}
        @Override public int getConnectionTimeout() { return -1; }
        @Override public int getReadTimeout() { return -1; }
        @Override public void setRetryPolicy(int r, int d) {}
        @Override public boolean isRetryEnabled() { return false; }
        @Override public int getMaxRetries() { return 0; }
        @Override public int getRetryDelay() { return 0; }
        @Override public HttpResponse executeRequest(HttpMethod m, String e) { return stubbedResponse; }
        @Override public HttpResponse executeRequest(HttpMethod m, String e, boolean f) { return stubbedResponse; }
        @Override public HttpResponse executeRequest(HttpMethod m, String e, int t) { return stubbedResponse; }
        @Override public HttpResponse executeRequest(HttpMethod m, String e, boolean f, int t) { return stubbedResponse; }
        @Override public HttpResponse get(String e) { return stubbedResponse; }
        @Override public HttpResponse post(String e) { return stubbedResponse; }
        @Override public HttpResponse put(String e) { return stubbedResponse; }
        @Override public HttpResponse delete(String e) { return stubbedResponse; }
        @Override public HttpResponse patch(String e) { return stubbedResponse; }
        @Override public String getLastRequestUrl() { return null; }
        @Override public String getLastRequestMethod() { return null; }
        @Override public long getLastRequestDuration() { return 0; }
        @Override public void clearRequestData() {}
        @Override public void reset() {}
        @Override public void setCookies(Map<String, String> c) {}
        @Override public Map<String, String> getCookies() { return new HashMap<>(); }
        @Override public void addCookie(String n, String v) {}
        @Override public String getCookie(String n) { return null; }
        @Override public boolean hasCookie(String n) { return false; }
        @Override public void removeCookie(String n) {}
        @Override public void clearCookies() {}
        @Override public void setAutomaticCookieHandling(boolean e) {}
        @Override public boolean isAutomaticCookieHandlingEnabled() { return false; }
        @Override public void setUserContext(String u, String s) {}
        @Override public void setUserContext(String u, String s, Map<String, String> a) {}
        @Override public String getCurrentUserId() { return null; }
        @Override public String getCurrentSessionId() { return null; }
        @Override public Map<String, String> getUserContext() { return new HashMap<>(); }
        @Override public String getContextValue(String k) { return null; }
        @Override public void setContextValue(String k, String v) {}
        @Override public boolean hasUserContext() { return false; }
        @Override public void clearUserContext() {}
        @Override public void setAutoApplyUserContext(boolean e) {}
        @Override public boolean isAutoApplyUserContextEnabled() { return false; }
        @Override public void setContentType(String ct) {}
        @Override public String getContentType() { return null; }
        @Override public void setAcceptType(String at) {}
        @Override public String getAcceptType() { return null; }
        @Override public void configureForJson() {}
        @Override public void configureForXml() {}
        @Override public void configureForFormData() {}
        @Override public void configureForPlainText() {}
        @Override public void configureForMultipart() {}
        @Override public boolean isJsonContentType() { return false; }
        @Override public boolean isXmlContentType() { return false; }
        @Override public boolean isFormDataContentType() { return false; }
        @Override public String detectContentType(String b) { return "application/json"; }
        @Override public void autoDetectAndSetContentType() {}
        @Override public String getDebugInfo() { return "StubHttpClient"; }
        @Override public String getLastRequestBody() { return capturedBody; }
    }

    // =========================================================================
    // Setup
    // =========================================================================


    /** Construye un ExecutionContext mínimo para los tests. */
    private static ExecutionContext buildContext() {
        return ExecutionContext.builder()
                .scenarioId("api-helper-test-" + java.util.UUID.randomUUID())
                .build();
    }

    private StubHttpClient stub;
    private ApiHelper helper;
    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        stub = new StubHttpClient();
        helper = new ApiHelper(stub);
        // Activar ExecutionContext para que ApiHelper.resolve() funcione correctamente
        ctx = buildContext();
        ctx.activate();
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    // =========================================================================
    // setBaseHost
    // =========================================================================

    @Nested
    @DisplayName("setBaseHost()")
    @Order(1)
    class SetBaseHostTests {

        @Test
        @DisplayName("Establece el host en el HttpClient")
        void estableceHostOk() {
            helper.setBaseHost("https://api.test.com");
            assertThat(stub.capturedHost).isEqualTo("https://api.test.com");
        }

        @Test
        @DisplayName("Reemplaza variables en el host")
        void reemplazaVariablesEnHost() {
            ctx.variables().set("host", "https://api.dynamic.com");
            helper.setBaseHost("${host}");
            assertThat(stub.capturedHost).isEqualTo("https://api.dynamic.com");
        }
    }

    // =========================================================================
    // addHeader
    // =========================================================================

    @Nested
    @DisplayName("addHeader()")
    @Order(2)
    class AddHeaderTests {

        @Test
        @DisplayName("Agrega header al HttpClient")
        void agregaHeaderOk() {
            helper.addHeader("Content-Type", "application/json");
            assertThat(stub.capturedHeaders).containsEntry("Content-Type", "application/json");
        }

        @Test
        @DisplayName("Reemplaza variables en el valor del header")
        void reemplazaVariablesEnHeader() {
            ctx.variables().set("token", "abc123");
            helper.addHeader("Authorization", "Bearer ${token}");
            assertThat(stub.capturedHeaders).containsEntry("Authorization", "Bearer abc123");
        }

        @Test
        @DisplayName("Puede agregar múltiples headers")
        void agregaMultiplesHeaders() {
            helper.addHeader("Content-Type", "application/json");
            helper.addHeader("Accept", "application/json");
            helper.addHeader("X-Custom", "value");
            assertThat(stub.capturedHeaders).hasSize(3);
        }
    }

    // =========================================================================
    // addQueryParam
    // =========================================================================

    @Nested
    @DisplayName("addQueryParam()")
    @Order(3)
    class AddQueryParamTests {

        @Test
        @DisplayName("Agrega query param al HttpClient")
        void agregaQueryParamOk() {
            helper.addQueryParam("page", "1");
            assertThat(stub.capturedQueryParams).containsEntry("page", "1");
        }

        @Test
        @DisplayName("Reemplaza variables en el valor del query param")
        void reemplazaVariables() {
            ctx.variables().set("userId", "42");
            helper.addQueryParam("id", "${userId}");
            assertThat(stub.capturedQueryParams).containsEntry("id", "42");
        }
    }

    // =========================================================================
    // setRequestBody / setJsonBodyFromString
    // =========================================================================

    @Nested
    @DisplayName("setRequestBody() / setJsonBodyFromString()")
    @Order(4)
    class SetBodyTests {

        @Test
        @DisplayName("setRequestBody establece el body en el HttpClient")
        void setBodyOk() {
            helper.setRequestBody("{\"key\":\"value\"}");
            assertThat(stub.capturedBody).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("setRequestBody reemplaza variables")
        void setBodyReemplazaVariables() {
            ctx.variables().set("name", "Abel");
            helper.setRequestBody("{\"name\":\"${name}\"}");
            assertThat(stub.capturedBody).isEqualTo("{\"name\":\"Abel\"}");
        }

        @Test
        @DisplayName("setJsonBodyFromString agrega Content-Type automáticamente")
        void setJsonBodyAgregaContentType() {
            helper.setJsonBodyFromString("{\"key\":\"value\"}");
            assertThat(stub.capturedHeaders).containsEntry("Content-Type", "application/json");
            assertThat(stub.capturedBody).isEqualTo("{\"key\":\"value\"}");
        }
    }

    // =========================================================================
    // validateResponseStatusCode
    // =========================================================================

    @Nested
    @DisplayName("validateResponseStatusCode()")
    @Order(5)
    class ValidateResponseStatusCodeTests {

        @Test
        @DisplayName("Pasa cuando el status coincide")
        void statusCoincideOk() {
            stub.stubbedResponse = new HttpResponse(200, "{}", new HashMap<>(), 0L);
            assertThatNoException().isThrownBy(() ->
                helper.validateResponseStatusCode(200));
        }

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando el status no coincide")
        void statusDistintoFalla() {
            stub.stubbedResponse = new HttpResponse(404, "Not Found", new HashMap<>(), 0L);
            assertThatThrownBy(() -> helper.validateResponseStatusCode(200))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("200")
                .hasMessageContaining("404");
        }
    }

    // =========================================================================
    // validateResponseContainsText
    // =========================================================================

    @Nested
    @DisplayName("validateResponseContainsText()")
    @Order(6)
    class ValidateResponseContainsTextTests {

        @Test
        @DisplayName("Pasa cuando el texto está en el body")
        void textoEncontradoOk() {
            stub.stubbedResponse = new HttpResponse(200, "{\"message\":\"success\"}", new HashMap<>(), 0L);
            assertThatNoException().isThrownBy(() ->
                helper.validateResponseContainsText("success"));
        }

        @Test
        @DisplayName("Falla cuando el texto no está en el body")
        void textoNoEncontradoFalla() {
            stub.stubbedResponse = new HttpResponse(200, "{\"message\":\"ok\"}", new HashMap<>(), 0L);
            assertThatThrownBy(() -> helper.validateResponseContainsText("success"))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("success");
        }

        @Test
        @DisplayName("Falla cuando el body es null")
        void bodyNullFalla() {
            stub.stubbedResponse = new HttpResponse(200, null, new HashMap<>(), 0L);
            assertThatThrownBy(() -> helper.validateResponseContainsText("text"))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // addBasicAuthentication
    // =========================================================================

    @Nested
    @DisplayName("addBasicAuthentication()")
    @Order(7)
    class AddBasicAuthTests {

        @Test
        @DisplayName("Agrega header Authorization con credenciales Base64")
        void agregaAuthHeaderOk() {
            helper.addBasicAuthentication("user", "pass");
            assertThat(stub.capturedHeaders).containsKey("Authorization");
            String authValue = stub.capturedHeaders.get("Authorization");
            assertThat(authValue).startsWith("Basic ");
            // Verificar que es Base64 decodificable
            String decoded = new String(java.util.Base64.getDecoder()
                .decode(authValue.substring("Basic ".length())));
            assertThat(decoded).isEqualTo("user:pass");
        }

        @Test
        @DisplayName("Reemplaza variables en usuario y contraseña")
        void reemplazaVariablesEnCredenciales() {
            ctx.variables().set("apiUser", "admin");
            ctx.variables().set("apiPass", "secret");
            helper.addBasicAuthentication("${apiUser}", "${apiPass}");
            String authValue = stub.capturedHeaders.get("Authorization");
            String decoded = new String(java.util.Base64.getDecoder()
                .decode(authValue.substring("Basic ".length())));
            assertThat(decoded).isEqualTo("admin:secret");
        }
    }

    // =========================================================================
    // addBearerToken
    // =========================================================================

    @Nested
    @DisplayName("addBearerToken()")
    @Order(8)
    class AddBearerTokenTests {

        @Test
        @DisplayName("Agrega header Authorization Bearer")
        void agregaBearerOk() {
            helper.addBearerToken("my-jwt-token");
            assertThat(stub.capturedHeaders)
                .containsEntry("Authorization", "Bearer my-jwt-token");
        }

        @Test
        @DisplayName("Reemplaza variables en el token")
        void reemplazaVariablesEnToken() {
            ctx.variables().set("jwt", "token123");
            helper.addBearerToken("${jwt}");
            assertThat(stub.capturedHeaders)
                .containsEntry("Authorization", "Bearer token123");
        }
    }

    // =========================================================================
    // addField
    // =========================================================================

    @Nested
    @DisplayName("addField()")
    @Order(9)
    class AddFieldTests {

        @Test
        @DisplayName("Invoca addField en el HttpClient con variables reemplazadas")
        void addFieldReemplazaVariables() {
            ctx.variables().set("fieldVal", "testValue");
            assertThatNoException().isThrownBy(() ->
                helper.addField("key", "${fieldVal}"));
        }
    }
}

