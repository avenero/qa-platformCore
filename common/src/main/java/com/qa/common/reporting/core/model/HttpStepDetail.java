package com.qa.common.reporting.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resumen seguro e inmutable de una interacción HTTP capturada durante un step de Cucumber.
 *
 * <p>Este objeto representa el "snapshot" de la petición/respuesta HTTP asociada a un step,
 * con datos sensibles ya redactados por {@link com.qa.common.reporting.core.util.HttpDetailRedactor}.
 * Es seguro para persistir en base de datos, incluir en reportes HTML, emitir por WebSocket
 * al Frontend o adjuntar a tickets de gestión de pruebas (Jira, Azure DevOps, etc.).
 *
 * <h3>Relación con el sistema de logging</h3>
 * <pre>
 * TestLogger          → logs estructurados en tiempo real (consola / archivos de log)
 * LoggingInitializer  → contexto MDC de Logback (thread name, scenario name, module)
 *                        ↕ capa completamente distinta
 * HttpStepDetail      → snapshot HTTP inmutable para REPORTES y persistencia
 * HttpDetailRedactor  → construye HttpStepDetail aplicando redacción de datos sensibles
 * </pre>
 *
 * <p>Los cuatro elementos se complementan: {@code TestLogger} escribe en tiempo real;
 * {@code HttpStepDetail} captura el "qué pasó" para análisis posterior.
 * Un módulo de pruebas que use el framework obtiene ambos beneficios automáticamente
 * a través de la integración en {@code BaseHttpClient} y el pipeline de reporting.
 *
 * <h3>Versioning del shape</h3>
 * El campo {@code v} permite evolucionar el contrato sin romper ejecuciones persistidas:
 * <ul>
 *   <li>{@code v=1} — shape completo con {@code requestId}, headers redactados y tamaños.</li>
 *   <li>{@code v=0} / constructor 5-args — compatibilidad con ejecuciones antiguas.</li>
 * </ul>
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public final class HttpStepDetail {

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
     * @param method                   método HTTP (GET, POST, ...)
     * @param urlPath                  path de la URL (sin query string ni credenciales)
     * @param httpStatus               código de estado HTTP
     * @param redactedRequestSummary   resumen del cuerpo de la petición (ya redactado)
     * @param redactedResponseSummary  resumen del cuerpo de la respuesta (ya redactado)
     */
    public HttpStepDetail(
            String method,
            String urlPath,
            int httpStatus,
            String redactedRequestSummary,
            String redactedResponseSummary) {
        this(
                1,
                null,
                method,
                urlPath,
                httpStatus,
                redactedRequestSummary,
                redactedResponseSummary,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Constructor completo versión 1 con trazabilidad y headers redactados.
     *
     * @param v                        versión del shape (usar {@code 1})
     * @param requestId                ID de correlación de la petición (puede ser null)
     * @param method                   método HTTP
     * @param urlPath                  path relativo de la URL
     * @param httpStatus               código de estado HTTP
     * @param redactedRequestSummary   resumen del body de petición (ya redactado, max 500 chars)
     * @param redactedResponseSummary  resumen del body de respuesta (ya redactado, max 500 chars)
     * @param redactedRequestHeaders   headers de petición con valores sensibles como "REDACTED"
     * @param redactedResponseHeaders  headers de respuesta con valores sensibles como "REDACTED"
     * @param requestBodySizeBytes     tamaño original del body de petición en bytes (puede ser null)
     * @param responseBodySizeBytes    tamaño original del body de respuesta en bytes (puede ser null)
     * @param durationMs               duración de la petición en milisegundos (puede ser null)
     */
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
}
