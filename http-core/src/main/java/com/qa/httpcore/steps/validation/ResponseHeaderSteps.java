package com.qa.httpcore.steps.validation;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.internal.runtime.ExecutionContext;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion de cabeceras de respuesta HTTP.
 * Nuevo componente introducido en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ResponseHeaderSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Then("valido que el header de respuesta {string} sea {string}")
    public void validoHeaderDeRespuesta(String header, String expectedValue) {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader(header)).
            as("Header '" + header + "' esperado: " + expectedValue).isEqualTo(expectedValue);
    }

    @Then("valido que el header de respuesta {string} contenga {string}")
    public void validoHeaderContenga(String header, String text) {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader(header)).
            as("Header '" + header + "' deberia contener: " + text).contains(text);
    }

    @Then("valido que la respuesta tenga Content-Type {string}")
    public void validoContentType(String contentType) {
        validoHeaderContenga("Content-Type", contentType);
    }

    @Then("valido que la respuesta tenga header Cache-Control")
    public void validoCacheControl() {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader("Cache-Control")).
            as("Se esperaba header Cache-Control").isNotNull();
    }

    @Then("valido que la respuesta tenga header Set-Cookie")
    public void validoSetCookie() {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader("Set-Cookie")).
            as("Se esperaba header Set-Cookie").isNotNull();
    }

    @Then("extraigo el header {string} y lo guardo como {string}")
    public void extraigoHeaderYGuardo(String header, String variable) {
        String headerValue = apiHelper().getLastResponse().getHeader(header);
        if (headerValue != null) {
            ExecutionContext.requireCurrent().variables().set(variable, headerValue);
        }
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Validación v2.2.1
    // =========================================================================

    /**
     * Valida que un header de respuesta NO esté presente.
     */
    @Then("valido que el header {string} NO esté presente")
    public void validoHeaderNoEstePresente(String header) {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader(header)).
            as("El header '%s' NO debería estar presente", header).isNull();
    }

    /**
     * Valida que el header de respuesta exista (sin importar su valor).
     */
    @Then("valido que el header {string} esté presente")
    public void validoHeaderEstePresente(String header) {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader(header)).
            as("El header '%s' debería estar presente", header).isNotNull();
    }
}
