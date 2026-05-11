package com.qa.webcore.steps.interaction;

import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;

/**
 * Steps para alertas y dialogos del navegador.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class AlertSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @Then("verifico si existe la alerta y selecciono aceptar")
    public void verificoSiExisteAlertaYSeleccionoAceptar() {
        helper.captureScreen(scenario);
        engine().acceptAlert();
    }

    @Then("acepto la alerta")
    public void aceptoLaAlerta() {
        engine().acceptAlert();
    }

    @Then("rechazo la alerta")
    public void rechazoLaAlerta() {
        engine().dismissAlert();
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
