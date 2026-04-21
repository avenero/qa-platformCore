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

    private static final String STEP_SELECT_TABLE_ROW =
        "recorro tabla {string} y selecciono la fila"
        + " que tenga el valor {string} en la columna que tenga el valor {string}";

    private static final String STEP_VALIDATE_SUM =
        "verifico que la suma de las variables temporales {string} y {string}"
        + " sea igual al valor de la variable temporal {string}";

    private final WebHelper helper = new WebHelper();

    @When(STEP_SELECT_TABLE_ROW)
    public void recorroTablaYSeleccionoLaFilaQueTengaElValorEnLaColumna(
            String tabla, String valor, String columna) {
        Assertions.assertThat(helper.selectRowTable(tabla, valor, columna)).
            as("Error buscando el valor en la tabla").isEqualTo("OK");
    }

    @Then("valido que las cabeceras de la tabla {string} sean {string}")
    public void validarQueLasCabecerasDeLaTablaSean(String tabla, String expectedHeaders) {
        Assertions.assertThat(helper.validateHeadTable(tabla, expectedHeaders)).isEqualTo("OK");
    }

    @Then(STEP_VALIDATE_SUM)
    public void validarSumaVariablesTemporales(String var1, String var2, String varResult) {
        Assertions.assertThat(helper.validateAdditionVariables(var1, var2, varResult)).
            as("La suma no es igual al resultado esperado").isTrue();
    }
}
