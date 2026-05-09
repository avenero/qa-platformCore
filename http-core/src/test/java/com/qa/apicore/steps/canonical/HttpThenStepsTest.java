package com.qa.apicore.steps.canonical;

import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.model.HttpMethod;
import com.qa.apicore.model.HttpResponse;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.exception.FrameworkBusinessException;
import com.qa.common.runtime.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpThenSteps — canonical English THEN vocabulary")
class HttpThenStepsTest {

    private StubHttpClient stub;
    private HttpThenSteps steps;
    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        stub = new StubHttpClient();
        ctx = ExecutionContext.builder()
            .scenarioId("then-steps-test-" + java.util.UUID.randomUUID())
            .build();
        ctx.registry().registerInstance(ApiHelper.class, new ApiHelper(stub));
        ctx.activate();
        steps = new HttpThenSteps();
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    // =========================================================================
    // Status code
    // =========================================================================

    @Nested
    @DisplayName("theResponseStatusShouldBe")
    class StatusCodeTests {

        @Test
        @DisplayName("passes when status matches")
        void status_matches_noException() throws FrameworkBusinessException {
            stub.stubbedResponse = responseWithStatus(200);
            assertThatCode(() -> steps.theResponseStatusShouldBe(200))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when status differs")
        void status_differs_throws() {
            stub.stubbedResponse = responseWithStatus(404);
            assertThatThrownBy(() -> steps.theResponseStatusShouldBe(200))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // Response time
    // =========================================================================

    @Nested
    @DisplayName("theResponseTimeShouldBeLessThan")
    class ResponseTimeTests {

        @Test
        @DisplayName("passes when actual time is below threshold")
        void time_below_noException() throws FrameworkBusinessException {
            stub.lastDuration = 100L;
            assertThatCode(() -> steps.theResponseTimeShouldBeLessThan(500))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when actual time equals threshold")
        void time_equals_threshold_throws() {
            stub.lastDuration = 500L;
            assertThatThrownBy(() -> steps.theResponseTimeShouldBeLessThan(500))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("500");
        }

        @Test
        @DisplayName("fails when actual time exceeds threshold")
        void time_exceeds_threshold_throws() {
            stub.lastDuration = 1200L;
            assertThatThrownBy(() -> steps.theResponseTimeShouldBeLessThan(1000))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("1200");
        }
    }

    // =========================================================================
    // Boolean field splits
    // =========================================================================

    @Nested
    @DisplayName("boolean field steps — two independent definitions")
    class BooleanFieldTests {

        @Test
        @DisplayName("theResponseFieldShouldBeTrue passes when field is true")
        void fieldTrue_passes() throws FrameworkBusinessException {
            stub.stubbedResponse = responseWithJson("{\"active\":true}");
            assertThatCode(() -> steps.theResponseFieldShouldBeTrue("$.active"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("theResponseFieldShouldBeFalse passes when field is false")
        void fieldFalse_passes() throws FrameworkBusinessException {
            stub.stubbedResponse = responseWithJson("{\"active\":false}");
            assertThatCode(() -> steps.theResponseFieldShouldBeFalse("$.active"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("theResponseFieldShouldBeTrue fails when field is false")
        void fieldTrue_whenFalse_throws() {
            stub.stubbedResponse = responseWithJson("{\"active\":false}");
            assertThatThrownBy(() -> steps.theResponseFieldShouldBeTrue("$.active"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("theResponseFieldShouldBeFalse fails when field is true")
        void fieldFalse_whenTrue_throws() {
            stub.stubbedResponse = responseWithJson("{\"active\":true}");
            assertThatThrownBy(() -> steps.theResponseFieldShouldBeFalse("$.active"))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // Body contains
    // =========================================================================

    @Nested
    @DisplayName("theResponseBodyShouldContain")
    class BodyContainsTests {

        @Test
        @DisplayName("passes when body contains the text")
        void body_contains_noException() throws FrameworkBusinessException {
            stub.stubbedResponse = responseWithJson("{\"message\":\"Hello World\"}");
            assertThatCode(() -> steps.theResponseBodyShouldContain("Hello World"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fails when body does not contain the text")
        void body_notContains_throws() {
            stub.stubbedResponse = responseWithJson("{\"message\":\"Goodbye\"}");
            assertThatThrownBy(() -> steps.theResponseBodyShouldContain("Hello"))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // Header assertions
    // =========================================================================

    @Nested
    @DisplayName("header assertion steps")
    class HeaderTests {

        @Test
        @DisplayName("theResponseHeaderShouldExist passes when header is present")
        void headerExists_passes() throws FrameworkBusinessException {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            stub.stubbedResponse = responseWithHeaders(headers);
            assertThatCode(() -> steps.theResponseHeaderShouldExist("Content-Type"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("theResponseHeaderShouldBe passes when value matches")
        void headerValue_matches_passes() throws FrameworkBusinessException {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            stub.stubbedResponse = responseWithHeaders(headers);
            assertThatCode(() -> steps.theResponseHeaderShouldBe("Content-Type", "application/json"))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static HttpResponse responseWithStatus(int status) {
        return new HttpResponse(status, "", new HashMap<>(), 0L);
    }

    private static HttpResponse responseWithJson(String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return new HttpResponse(200, body, headers, 0L);
    }

    private static HttpResponse responseWithHeaders(Map<String, String> headers) {
        return new HttpResponse(200, "{}", headers, 0L);
    }

    // =========================================================================
    // Stub HttpClient
    // =========================================================================

    static class StubHttpClient implements HttpClient {
        HttpResponse stubbedResponse = responseWithStatus(200);
        long lastDuration = 0L;

        @Override public void setHost(String h) {}
        @Override public String getHost() { return "http://stub"; }
        @Override public boolean hasValidHost() { return true; }
        @Override public void addHeader(String k, String v) {}
        @Override public void addHeaders(Map<String, String> h) {}
        @Override public void addQueryParam(String k, String v) {}
        @Override public void addField(String k, String v) {}
        @Override public void setBody(String b) {}
        @Override public HttpResponse getLastResponse() { return stubbedResponse; }
        @Override public long getLastRequestDuration() { return lastDuration; }
        @Override public void addQueryParams(Map<String, Object> p) {}
        @Override public void addFields(Map<String, String> f) {}
        @Override public boolean hasBody() { return false; }
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
        @Override public String getLastRequestBody() { return null; }
    }
}
