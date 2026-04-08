package com.qa.webcore.steps;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.driver.DriverManager;
import com.qa.webcore.driver.WebDriverFactory;
import com.qa.webcore.driver.WebDriverFactory.BrowserType;
import com.qa.webcore.utils.WebHelper;
import com.qa.webcore.utils.WaitUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

/**
 * Hooks de ciclo de vida de Cucumber para la capa Web.
 *
 * <p>Extrae los @Before y @After de WebSteps para mantener SRP.
 * Inicializa y libera el WebDriver por escenario.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebHooksSteps {

    private final WebHelper helper = new WebHelper();

    @Before(value = "@web or @ui or @selenium or @browser", order = 100)
    public void beforeScenario(Scenario scenario) {
        // Preferir ExecutionContext.config() (por ejecución, safe en paralelo) → fallback a ConfigManager
        String moduleName = ExecutionContext.current()
                .map(ctx -> ctx.config().getProperty("framework.module.name", "PLATFORM"))
                .orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "PLATFORM"));
        TestLogger.setFramework(moduleName);

        if (!DriverManager.isDriverInitialized()) {
            BrowserType browser = getBrowserForScenario();
            boolean headless = getHeadlessModeForScenario();
            WebDriver driver = WebDriverFactory.createDriver(browser, headless);
            DriverManager.setDriver(driver);
            TestLogger.logInfo("WEB_HOOKS",
                "Driver inicializado",
                java.util.Map.of("browser", browser.name(), "headless", headless));
        }

        WebDriver driver = DriverManager.getDriver();
        String host = helper.getConfigProperty("host", "about:blank");
        driver.navigate().to(host);
        driver.manage().window().maximize();
        WaitUtils.setPageLoadTimeout(90);
        TestLogger.logInfo("WEB_HOOKS", "Escenario iniciado: " + scenario.getName(), null);
    }

    @After(value = "@web or @ui or @selenium or @browser", order = 100)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("WEB_HOOKS", "Finalizando escenario", null);
        try {
            if (scenario.isFailed()) {
                helper.captureScreenOnFailure(scenario);
            }
            DriverManager.getDriver().manage().deleteAllCookies();
            DriverManager.quitDriver();
        } catch (Exception e) {
            TestLogger.logError("WEB_HOOKS", "Error en afterScenario: " + e.getMessage(), null);
            DriverManager.quitDriverSafely();
        }
    }

    private BrowserType getBrowserForScenario() {
        // 1. ExecutionConfig.browser (por ejecución, inmutable, safe en paralelo)
        BrowserType fromConfig = ExecutionContext.current()
                .map(ctx -> ctx.config().getBrowser())
                .filter(b -> !b.isEmpty())
                .map(b -> {
                    try { return helper.parseBrowserType(b); }
                    catch (Exception e) { return null; }
                })
                .orElse(null);
        if (fromConfig != null) return fromConfig;

        // 2. Override explícito por variable de contexto (ej: desde steps de config)
        BrowserType fromCtx = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get("web.browser.type", BrowserType.class))
                .orElse(null);
        if (fromCtx != null) return fromCtx;

        // 3. Fallback a ConfigManager (compatible con ejecución sin Engine)
        return helper.parseBrowserType(ConfigManager.getInstance().getWithPriority("web.browser", "chrome"));
    }

    private boolean getHeadlessModeForScenario() {
        // 1. ExecutionConfig.property("web.headless") (por ejecución, safe en paralelo)
        Boolean fromConfig = ExecutionContext.current()
                .flatMap(ctx -> ctx.config().getProperty("web.headless"))
                .map(Boolean::parseBoolean)
                .orElse(null);
        if (fromConfig != null) return fromConfig;

        // 2. Override explícito por variable de contexto
        Boolean fromCtx = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get("web.headless.override", Boolean.class))
                .orElse(null);
        if (fromCtx != null) return fromCtx;

        // 3. Fallback a ConfigManager
        return ConfigManager.getInstance().getBoolean("web.headless", false);
    }
}
