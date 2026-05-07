package com.qa.webcore.steps;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks de ciclo de vida de Cucumber para la capa Web.
 *
 * <p>Desde v2.1.0 el ciclo de vida del navegador se maneja en {@code WebPlugin}
 * usando Playwright. Estos hooks quedan enfocados en logging y evidencia.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebHooksSteps {

    private final WebHelper helper = new WebHelper();

    // =========================================================================
    // @Before — inicialización por escenario
    // =========================================================================

    @Before(value = "@web or @ui or @playwright or @browser", order = 100)
    public void beforeScenario(Scenario scenario) {
        String moduleName = ExecutionContext.current().
                map(ctx -> ctx.config().getProperty("framework.module.name", "PLATFORM")).
                orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "PLATFORM"));
        TestLogger.setFramework(moduleName);
        TestLogger.logInfo("WEB_HOOKS", "Escenario iniciado: " + scenario.getName(), null);
    }

    // =========================================================================
    // @After — limpieza por escenario
    // =========================================================================

    @After(value = "@web or @ui or @playwright or @browser", order = 100)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("WEB_HOOKS", "Finalizando escenario: " + scenario.getName(), null);
        try {
            if (scenario.isFailed()) {
                helper.captureScreenOnFailure(scenario);
            }
        } catch (Exception e) {
            TestLogger.logError("WEB_HOOKS", "Error en afterScenario: " + e.getMessage(), null);
        }
    }
}
