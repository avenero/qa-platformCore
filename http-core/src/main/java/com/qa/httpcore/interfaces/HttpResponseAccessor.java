package com.qa.httpcore.interfaces;

import com.qa.httpcore.model.HttpResponse;

import java.util.Collections;
import java.util.Map;

/**
 * Contrato de solo lectura sobre la ultima respuesta HTTP recibida.
 *
 * <p>Los then-steps de validacion dependen unicamente de este contrato.
 * Segregado desde {@link HttpClient} para cumplir el ISP.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see HttpClient
 */
public interface HttpResponseAccessor {

    /**
     * Obtiene la respuesta de la ultima peticion ejecutada.
     *
     * @return Ultima respuesta HTTP o null si no se ha ejecutado ninguna peticion
     */
    HttpResponse getLastResponse();

    /**
     * Obtiene la URL completa de la ultima peticion ejecutada.
     *
     * @return URL de la ultima peticion o null si no se ha ejecutado ninguna
     */
    String getLastRequestUrl();

    /**
     * Obtiene el metodo HTTP de la ultima peticion ejecutada.
     *
     * @return Metodo de la ultima peticion o null si no se ha ejecutado ninguna
     */
    String getLastRequestMethod();

    /**
     * Obtiene la duracion en ms de la ultima peticion ejecutada.
     *
     * @return Duracion en ms, -1 si no se ha ejecutado ninguna peticion
     */
    long getLastRequestDuration();

    /**
     * Obtiene el body de la ultima peticion HTTP ejecutada (antes de ser limpiado).
     * Se usa para construir {@code HttpStepDetail} desde el pipeline de {@code HttpDetailRedactor}.
     *
     * @return body de la ultima peticion, o null si no aplica
     */
    String getLastRequestBody();

    /**
     * Identificador de correlación de la última petición (p. ej. UUID), si existe.
     *
     * @return id o {@code null} si no aplica
     */
    default String getLastRequestId() {
        return null;
    }

    /**
     * Snapshot de headers de la última petición enviada (antes de limpiar el cliente).
     */
    default Map<String, String> getLastRequestHeadersSnapshot() {
        return Collections.emptyMap();
    }

    /**
     * Body de la última respuesta HTTP, cacheado en el momento de recibir la respuesta.
     * Más confiable que {@code getLastResponse().getBody()} en algunos contextos de ejecución.
     *
     * @return body de la última respuesta, o null si no aplica
     */
    default String getLastResponseBody() {
        com.qa.httpcore.model.HttpResponse r = getLastResponse();
        return r != null ? r.getBody() : null;
    }
}
