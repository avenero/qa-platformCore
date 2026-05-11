package com.qa.httpcore.steps.validation;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.annotation.StepDef;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validación del código de estado HTTP.
 *
 * <p>Componente padre: {@code api.status}
 * ({@link com.qa.httpcore.components.ApiStatusCodeComponent}).
 * Fase BDD: THEN.
 *
 * <p>Todos los steps canónicos llevan {@link StepDef} con ID explícito para garantizar
 * estabilidad frente a refactorizaciones. El formato es {@code api.status.{sub-id}}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class StatusCodeSteps {

    /** Upper bound (inclusive) of 2xx success status codes. */
    private static final int STATUS_2XX_MAX = 299;
    /** Upper bound (inclusive) of 4xx client error status codes. */
    private static final int STATUS_4XX_MAX = 499;
    /** Upper bound (inclusive) of 5xx server error status codes. */
    private static final int STATUS_5XX_MAX = 599;

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    // =========================================================================
    // Steps canónicos — validación de status code
    // =========================================================================

    /**
     * Valida que el código de respuesta HTTP sea exactamente el esperado.
     */
    @StepDef(value = "api.status.exact",
             displayName = "Validar código HTTP de respuesta")
    @Then("valido que el código HTTP de respuesta sea {int}")
    public void validoQueElCodigoHttpDeRespuestaSea(int statusCode)
            throws FrameworkBusinessException {
        apiHelper().validateResponseStatusCode(statusCode);
    }

    /**
     * Valida que la respuesta sea exitosa (código 2xx: 200–299).
     */
    @StepDef(value = "api.status.success",
             displayName = "Validar respuesta exitosa (2xx)")
    @Then("valido que la respuesta sea exitosa \\(2xx\\)")
    public void validoQueLaRespuestaSeaExitosa() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status 2xx (200-299)").isBetween(200, STATUS_2XX_MAX);
    }

    /**
     * Valida que la respuesta sea un error de cliente (código 4xx: 400–499).
     */
    @StepDef(value = "api.status.client-error",
             displayName = "Validar error de cliente (4xx)")
    @Then("valido que la respuesta sea error de cliente \\(4xx\\)")
    public void validoQuelaRespuestaSeaErrorDeCliente() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status 4xx (400-499)").isBetween(400, STATUS_4XX_MAX);
    }

    /**
     * Valida que la respuesta sea un error de servidor (código 5xx: 500–599).
     */
    @StepDef(value = "api.status.server-error",
             displayName = "Validar error de servidor (5xx)")
    @Then("valido que la respuesta sea error de servidor \\(5xx\\)")
    public void validoQuelaRespuestaSeaErrorDeServidor() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status 5xx (500-599)").isBetween(500, STATUS_5XX_MAX);
    }

    /**
     * Valida que el código de respuesta esté dentro de un rango numérico inclusivo.
     */
    @StepDef(value = "api.status.range",
             displayName = "Validar código de respuesta en rango")
    @Then("valido que el código de respuesta esté entre {int} y {int}")
    public void validoQueElCodigoDeRespuestaEsteEntre(int min, int max) {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status entre " + min + " y " + max).isBetween(min, max);
    }

    /**
     * Valida que el código de respuesta NO sea el indicado.
     */
    @StepDef(value = "api.status.not",
             displayName = "Validar que código de respuesta NO sea el indicado")
    @Then("valido que el código de respuesta NO sea {int}")
    public void validoQueElCodigoDeRespuestaNoSea(int statusCode) {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Código de respuesta NO debería ser " + statusCode).isNotEqualTo(statusCode);
    }
}
