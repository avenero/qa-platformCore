package com.scotia.qa.common.http.model;

import java.util.Map;
import java.util.HashMap;

/**
 * Clase común para representar respuestas HTTP entre todos los frameworks.
 * Proporciona una interfaz unificada para manejar respuestas de diferentes clientes HTTP.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class HttpResponse {

    private final int statusCode;
    private final String body;
    private Map<String, String> headers;  // No puede ser final - usado por setHeaders()
    private final long duration;

    /**
     * Constructor básico para respuesta HTTP.
     */
    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = new HashMap<>();
        this.duration = 0;
    }

    /**
     * Constructor completo para respuesta HTTP.
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

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

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
     */
    public String getSummary() {
        return String.format("Status: %d, Content-Length: %d bytes, Duration: %dms",
            statusCode,
            body != null ? body.length() : 0,
            duration);
    }
}
