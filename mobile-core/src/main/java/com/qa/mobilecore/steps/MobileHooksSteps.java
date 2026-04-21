package com.qa.mobilecore.steps;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.driver.MobileDriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks de ciclo de vida de Cucumber para la capa mobile-core.
 *
 * <h2>Flujo de ejecución por escenario</h2>
 * <ol>
 *   <li>{@link #beforeScenario} — inicializa el contexto de logging; la sesión Appium
 *       se crea de forma lazy cuando el primer step llama a {@code MobileHelper.driver()}</li>
 *   <li>[Steps GIVEN/WHEN/THEN] — acceden al driver vía {@code MobileHelper}</li>
 *   <li>{@link #afterScenario} — captura screenshot si el escenario falló; el cierre
 *       del driver es responsabilidad del ciclo de vida SPI ({@code MobilePlugin.onScenarioEnd})
 *       cuando hay un {@link ExecutionContext} activo</li>
 * </ol>
 *
 * <h2>Nota sobre el cierre del driver</h2>
 * <p>Cuando hay un {@link ExecutionContext} activo (runtime con lifecycle engine),
 * el cierre autoritativo lo realiza {@link com.qa.mobilecore.plugin.MobilePlugin#onScenarioEnd}
 * vía {@link com.qa.mobilecore.driver.MobileDriverFactory#quitIfCreated()}.
 * Este hook solo cierra el driver como safety-net para escenarios ejecutados
 * sin el engine (ejecución directa con Cucumber runner standalone).
 *
 * @author Abel Venero
 * @since 2.0.0
 * @since 2.2.0 Cierre del driver delegado a MobilePlugin.onScenarioEnd en modo plugin
 */
public class MobileHooksSteps {

    private static final int HOOK_ORDER = 150;

    @Before(value = "@mobile or @ios or @android or @appium", order = HOOK_ORDER)
    public void beforeScenario(Scenario scenario) {
        String moduleName = ExecutionContext.current().
                map(ctx -> ctx.config().getProperty("framework.module.name", "MOBILE")).
                orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "MOBILE"));

        TestLogger.setFramework(moduleName);
        TestLogger.logInfo("MOBILE_HOOKS",
            "Iniciando escenario mobile: " + scenario.getName(), null);

        // El driver Appium se crea lazy cuando el primer step llame a MobileHelper.driver()
        // No forzar creación aquí — el pool y el servidor se inicializan en MobilePlugin.onScenarioStart()
    }

    /**
     * Hook de post-escenario.
     *
     * <p><b>Nota sobre el cierre del driver:</b>
     * Cuando hay un {@link ExecutionContext} activo (runtime con lifecycle engine),
     * el cierre autoritativo lo realiza {@link com.qa.mobilecore.plugin.MobilePlugin#onScenarioEnd}.
     * Este hook solo cierra el driver como safety-net para escenarios ejecutados
     * sin el engine (ejecución directa con Cucumber runner standalone).
     */
    @After(value = "@mobile or @ios or @android or @appium", order = HOOK_ORDER)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("MOBILE_HOOKS",
            "Finalizando escenario mobile: " + scenario.getName()
            + " | Estado: " + scenario.getStatus(), null);

        // 1. Captura de screenshot ante fallo (si el driver fue inicializado)
        if (scenario.isFailed() && MobileDriverManager.isInitialized()) {
            try {
                org.openqa.selenium.TakesScreenshot ts =
                    (org.openqa.selenium.TakesScreenshot) MobileDriverManager.getDriver();
                byte[] screenshot = ts.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                scenario.attach(screenshot, "image/png", "screenshot-failure");
                TestLogger.logInfo("MOBILE_HOOKS", "Screenshot de fallo adjunto al reporte", null);
            } catch (Exception e) {
                TestLogger.logWarning("MOBILE_HOOKS",
                    "No se pudo capturar screenshot: " + e.getMessage(), null);
            }
        }

        // 2. Safety-net: cierra el driver SOLO si NO hay ExecutionContext activo (modo standalone).
        //    Con ExecutionContext, MobilePlugin.onScenarioEnd se encarga del cierre autoritativo.
        if (ExecutionContext.current().isEmpty()) {
            MobileDriverManager.quitDriverSafely();
            TestLogger.logInfo("MOBILE_HOOKS", "Driver cerrado (modo standalone)", null);
        }

        TestLogger.clearTestContext();
    }
}
