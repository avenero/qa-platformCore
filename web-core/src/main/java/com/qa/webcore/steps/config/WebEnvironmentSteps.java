package com.qa.webcore.steps.config;

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
        helper.setCookie(nombre, valor);
    }
}
