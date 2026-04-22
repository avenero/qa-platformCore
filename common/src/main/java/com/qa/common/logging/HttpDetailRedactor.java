package com.qa.common.logging;

import com.qa.common.utils.TextUtilities;

import java.net.URI;

/**
 * Pipeline de redacción para detalle HTTP de steps — extiende el sistema de logging existente.
 *
 * <p>Consume los mismos mecanismos de sanitización que usa {@code BaseHttpClient} para sus logs
 * internos ({@link TextUtilities#sanitizeBody} y {@link TextUtilities#truncateContent}), y añade
 * la etapa de extracción de path seguro y límite de tamaño explícito para el payload que viaja
 * hacia el FE.
 *
 * <p><b>Integración con el refactor de logging:</b> {@code BaseHttpClient} ya redacta headers y
 * bodies mediante {@code TextUtilities.sanitizeValue/sanitizeBody} antes de escribirlos en los
 * logs internos. Esta clase aplica la misma lógica pero produce un {@link HttpStepDetail} acotado
 * para consumo externo (WebSocket / REST), garantizando que nunca viajen secretos en el payload
 * de red.
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see TextUtilities#sanitizeBody(String)
 * @see TextUtilities#truncateContent(String, int)
 */
public final class HttpDetailRedactor {

    /**
     * Límite de caracteres para los resúmenes de request/response que llegan al FE.
     * Mucho más conservador que el límite de logs internos (4000 chars en BaseHttpClient).
     */
    public static final int SUMMARY_MAX_CHARS = 500;

    private HttpDetailRedactor() {
        // Utility class
    }

    /**
     * Construye un {@link HttpStepDetail} a partir de los datos brutos de una llamada HTTP.
     *
     * <p>El método aplica, en orden:
     * <ol>
     *   <li>Extracción del path sin query string ({@link #extractPath}).</li>
     *   <li>Sanitización del body ({@link TextUtilities#sanitizeBody}) — enmascara campos
     *       sensibles como {@code password}, {@code token}, {@code secret}, {@code key}.</li>
     *   <li>Truncado a {@link #SUMMARY_MAX_CHARS} caracteres ({@link TextUtilities#truncateContent}).</li>
     * </ol>
     *
     * @param method       método HTTP (GET, POST, etc.)
     * @param fullUrl      URL completa incluyendo host y query string
     * @param httpStatus   código de estado de la respuesta
     * @param requestBody  body crudo de la petición (puede ser null)
     * @param responseBody body crudo de la respuesta (puede ser null)
     * @return detalle HTTP redactado y listo para exponer en el FE
     */
    public static HttpStepDetail build(
            String method,
            String fullUrl,
            int httpStatus,
            String requestBody,
            String responseBody) {

        return new HttpStepDetail(
                method != null ? method.toUpperCase() : null,
                extractPath(fullUrl),
                httpStatus,
                redactSummary(requestBody),
                redactSummary(responseBody));
    }

    /**
     * Extrae únicamente el path de una URL, descartando el query string que puede
     * contener credenciales o tokens en parámetros (e.g., {@code ?api_key=xxx}).
     */
    static String extractPath(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(fullUrl);
            String path = uri.getPath();
            return (path == null || path.isBlank()) ? "/" : path;
        } catch (Exception e) {
            // Fallback: cortar en el primer '?'
            int queryIndex = fullUrl.indexOf('?');
            if (queryIndex >= 0) {
                return fullUrl.substring(0, queryIndex);
            }
            // Si tampoco tiene '?', devolver como está (mejor que null)
            return fullUrl;
        }
    }

    /**
     * Aplica el pipeline de sanitización y truncado sobre un body crudo.
     * Devuelve null si el body está vacío (sin body → no se muestra sección en el FE).
     */
    static String redactSummary(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        // Paso 1: reutilizar el mismo sanitizador que BaseHttpClient usa en sus logs internos
        String sanitized = TextUtilities.sanitizeBody(rawBody);
        // Paso 2: truncar al límite para el FE (mucho más conservador que los 4000 del log)
        return TextUtilities.truncateContent(sanitized, SUMMARY_MAX_CHARS);
    }
}
