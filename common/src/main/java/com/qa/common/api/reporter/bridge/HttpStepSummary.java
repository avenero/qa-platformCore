package com.qa.common.api.reporter.bridge;

import java.util.Map;

/**
 * Contrato mínimo de un snapshot HTTP seguro para el pipeline de reporting de {@code common}.
 *
 * @deprecated since 2.3.0, forRemoval = true — usar {@link com.qa.common.api.reporter.detail.StepDetail} en su lugar.
 *             {@code StepDetail} es el contrato genérico de auto-renderizado que permite
 *             que {@code common} sea completamente agnóstico al protocolo HTTP.
 *             <p>Migración: implementar {@code com.qa.common.api.reporter.detail.StepDetail#renderHtml()} y
 *             {@code com.qa.common.api.reporter.detail.StepDetail#renderText()} en los tipos concretos
 *             de {@code http-core}.
 *             El campo {@code StepData.httpDetail} fue renombrado a {@code StepData.protocolDetail}.
 *
 * @since 2.2.0
 * @see com.qa.common.api.reporter.detail.StepDetail
 * @see StepData
 */
@Deprecated(since = "2.3.0", forRemoval = true)
public interface HttpStepSummary {

    /** Método HTTP en mayúsculas (GET, POST, PUT, …). Puede ser null si desconocido. */
    String method();

    /** Path relativo de la URL, sin host, query string ni credenciales. */
    String urlPath();

    /** Código de estado HTTP de la respuesta. */
    int httpStatus();

    /** ID de correlación de la petición para trazabilidad. Puede ser null. */
    String requestId();

    /** Resumen redactado del cuerpo de la petición, truncado a ≤500 chars. Puede ser null. */
    String requestSummary();

    /** Resumen redactado del cuerpo de la respuesta, truncado a ≤500 chars. Puede ser null. */
    String responseSummary();

    /** Tamaño original del body de la petición en bytes. Puede ser null. */
    Integer requestBodySizeBytes();

    /** Tamaño original del body de la respuesta en bytes. Puede ser null. */
    Integer responseBodySizeBytes();

    /** Duración de la petición en milisegundos. Puede ser null. */
    Long durationMs();

    /**
     * Headers de petición con valores sensibles reemplazados por {@code "REDACTED"}.
     * Puede ser null si no se capturaron headers.
     */
    Map<String, String> requestHeaders();

    /**
     * Headers de respuesta con valores sensibles reemplazados por {@code "REDACTED"}.
     * Puede ser null si no se capturaron headers.
     */
    Map<String, String> responseHeaders();
}
