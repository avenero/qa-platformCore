package com.qa.apicore.reporting;

import com.qa.apicore.reporting.model.HttpStepDetail;
import com.qa.apicore.reporting.util.HttpDetailRedactor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link HttpDetailRedactor} via su API pública {@code build()}.
 * Los helpers package-private (extractPath, redactHeaderMap, redactSummary) quedan
 * cubiertos indirectamente por estos tests.
 */
class HttpDetailRedactorTest {

    // -------------------------------------------------------------------------
    // URL → path extraction (via build)
    // -------------------------------------------------------------------------

    @Test
    void build_fullUrl_extractsPathOnly() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://api.example.com/v1/users?page=1",
                200, null, null, null, null, null, null);
        assertThat(d.urlPath()).isEqualTo("/v1/users");
    }

    @Test
    void build_urlWithNoPath_storesSlash() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://api.example.com",
                200, null, null, null, null, null, null);
        assertThat(d.urlPath()).isEqualTo("/");
    }

    @Test
    void build_nullUrl_urlPathIsNull() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", null, 200, null, null, null, null, null, null);
        assertThat(d.urlPath()).isNull();
    }

    // -------------------------------------------------------------------------
    // Header redaction (via build)
    // -------------------------------------------------------------------------

    @Test
    void build_authorizationHeader_isRedacted() {
        Map<String, String> reqHeaders = new LinkedHashMap<>();
        reqHeaders.put("Authorization", "Bearer secret");
        reqHeaders.put("Content-Type", "application/json");

        HttpStepDetail d = HttpDetailRedactor.build(
                "POST", "https://example.com/api", 201,
                null, null, null, null, reqHeaders, null);

        assertThat(d.requestHeaders()).containsEntry("Authorization", "REDACTED");
        assertThat(d.requestHeaders()).containsEntry("Content-Type", "application/json");
    }

    @Test
    void build_cookieHeader_isRedacted() {
        Map<String, String> reqHeaders = Map.of("Cookie", "session=abc123");

        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://example.com/session", 200,
                null, null, null, null, reqHeaders, null);

        assertThat(d.requestHeaders()).containsEntry("Cookie", "REDACTED");
    }

    @Test
    void build_nullHeaders_storedAsNull() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://example.com/ping", 200,
                null, null, null, null, null, null);

        assertThat(d.requestHeaders()).isNull();
        assertThat(d.responseHeaders()).isNull();
    }

    // -------------------------------------------------------------------------
    // Body handling (via build)
    // -------------------------------------------------------------------------

    @Test
    void build_normalBody_isSanitizedAndStored() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "POST", "https://example.com/items", 201,
                "{\"name\":\"test\"}", "{\"id\":1}",
                null, null, null, null);

        assertThat(d.requestSummary()).isNotNull();
        assertThat(d.responseSummary()).isNotNull();
    }

    @Test
    void build_nullBody_sizeIsNull() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://example.com/items", 200,
                null, null, null, null, null, null);

        assertThat(d.requestBodySizeBytes()).isNull();
        assertThat(d.responseBodySizeBytes()).isNull();
    }

    @Test
    void build_emptyRequestBody_sizeIsNull() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://example.com/", 200, "", null, null, null, null, null);
        assertThat(d.requestBodySizeBytes()).isNull();
    }

    @Test
    void build_longBody_isTruncated() {
        String body = "x".repeat(HttpDetailRedactor.SUMMARY_MAX_CHARS + 100);
        HttpStepDetail d = HttpDetailRedactor.build(
                "POST", "https://example.com/big", 200,
                body, null, null, null, null, null);

        assertThat(d.requestSummary()).hasSizeLessThanOrEqualTo(HttpDetailRedactor.SUMMARY_MAX_CHARS + 20);
    }

    // -------------------------------------------------------------------------
    // Method normalization
    // -------------------------------------------------------------------------

    @Test
    void build_lowercaseMethod_isUppercased() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "post", "https://example.com/api", 200, null, null, null, null, null, null);
        assertThat(d.method()).isEqualTo("POST");
    }

    @Test
    void build_nullMethod_isHandled() {
        HttpStepDetail d = HttpDetailRedactor.build(
                null, "https://example.com/path", 404, null, null, null, null, null, null);
        assertThat(d.method()).isNull();
    }

    // -------------------------------------------------------------------------
    // Full happy path
    // -------------------------------------------------------------------------

    @Test
    void build_allFields_snapshotIsComplete() {
        Map<String, String> reqH = Map.of("Authorization", "Bearer token", "Content-Type", "application/json");
        Map<String, String> resH = Map.of("X-Response-Time", "15ms");

        HttpStepDetail d = HttpDetailRedactor.build(
                "POST", "https://api.example.com/auth/login", 200,
                "{\"user\":\"admin\",\"password\":\"secret\"}",
                "{\"token\":\"jwt-value\"}",
                "req-xyz", 150L, reqH, resH);

        assertThat(d.method()).isEqualTo("POST");
        assertThat(d.urlPath()).isEqualTo("/auth/login");
        assertThat(d.httpStatus()).isEqualTo(200);
        assertThat(d.requestId()).isEqualTo("req-xyz");
        assertThat(d.durationMs()).isEqualTo(150L);
        assertThat(d.requestHeaders()).containsEntry("Authorization", "REDACTED");
        assertThat(d.requestBodySizeBytes()).isGreaterThan(0);
        assertThat(d.responseBodySizeBytes()).isGreaterThan(0);
    }

    @Test
    void build_implementsHttpStepSummaryContract() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "GET", "https://api.example.com/items", 200,
                null, "{\"items\":[]}", null, 20L, null, null);

        assertThat(d.method()).isEqualTo("GET");
        assertThat(d.urlPath()).isEqualTo("/items");
        assertThat(d.responseSummary()).isNotNull();
        assertThat(d.durationMs()).isEqualTo(20L);
    }

    // -------------------------------------------------------------------------
    // Deprecated 5-arg overload
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    void build_deprecatedOverload_createsSnapshot() {
        HttpStepDetail d = HttpDetailRedactor.build(
                "POST", "https://example.com/submit", 201,
                "{\"data\":\"value\"}", "{\"id\":1}");

        assertThat(d.method()).isEqualTo("POST");
        assertThat(d.urlPath()).isEqualTo("/submit");
        assertThat(d.httpStatus()).isEqualTo(201);
        assertThat(d.requestId()).isNull();
    }
}
