package com.qa.apicore.steps.config;

import com.qa.apicore.utils.ApiHelper;
import io.cucumber.java.en.Given;

/**
 * Steps de configuracion de URL/Ambiente para peticiones HTTP.
 * Migrado de ApiSteps.java + nuevos steps.
 * @author Abel Venero
 * @since 2.0.0
 */
public class UrlConfigSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Given("configuro el endpoint {string}")
    public void configuroElEndpoint(String propertyKey) {
        apiHelper().configureEndpoint(propertyKey);
    }

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

    @Given("establezco el host base como {string}")
    public void establezcoElHostBaseComo(String host) {
        apiHelper().setBaseHost(host);
    }

    @Given("configuro el ambiente {string}")
    public void configuroElAmbiente(String environment) {
        // Delega a ApiHelper.configureEndpoint que ya prioriza ExecutionContext.config()
        // antes de caer al ConfigManager (safe en ejecución paralela)
        apiHelper().configureEndpoint("api.baseurl." + environment.toLowerCase());
    }

    @Given("configuro la URL completa {string}")
    public void configuroLaUrlCompleta(String fullUrl) {
        apiHelper().setBaseHost(fullUrl);
    }

    @Given("configuro el protocolo {string}")
    public void configuroElProtocolo(String protocol) {
        apiHelper().setProtocol(protocol.toLowerCase());
    }
}
