package com.qa.common.http.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase común para representar respuestas HTTP entre todos los frameworks.
 * Proporciona una interfaz unificada para manejar respuestas de diferentes clientes HTTP.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class HttpResponse {

    /** Lower bound of HTTP 3xx (redirect) status code range. */
    private static final int HTTP_REDIRECT_MIN = 300;

    /** Upper bound of HTTP 5xx (server error) status code range (exclusive). */
    private static final int HTTP_SERVER_ERROR_MAX = 600;

    private final int statusCode;
    private final String body;
    private Map<String, String> headers;  // No puede ser final - usado por setHeaders()
    private final long duration;

    /**
     * Constructor básico para respuesta HTTP sin headers ni duración.
     *
     * @param statusCode código de estado HTTP
     * @param body       cuerpo de la respuesta
     */
    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = new HashMap<>();
        this.duration = 0;
    }

    /**
     * Constructor completo para respuesta HTTP.
     *
     * @param statusCode código de estado HTTP
     * @param body       cuerpo de la respuesta
     * @param headers    mapa de headers HTTP de la respuesta
     * @param duration   duración de la solicitud en milisegundos
     */
    public HttpResponse(int statusCode, String body, Map<String, String> headers, long duration) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.duration = duration;
    }

    // =================================================================================
    // GETTERS
    // =================================================================================

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    /**
     * Alias para getBody() - para compatibilidad con código existente.
     */
    public String getResponseBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public long getDuration() {
        return duration;
    }

    // =================================================================================
    // SETTERS (para compatibilidad con código existente)
    // =================================================================================

    /**
     * Establece los headers de la respuesta.
     * Para compatibilidad con código existente que modifica headers después de crear la respuesta.
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    // =================================================================================
    // MÉTODOS DE VALIDACIÓN
    // =================================================================================

    /**
     * Indica si la respuesta es exitosa (código 2xx).
     *
     * @return {@code true} si el código de estado está entre 200 y 299 inclusive
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < HTTP_REDIRECT_MIN;
    }

    /**
     * Indica si la respuesta es un error del cliente (código 4xx).
     *
     * @return {@code true} si el código de estado está entre 400 y 499 inclusive
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Indica si la respuesta es un error del servidor (código 5xx).
     *
     * @return {@code true} si el código de estado está entre 500 y 599 inclusive
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < HTTP_SERVER_ERROR_MAX;
    }

    /**
     * Indica si la respuesta contiene cuerpo no vacío.
     *
     * @return {@code true} si el body no es null ni está vacío
     */
    public boolean hasContent() {
        return body != null && !body.trim().isEmpty();
    }

    // =================================================================================
    // UTILITY METHODS
    // =================================================================================

    @Override
    public String toString() {
        return String.format("HttpResponse{statusCode=%d, bodyLength=%d, headers=%d, duration=%dms}",
            statusCode,
            body != null ? body.length() : 0,
            headers.size(),
            duration);
    }

    /**
     * Obtiene un resumen de la respuesta para logging.
     *
     * @return cadena de texto con estado, tamaño del body y duración
     */
    public String getSummary() {
        return String.format("Status: %d, Content-Length: %d bytes, Duration: %dms",
            statusCode,
            body != null ? body.length() : 0,
            duration);
    }
}
