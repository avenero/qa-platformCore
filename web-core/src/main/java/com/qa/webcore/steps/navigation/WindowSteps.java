package com.qa.webcore.steps.navigation;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

/**
 * Steps de gestion de ventanas y pestanas del navegador.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WindowSteps {

    private final WebHelper helper = new WebHelper();

    @When("vuelvo a la ventana principal")
    public void vuelvoALaVentanaPrincipal() {
        helper.backToPrincipalWindow();
    }

    @When("cierro la ventana")
    public void cierroLaVentana() {
        engine().closeCurrentWindow();
    }

    @When("cambio foco a la ventana nueva")
    public void cambiarPagina() {
        engine().switchToNewWindow();
    }

    @When("cambio de ventana")
    public void cambiodeVentana() {
        helper.handleTabs();
    }

    @When("valido si hay dos ventanas y cierro la ultima que se abrio")
    public void validoSiHayDosVentanasYCierroLaUltimaQueSeAbrio() {
        helper.closedLastWindows();
    }

    @Then("valido que se despliegue la nueva ventana {string}")
    public void validoQueSeDespliegueLaNuevaVentana(String expectedWindowName) {
        Assertions.assertThat(engine().getWindowTitle()).
            as("Error, los valores no son iguales").isEqualTo(expectedWindowName);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
