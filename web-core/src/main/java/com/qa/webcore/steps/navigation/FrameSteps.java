package com.qa.webcore.steps.navigation;

import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
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

    private static final String CURRENT_FRAME_SELECTOR_VAR = "web.frame.current.selector";
    private static final String FRAME_ROOT_ELEMENT_SELECTOR = "body";

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("cambio al IFrame path {string}")
    public void cambioIFramePath(String path) {
        String frameSelector = normalizeFrameSelector(path);
        engine().findInFrame(frameSelector, FRAME_ROOT_ELEMENT_SELECTOR);
        ExecutionContext.requireCurrent().variables().set(CURRENT_FRAME_SELECTOR_VAR, frameSelector);
        helper.captureScreen(scenario);
    }

    @When("cambio de Iframe nombre {string}")
    public void cambioIframeNombre(String name) {
        String cleanName = requireNonBlank(name, "Nombre de iframe no puede ser vacío");
        String frameSelector = "iframe[name='" + cleanName + "'], iframe#" + cleanName;
        engine().findInFrame(frameSelector, FRAME_ROOT_ELEMENT_SELECTOR);
        ExecutionContext.requireCurrent().variables().set(CURRENT_FRAME_SELECTOR_VAR, frameSelector);
        helper.captureScreen(scenario);
    }

    @When("inicializo Iframe principal")
    public void inicializarIframePrincipal() {
        ExecutionContext.requireCurrent().variables().remove(CURRENT_FRAME_SELECTOR_VAR);
    }

    @When("Selecciono el iframe con atributo css {string}")
    public void seleccionoElIframeConAtributoCss(String cssSelector) {
        String frameSelector = normalizeFrameSelector(cssSelector);
        engine().findInFrame(frameSelector, FRAME_ROOT_ELEMENT_SELECTOR);
        ExecutionContext.requireCurrent().variables().set(CURRENT_FRAME_SELECTOR_VAR, frameSelector);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }

    private static String normalizeFrameSelector(String selector) {
        String clean = requireNonBlank(selector, "Selector de frame no puede ser vacío");
        if (clean.startsWith("xpath=") || clean.startsWith("//") || clean.startsWith("(//")) {
            return clean.startsWith("xpath=") ? clean : "xpath=" + clean;
        }
        return clean;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
