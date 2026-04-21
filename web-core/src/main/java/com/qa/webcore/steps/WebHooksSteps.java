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

import java.util.Map;
import java.util.Optional;

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

    /** Timeout de carga de pagina para el hook before (en segundos). */
    private static final int HOOK_PAGE_LOAD_TIMEOUT = 90;

    private final WebHelper helper = new WebHelper();

    // =========================================================================
    // @Before — inicialización por escenario
    // =========================================================================

    // HOST_LEGACY: mantenida para retrocompatibilidad con proyectos legacy — ver WebConfigKeys
    @SuppressWarnings("deprecation")
    @Before(value = "@web or @ui or @selenium or @browser", order = 100)
    public void beforeScenario(Scenario scenario) {
        // Nombre del módulo para el logger (ExecutionConfig tiene prioridad)
        String moduleName = ExecutionContext.current().
                map(ctx -> ctx.config().getProperty("framework.module.name", "PLATFORM")).
                orElseGet(() -> ConfigManager.getInstance().get("framework.module.name", "PLATFORM"));
        TestLogger.setFramework(moduleName);

        Optional<ExecutionContext> ctxOpt = ExecutionContext.current();

        // Verificar presencia de driver: ServiceRegistry cuando hay contexto, DriverManager en standalone.
        // Esto garantiza que el driver vive en el ExecutionContext y no en un singleton global.
        boolean driverPresente = ctxOpt.map(ctx -> ctx.registry().isRegistered(WebDriver.class)).
                orElseGet(DriverManager::isDriverInitialized);

        if (!driverPresente) {
            BrowserType browser  = resolveBrowser();
            boolean     headless = resolveHeadless();
            WebDriverFactory.DriverConfig driverConfig =
                    new WebDriverFactory.DriverConfig(browser).withHeadless(headless);

            if (ctxOpt.isPresent()) {
                // Modo con lifecycle engine: crear y registrar atómicamente en el ServiceRegistry
                // del contexto activo. DriverManager NO se actualiza deliberadamente: el driver
                // vive exclusivamente en el contexto y se limpia con WebPlugin.onScenarioEnd.
                WebDriverFactory.createDriver(driverConfig, ctxOpt.get());
            } else {
                // Modo standalone (sin engine de lifecycle): DriverManager como única fuente.
                WebDriver standaloneDriver = WebDriverFactory.createDriver(driverConfig);
                DriverManager.setDriver(standaloneDriver);
            }

            TestLogger.logInfo("WEB_HOOKS", "Driver inicializado",
                    Map.of("browser", browser.name(), "headless", headless));
        }

        // Navegar a URL base — prioridad: ServiceRegistry del contexto → DriverManager standalone
        WebDriver driver = ctxOpt.flatMap(ctx -> ctx.registry().get(WebDriver.class)).
                orElseGet(DriverManager::getDriver);
        String baseUrl = helper.getConfigProperty(WebConfigKeys.HOST_LEGACY, "about:blank");
        driver.navigate().to(baseUrl);
        driver.manage().window().maximize();
        WaitUtils.setPageLoadTimeout(HOOK_PAGE_LOAD_TIMEOUT);
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
            // 2. Limpiar cookies mientras el driver aún está abierto.
            //    Prioridad: ServiceRegistry del contexto → DriverManager standalone.
            WebDriver current = ExecutionContext.current().flatMap(ctx -> ctx.registry().get(WebDriver.class)).
                    orElseGet(DriverManager::getDriverOrNull);
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
        BrowserType fromConfig = ExecutionContext.current().map(ctx -> ctx.config().getBrowser()).
                filter(b -> b != null && !b.isEmpty()).map(b -> {
                    try { return helper.parseBrowserType(b); }
                    catch (Exception e) { return null; }
                }).orElse(null);
        if (fromConfig != null) {
            return fromConfig;
        }

        // 2. VariableStore override (set por steps de configuración)
        BrowserType fromVar = ExecutionContext.current().
                flatMap(ctx -> ctx.variables().get(WebConfigKeys.BROWSER_RUNTIME_VAR, BrowserType.class)).orElse(null);
        if (fromVar != null) {
            return fromVar;
        }

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
        Boolean fromConfig = ExecutionContext.current().
                flatMap(ctx -> ctx.config().getProperty(WebConfigKeys.HEADLESS)).
                map(Boolean::parseBoolean).orElse(null);
        if (fromConfig != null) {
            return fromConfig;
        }

        // 2. VariableStore override
        Boolean fromVar = ExecutionContext.current().
                flatMap(ctx -> ctx.variables().get(WebConfigKeys.HEADLESS_RUNTIME_VAR, Boolean.class)).orElse(null);
        if (fromVar != null) {
            return fromVar;
        }

        // 3. Fallback a ConfigManager
        return ConfigManager.getInstance().getBoolean(WebConfigKeys.HEADLESS, false);
    }
}
