package com.qa.mobilecore.steps.config;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Steps de gestion de la aplicacion movil: instalar, lanzar, cerrar.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class AppManagementSteps {

    @Given("instalo la app desde {string}")
    public void instaloApp(String appPath) {
        TestLogger.logInfo("APP_MGT", "Instalando app: " + appPath, null);
    }

    @Given("lanzo la aplicacion")
    public void lanzoLaAplicacion() {
        TestLogger.logInfo("APP_MGT", "Lanzando aplicacion", null);
    }

    @When("cierro la aplicacion")
    public void cierroLaAplicacion() {
        TestLogger.logInfo("APP_MGT", "Cerrando aplicacion", null);
    }

    @When("pongo la app en background por {int} segundos")
    public void pongoAppEnBackground(int seconds) {
        TestLogger.logInfo("APP_MGT", "App en background " + seconds + "s", null);
    }
}
