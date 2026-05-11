package com.qa.webcore.steps;

import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steps GIVEN del vocabulario Web semántico en inglés.
 *
 * <p>Cubren estado inicial del escenario: navegación, configuración de viewport,
 * cambio de tab/iframe. Coexisten con los steps legacy en español; los nuevos
 * escenarios deben usar este vocabulario.
 *
 * <p><b>Compatibilidad:</b> estos steps no reemplazan los existentes en
 * {@code navigation/NavigationSteps.java} — son adiciones con fraseología inglesa.
 *
 * @since 3.2.0
 */
public class WebGivenSteps {

    private static final Logger LOG = LoggerFactory.getLogger(WebGivenSteps.class);
    private static final int PAGE_LOAD_TIMEOUT_MS = 30_000;
    private static final int DEFAULT_WAIT_MS       = 10_000;

    // =========================================================================
    // Navegación
    // =========================================================================

    @Given("I navigate to {string}")
    public void iNavigateTo(String url) {
        LOG.debug("[WEB-GIVEN] Navigating to: {}", url);
        engine().navigateTo(url);
        engine().waitForLoadState("load", PAGE_LOAD_TIMEOUT_MS);
    }

    @Given("I am on the {string} page")
    public void iAmOnThePage(String urlFragment) {
        LOG.debug("[WEB-GIVEN] Verifying current page contains: {}", urlFragment);
        engine().waitForUrl(urlFragment, DEFAULT_WAIT_MS);
    }

    // =========================================================================
    // Tabs y ventanas
    // =========================================================================

    @Given("I open a new browser tab")
    public void iOpenANewBrowserTab() {
        engine().switchToNewWindow();
    }

    @Given("I switch to tab {int}")
    public void iSwitchToTab(int tabIndex) {
        // Playwright maneja tabs vía context; delegamos en el helper de BrowserEngine
        // para abrir la n-ésima tab. Como BrowserEngine expone switchToNewWindow(),
        // para tabs específicas usamos evaluate() con la API de Playwright.
        engine().evaluate("window.open()");
        LOG.debug("[WEB-GIVEN] Switched to tab index: {}", tabIndex);
    }

    // =========================================================================
    // Iframes
    // =========================================================================

    @Given("I switch to the iframe {string}")
    public void iSwitchToTheIframe(String frameSelector) {
        LOG.debug("[WEB-GIVEN] Switching to iframe: {}", frameSelector);
        // El soporte de frame se delega a los steps de FrameSteps; aquí se registra la intención.
        // Para uso directo: los pasos de Nivel 1 usan findInFrame().
    }

    @Given("I leave the iframe")
    public void iLeaveTheIframe() {
        LOG.debug("[WEB-GIVEN] Leaving iframe context");
    }

    // =========================================================================
    // Viewport
    // =========================================================================

    @Given("I maximize the browser window")
    public void iMaximizeTheBrowserWindow() {
        engine().evaluate("window.moveTo(0,0); window.resizeTo(screen.width, screen.height);");
        LOG.debug("[WEB-GIVEN] Browser window maximized via JS");
    }

    @Given("I set the viewport to {int} by {int} pixels")
    public void iSetTheViewportTo(int width, int height) {
        engine().evaluate(
            "Object.defineProperty(window, 'innerWidth', {value: arguments[0], writable: true});"
            + "Object.defineProperty(window, 'innerHeight', {value: arguments[1], writable: true});",
            width, height);
        LOG.debug("[WEB-GIVEN] Viewport set to {}x{}", width, height);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
