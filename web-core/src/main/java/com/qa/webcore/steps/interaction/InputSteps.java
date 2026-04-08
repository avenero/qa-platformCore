package com.qa.webcore.steps.interaction;

import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps de entrada de texto: escribir, limpiar, variables temporales.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class InputSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("ingreso el texto {string} en el elemento {string}")
    public void ingresoElTextoEnElElemento(String texto, String locator) {
        helper.setTextWithWait(texto, locator);
        helper.captureScreen(scenario);
    }

    @When("ingreso texto de la variable temporal {string} en elemento {string}")
    public void setTextoVariableTemporalEnElemento(String variableName, String locator) {
        helper.setText(locator, helper.getTextVariableTemp(variableName));
    }

    @When("Ingreso nombre aleatorio en el elemento {string}")
    public void nombreAleatorio(String locator) {
        String ts = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        ingresoElTextoEnElElemento(ts, locator);
    }

    @Then("verifico si existe el elemento {string} e ingreso el texto {string}")
    public void verificoSiExisteElElementoYIngresoTexto(String locator, String texto) {
        if (helper.waitForVisibleElement(locator)) {
            helper.setText(locator, texto);
        }
    }
}
