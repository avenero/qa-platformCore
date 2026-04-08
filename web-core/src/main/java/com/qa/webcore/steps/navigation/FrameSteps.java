package com.qa.webcore.steps.navigation;

import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.When;

/**
 * Steps de cambio de contexto a frames e iFrames.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class FrameSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("cambio al IFrame path {string}")
    public void cambioIFramePath(String path) {
        helper.changeIFrame(path, "");
        helper.captureScreen(scenario);
    }

    @When("cambio de Iframe nombre {string}")
    public void cambioIframeNombre(String name) {
        helper.changeIFrame("", name);
        helper.captureScreen(scenario);
    }

    @When("inicializo Iframe principal")
    public void inicializarIframePrincipal() {
        helper.leaveIFrame();
    }

    @When("Selecciono el iframe con atributo css {string}")
    public void seleccionoElIframeConAtributoCss(String cssSelector) {
        helper.selectIframeByCssSelector(cssSelector);
    }
}
