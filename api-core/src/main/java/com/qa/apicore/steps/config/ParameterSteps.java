package com.qa.apicore.steps.config;

import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.interfaces.HttpRequestBuilder;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
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
        return ExecutionContext.current()
                .map(ctx -> (HttpRequestBuilder) ctx.service(HttpClient.class))
                .orElseGet(BaseHttpClient::new);
    }

    @Given("agrego el query param {string} con valor {string}")
    public void agregoElQueryParamConValor(String param, String value) {
        getHttpRequestBuilder().addQueryParam(param,
                ExecutionContext.requireCurrent().variables().resolve(value));
        TestLogger.logInfo("PARAM_STEPS", "Query param agregado: " + param, null);
    }

    @StepDef(value = "api.parameters.legacy-queryparam",
             deprecated = true, replacedBy = "api.parameters#agregoElQueryParamConValor")
    @Given("agrego el queryparam {string} con el valor {string}")
    public void agregoElQueryparamConElValor(String param, String value) {
        var vars = ExecutionContext.requireCurrent().variables();
        getHttpRequestBuilder().addQueryParam(vars.resolve(param), vars.resolve(value));
    }

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
