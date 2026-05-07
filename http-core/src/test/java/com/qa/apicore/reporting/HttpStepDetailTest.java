package com.qa.apicore.reporting;

import com.qa.apicore.reporting.model.HttpStepDetail;
import com.qa.common.reporting.core.bridge.HttpStepSummary;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link HttpStepDetail}: constructores, inmutabilidad, y contrato
 * {@link HttpStepSummary}.
 */
class HttpStepDetailTest {

    @Test
    void fullConstructor_allFields_accessorsReturnCorrectValues() {
        Map<String, String> reqHeaders  = Map.of("Content-Type", "application/json");
        Map<String, String> resHeaders  = Map.of("X-Request-Id", "abc");

        HttpStepDetail detail = new HttpStepDetail(
                1, "req-abc", "GET", "/api/items", 200,
                "request-summary", "response-summary",
                reqHeaders, resHeaders,
                100, 200, 75L);

        assertThat(detail.getV()).isEqualTo(1);
        assertThat(detail.getRequestId()).isEqualTo("req-abc");
        assertThat(detail.getMethod()).isEqualTo("GET");
        assertThat(detail.getUrlPath()).isEqualTo("/api/items");
        assertThat(detail.getHttpStatus()).isEqualTo(200);
        assertThat(detail.getRedactedRequestSummary()).isEqualTo("request-summary");
        assertThat(detail.getRedactedResponseSummary()).isEqualTo("response-summary");
        assertThat(detail.getRedactedRequestHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(detail.getRedactedResponseHeaders()).containsEntry("X-Request-Id", "abc");
        assertThat(detail.getRequestBodySizeBytes()).isEqualTo(100);
        assertThat(detail.getResponseBodySizeBytes()).isEqualTo(200);
        assertThat(detail.getDurationMs()).isEqualTo(75L);
    }

    @Test
    void fullConstructor_implementsHttpStepSummary() {
        HttpStepDetail detail = new HttpStepDetail(
                1, "r1", "POST", "/path", 201,
                "reqSummary", "resSummary",
                null, null, 10, 20, 50L);

        HttpStepSummary summary = detail;
        assertThat(summary.method()).isEqualTo("POST");
        assertThat(summary.urlPath()).isEqualTo("/path");
        assertThat(summary.httpStatus()).isEqualTo(201);
        assertThat(summary.requestId()).isEqualTo("r1");
        assertThat(summary.requestSummary()).isEqualTo("reqSummary");
        assertThat(summary.responseSummary()).isEqualTo("resSummary");
        assertThat(summary.requestBodySizeBytes()).isEqualTo(10);
        assertThat(summary.responseBodySizeBytes()).isEqualTo(20);
        assertThat(summary.durationMs()).isEqualTo(50L);
    }

    @Test
    void headers_areImmutable_onceConstructed() {
        Map<String, String> reqHeaders = new java.util.LinkedHashMap<>();
        reqHeaders.put("Accept", "application/json");

        HttpStepDetail detail = new HttpStepDetail(
                1, null, "GET", "/", 200,
                null, null, reqHeaders, null, null, null, null);

        reqHeaders.put("X-Extra", "should-not-appear");

        assertThat(detail.getRedactedRequestHeaders()).doesNotContainKey("X-Extra");
    }

    @Test
    void emptyHeaders_storedAsNull() {
        HttpStepDetail detail = new HttpStepDetail(
                1, null, "GET", "/", 200,
                null, null, Map.of(), Map.of(), null, null, null);

        assertThat(detail.getRedactedRequestHeaders()).isNull();
        assertThat(detail.getRedactedResponseHeaders()).isNull();
    }

    @Test
    void toString_containsMethodUrlAndStatus() {
        HttpStepDetail detail = new HttpStepDetail(
                1, null, "DELETE", "/api/item/42", 204,
                null, null, null, null, null, null, null);

        assertThat(detail.toString()).contains("DELETE", "/api/item/42", "204");
    }

    @Test
    @SuppressWarnings("deprecation")
    void compatibilityConstructor_5args_setsMinimalFields() {
        HttpStepDetail detail = new HttpStepDetail(
                "PATCH", "/api/update", 200, "reqBody", "resBody");

        assertThat(detail.getMethod()).isEqualTo("PATCH");
        assertThat(detail.getUrlPath()).isEqualTo("/api/update");
        assertThat(detail.getHttpStatus()).isEqualTo(200);
        assertThat(detail.getRedactedRequestSummary()).isEqualTo("reqBody");
        assertThat(detail.getRedactedResponseSummary()).isEqualTo("resBody");
        assertThat(detail.getRequestId()).isNull();
        assertThat(detail.getDurationMs()).isNull();
    }
}
