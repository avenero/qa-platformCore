package com.qa.webcore.steps.interaction;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import java.util.List;

/**
 * Steps de seleccion: combobox, radio button, checkbox, validacion de opciones.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class SelectSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("selecciono el valor {string} en el combobox {string}")
    public void seleccionoElTextoEnElCombobox(String valor, String locator) {
        engine().selectOption(locator, valor);
        helper.captureScreen(scenario);
    }

    @When("selecciono el valor de la variable {string} en el combobox {string}")
    public void seleccionoElTextoDeVariableEnElCombobox(String variableName, String locator) {
        engine().selectOption(locator, helper.getTextVariableTemp(variableName));
        helper.captureScreen(scenario);
    }

    @And("selecciono la opcion con el valor {string} en el combobox {string}")
    public void seleccionoLaOpcionConElValorEnElCombobox(String valor, String locator) {
        engine().selectOption(locator, valor);
        helper.captureScreen(scenario);
    }

    @When("selecciono el RadioButon con Valor {string}")
    public void seleccionRadioButonValor(String valor) {
        // Placeholder para implementacion de radio button
    }

    @When("espero que el checkbox {string} salga seleccionado")
    public void checkboxwait(String locator) {
        helper.waitCheckBox(locator);
    }

    @Then("verifico si existe el combobox {string} y selecciono el valor {string}")
    public void verificoSiExisteElElementoYSeleccionoOpcion(String locator, String valor) {
        if (engine().isPresent(locator)) {
            engine().selectOption(locator, valor);
        }
    }

    @Then("verifico que la lista de opciones para el combobox {string} sea")
    public void verificoQueLaListaDeOpcionesParaElComboboxSea(String locator, List<String> expectedOptions) {
        Assertions.assertThat(helper.getListComboBox(locator)).
            as("Lista de opciones no coincide").isEqualTo(expectedOptions);
        helper.captureScreen(scenario);
    }

    @Then("verifico el texto del combobox {string} sea {string}")
    public void verificoElTextoDelComboboxSea(String locator, String expectedText) {
        Assertions.assertThat(helper.getTextOfCombobox(locator)).as("Texto no coincide").isEqualTo(expectedText);
    }

    @Then("verifico que el check {string} este seleccionado")
    public void verificoQueElCheckEsteSeleccionado(String locator) {
        Assertions.assertThat(helper.radioBtnIsSelect(locator)).
            as("El elemento " + locator + " no esta seleccionado!").isTrue();
    }

    @Then("las opciones del campo {string} deben ser {string}")
    public void lasOpcionesDelCampoDebenSer(String locator, String expectedOptions) {
        helper.validateDropdownOptions(locator, expectedOptions);
    }

    @Then("el campo {string} debe tener {int} opciones")
    public void elCampoDebeTenerNOpciones(String locator, int expectedCount) {
        helper.validateDropdownOptionCount(locator, expectedCount);
    }

    @Then("el campo {string} debe permitir seleccion unica")
    public void elCampoDebePermitirSeleccionUnica(String locator) {
        helper.validateSingleSelection(locator);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
