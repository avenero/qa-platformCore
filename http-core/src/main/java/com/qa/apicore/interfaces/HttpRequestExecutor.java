package com.qa.apicore.interfaces;

import com.qa.common.http.enums.HttpMethod;
import com.qa.common.exception.FrameworkTechnicalException;
import com.qa.common.http.model.HttpResponse;

/**
 * Contrato para ejecutar peticiones HTTP (ISP).
 * Los when-steps de API dependen unicamente de este contrato.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see HttpClient
 */
public interface HttpRequestExecutor {

    /**
     * Ejecuta una peticion HTTP con metodo y endpoint.
     *
     * @param method   Metodo HTTP (type-safe enum)
     * @param endpoint Endpoint relativo al host configurado
     * @return Respuesta HTTP encapsulada (nunca null)
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     * @throws IllegalArgumentException    Si endpoint es null o vacio
     * @throws IllegalStateException       Si no hay host configurado
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint)
            throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion HTTP con control de redirects.
     *
     * @param method          Metodo HTTP
     * @param endpoint        Endpoint relativo
     * @param followRedirects Si debe seguir redirects automaticamente
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint, boolean followRedirects)
            throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion HTTP con timeout especifico.
     *
     * @param method    Metodo HTTP
     * @param endpoint  Endpoint relativo
     * @param timeoutMs Timeout en ms (debe ser mayor a 0)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos o timeout
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint, int timeoutMs)
            throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion HTTP con configuracion completa per-request.
     *
     * @param method          Metodo HTTP
     * @param endpoint        Endpoint relativo
     * @param followRedirects Si debe seguir redirects
     * @param timeoutMs       Timeout en ms (debe ser mayor a 0)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint,
                                boolean followRedirects, int timeoutMs)
            throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion GET.
     *
     * @param endpoint Endpoint relativo
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse get(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion POST.
     *
     * @param endpoint Endpoint relativo
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse post(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion PUT.
     *
     * @param endpoint Endpoint relativo
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse put(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion DELETE.
     *
     * @param endpoint Endpoint relativo
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse delete(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una peticion PATCH.
     *
     * @param endpoint Endpoint relativo
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas tecnicos
     */
    HttpResponse patch(String endpoint) throws FrameworkTechnicalException;
}
