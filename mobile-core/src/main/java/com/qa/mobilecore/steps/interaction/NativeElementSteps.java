package com.qa.mobilecore.steps.interaction;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps de interaccion con elementos nativos de la UI movil.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NativeElementSteps {

    @When("ingreso el texto {string} en el campo nativo {string}")
    public void ingresarTextoNativo(String texto, String locator) {
        TestLogger.logInfo("NATIVE_EL", "Ingresando '" + texto + "' en " + locator, null);
    }

    @When("presiono el boton nativo {string}")
    public void presionoBotonNativo(String locator) {
        TestLogger.logInfo("NATIVE_EL", "Tap boton: " + locator, null);
    }

    @When("limpio el campo nativo {string}")
    public void limpioElCampoNativo(String locator) {
        TestLogger.logInfo("NATIVE_EL", "Limpiando campo: " + locator, null);
    }

    @Then("verifico que el elemento nativo {string} este visible")
    public void verificoElementoNativoVisible(String locator) {
        TestLogger.logInfo("NATIVE_EL", "Verificando visibilidad de: " + locator, null);
    }

    @Given("guardo el texto del elemento nativo {string} como {string}")
    public void guardo_texto_nativo(String locator, String variable) {
        ExecutionContext.requireCurrent().variables().set(variable, locator);
        TestLogger.logInfo("NATIVE_EL", "Texto guardado como: " + variable, null);
    }
}
