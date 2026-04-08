package com.qa.apicore.steps.validation;

import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion del codigo de estado HTTP.
 * Migrado de ApiSteps.java + nuevos steps (rangos, negacion, familias 2xx/4xx/5xx).
 * @author Abel Venero
 * @since 2.0.0
 */
public class StatusCodeSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Then("valido que el codigo de respuesta del servicio sea {int}")
    public void validoQueElCodigoDeRespuestaDelServicioSea(int statusCode) throws FrameworkBusinessException {
        apiHelper().validateResponseStatusCode(statusCode);
    }

    @Then("valido que el servicio responda con éxito")
    public void validoExito() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode())
            .as("Se esperaba status 2xx").isBetween(200, 299);
    }

    @Then("valido que el servicio responda con error de cliente")
    public void validoErrorCliente() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode())
            .as("Se esperaba status 4xx").isBetween(400, 499);
    }

    @Then("valido que el servicio responda con error de servidor")
    public void validoErrorServidor() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode())
            .as("Se esperaba status 5xx").isBetween(500, 599);
    }

    @Then("valido que el status code esté entre {int} y {int}")
    public void validoStatusCodeEnRango(int min, int max) {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode())
            .as("Se esperaba status entre " + min + " y " + max).isBetween(min, max);
    }

    @Then("valido que el status code NO sea {int}")
    public void validoStatusCodeNoSea(int statusCode) {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode())
            .as("Status code NO deberia ser " + statusCode).isNotEqualTo(statusCode);
    }
}
