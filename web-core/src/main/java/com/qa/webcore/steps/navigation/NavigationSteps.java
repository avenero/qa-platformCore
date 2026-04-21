package com.qa.webcore.steps.navigation;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.webcore.driver.DriverManager;
import com.qa.webcore.utils.WebHelper;
import com.qa.webcore.utils.WaitUtils;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

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

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

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
        WebDriver driver = DriverManager.getDriver();
        driver.navigate().to(url);
        WaitUtils.waitForPageReady();
        TestLogger.logInfo("NAV_STEPS", "URL actualizada: " + url, null);
    }

    /**
     * Recarga la página actual del navegador.
     */
    @StepDef(value = "web.navigation.refresh",
             displayName = "Recargar página")
    @When("recargo pagina")
    public void recargoPagina() {
        helper.refreshPage();
    }

    /**
     * Navega hacia atrás en el historial del navegador.
     */
    @StepDef(value = "web.navigation.back",
             displayName = "Navegar hacia atrás")
    @When("navego hacia atrás")
    public void navegoHaciaAtras() {
        DriverManager.getDriver().navigate().back();
        WaitUtils.waitForPageReady();
        TestLogger.logInfo("NAV_STEPS", "Navegado hacia atrás", null);
    }

    /**
     * Navega hacia adelante en el historial del navegador.
     */
    @StepDef(value = "web.navigation.forward",
             displayName = "Navegar hacia adelante")
    @When("navego hacia adelante")
    public void navegoHaciaAdelante() {
        DriverManager.getDriver().navigate().forward();
        WaitUtils.waitForPageReady();
        TestLogger.logInfo("NAV_STEPS", "Navegado hacia adelante", null);
    }

    // =========================================================================
    // Step DEPRECATED — macro de negocio, no pertenece a un step atómico
    // =========================================================================

    /**
     * @deprecated Step macro de negocio — debe descomponerse en steps individuales:
     *             {@code web.navigation.go-to-url}, pasos de input y click.
     */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @StepDef(value = "web.navigation.legacy-completo-flujo",
             deprecated = true,
             displayName = "completo el flujo navegando… (DEPRECATED — macro de negocio, usar steps individuales)")
    @Given("completo el flujo navegando a {string} con campos {string} {string}"
        + " {string} con textos {string} {string} {string} y presionando {string}")
    @SuppressWarnings("checkstyle:LineLength")
    public void completoElFlujoNavegandoConCamposYPresionando(
            String url, String c1, String c2, String c3,
            String t1, String t2, String t3, String boton) {
        WebDriver driver = DriverManager.getDriver();
        driver.navigate().to(url);
        WaitUtils.waitForPageReady();
        helper.setTextWithWait(t1, c1);
        if (c2 != null && !c2.trim().isEmpty()) {
            helper.setTextWithWait(t2, c2);
        }
        if (c3 != null && !c3.trim().isEmpty()) {
            helper.setTextWithWait(t3, c3);
        }
        helper.clicButton(boton);
        helper.captureScreen(scenario);
    }
}
