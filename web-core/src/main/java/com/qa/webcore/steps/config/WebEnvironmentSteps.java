package com.qa.webcore.steps.config;

import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.And;

/**
 * Steps de configuracion del ambiente web: cookies, URL, timeouts.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebEnvironmentSteps {

    private final WebHelper helper = new WebHelper();

    @And("seteo cookie: {string} con valor {string}")
    public void seteoCookie(String nombre, String valor) {
        if (ExecutionContext.current().isPresent()) {
            engine().evaluate(
                "document.cookie = `${arguments[0]}=${arguments[1]}; path=/`;",
                nombre, valor
            );
            return;
        }
        // Fallback standalone para no romper ejecuciones legacy fuera de lifecycle engine.
        helper.setCookie(nombre, valor);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
