package com.qa.webcore.steps;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.annotation.StepDef;
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

    @StepDef(value = "web.variables.legacy-genero-rut",
             deprecated = true,
             displayName = "genero RUT (DEPRECATED — step de dominio específico, no pertenece al core)")
    @When("genero RUT y guardo en variable {string}")
    public void generateRut(String variableName) {
        helper.saveVariableTemp(helper.generateRutUy(), variableName);
    }

    @StepDef(value = "web.variables.legacy-busco-doc-hb",
             deprecated = true,
             displayName = "busco documento en HB (DEPRECATED — step de negocio, no pertenece al core)")
    @When("busco un documento que no exista en la bbdd de homebanking y guardo en {string}")
    public void buscoDocumentoValidoHb(String variableName) {
        TestLogger.logInfo("WEB_VAR_STEPS", "Buscando documento valido en HB", null);
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Variables v2.2.1
    // =========================================================================

    /**
     * Guarda el valor de un atributo de un elemento como variable temporal.
     */
    @When("guardo el valor del atributo {string} del elemento {string} como {string}")
    public void guardoAtributoComoVariable(String attribute, String locator, String variableName) {
        String value = helper.getAttributeValue(locator, attribute);
        helper.saveVariableTemp(value, variableName);
        TestLogger.logInfo("WEB_VAR_STEPS",
            "Atributo '" + attribute + "' de '" + locator + "' guardado como '" + variableName + "'", null);
    }
}
