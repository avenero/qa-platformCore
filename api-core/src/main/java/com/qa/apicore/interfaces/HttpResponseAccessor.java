package com.qa.apicore.interfaces;

import com.qa.common.http.model.HttpResponse;

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
}
