package com.qa.webcore.plugin;


import com.qa.common.spi.CorePlugin;
import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.config.WebConfig;
import com.qa.common.api.driver.CapabilityDescriptor;
import com.qa.common.api.driver.CapabilityReport;
import com.qa.common.api.runtime.ApiContextHolder;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.ServiceRegistry;
import com.qa.common.api.runtime.StepComponent;
import com.qa.webcore.components.bdd.AlertComponent;
import com.qa.webcore.components.bdd.BrowserConfigComponent;
import com.qa.webcore.components.bdd.ClickComponent;
import com.qa.webcore.components.bdd.DragDropComponent;
import com.qa.webcore.components.bdd.ElementValidationComponent;
import com.qa.webcore.components.bdd.FrameComponent;
import com.qa.webcore.components.bdd.InputComponent;
import com.qa.webcore.components.bdd.NavigationComponent;
import com.qa.webcore.components.bdd.PageValidationComponent;
import com.qa.webcore.components.bdd.ScreenshotComponent;
import com.qa.webcore.components.bdd.ScrollComponent;
import com.qa.webcore.components.bdd.SelectComponent;
import com.qa.webcore.components.bdd.TableValidationComponent;
import com.qa.webcore.components.bdd.WaitComponent;
import com.qa.webcore.components.bdd.WebEnvironmentComponent;
import com.qa.webcore.components.bdd.WindowComponent;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.driver.engine.playwright.PlaywrightBrowserEngine;
import com.qa.webcore.driver.playwright.PlaywrightManager;
import com.qa.webcore.config.WebConfigKeys;
import com.qa.webcore.utils.WebHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Plugin de Web para el runtime de ejecución BDD.
 *
 * <p>Registra servicios Web y declara los
 * 16 componentes de steps Web organizados por responsabilidad.
 *
 * <h2>Ciclo de vida Playwright</h2>
 * <p>El ciclo de vida del navegador se centraliza en este plugin.
 *
 * <pre>
 * Cucumber Runtime
 *   ├─ @Before (WebHooksSteps)
 *   ├─ [steps del escenario]
 *   ├─ @After  (WebHooksSteps)
 *   └─ TestCaseFinished
 *         └─► ScenarioLifecycleBridge
 *               └─► WebPlugin.onScenarioEnd()
 *                     └─► PlaywrightManager.endScenario()
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebPlugin implements CorePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(WebPlugin.class);
    private static volatile PlaywrightScenarioInitializer playwrightScenarioInitializer =
            WebPlugin::initializePlaywrightScenarioDefault;
    private static volatile ScenarioTerminator scenarioTerminator = WebPlugin::terminateScenarioDefault;

    // =========================================================================
    // Metadatos del plugin
    // =========================================================================

    @Override
    public String platformId() {
        return "WEB";
    }

    @Override
    public String displayName() {
        return "Web Browser Testing";
    }

    @Override
    public CapabilityReport describeCapabilities() {
        return CapabilityReport.available("WEB", List.of(
            new CapabilityDescriptor("chromium", "Chromium", "Chromium-based browser (default via Playwright)"),
            new CapabilityDescriptor("firefox",  "Firefox",  "Mozilla Firefox (via Playwright)"),
            new CapabilityDescriptor("webkit",   "WebKit",   "WebKit/Safari engine (via Playwright)")
        ));
    }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@web", "@ui", "@browser", "@playwright");
    }

    @Override
    public int getOrder() {
        return 100;
    }

    // =========================================================================
    // Servicios
    // =========================================================================

    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        LOG.debug("[WebPlugin] Registrando servicios Web...");
        registry.registerLazy(WebHelper.class, WebHelper::new);
        LOG.info("[WebPlugin] Servicio registrado: WebHelper (lazy)");
    }

    // =========================================================================
    // Ciclo de vida por escenario
    // =========================================================================

    @Override
    public void onScenarioStart(ExecutionContext context) {
        String browserName = context.config().getProperty(WebConfigKeys.PLAYWRIGHT_BROWSER, "chromium");
        boolean headless = resolveHeadless(context, true);
        LOG.info("[WebPlugin] onScenarioStart — inicializando Playwright (browser={}, headless={})",
                browserName, headless);
        playwrightScenarioInitializer.initialize(context, browserName, headless);
    }

    @Override
    public void onScenarioEnd(ExecutionContext context) {
        LOG.debug("[WebPlugin] onScenarioEnd — finalizando Playwright");
        scenarioTerminator.terminate(context);
        LOG.debug("[WebPlugin] onScenarioEnd — completado");
    }

    private static void initializePlaywrightScenarioDefault(
            ExecutionContext context,
            String browserName,
            boolean headless
    ) {
        PlaywrightManager.initSuite(browserName, headless);
        PlaywrightManager.startScenario();
        // FEC-API-SHIP-WEB-SHARE: publica el APIRequestContext del browser en el puerto neutral
        // para que los steps @api (motor PLAYWRIGHT) reutilicen cookies/sesión del navegador
        // en escenarios híbridos @web+@api. getApiContext() devuelve browserContext.request().
        ApiContextHolder.set(PlaywrightManager.getApiContext());
        context.registry().registerInstance(BrowserEngine.class,
                new PlaywrightBrowserEngine(PlaywrightManager.getPage()));
    }

    private static void terminateScenarioDefault(ExecutionContext context) {
        try {
            PlaywrightManager.endScenario();
        } finally {
            // FEC-API-SHIP-WEB-SHARE: limpia el holder para no filtrar un contexto ya cerrado
            // al siguiente escenario. En finally: se limpia aunque endScenario() falle.
            ApiContextHolder.clear();
        }
    }

    private static boolean resolveHeadless(ExecutionContext context, boolean defaultValue) {
        // TASK-K03M-M3: ExecutionContext per-thread sigue ganando si tiene la key explícita;
        // si no, fallback a WebConfig.headless() (cubre SysProp/Env/Properties/Yaml + alias F7).
        // El defaultValue del param se preserva como sentinel del context.getProperty.
        String defaultAsString = Boolean.toString(defaultValue);
        String fromContext = context.config().getProperty(WebConfigKeys.BROWSER_HEADLESS, defaultAsString);
        if (fromContext != null && !fromContext.isBlank()) {
            return Boolean.parseBoolean(fromContext);
        }
        return ConfigLoaderHolder.get().load(WebConfig.class).headless();
    }

    static void setPlaywrightScenarioInitializerForTesting(PlaywrightScenarioInitializer initializer) {
        playwrightScenarioInitializer = initializer;
    }

    static void setScenarioTerminatorForTesting(ScenarioTerminator terminator) {
        scenarioTerminator = terminator;
    }

    static void resetLifecycleHooksForTesting() {
        playwrightScenarioInitializer = WebPlugin::initializePlaywrightScenarioDefault;
        scenarioTerminator = WebPlugin::terminateScenarioDefault;
    }

    @FunctionalInterface
    interface PlaywrightScenarioInitializer {
        void initialize(ExecutionContext context, String browserName, boolean headless);
    }

    @FunctionalInterface
    interface ScenarioTerminator {
        void terminate(ExecutionContext context);
    }

    // =========================================================================
    // Componentes de steps (16)
    // =========================================================================

    /**
     * Declara los 16 componentes de steps Web.
     * Cada componente agrupa steps cohesivos por responsabilidad y fase BDD.
     *
     * <p><em>Nota de deprecación:</em> {@code DragDropComponent} (stepId {@code "web.dragdrop"})
     * se incluye durante el ciclo de transición hacia {@code "web.drag.drop"} (v2.2.0).
     * Remover en v2.3.0 una vez migrados todos los escenarios persistidos.
     */
    @SuppressWarnings("deprecation")
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                // GIVEN — Configuración
                new BrowserConfigComponent(),
                new WebEnvironmentComponent(),
                // WHEN — Navegación
                new NavigationComponent(),
                new FrameComponent(),
                new WindowComponent(),
                // WHEN — Interacción
                new ClickComponent(),
                new InputComponent(),
                new SelectComponent(),
                new ScrollComponent(),
                new DragDropComponent(),
                new AlertComponent(),
                // WHEN — Esperas
                new WaitComponent(),
                // THEN — Validación
                new ElementValidationComponent(),
                new PageValidationComponent(),
                new TableValidationComponent(),
                new ScreenshotComponent()
        );
    }

    @Override
    public List<String> getGluePackages() {
        List<String> glue = new ArrayList<>(CorePlugin.super.getGluePackages());
        glue.add("com.qa.webcore.steps");
        glue = glue.stream().distinct().sorted().toList();
        return Collections.unmodifiableList(glue);
    }
}
