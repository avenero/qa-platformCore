package com.qa.apicore.reporting;

import com.qa.apicore.reporting.bridge.HttpDetailData;
import com.qa.common.reporting.core.bridge.HttpStepSummary;
import com.qa.common.reporting.core.bridge.StepDetail;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que {@link HttpDetailData} satisface los contratos {@link StepDetail}
 * y {@link HttpStepSummary} y que sus record-accessors retornan los valores correctos.
 */
@SuppressWarnings("deprecation")
class HttpDetailDataTest {

    @Test
    void record_implementsStepDetail() {
        HttpDetailData data = new HttpDetailData(
                "POST", "/api/login", 201, "req-abc",
                "{\"user\":\"test\"}", "{\"token\":\"jwt\"}",
                20, 40, 85L,
                Map.of("Content-Type", "application/json"),
                Map.of("X-Request-Id", "req-abc"));

        assertThat(data).isInstanceOf(StepDetail.class);
    }

    @Test
    void record_implementsHttpStepSummary_deprecated() {
        HttpDetailData data = new HttpDetailData(
                "POST", "/api/login", 201, "req-abc",
                "{\"user\":\"test\"}", "{\"token\":\"jwt\"}",
                20, 40, 85L,
                Map.of("Content-Type", "application/json"),
                Map.of("X-Request-Id", "req-abc"));

        assertThat(data).isInstanceOf(HttpStepSummary.class);
    }

    @Test
    void renderHtml_containsMethodAndPath() {
        HttpDetailData data = new HttpDetailData(
                "GET", "/api/users", 200, null, null, null,
                null, null, null, null, null);

        String html = data.renderHtml();

        assertThat(html).contains("GET");
        assertThat(html).contains("/api/users");
        assertThat(html).contains("200");
    }

    @Test
    void renderHtml_errorStatus_usesRedColor() {
        HttpDetailData data = new HttpDetailData(
                "POST", "/api/create", 500, null, null, null,
                null, null, null, null, null);

        assertThat(data.renderHtml()).contains("#f44336");
    }

    @Test
    void renderHtml_successStatus_usesGreenColor() {
        HttpDetailData data = new HttpDetailData(
                "GET", "/api/items", 200, null, null, null,
                null, null, null, null, null);

        assertThat(data.renderHtml()).contains("#4CAF50");
    }

    @Test
    void renderHtml_withDuration_includesDurationMs() {
        HttpDetailData data = new HttpDetailData(
                "GET", "/api/items", 200, null, null, null,
                null, null, 75L, null, null);

        assertThat(data.renderHtml()).contains("75ms");
    }

    @Test
    void renderHtml_withSummaries_includesBodies() {
        HttpDetailData data = new HttpDetailData(
                "POST", "/api/create", 201, null,
                "{\"name\":\"item\"}", "{\"id\":1}",
                null, null, null, null, null);

        String html = data.renderHtml();

        // Quotes in JSON are HTML-escaped to &quot; in the rendered output
        assertThat(html).contains("name");
        assertThat(html).contains("item");
        assertThat(html).contains("id");
    }

    @Test
    void renderHtml_xssInSummary_isEscaped() {
        HttpDetailData data = new HttpDetailData(
                "GET", "/api", 200, null,
                "<script>alert(1)</script>", null,
                null, null, null, null, null);

        String html = data.renderHtml();

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void renderText_format() {
        HttpDetailData data = new HttpDetailData(
                "DELETE", "/api/item/1", 204, null, null, null,
                null, null, null, null, null);

        assertThat(data.renderText()).isEqualTo("DELETE /api/item/1 → 204");
    }

    @Test
    void renderText_nullMethodAndPath_doesNotThrow() {
        HttpDetailData data = new HttpDetailData(
                null, null, 200, null, null, null,
                null, null, null, null, null);

        String text = data.renderText();

        assertThat(text).contains("200");
        assertThat(text).doesNotContain("null");
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
