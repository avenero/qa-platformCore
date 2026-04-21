package com.qa.apicore.steps.config;

import com.qa.apicore.utils.ApiHelper;
import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;

/**
 * Steps de configuración de URL/Ambiente para peticiones HTTP.
 *
 * <p>Componente padre: {@code api.url} ({@link com.qa.apicore.components.ApiUrlComponent}).
 * Fase BDD: GIVEN.
 *
 * <p>Todos los steps canónicos de este archivo llevan {@link StepDef} con ID explícito
 * para garantizar estabilidad frente a refactorizaciones del código fuente.
 * El formato de ID es {@code api.url.{sub-id}}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class UrlConfigSteps {

    /** Milliseconds per second, used for converting timeout seconds to milliseconds. */
    private static final int MILLIS_PER_SECOND = 1000;

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    // =========================================================================
    // Steps canónicos — con @StepDef estable
    // =========================================================================

    /**
     * Configura el endpoint a partir de una clave de propiedad (ej: {@code "api.baseurl"})
     * o de una URL directa. Es el step principal de configuración de destino HTTP.
     */
    @StepDef(value = "api.url.set-endpoint",
             displayName = "Configurar endpoint")
    @Given("configuro el endpoint {string}")
    public void configuroElEndpoint(String propertyKey) {
        apiHelper().configureEndpoint(propertyKey);
    }

    /**
     * Configura el endpoint ensamblando base URL y path por separado.
     * Acepta tanto claves de configuración como URLs literales como base.
     */
    @StepDef(value = "api.url.set-base-path",
             displayName = "Configurar endpoint con base y path")
    @Given("configuro endpoint con base {string} y path {string}")
    public void configuroEndpointConBaseYPath(String baseOrKey, String pathOrKey) {
        ApiHelper helper = apiHelper();
        if (baseOrKey.startsWith("http://") || baseOrKey.startsWith("https://")) {
            // Modo URL directa: se ensambla base + path sin consultar config
            String base = baseOrKey.endsWith("/") ? baseOrKey : baseOrKey + "/";
            String path = pathOrKey.startsWith("/") ? pathOrKey.substring(1) : pathOrKey;
            helper.setBaseHost(base + path);
        } else {
            // Modo clave de configuración: delega a ConfigManager / ExecutionContext.config()
            helper.configureEndpointFromConfig(baseOrKey, pathOrKey);
        }
    }

    /**
     * Establece el host base directamente (sin path). Útil cuando se quiere fijar la
     * base URL antes de agregar paths en steps posteriores.
     */
    @StepDef(value = "api.url.set-host",
             displayName = "Establecer host base")
    @Given("establezco el host base como {string}")
    public void establezcoElHostBaseComo(String host) {
        apiHelper().setBaseHost(host);
    }

    /**
     * Configura la URL completa del request en un único step.
     */
    @StepDef(value = "api.url.set-full-url",
             displayName = "Configurar URL completa")
    @Given("configuro la URL completa {string}")
    public void configuroLaUrlCompleta(String fullUrl) {
        apiHelper().setBaseHost(fullUrl);
    }

    /**
     * Configura el protocolo de comunicación ({@code "http"} o {@code "https"}).
     */
    @StepDef(value = "api.url.set-protocol",
             displayName = "Configurar protocolo HTTP/HTTPS")
    @Given("configuro el protocolo {string}")
    public void configuroElProtocolo(String protocol) {
        apiHelper().setProtocol(protocol.toLowerCase());
    }

    /**
     * Configura el timeout global de la petición HTTP en segundos.
     */
    @StepDef(value = "api.url.set-timeout",
             displayName = "Configurar timeout de petición")
    @Given("configuro el timeout de la petición a {int} segundos")
    public void configuroTimeoutEnSegundos(int seconds) {
        apiHelper().setTimeout(seconds * MILLIS_PER_SECOND);
    }

    /**
     * Configura la codificación del body de la petición (ej: {@code "UTF-8"}).
     */
    @StepDef(value = "api.url.set-encoding",
             displayName = "Configurar codificación de petición")
    @Given("configuro la codificación de la petición como {string}")
    public void configuroCodificacion(String encoding) {
        apiHelper().setEncoding(encoding);
    }

    // =========================================================================
    // Step DEPRECATED — mantener hasta próxima release mayor
    // =========================================================================

    /**
     * @deprecated desde v2.1.0 — usar {@link #configuroElEndpoint(String)} con
     *             la clave {@code "api.baseurl.<ambiente>"}.
     */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @StepDef(value = "api.url.legacy-ambiente",
             deprecated = true, replacedBy = "api.url.set-endpoint",
             displayName = "configuro el ambiente (DEPRECATED)")
    @Given("configuro el ambiente {string}")
    public void configuroElAmbiente(String environment) {
        apiHelper().configureEndpoint("api.baseurl." + environment.toLowerCase());
    }
}
