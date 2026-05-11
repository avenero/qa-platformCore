package com.qa.webcore.steps.interaction;

import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.api.runtime.annotation.StepDef;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps de click e interacciones de puntero: click, doble click, derecho, JS, shadow DOM.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ClickSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("presiono el boton {string}")
    public void presionoElBoton(String locator) {
        engine().click(locator);
        helper.captureScreen(scenario);
    }

    @When("presiono tab en el elemento {string}")
    public void presionoTabEnElElemento(String locator) {
        helper.tabAction(locator);
    }

    @When("presiono el boton al doble contenedor {string} {string} con shadow elemento {string}")
    public void presionoElBotonAlDobleContenedorConShadowElemento(String h1, String h2, String el) {
        helper.clickNestedShadow(h1, h2, el);
    }

    @When("presiono el boton {string} que se encuentra dentro del shadow {string}")
    public void presionoElBotonDentroDelShadow(String elemento, String shadowHost) {
        helper.clickElementInShadow(elemento, shadowHost);
    }

    @When("hago click de js al elemento {string}")
    public void hagoClickDeJsAlElemento(String locator) {
        helper.clickJs(locator);
    }

    @When("realizo click derecho en elemento {string}")
    public void realizarClickDerecho(String locator) {
        helper.rightClick(locator);
    }

    @When("situo el cursor del mouse sobre el elemento {string}")
    public void situoElCursorDelMouseSobreElElemento(String locator) {
        engine().hover(locator);
    }

    @StepDef(value = "web.click.legacy-cerrar-banner",
             deprecated = true, replacedBy = "web.click#presionoLaTecla",
             displayName = "cerrar banner lateral (DEPRECATED — usar presiono la tecla)")
    @And("cerrar banner lateral")
    public void cerrarBannerShadowHome() {
        helper.doClicTeclaESC();
    }

    /**
     * Presiona una tecla del teclado (Enter, Escape, Tab, etc.) — step genérico.
     */
    @When("presiono la tecla {string}")
    public void presionoLaTecla(String tecla) {
        helper.pressKey(tecla);
    }

    /**
     * Presiona una tecla del teclado en un elemento específico.
     */
    @When("presiono la tecla {string} en el elemento {string}")
    public void presionoLaTeclaEnElElemento(String tecla, String locator) {
        helper.pressKeyOnElement(tecla, locator);
    }

    /**
     * Hace doble clic en un elemento.
     */
    @When("hago doble clic en el elemento {string}")
    public void hagoDobleClicEnElElemento(String locator) {
        engine().doubleClick(locator);
    }

    @And("verifico si existe el elemento {string} con host {string} y hago clic")
    public void verificoSiExisteElementoShadowYHagoClic(String elemento, String host) {
        helper.clickAndGoToElementInShadow(elemento, host);
        helper.captureScreen(scenario);
    }

    @And("verifico si existe el elemento {string} con host principal {string}, host secundario {string} y hago clic")
    public void verificoSiExisteElementoConHostDentroDeOtroHostYHagoClic(String el, String h1, String h2) {
        helper.clickAndGoToElementInShadowInNode(el, h1, h2);
        helper.captureScreen(scenario);
    }

    @Then("verifico si existe el elemento {string} y hago clic")
    public void verificoSiExisteElElementoYHagoClic(String locator) {
        if (engine().isPresent(locator)) {
            presionoElBoton(locator);
        }
    }

    @Then("verifico si existe el texto {string} y hago clic")
    public void verificoSiExisteElTextoYHagoClic(String texto) {
        helper.checkTextAndClic(texto);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
