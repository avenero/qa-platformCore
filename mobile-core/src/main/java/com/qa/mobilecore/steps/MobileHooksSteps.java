package com.qa.mobilecore.steps;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.driver.MobileDriverManager;
import com.qa.mobilecore.helper.MobileHelper;
import com.qa.mobilecore.model.DeviceDescriptor;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks de ciclo de vida de Cucumber para la capa mobile-core.
 *
 * <p><b>Flujo de ejecución por escenario:</b>
 * <ol>
 *   <li>{@link #beforeScenario} — inicializa contexto de logging; la sesión Appium
 *       se crea de forma lazy cuando el primer step la necesita</li>
 *   <li>[Steps GIVEN/WHEN/THEN] — acceden al driver vía {@code MobileHelper}</li>
 *   <li>{@link #afterScenario} — captura screenshot si el escenario falló, luego
 *       cierra la sesión Appium y libera el dispositivo del pool</li>
 * </ol>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class MobileHooksSteps {

    @Before(value = "@mobile or @ios or @android or @appium", order = 150)
    public void beforeScenario(Scenario scenario) {
        String moduleName = ExecutionContext.current()
                .map(ctx -> ctx.config().getProperty("framework.module.name", "MOBILE"))
                .orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "MOBILE"));

        TestLogger.setFramework(moduleName);
        TestLogger.logInfo("MOBILE_HOOKS",
            "Iniciando escenario mobile: " + scenario.getName(), null);

        // El driver Appium se crea lazy cuando el primer step llame a MobileHelper.driver()
        // No forzar creación aquí — el pool y el servidor se inicializan en MobilePlugin.onScenarioStart()
    }

    @After(value = "@mobile or @ios or @android or @appium", order = 150)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("MOBILE_HOOKS",
            "Finalizando escenario mobile: " + scenario.getName()
            + " | Estado: " + scenario.getStatus(), null);

        // Captura de screenshot ante fallo (si el driver fue inicializado)
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

        // Cierre de sesión via MobileHelper (libera pool + quit driver)
        ExecutionContext.current()
            .flatMap(ctx -> ctx.registry().get(MobileHelper.class))
            .ifPresentOrElse(
                MobileHelper::quitSession,
                () -> {
                    // Si MobileHelper no fue inicializado (escenario sin steps mobile),
                    // asegurar que el driver quede limpio de todas formas
                    MobileDriverManager.quitDriverSafely();
                }
            );

        TestLogger.clearTestContext();
    }
}
