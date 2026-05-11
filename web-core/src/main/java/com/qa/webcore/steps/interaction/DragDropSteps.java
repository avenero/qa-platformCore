package com.qa.webcore.steps.interaction;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import io.cucumber.java.en.When;

/**
 * Steps de drag and drop.
 * Nuevo componente en Fase 2 (placeholder para implementacion futura).
 * @author Abel Venero
 * @since 2.0.0
 */
public class DragDropSteps {

    @When("arrastro el elemento {string} hacia {string}")
    public void arrastroElementoHacia(String source, String target) {
        engine().evaluate(
                "const src = document.querySelector(arguments[0]);"
                        + "const dst = document.querySelector(arguments[1]);"
                        + "if (!src || !dst) { throw new Error('No se encontró source/target para drag and drop'); }"
                        + "const dt = new DataTransfer();"
                        + "src.dispatchEvent(new DragEvent('dragstart', { dataTransfer: dt, bubbles: true }));"
                        + "dst.dispatchEvent(new DragEvent('dragover', { dataTransfer: dt, bubbles: true }));"
                        + "dst.dispatchEvent(new DragEvent('drop', { dataTransfer: dt, bubbles: true }));"
                        + "src.dispatchEvent(new DragEvent('dragend', { dataTransfer: dt, bubbles: true }));",
                source, target
        );
    }

    @When("redimensiono el elemento {string} a ancho {int} alto {int}")
    public void redimensionoElemento(String locator, int width, int height) {
        engine().evaluate(
                "const el = document.querySelector(arguments[0]);"
                        + "if (!el) { throw new Error('No se encontró elemento para resize'); }"
                        + "el.style.width = arguments[1] + 'px';"
                        + "el.style.height = arguments[2] + 'px';",
                locator, width, height
        );
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
