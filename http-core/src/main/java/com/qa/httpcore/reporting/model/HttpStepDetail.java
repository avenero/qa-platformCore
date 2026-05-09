package com.qa.httpcore.reporting.model;

import com.qa.common.reporting.core.bridge.HttpStepSummary;
import com.qa.common.reporting.core.bridge.StepDetail;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resumen seguro e inmutable de una interacción HTTP capturada durante un step de Cucumber.
 *
 * <p>Implementa {@link HttpStepSummary} para participar en el pipeline de reporting de
 * {@code common} sin crear dependencia inversa. La dirección correcta se mantiene:
 * {@code http-core → common}.
 *
 * <p>Este objeto representa el "snapshot" de la petición/respuesta HTTP asociada a un step,
 * con datos sensibles ya redactados por {@link com.qa.httpcore.reporting.util.HttpDetailRedactor}.
 * Es seguro para persistir en base de datos, incluir en reportes HTML, emitir por WebSocket
 * al Frontend o adjuntar a tickets de gestión de pruebas.
 *
 * <h3>Versioning del shape</h3>
 * <ul>
 *   <li>{@code v=1} — shape completo con {@code requestId}, headers redactados y tamaños.</li>
 *   <li>{@code v=0} / constructor 5-args — compatibilidad con ejecuciones antiguas.</li>
 * </ul>
 *
 * @since 1.0.0 (movido a http-core en 2.2.0; implements StepDetail desde 2.3.0)
 * @see com.qa.httpcore.reporting.util.HttpDetailRedactor
 */
@SuppressWarnings("deprecation")
public final class HttpStepDetail implements HttpStepSummary, StepDetail {

    private final int v;
    private final String requestId;
    private final String method;
    private final String urlPath;
    private final int httpStatus;
    private final String redactedRequestSummary;
    private final String redactedResponseSummary;
    private final Map<String, String> redactedRequestHeaders;
    private final Map<String, String> redactedResponseHeaders;
    private final Integer requestBodySizeBytes;
    private final Integer responseBodySizeBytes;
    private final Long durationMs;

    /**
     * Constructor de compatibilidad: detalle mínimo sin requestId ni headers.
     *
     * @deprecated Usar el constructor completo.
     */
    @Deprecated
    public HttpStepDetail(
            String method,
            String urlPath,
            int httpStatus,
            String redactedRequestSummary,
            String redactedResponseSummary) {
        this(1, null, method, urlPath, httpStatus,
                redactedRequestSummary, redactedResponseSummary,
                null, null, null, null, null);
    }

