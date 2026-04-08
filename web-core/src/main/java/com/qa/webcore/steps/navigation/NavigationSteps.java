package com.qa.webcore.steps.navigation;

import com.qa.common.logging.TestLogger;
import com.qa.webcore.driver.DriverManager;
import com.qa.webcore.utils.WebHelper;
import com.qa.webcore.utils.WaitUtils;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

/**
 * Steps de navegacion: ir a URL, historial, refresh, flujos complejos.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NavigationSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @Given("actualizo URL en el navegador {string}")
    public void actualizoUrlEnElNavegador(String url) {
        WebDriver driver = DriverManager.getDriver();
        driver.navigate().to(url);
        WaitUtils.waitForPageReady();
        TestLogger.logInfo("NAV_STEPS", "URL actualizada: " + url, null);
    }

    @When("recargo pagina")
    public void recargoPagina() {
        helper.refreshPage();
    }

    @Given("completo el flujo navegando a {string} con campos {string} {string} {string} con textos {string} {string} {string} y presionando {string}")
    public void completoElFlujoNavegandoConCamposYPresionando(
            String url, String c1, String c2, String c3,
            String t1, String t2, String t3, String boton) {
        WebDriver driver = DriverManager.getDriver();
        driver.navigate().to(url);
        WaitUtils.waitForPageReady();
        helper.setTextWithWait(t1, c1);
        if (c2 != null && !c2.trim().isEmpty()) helper.setTextWithWait(t2, c2);
        if (c3 != null && !c3.trim().isEmpty()) helper.setTextWithWait(t3, c3);
        helper.clicButton(boton);
        helper.captureScreen(scenario);
    }
}
