package com.qa.webcore.steps.validation;

import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion de pagina: URL, titulo, texto en pagina, variables.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class PageValidationSteps {

    private final WebHelper helper = new WebHelper();

    @Then("valido que la url abierta sea {string}")
    public void validoUrl(String expectedUrl) {
        helper.urlValid(expectedUrl);
    }

    @Then("valido que exista el texto")
    public void validoQueExistaElTexto(String texto) {
        Assertions.assertThat(helper.existText(texto))
            .as("El texto '" + texto + "' no existe").isTrue();
    }

    @Then("valido que la variable {string} sea igual a la variable {string}")
    public void validoQueLaVariableSeaIgualALaVariable(String varName1, String varName2) {
        Assertions.assertThat(helper.getTextVariableTemp(varName1))
            .as("Las variables no son iguales").isEqualTo(helper.getTextVariableTemp(varName2));
    }
}
