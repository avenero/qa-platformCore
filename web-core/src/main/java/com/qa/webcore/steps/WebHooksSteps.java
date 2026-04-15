package com.qa.webcore.steps;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.config.WebConfigKeys;
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
 * <h2>Responsabilidades</h2>
 * <ul>
 *   <li>{@code @Before}: crea el WebDriver con prioridad de configuración, lo registra en
 *       {@link com.qa.common.runtime.ServiceRegistry} (para lifecycle SPI) y en
 *       {@link DriverManager} (para compatibilidad ThreadLocal).</li>
 *   <li>{@code @After}: captura screenshot si el escenario falló, limpia cookies.
 *       El cierre del driver es responsabilidad de
 *       {@link com.qa.webcore.plugin.WebPlugin#onScenarioEnd}; este hook solo cierra
 *       el driver como safety-net cuando no hay {@link ExecutionContext} activo
 *       (ejecución sin lifecycle engine).</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebHooksSteps {

    private final WebHelper helper = new WebHelper();

    // =========================================================================
    // @Before — inicialización por escenario
    // =========================================================================

    @SuppressWarnings("deprecation") // HOST_LEGACY: mantenida para retrocompatibilidad con proyectos legacy — ver WebConfigKeys
    @Before(value = "@web or @ui or @selenium or @browser", order = 100)
    public void beforeScenario(Scenario scenario) {
        // Nombre del módulo para el logger (ExecutionConfig tiene prioridad)
        String moduleName = ExecutionContext.current()
                .map(ctx -> ctx.config().getProperty("framework.module.name", "PLATFORM"))
                .orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "PLATFORM"));
        TestLogger.setFramework(moduleName);

        if (!DriverManager.isDriverInitialized()) {
            BrowserType browser   = resolveBrowser();
            boolean     headless  = resolveHeadless();

            WebDriver driver = WebDriverFactory.createDriver(browser, headless);

            // 1. Almacenar en DriverManager (ThreadLocal, compatibilidad)
            DriverManager.setDriver(driver);

            // 2. Registrar en ServiceRegistry para lifecycle SPI (WebPlugin.onScenarioEnd)
            ExecutionContext.current().ifPresent(ctx ->
                    ctx.registry().registerInstance(WebDriver.class, driver));

            TestLogger.logInfo("WEB_HOOKS", "Driver inicializado",
                    java.util.Map.of("browser", browser.name(), "headless", headless));
        }

        // Navegar a URL base y preparar ventana
        WebDriver driver = DriverManager.getDriver();
        String baseUrl = helper.getConfigProperty(WebConfigKeys.HOST_LEGACY, "about:blank");
        driver.navigate().to(baseUrl);
        driver.manage().window().maximize();
        WaitUtils.setPageLoadTimeout(90);
        TestLogger.logInfo("WEB_HOOKS", "Escenario iniciado: " + scenario.getName(), null);
    }

    // =========================================================================
    // @After — limpieza por escenario
    // =========================================================================

    /**
     * Hook de post-escenario.
     *
     * <p><b>Nota sobre el cierre del driver:</b>
     * Cuando hay un {@link ExecutionContext} activo (runtime con lifecycle engine),
     * el cierre autoritativo lo realiza {@link com.qa.webcore.plugin.WebPlugin#onScenarioEnd}
     * vía {@link WebDriverFactory#closeDriver(ExecutionContext)}.
     * Este hook solo cierra el driver como safety-net para escenarios ejecutados
     * sin el engine (ejecución directa con Cucumber runner standalone).
     */
    @After(value = "@web or @ui or @selenium or @browser", order = 100)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("WEB_HOOKS", "Finalizando escenario: " + scenario.getName(), null);
        try {
            // 1. Screenshot antes de cerrar el driver (necesita driver activo)
            if (scenario.isFailed()) {
                helper.captureScreenOnFailure(scenario);
            }
            // 2. Limpiar cookies mientras el driver aún está abierto
            WebDriver current = DriverManager.getDriverOrNull();
            if (current != null) {
                try {
                    current.manage().deleteAllCookies();
                } catch (Exception e) {
                    TestLogger.logWarning("WEB_HOOKS", "Error limpiando cookies: " + e.getMessage(), null);
                }
            }
        } catch (Exception e) {
            TestLogger.logError("WEB_HOOKS", "Error en afterScenario: " + e.getMessage(), null);
        } finally {
            // Safety-net: cierra driver solo si NO hay ExecutionContext activo.
            // Con ExecutionContext, WebPlugin.onScenarioEnd se encarga del cierre.
            if (ExecutionContext.current().isEmpty()) {
                DriverManager.quitDriverSafely();
                TestLogger.logInfo("WEB_HOOKS", "Driver cerrado (modo standalone)", null);
            }
        }
    }

    // =========================================================================
    // Resolución de configuración — prioridad explícita
    // =========================================================================

    /**
     * Resuelve el tipo de navegador con prioridad:
     * <ol>
     *   <li>{@link com.qa.common.runtime.ExecutionConfig#getBrowser()} — inmutable, safe en paralelo</li>
     *   <li>{@code VariableStore[web.browser.type]} — override de step dentro del escenario</li>
     *   <li>{@code ConfigManager.getWithPriority("web.browser", "chrome")} — fallback</li>
     * </ol>
     */
    private BrowserType resolveBrowser() {
        // 1. ExecutionConfig.browser (inmutable, por ejecución)
        BrowserType fromConfig = ExecutionContext.current()
                .map(ctx -> ctx.config().getBrowser())
                .filter(b -> b != null && !b.isEmpty())
                .map(b -> {
                    try { return helper.parseBrowserType(b); }
                    catch (Exception e) { return null; }
                })
                .orElse(null);
        if (fromConfig != null) return fromConfig;

        // 2. VariableStore override (set por steps de configuración)
        BrowserType fromVar = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get(WebConfigKeys.BROWSER_RUNTIME_VAR, BrowserType.class))
                .orElse(null);
        if (fromVar != null) return fromVar;

        // 3. Fallback a ConfigManager
        return helper.parseBrowserType(
                ConfigManager.getInstance().getWithPriority(WebConfigKeys.BROWSER, "chrome"));
    }

    /**
     * Resuelve el modo headless con prioridad:
     * <ol>
     *   <li>{@code ExecutionConfig.property("web.headless")} — inmutable, safe en paralelo</li>
     *   <li>{@code VariableStore[web.headless.override]} — override de step dentro del escenario</li>
     *   <li>{@code ConfigManager.getBoolean("web.headless", false)} — fallback</li>
     * </ol>
     */
    private boolean resolveHeadless() {
        // 1. ExecutionConfig.property
        Boolean fromConfig = ExecutionContext.current()
                .flatMap(ctx -> ctx.config().getProperty(WebConfigKeys.HEADLESS))
                .map(Boolean::parseBoolean)
                .orElse(null);
        if (fromConfig != null) return fromConfig;

        // 2. VariableStore override
        Boolean fromVar = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get(WebConfigKeys.HEADLESS_RUNTIME_VAR, Boolean.class))
                .orElse(null);
        if (fromVar != null) return fromVar;

        // 3. Fallback a ConfigManager
        return ConfigManager.getInstance().getBoolean(WebConfigKeys.HEADLESS, false);
    }
}
