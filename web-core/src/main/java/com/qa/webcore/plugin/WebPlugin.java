package com.qa.webcore.plugin;

import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
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
import com.qa.webcore.driver.DriverManager;
import com.qa.webcore.driver.WebDriverFactory;
import com.qa.webcore.utils.WebHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Plugin de Web para el runtime de ejecución BDD.
 *
 * <p>Registra los servicios Selenium ({@link WebHelper}) y declara los
 * 16 componentes de steps Web organizados por responsabilidad.
 *
 * <h2>Ciclo de vida del WebDriver</h2>
 * <p>El driver es <b>lazy</b>: no se crea en {@link #onScenarioStart}, sino en el
 * {@code @Before} hook de Cucumber ({@link com.qa.webcore.steps.WebHooksSteps}),
 * que también lo registra en el {@link ServiceRegistry} del contexto.
 *
 * <p>En {@link #onScenarioEnd} este plugin recupera el driver del {@code ServiceRegistry}
 * vía {@link WebDriverFactory#closeDriver(ExecutionContext)} y lo cierra. De esta forma
 * el cierre del driver es responsabilidad del ciclo de vida SPI del plugin, no solo del
 * hook {@code @After} de Cucumber.
 *
 * <pre>
 * Cucumber Runtime
 *   ├─ @Before (WebHooksSteps)  → crea driver, registra en ServiceRegistry + DriverManager
 *   ├─ [steps del escenario]
 *   ├─ @After  (WebHooksSteps)  → screenshot si fallo, limpia cookies
 *   └─ TestCaseFinished
 *         └─► ScenarioLifecycleBridge
 *               └─► WebPlugin.onScenarioEnd()
 *                     └─► WebDriverFactory.closeDriver(ctx)  ← cierre autoritativo
 *                     └─► DriverManager.quitDriverSafely()   ← limpieza ThreadLocal
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebPlugin implements CorePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(WebPlugin.class);

    // =========================================================================
    // Metadatos del plugin
    // =========================================================================

    @Override
    public String getName() {
        return "web";
    }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@web", "@ui", "@browser", "@selenium");
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

    /**
     * Callback al inicio de cada escenario.
     *
     * <p>El driver es <b>lazy</b>: se crea en el {@code @Before} hook de Cucumber
     * ({@link com.qa.webcore.steps.WebHooksSteps#beforeScenario}), no aquí.
     * Este método solo emite un log de trazabilidad.
     */
    @Override
    public void onScenarioStart(ExecutionContext context) {
        LOG.debug("[WebPlugin] onScenarioStart — driver lazy, se creará en @Before de Cucumber");
    }

    /**
     * Callback al final de cada escenario.
     *
     * <p>Recupera el {@link WebDriver} del {@link ServiceRegistry} del contexto
     * y lo cierra vía {@link WebDriverFactory#closeDriver(ExecutionContext)}.
     * Adicionalmente limpia el {@link DriverManager} ThreadLocal como safety-net.
     *
     * <p>Es idempotente: si el driver ya fue cerrado (o nunca se registró), no lanza excepción.
     */
    @Override
    public void onScenarioEnd(ExecutionContext context) {
        LOG.debug("[WebPlugin] onScenarioEnd — cerrando WebDriver si existe en ServiceRegistry");
        // Cierre autoritativo vía ServiceRegistry (registrado por WebHooksSteps.beforeScenario)
        WebDriverFactory.closeDriver(context);
        // Safety-net: limpia el ThreadLocal de DriverManager para evitar leaks entre escenarios
        DriverManager.quitDriverSafely();
        LOG.debug("[WebPlugin] onScenarioEnd — completado");
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
}
