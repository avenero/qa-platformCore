package com.qa.webcore.steps.wait;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.webcore.utils.WebHelper;
import com.qa.webcore.utils.WaitUtils;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

/**
 * Steps de espera explicita e implicita.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WaitSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("espero hasta que elemento {string} este visible")
    public void esperarHastaQueElementoEsteVisible(String locator) {
        helper.waitForVisibleElement(locator, 60);
    }

    @When("espero hasta que elemento {string} este habilitado")
    public void esperarHastaQueElementoEsteHabilitado(String locator) {
        helper.waitAndValidateEnabled(locator, 60);
    }

    @When("espero hasta que elemento {string} no este visible")
    public void esperarHastaQueElementoNoEsteVisible(String locator) {
        helper.waitFromElementNoVisible(locator);
        helper.captureScreen(scenario);
    }

    @When("espero {string} segundos")
    public void esperarUnTiempo(String segundos) {
        WaitUtils.waitMilliseconds(Integer.parseInt(segundos) * 1000L);
        helper.captureScreen(scenario);
        TestLogger.logWarning("WAIT_STEPS", "Wait fijo: " + segundos + "s", null);
    }

    @When("espero hasta que la seccion termine de cargar")
    public void esperoHastaQueLaSeccionTermineDeCargar() {
        helper.captureScreen(scenario);
        helper.waitForPageLoad(90);
    }

    @StepDef(value = "web.wait.legacy-esperar-home",
             deprecated = true,
             displayName = "esperar que se muestre home (DEPRECATED — step de negocio)")
    @And("esperar que se muestre home")
    public void esperarQueSeMuestreHome() {
        helper.waitForHome();
        helper.captureScreen(scenario);
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Waits v2.2.1
    // =========================================================================

    /**
     * Espera hasta que el elemento desaparezca (alias más claro).
     */
    @When("espero hasta que el elemento {string} desaparezca")
    public void esperarHastaQueElElementoDesaparezca(String locator) {
        helper.waitFromElementNoVisible(locator);
    }

    /**
     * Espera hasta que un texto sea visible en la página.
     */
    @When("espero hasta que el texto {string} sea visible en la página")
    public void esperarHastaQueElTextoSeaVisible(String text) {
        helper.waitForTextVisible(text, 15);
    }

    /**
     * Espera un número específico de segundos (int, más natural que string).
     */
    @When("espero {int} segundos")
    public void esperarSegundosInt(int segundos) {
        WaitUtils.waitMilliseconds(segundos * 1000L);
        TestLogger.logWarning("WAIT_STEPS", "Wait fijo: " + segundos + "s", null);
    }
}
