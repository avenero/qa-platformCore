package com.qa.webcore.steps.validation;

import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion y navegacion de tablas HTML.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class TableValidationSteps {

    private final WebHelper helper = new WebHelper();

    @When("recorro tabla {string} y selecciono la fila que tenga el valor {string} en la columna que tenga el valor {string}")
    public void recorroTablaYSeleccionoLaFilaQueTengaElValorEnLaColumna(String tabla, String valor, String columna) {
        Assertions.assertThat(helper.selectRowTable(tabla, valor, columna))
            .as("Error buscando el valor en la tabla").isEqualTo("OK");
    }

    @Then("valido que las cabeceras de la tabla {string} sean {string}")
    public void validarQueLasCabecerasDeLaTablaSean(String tabla, String expectedHeaders) {
        Assertions.assertThat(helper.validateHeadTable(tabla, expectedHeaders)).isEqualTo("OK");
    }

    @Then("verifico que la suma de las variables temporales {string} y {string} sea igual al valor de la variable temporal {string}")
    public void validarSumaVariablesTemporales(String var1, String var2, String varResult) {
        Assertions.assertThat(helper.validateAdditionVariables(var1, var2, varResult))
            .as("La suma no es igual al resultado esperado").isTrue();
    }
}
