package com.qa.httpcore.reporting.bridge;

import com.qa.common.api.reporter.bridge.HttpStepSummary;
import com.qa.common.api.reporter.detail.StepDetail;

import java.util.Map;

/**
 * Registro puente inmutable de una interacción HTTP capturada durante un step de prueba.
 *
 * <p>Implementa {@link StepDetail} (contrato de auto-renderizado) y
 * {@link HttpStepSummary} (mantenido por compatibilidad, deprecado desde 2.3.0).
 * La dirección de dependencia correcta se mantiene: {@code http-core → common}.
 *
 * <p>Todos los datos sensibles deben estar <b>ya redactados</b> antes de construir este record.
 * Usar {@link com.qa.httpcore.reporting.util.HttpDetailRedactor} como fábrica.
 *
 * @since 2.2.0 (implements StepDetail desde 2.3.0)
 * @see com.qa.httpcore.reporting.util.HttpDetailRedactor
 */
@SuppressWarnings("deprecation")
public record HttpDetailData(
        String method,
        String urlPath,
        int httpStatus,
        String requestId,
        String requestSummary,
        String responseSummary,
        Integer requestBodySizeBytes,
        Integer responseBodySizeBytes,
        Long durationMs,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders
) implements HttpStepSummary, StepDetail {

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
        if (requestSummary != null) {
            sb.append("<b>Request:</b> <code>").append(escapeHtml(requestSummary)).append("</code><br/>");
        }
        if (responseSummary != null) {
            sb.append("<b>Response:</b> <code>").append(escapeHtml(responseSummary)).append("</code>");
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
        String m = method != null ? method : "";
        String p = urlPath != null ? urlPath : "";
        return m + " " + p + " → " + httpStatus;
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
