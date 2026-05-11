package com.qa.httpcore.steps.config;

import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.interfaces.HttpRequestBuilder;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de configuracion de query params y path params.
 * Migrado de ApiSteps.java + nuevos steps.
 *
 * <p>Depende únicamente de {@link HttpRequestBuilder} (ISP): estos steps
 * solo configuran parámetros de la petición antes de ejecutarla.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class ParameterSteps {

    // ─── Obtención de la sub-interface más estrecha desde el ServiceRegistry ───

    private HttpRequestBuilder getHttpRequestBuilder() {
        return ExecutionContext.current().map(ctx -> (HttpRequestBuilder) ctx.service(HttpClient.class)).
                orElseGet(HttpClientFactory::getInstance);
    }

    @Given("agrego el query param {string} con valor {string}")
    public void agregoElQueryParamConValor(String param, String value) {
        getHttpRequestBuilder().addQueryParam(param,
                ExecutionContext.requireCurrent().variables().resolve(value));
        TestLogger.logInfo("PARAM_STEPS", "Query param agregado: " + param, null);
    }

    @Given("agrego los siguientes query params:")
    @Given("agrego los siguientes query params")
    public void agregoQueryParams(Map<String, String> params) {
        var builder = getHttpRequestBuilder();
        var vars = ExecutionContext.requireCurrent().variables();
        params.forEach((k, v) -> builder.addQueryParam(k, vars.resolve(v)));
    }

    @Given("reemplazo el path param {string} con el valor {string}")
    public void reemplazoPathParam(String param, String value) {
        getHttpRequestBuilder().addPathParam(param,
                ExecutionContext.requireCurrent().variables().resolve(value));
    }

    @Given("no envío query params")
    public void noEnvioQueryParams() {
        getHttpRequestBuilder().clearQueryParams();
    }

    @Given("agrego query param {string} sin valor")
    public void agregoQueryParamSinValor(String param) {
        getHttpRequestBuilder().addQueryParam(param, "");
    }
}
