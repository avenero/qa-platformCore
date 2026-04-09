package com.qa.mobilecore.steps.config;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Steps de gestión del ciclo de vida de la aplicación mobile.
 *
 * <p>Fase BDD: GIVEN (configuración) / WHEN (acciones sobre la app).
 * Cubre instalación, arranque, cierre, reset y gestión de fondo/primer plano.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class AppManagementSteps {

    // =========================================================================
    // Configuración de la app (GIVEN — antes de la sesión)
    // =========================================================================

    @Given("configuro el paquete de la app como {string}")
    public void configuroPaqueteApp(String appPackage) {
        String resolved = ctx().variables().resolve(appPackage);
        mobile().setAppPackage(resolved);
        TestLogger.logInfo("APP_MGT", "App package: " + resolved, null);
    }

    @Given("configuro la actividad principal como {string}")
    public void configuroActividadPrincipal(String activity) {
        String resolved = ctx().variables().resolve(activity);
        mobile().setAppActivity(resolved);
        TestLogger.logInfo("APP_MGT", "App activity: " + resolved, null);
    }

    @Given("configuro el bundle id de la app como {string}")
    public void configuroBundleId(String bundleId) {
        String resolved = ctx().variables().resolve(bundleId);
        mobile().setBundleId(resolved);
        TestLogger.logInfo("APP_MGT", "Bundle ID: " + resolved, null);
    }

    @Given("configuro el archivo de la app en la ruta {string}")
    public void configuroRutaApp(String appPath) {
        String resolved = ctx().variables().resolve(appPath);
        mobile().setAppPath(resolved);
        TestLogger.logInfo("APP_MGT", "App path: " + resolved, null);
    }

    // =========================================================================
    // Instalación / desinstalación (GIVEN)
    // =========================================================================

    @Given("instalo la app desde {string}")
    public void instaloApp(String appPath) {
        String resolved = ctx().variables().resolve(appPath);
        mobile().installApp(resolved);
        TestLogger.logInfo("APP_MGT", "App instalada desde: " + resolved, null);
    }

    @Given("desinstalo la aplicacion {string}")
    public void desinstaloLaAplicacion(String bundleOrPackage) {
        String resolved = ctx().variables().resolve(bundleOrPackage);
        mobile().removeApp(resolved);
        TestLogger.logInfo("APP_MGT", "App desinstalada: " + resolved, null);
    }

    // =========================================================================
    // Arranque (GIVEN / WHEN)
    // =========================================================================

    @Given("lanzo la aplicacion")
    public void lanzoLaAplicacion() {
        mobile().launchApp();
        TestLogger.logInfo("APP_MGT", "Aplicacion lanzada", null);
    }

    // =========================================================================
    // Ciclo de vida (WHEN)
    // =========================================================================

    @When("cierro la aplicacion")
    public void cierroLaAplicacion() {
        mobile().closeApp();
        TestLogger.logInfo("APP_MGT", "Aplicacion cerrada", null);
    }

    @When("reinicio la aplicacion movil")
    public void reinicioLaAplicacion() {
        mobile().restartApp();
        TestLogger.logInfo("APP_MGT", "Aplicacion reiniciada", null);
    }

    @When("pongo la app en background por {int} segundos")
    public void pongoAppEnBackground(int seconds) {
        mobile().backgroundApp(seconds);
        TestLogger.logInfo("APP_MGT", "App en background por " + seconds + "s", null);
    }

    @When("traigo la aplicacion al primer plano con id {string}")
    public void traigoAplicacionAlPrimerPlano(String bundleOrPackage) {
        String resolved = ctx().variables().resolve(bundleOrPackage);
        mobile().activateApp(resolved);
        TestLogger.logInfo("APP_MGT", "App activada: " + resolved, null);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ExecutionContext ctx() {
        return ExecutionContext.requireCurrent();
    }

    private MobileHelper mobile() {
        return ctx().service(MobileHelper.class);
    }
}
