package com.qa.webcore.steps;

import com.qa.common.logging.TestLogger;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.When;

/**
 * Steps transversales de gestion de variables temporales para la capa Web.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebVariableSteps {

    private final WebHelper helper = new WebHelper();

    @When("guardo texto del elemento {string} en variable temporal llamada {string}")
    public void guardarTextoDelElementoEnVariableTemporalLlamada(String locator, String variableName) {
        helper.saveVariableTemp(helper.getTextOf(locator), variableName);
    }

    @When("guardo texto {string} en variable temporal llamada {string}")
    public void guardarTextoVariableTemporalLlamada(String texto, String variableName) {
        helper.saveVariableTemp(texto, variableName);
    }

    @When("genero RUT y guardo en variable {string}")
    public void generateRut(String variableName) {
        helper.saveVariableTemp(helper.generateRutUy(), variableName);
    }

    @When("busco un documento que no exista en la bbdd de homebanking y guardo en {string}")
    public void buscoDocumentoValidoHb(String variableName) {
        TestLogger.logInfo("WEB_VAR_STEPS", "Buscando documento valido en HB", null);
    }
}
