package com.qa.common.logging;

/**
 * Resumen seguro de una interacción HTTP capturada durante un step de Cucumber.
 *
 * <p>Todos los campos son el resultado del pipeline de redacción ({@link HttpDetailRedactor}):
 * valores sensibles ya enmascarados, bodies truncados. Diseñado para viajar hacia el FE sin
 * exponer secretos ni payloads completos.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
public final class HttpStepDetail {

    /** Método HTTP de la petición (GET, POST, PUT, PATCH, DELETE, etc.). */
    private final String method;

    /**
     * Path de la URL sin query string. Los query params se omiten porque pueden contener
     * credenciales o tokens (ej: ?api_key=xxx).
     */
    private final String urlPath;

    /** Código de estado HTTP de la respuesta. */
    private final int httpStatus;

    /**
     * Resumen redactado del body de la petición. Null si la petición no tiene body.
     * Limitado a {@link HttpDetailRedactor#SUMMARY_MAX_CHARS} caracteres.
     */
    private final String redactedRequestSummary;

    /**
     * Resumen redactado del body de la respuesta. Null si la respuesta no tiene body.
     * Limitado a {@link HttpDetailRedactor#SUMMARY_MAX_CHARS} caracteres.
     */
    private final String redactedResponseSummary;

    public HttpStepDetail(
            String method,
            String urlPath,
            int httpStatus,
            String redactedRequestSummary,
            String redactedResponseSummary) {
        this.method = method;
        this.urlPath = urlPath;
        this.httpStatus = httpStatus;
        this.redactedRequestSummary = redactedRequestSummary;
        this.redactedResponseSummary = redactedResponseSummary;
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

    @Override
    public String toString() {
        return method + " " + urlPath + " → " + httpStatus;
    }
}
