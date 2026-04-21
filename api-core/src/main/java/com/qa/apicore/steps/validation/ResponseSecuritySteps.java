package com.qa.apicore.steps.validation;

import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.model.HttpResponse;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import java.util.List;

/**
 * Steps de validacion de controles de seguridad HTTP.
 * Nuevo componente en Fase 2: HTTPS, headers de seguridad, XSS, SQL Injection.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ResponseSecuritySteps {

    /** HTTP 401 Unauthorized — expected rejection code for security validations. */
    private static final int HTTP_UNAUTHORIZED = 401;
    /** HTTP 403 Forbidden — expected rejection code for security validations. */
    private static final int HTTP_FORBIDDEN = 403;
    /** HTTP 422 Unprocessable Entity — expected rejection code for security validations. */
    private static final int HTTP_UNPROCESSABLE = 422;

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }
    private static final List<String> SENSITIVE_HEADERS =
        List.of("Server", "X-Powered-By", "X-AspNet-Version", "X-AspNetMvc-Version");

    @Then("valido que la respuesta use HTTPS")
    public void validoHttps() {
        Assertions.assertThat(apiHelper().getLastRequestUrl()).
            as("La peticion deberia usar HTTPS").startsWith("https://");
    }

    @Then("valido que no haya headers de información sensible expuestos")
    public void validoSinHeadersSensibles() {
        HttpResponse r = apiHelper().getLastResponse();
        SENSITIVE_HEADERS.forEach(h ->
            Assertions.assertThat(r.getHeader(h)).as("Header sensible '" + h + "' no deberia estar expuesto").isNull());
    }

    @Then("valido que el header X-Content-Type-Options sea {string}")
    public void validoXContentTypeOptions(String value) {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader("X-Content-Type-Options")).
            as("X-Content-Type-Options deberia ser: " + value).isEqualTo(value);
    }

    @Then("valido que el header X-Frame-Options esté presente")
    public void validoXFrameOptions() {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader("X-Frame-Options")).
            as("X-Frame-Options deberia estar presente").isNotNull();
    }

    @Then("valido que la respuesta no contenga trazas de stack")
    public void validoSinStackTraces() {
        String body = apiHelper().getLastResponse().getBody();
        if (body == null) {
            return;
        }
        Assertions.assertThat(body).doesNotContainIgnoringCase("at com.").doesNotContainIgnoringCase("StackTrace").
            doesNotContainIgnoringCase("NullPointerException");
    }

    @Then("valido protección contra SQL injection intentando {string}")
    public void validoSqlInjection(String payload) {
        int code = apiHelper().getLastResponse().getStatusCode();
        Assertions.assertThat(code).as("SQL Injection deberia ser rechazado").
            isIn(400, HTTP_UNAUTHORIZED, HTTP_FORBIDDEN, HTTP_UNPROCESSABLE);
    }

    @Then("valido protección contra XSS intentando {string}")
    public void validoXss(String payload) {
        String body = apiHelper().getLastResponse().getBody();
        if (body != null) {
            Assertions.assertThat(body).as("La respuesta no deberia reflejar payload XSS").doesNotContain("<script>");
        }
    }
}
