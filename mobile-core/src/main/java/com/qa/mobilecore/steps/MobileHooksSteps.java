package com.qa.mobilecore.steps;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks de ciclo de vida de Cucumber para la capa Mobile.
 * Inicializa el driver Appium y lo libera al finalizar el escenario.
 * @author QA Frameworks Core
 * @since 2.0.0
 */
public class MobileHooksSteps {

    @Before(value = "@mobile or @ios or @android or @appium", order = 150)
    public void beforeScenario(Scenario scenario) {
        // Preferir ExecutionContext.config() (por ejecución, safe en paralelo) → fallback a ConfigManager
        String moduleName = ExecutionContext.current()
                .map(ctx -> ctx.config().getProperty("framework.module.name", "PLATFORM"))
                .orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "PLATFORM"));
        TestLogger.setFramework(moduleName);
        TestLogger.logInfo("MOBILE_HOOKS", "Escenario iniciado: " + scenario.getName(), null);
        // TODO: inicializar AppiumDriver aquí
    }

    @After(value = "@mobile or @ios or @android or @appium", order = 150)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("MOBILE_HOOKS", "Finalizando escenario mobile", null);
        // TODO: cerrar AppiumDriver aquí
    }
}
