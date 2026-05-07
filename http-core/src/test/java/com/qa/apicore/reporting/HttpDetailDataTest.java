package com.qa.apicore.reporting;

import com.qa.apicore.reporting.bridge.HttpDetailData;
import com.qa.common.reporting.core.bridge.HttpStepSummary;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que {@link HttpDetailData} satisface el contrato {@link HttpStepSummary}
 * y que sus record-accessors retornan los valores correctos.
 */
class HttpDetailDataTest {

    @Test
    void record_implementsHttpStepSummary() {
        HttpDetailData data = new HttpDetailData(
                "POST", "/api/login", 201, "req-abc",
                "{\"user\":\"test\"}", "{\"token\":\"jwt\"}",
                20, 40, 85L,
                Map.of("Content-Type", "application/json"),
                Map.of("X-Request-Id", "req-abc"));

        assertThat(data).isInstanceOf(HttpStepSummary.class);
    }

    @Test
    void accessors_returnExpectedValues() {
        HttpDetailData data = new HttpDetailData(
                "GET", "/api/users", 200, "rid-1",
                null, "{\"count\":5}",
                null, 80, 30L, null, null);

        assertThat(data.method()).isEqualTo("GET");
        assertThat(data.urlPath()).isEqualTo("/api/users");
        assertThat(data.httpStatus()).isEqualTo(200);
        assertThat(data.requestId()).isEqualTo("rid-1");
        assertThat(data.requestSummary()).isNull();
        assertThat(data.responseSummary()).isEqualTo("{\"count\":5}");
        assertThat(data.requestBodySizeBytes()).isNull();
        assertThat(data.responseBodySizeBytes()).isEqualTo(80);
        assertThat(data.durationMs()).isEqualTo(30L);
    }

    @Test
    void httpStepSummary_interfaceMethods_delegateCorrectly() {
        HttpDetailData data = new HttpDetailData(
                "DELETE", "/api/item/1", 204, null,
                null, null, null, null, 12L, null, null);

        HttpStepSummary summary = data;
        assertThat(summary.method()).isEqualTo("DELETE");
        assertThat(summary.urlPath()).isEqualTo("/api/item/1");
        assertThat(summary.httpStatus()).isEqualTo(204);
        assertThat(summary.requestId()).isNull();
        assertThat(summary.durationMs()).isEqualTo(12L);
    }

    @Test
    void record_withNullOptionalFields_doesNotThrow() {
        HttpDetailData data = new HttpDetailData(
                "PUT", "/api/res", 200, null,
                null, null, null, null, null, null, null);

        assertThat(data.requestBodySizeBytes()).isNull();
        assertThat(data.responseBodySizeBytes()).isNull();
        assertThat(data.durationMs()).isNull();
        assertThat(data.requestHeaders()).isNull();
        assertThat(data.responseHeaders()).isNull();
    }
}
