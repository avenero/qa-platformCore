package com.qa.webcore.steps.navigation;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.annotation.StepDef;
import com.qa.webcore.driver.engine.BrowserEngine;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Steps de navegación: ir a URL, historial, refresh, flujos complejos.
 *
 * <p>Componente padre: {@code web.navigation}
 * ({@link com.qa.webcore.components.bdd.NavigationComponent}).
 * Fase BDD: WHEN.
 *
 * <p>Todos los steps canónicos llevan {@link StepDef} con ID explícito para garantizar
 * estabilidad frente a refactorizaciones. El formato es {@code web.navigation.SUB_ID}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class NavigationSteps {

    private static final int PAGE_LOAD_TIMEOUT_MS = 30_000;

    // =========================================================================
    // Steps canónicos de navegación
    // =========================================================================

    /**
     * Navega el navegador a la URL indicada y espera a que la página esté lista.
     * Es el step principal de navegación.
     */
    @StepDef(value = "web.navigation.go-to-url",
             displayName = "Navegar a URL")
    @Given("actualizo URL en el navegador {string}")
    public void actualizoUrlEnElNavegador(String url) {
        engine().navigateTo(url);
        engine().waitForLoadState("load", PAGE_LOAD_TIMEOUT_MS);
        TestLogger.logInfo("NAV_STEPS", "URL actualizada: " + url, null);
    }

    /**
     * Recarga la página actual del navegador.
     */
    @StepDef(value = "web.navigation.refresh",
             displayName = "Recargar página")
    @When("recargo pagina")
    public void recargoPagina() {
        engine().refresh();
        engine().waitForLoadState("load", PAGE_LOAD_TIMEOUT_MS);
    }

    /**
     * Navega hacia atrás en el historial del navegador.
     */
    @StepDef(value = "web.navigation.back",
             displayName = "Navegar hacia atrás")
    @When("navego hacia atrás")
    public void navegoHaciaAtras() {
        engine().back();
        engine().waitForLoadState("load", PAGE_LOAD_TIMEOUT_MS);
        TestLogger.logInfo("NAV_STEPS", "Navegado hacia atrás", null);
    }

    /**
     * Navega hacia adelante en el historial del navegador.
     */
    @StepDef(value = "web.navigation.forward",
             displayName = "Navegar hacia adelante")
    @When("navego hacia adelante")
    public void navegoHaciaAdelante() {
        engine().evaluate("window.history.forward()");
        engine().waitForLoadState("load", PAGE_LOAD_TIMEOUT_MS);
        TestLogger.logInfo("NAV_STEPS", "Navegado hacia adelante", null);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
