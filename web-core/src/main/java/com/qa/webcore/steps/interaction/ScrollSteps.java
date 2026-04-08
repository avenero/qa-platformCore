package com.qa.webcore.steps.interaction;

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
        helper.scroll(locator);
        helper.captureScreen(scenario);
    }

    @When("hago scroll hacia {string}")
    public void scrollDirection(String direction) {
        helper.scrollByDirection(direction);
        helper.captureScreen(scenario);
    }
}
