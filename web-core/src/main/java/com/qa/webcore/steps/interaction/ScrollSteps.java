package com.qa.webcore.steps.interaction;

import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.When;

/**
 * Steps de scroll en la pagina y hacia elementos.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ScrollSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("hago scroll hasta el elemento {string}")
    public void irAlElemento(String locator) {
        engine().scrollIntoView(locator);
        helper.captureScreen(scenario);
    }

    @When("hago scroll hacia {string}")
    public void scrollDirection(String direction) {
        String normalized = direction == null ? "" : direction.trim().toLowerCase();
        switch (normalized) {
            case "arriba", "up", "top" ->
                    engine().evaluate("window.scrollTo({ top: 0, behavior: 'smooth' });");
            case "abajo", "down", "bottom" ->
                    engine().evaluate("window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });");
            default ->
                    throw new IllegalArgumentException("Dirección de scroll no soportada: " + direction);
        }
        helper.captureScreen(scenario);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