    /**
     * Constructor completo versión 1 con trazabilidad y headers redactados.
     */
    //CHECKSTYLE:OFF ParameterNumber
    public HttpStepDetail(
            int v,
            String requestId,
            String method,
            String urlPath,
            int httpStatus,
            String redactedRequestSummary,
            String redactedResponseSummary,
            Map<String, String> redactedRequestHeaders,
            Map<String, String> redactedResponseHeaders,
            Integer requestBodySizeBytes,
            Integer responseBodySizeBytes,
            Long durationMs) {
    //CHECKSTYLE:ON ParameterNumber
        this.v = v;
        this.requestId = requestId;
        this.method = method;
        this.urlPath = urlPath;
        this.httpStatus = httpStatus;
        this.redactedRequestSummary = redactedRequestSummary;
        this.redactedResponseSummary = redactedResponseSummary;
        this.redactedRequestHeaders = redactedRequestHeaders == null || redactedRequestHeaders.isEmpty()
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(redactedRequestHeaders));
        this.redactedResponseHeaders = redactedResponseHeaders == null || redactedResponseHeaders.isEmpty()
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(redactedResponseHeaders));
        this.requestBodySizeBytes = requestBodySizeBytes;
        this.responseBodySizeBytes = responseBodySizeBytes;
        this.durationMs = durationMs;
    }

    // -------------------------------------------------------------------------
    // HttpStepSummary — métodos canónicos usados por common/reporting
    // -------------------------------------------------------------------------

    @Override
    public String method() {
        return method;
    }

    @Override
    public String urlPath() {
        return urlPath;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    /** Alias a {@link #getRedactedRequestSummary()} para satisfacer {@link HttpStepSummary}. */
    @Override
    public String requestSummary() {
        return redactedRequestSummary;
    }

    /** Alias a {@link #getRedactedResponseSummary()} para satisfacer {@link HttpStepSummary}. */
    @Override
    public String responseSummary() {
        return redactedResponseSummary;
    }

    @Override
    public Integer requestBodySizeBytes() {
        return requestBodySizeBytes;
    }

    @Override
    public Integer responseBodySizeBytes() {
        return responseBodySizeBytes;
    }

    @Override
    public Long durationMs() {
        return durationMs;
    }

    /** Alias a {@link #getRedactedRequestHeaders()} para satisfacer {@link HttpStepSummary}. */
    @Override
    public Map<String, String> requestHeaders() {
        return redactedRequestHeaders;
    }

    /** Alias a {@link #getRedactedResponseHeaders()} para satisfacer {@link HttpStepSummary}. */
    @Override
    public Map<String, String> responseHeaders() {
        return redactedResponseHeaders;
    }

    // -------------------------------------------------------------------------
    // Getters con nombre legacy (para compatibilidad con código interno de http-core)
    // -------------------------------------------------------------------------

    public int getV() {
        return v;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMethod() {
        return method;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getRedactedRequestSummary() {
        return redactedRequestSummary;
    }

    public String getRedactedResponseSummary() {
        return redactedResponseSummary;
    }

    public Map<String, String> getRedactedRequestHeaders() {
        return redactedRequestHeaders;
    }

    public Map<String, String> getRedactedResponseHeaders() {
        return redactedResponseHeaders;
    }

    public Integer getRequestBodySizeBytes() {
        return requestBodySizeBytes;
    }

    public Integer getResponseBodySizeBytes() {
        return responseBodySizeBytes;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return Objects.toString(method, "") + " " + Objects.toString(urlPath, "") + " → " + httpStatus;
    }

    // -------------------------------------------------------------------------
    // StepDetail — auto-renderizado (lógica HTTP específica vive aquí, no en common)
    // -------------------------------------------------------------------------

    @Override
    public String renderHtml() {
        String statusColor = httpStatus >= 400 ? "#f44336" : "#4CAF50";
        StringBuilder sb = new StringBuilder();
        sb.append("<details style='font-size:13px;margin-top:4px'>")
          .append("<summary style='cursor:pointer'><span style='font-weight:500'>🌐 HTTP: ")
          .append(escapeHtml(method)).append(" ").append(escapeHtml(urlPath))
          .append(" <span style='color:").append(statusColor).append("'>").append(httpStatus)
          .append("</span></span>");
        if (durationMs != null) {
            sb.append(" <span style='color:#aaa;font-size:12px'>(").append(durationMs).append("ms)</span>");
        }
        sb.append("</summary><div style='margin-top:6px;font-size:12px'>");
        if (redactedRequestSummary != null) {
            sb.append("<b>Request:</b> <code>").append(escapeHtml(redactedRequestSummary)).append("</code><br/>");
        }
        if (redactedResponseSummary != null) {
            sb.append("<b>Response:</b> <code>").append(escapeHtml(redactedResponseSummary)).append("</code>");
        }
        if (requestBodySizeBytes != null || responseBodySizeBytes != null) {
            sb.append("<br/><span style='color:#aaa'>Req: ").append(requestBodySizeBytes)
              .append("b | Resp: ").append(responseBodySizeBytes).append("b</span>");
        }
        sb.append("</div></details>");
        return sb.toString();
    }

    @Override
    public String renderText() {
        return Objects.toString(method, "") + " " + Objects.toString(urlPath, "") + " → " + httpStatus;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
