package com.qa.webcore.steps.config;

import com.qa.common.exception.FrameworkBusinessException;
import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.config.WebConfigKeys;
import com.qa.webcore.driver.WebDriverFactory.BrowserType;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.Given;

/**
 * Steps de configuracion del navegador.
 * Migrado de WebSteps.java - seccion GIVEN configuracion de driver.
 * @author Abel Venero
 * @since 2.0.0
 */
public class BrowserConfigSteps {

    private final WebHelper helper = new WebHelper();

    @Given("configuro el driver del navegador {string} en modo headless {string}")
    public void configurarDriverDelNavegador(String browserName, String headlessStr)
            throws FrameworkBusinessException {
        BrowserType browser = helper.parseBrowserType(browserName);
        boolean headless = helper.parseBoolean(headlessStr);
        ExecutionContext.requireCurrent().variables().set(WebConfigKeys.BROWSER_RUNTIME_VAR, browser);
        ExecutionContext.requireCurrent().variables().set(WebConfigKeys.HEADLESS_RUNTIME_VAR, headless);
    }
}
