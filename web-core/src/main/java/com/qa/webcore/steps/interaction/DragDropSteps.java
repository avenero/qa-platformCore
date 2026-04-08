package com.qa.webcore.steps.interaction;

import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.When;

/**
 * Steps de drag and drop.
 * Nuevo componente en Fase 2 (placeholder para implementacion futura).
 * @author Abel Venero
 * @since 2.0.0
 */
public class DragDropSteps {

    private final WebHelper helper = new WebHelper();

    @When("arrastro el elemento {string} hacia {string}")
    public void arrastroElementoHacia(String source, String target) {
        helper.dragAndDrop(source, target);
    }

    @When("redimensiono el elemento {string} a ancho {int} alto {int}")
    public void redimensionoElemento(String locator, int width, int height) {
        helper.resizeElement(locator, width, height);
    }
}
