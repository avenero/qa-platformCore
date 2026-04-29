package com.qa.common.reporting.core.util;

import com.qa.common.reporting.core.model.HttpStepDetail;
import com.qa.common.utils.TextUtilities;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Construye objetos {@link HttpStepDetail} aplicando redacción de datos sensibles.
 *
 * <p>Actúa como pipeline de sanitización HTTP antes de que los detalles de una
 * petición/respuesta sean persistidos, emitidos por WebSocket o incluidos en reportes.
 * Garantiza que ningún credential, token ni dato PCI-DSS escape al sistema de reporting.
 *
 * <h3>Operaciones de redacción aplicadas</h3>
 * <ul>
 *   <li><b>Headers sensibles</b> — Authorization, Cookie, Set-Cookie y similares
 *       se reemplazan por {@code "REDACTED"}.</li>
 *   <li><b>URL</b> — solo el path relativo se almacena; host, query string
 *       y fragmentos se descartan.</li>
 *   <li><b>Body</b> — sanitizado (claves sensibles enmascaradas) y truncado a
 *       {@value #SUMMARY_MAX_CHARS} caracteres.</li>
 * </ul>
 *
 * <h3>Relación con el sistema de logging y el FE</h3>
 * <pre>
 * TestLogger / LoggingInitializer  → logging estructurado en tiempo real
 *                                    (consola, archivos .log)
 *                  ↕ capas distintas, se complementan
 * HttpDetailRedactor               → construye HttpStepDetail para reporting
 * HttpStepDetail                   → DTO persistido en DB, emitido por WS al FE,
 *                                    incluido en reportes HTML y adjuntos externos
 * </pre>
 *
 * <p>Un módulo de pruebas que importe el framework obtiene ambos beneficios
 * automáticamente: logging en tiempo real via {@code TestLogger} y captura de
 * snapshots HTTP para reporting via este pipeline.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 * @see HttpStepDetail
 * @see TextUtilities#isSensitiveHttpHeaderName(String)
 */
public final class HttpDetailRedactor {

    /** Longitud máxima del resumen de body (request o response) en el snapshot. */
    public static final int SUMMARY_MAX_CHARS = 500;

    private HttpDetailRedactor() {
    }

    /**
     * Construye un {@link HttpStepDetail} mínimo (sin requestId, headers ni tamaños).
     *
     * @param method       método HTTP
     * @param fullUrl      URL completa (se extrae solo el path)
     * @param httpStatus   código de estado HTTP
     * @param requestBody  cuerpo de la petición (se redacta y trunca)
     * @param responseBody cuerpo de la respuesta (se redacta y trunca)
     * @return snapshot redactado
     * @deprecated Usar {@link #build(String, String, int, String, String, String, Long, Map, Map)}.
     */
    @Deprecated
    public static HttpStepDetail build(
            String method,
            String fullUrl,
            int httpStatus,
            String requestBody,
            String responseBody) {
        return build(method, fullUrl, httpStatus, requestBody, responseBody, null, null, null, null);
    }

    /**
     * Construye un {@link HttpStepDetail} versión 1 completo con trazabilidad y headers.
     *
     * @param method          método HTTP (GET, POST, PUT, ...)
     * @param fullUrl         URL completa — solo se persiste el path
     * @param httpStatus      código de estado HTTP
     * @param requestBody     cuerpo de la petición original (puede ser null)
     * @param responseBody    cuerpo de la respuesta original (puede ser null)
     * @param requestId       ID de correlación de la petición (puede ser null)
     * @param durationMs      duración de la petición en milisegundos (puede ser null)
     * @param requestHeaders  headers de la petición (valores sensibles se redactan)
     * @param responseHeaders headers de la respuesta (valores sensibles se redactan)
     * @return snapshot HTTP redactado e inmutable
     */
    public static HttpStepDetail build(
            String method,
            String fullUrl,
            int httpStatus,
            String requestBody,
            String responseBody,
            String requestId,
            Long durationMs,
            Map<String, String> requestHeaders,
            Map<String, String> responseHeaders) {

        Integer reqBytes = requestBody == null || requestBody.isEmpty()
                ? null
                : requestBody.getBytes(StandardCharsets.UTF_8).length;
        Integer resBytes = responseBody == null
                ? null
                : responseBody.getBytes(StandardCharsets.UTF_8).length;

        return new HttpStepDetail(
                1,
                requestId,
                method != null ? method.toUpperCase(Locale.ROOT) : null,
                extractPath(fullUrl),
                httpStatus,
                redactSummary(requestBody),
                redactSummary(responseBody),
                redactHeaderMap(requestHeaders),
                redactHeaderMap(responseHeaders),
                reqBytes,
                resBytes,
                durationMs);
    }

    static Map<String, String> redactHeaderMap(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String k = e.getKey();
            String v = e.getValue();
            if (TextUtilities.isSensitiveHttpHeaderName(k)) {
                out.put(k, "REDACTED");
            } else if (v != null) {
                out.put(k, TextUtilities.truncateContent(v, 512));
            } else {
                out.put(k, "");
            }
        }
        return out.isEmpty() ? null : out;
    }

    static String extractPath(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(fullUrl);
            String path = uri.getPath();
            return (path == null || path.isBlank()) ? "/" : path;
        } catch (Exception e) {
            int queryIndex = fullUrl.indexOf('?');
            if (queryIndex >= 0) {
                return fullUrl.substring(0, queryIndex);
            }
            return fullUrl;
        }
    }

    static String redactSummary(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        String sanitized = TextUtilities.sanitizeBody(rawBody);
        return TextUtilities.truncateContent(sanitized, SUMMARY_MAX_CHARS);
    }
}
