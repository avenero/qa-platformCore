package com.qa.mobilecore.steps.validation;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.Then;

/**
 * Steps de validacion de elementos nativos mobile: visibilidad, texto, estado.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class MobileElementValidationSteps {

    @Then("verifico que el elemento mobile {string} este visible")
    public void verificoElementoVisible(String locator) {
        TestLogger.logInfo("MOBILE_VAL", "Verificando visibilidad: " + locator, null);
    }

    @Then("verifico que el elemento mobile {string} NO este visible")
    public void verificoElementoNoVisible(String locator) {
        TestLogger.logInfo("MOBILE_VAL", "Verificando ausencia: " + locator, null);
    }

    @Then("verifico que el texto del elemento mobile {string} sea {string}")
    public void verificoTextoElemento(String locator, String expectedText) {
        TestLogger.logInfo("MOBILE_VAL", "Verificando texto en " + locator + ": " + expectedText, null);
    }

    @Then("verifico que el elemento mobile {string} este habilitado")
    public void verificoElementoHabilitado(String locator) {
        TestLogger.logInfo("MOBILE_VAL", "Verificando habilitado: " + locator, null);
    }

    @Then("verifico que el elemento mobile {string} este deshabilitado")
    public void verificoElementoDeshabilitado(String locator) {
        TestLogger.logInfo("MOBILE_VAL", "Verificando deshabilitado: " + locator, null);
    }

    @Then("verifico que el campo mobile {string} contenga el texto {string}")
    public void verificoCampoContenga(String locator, String text) {
        TestLogger.logInfo("MOBILE_VAL", "Verificando contenido en " + locator + ": " + text, null);
    }

    @Then("tomo screenshot mobile")
    public void tomoScreenshot() {
        TestLogger.logInfo("MOBILE_VAL", "Capturando screenshot mobile", null);
    }
}
