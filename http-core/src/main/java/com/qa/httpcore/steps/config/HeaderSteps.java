package com.qa.httpcore.steps.config;

import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.interfaces.HttpRequestBuilder;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de configuracion de headers HTTP de la peticion.
 * Migrado de ApiSteps.java + nuevos steps.
 *
 * <p>Depende únicamente de {@link HttpRequestBuilder} (ISP): estos steps
 * solo configuran la petición, nunca la ejecutan ni acceden a la respuesta.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class HeaderSteps {

    // ─── Obtención de la sub-interface más estrecha desde el ServiceRegistry ───

    private HttpRequestBuilder getHttpRequestBuilder() {
        return ExecutionContext.current().map(ctx -> (HttpRequestBuilder) ctx.service(HttpClient.class)).
                orElseGet(ApacheHttpClientImpl::new);
    }

    @Given("agrego el header {string} con valor {string}")
    public void agregoElHeaderConValor(String header, String value) {
        getHttpRequestBuilder().addHeader(header, ExecutionContext.requireCurrent().variables().resolve(value));
        TestLogger.logInfo("HEADER_STEPS", "Header agregado: " + header, null);
    }

    @Given("agrego Content-Type {string}")
    public void agregoContentType(String contentType) {
        getHttpRequestBuilder().addHeader("Content-Type", contentType);
    }

    @Given("agrego Accept {string}")
    public void agregoAccept(String accept) {
        getHttpRequestBuilder().addHeader("Accept", accept);
    }

    @Given("agrego los siguientes headers")
    public void agregoLosSiguientesHeaders(Map<String, String> headers) {
        var builder = getHttpRequestBuilder();
        var vars = ExecutionContext.requireCurrent().variables();
        headers.forEach((k, v) -> builder.addHeader(k, vars.resolve(v)));
    }

    @Given("agrego header de versión API {string}")
    public void agregoHeaderDeVersionApi(String version) {
        getHttpRequestBuilder().addHeader("X-API-Version", version);
    }

    @Given("remuevo el header {string}")
    public void remuevoElHeader(String header) {
        getHttpRequestBuilder().removeHeader(header);
    }
}
