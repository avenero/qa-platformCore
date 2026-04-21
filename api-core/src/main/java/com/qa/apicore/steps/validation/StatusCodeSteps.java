package com.qa.apicore.steps.validation;

import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validación del código de estado HTTP.
 *
 * <p>Componente padre: {@code api.status}
 * ({@link com.qa.apicore.components.ApiStatusCodeComponent}).
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
     * Es el step de validación de status más utilizado.
     */
    @StepDef(value = "api.status.exact",
             displayName = "Validar código de respuesta exacto")
    @Then("valido que el codigo de respuesta del servicio sea {int}")
    public void validoQueElCodigoDeRespuestaDelServicioSea(int statusCode)
            throws FrameworkBusinessException {
        apiHelper().validateResponseStatusCode(statusCode);
    }

    /**
     * Valida que la respuesta sea exitosa (código 2xx: 200–299).
     */
    @StepDef(value = "api.status.success",
             displayName = "Validar respuesta exitosa (2xx)")
    @Then("valido que el servicio responda con éxito")
    public void validoExito() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status 2xx").isBetween(200, STATUS_2XX_MAX);
    }

    /**
     * Valida que la respuesta sea un error de cliente (código 4xx: 400–499).
     */
    @StepDef(value = "api.status.client-error",
             displayName = "Validar error de cliente (4xx)")
    @Then("valido que el servicio responda con error de cliente")
    public void validoErrorCliente() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status 4xx").isBetween(400, STATUS_4XX_MAX);
    }

    /**
     * Valida que la respuesta sea un error de servidor (código 5xx: 500–599).
     */
    @StepDef(value = "api.status.server-error",
             displayName = "Validar error de servidor (5xx)")
    @Then("valido que el servicio responda con error de servidor")
    public void validoErrorServidor() {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status 5xx").isBetween(500, STATUS_5XX_MAX);
    }

    /**
     * Valida que el código de respuesta esté dentro de un rango numérico inclusivo.
     */
    @StepDef(value = "api.status.range",
             displayName = "Validar código en rango")
    @Then("valido que el status code esté entre {int} y {int}")
    public void validoStatusCodeEnRango(int min, int max) {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Se esperaba status entre " + min + " y " + max).isBetween(min, max);
    }

    /**
     * Valida que el código de respuesta NO sea el indicado.
     * Útil para verificar que ciertos errores no ocurren.
     */
    @StepDef(value = "api.status.not",
             displayName = "Validar que código NO sea el indicado")
    @Then("valido que el status code NO sea {int}")
    public void validoStatusCodeNoSea(int statusCode) {
        Assertions.assertThat(apiHelper().getLastResponse().getStatusCode()).
            as("Status code NO deberia ser " + statusCode).isNotEqualTo(statusCode);
    }
}
