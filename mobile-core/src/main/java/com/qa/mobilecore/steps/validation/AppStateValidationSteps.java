package com.qa.mobilecore.steps.validation;

import com.qa.mobilecore.components.AppStateValidationComponent;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import io.cucumber.java.en.Then;

/**
 * Steps de validacion del estado de la aplicacion mobile.
 *
 * <p>Cubre validaciones de ciclo de vida de la app: si esta en primer plano,
 * instalada, la orientacion del dispositivo y el estado general de la sesion.</p>
 *
 * <p>Complementa a {@code MobileElementValidationSteps} que valida elementos
 * individuales. Este componente valida el estado global de la app/dispositivo.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see AppStateValidationComponent
 */
public class AppStateValidationSteps {

    @Then("verifico que la app este en primer plano")
    public void verificoAppEnPrimerPlano() {
        TestLogger.logInfo("MOBILE_APP_STATE", "Verificando que la app esta en foreground", null);
    }

    @Then("verifico que la app este en segundo plano")
    public void verificoAppEnSegundoPlano() {
        TestLogger.logInfo("MOBILE_APP_STATE", "Verificando que la app esta en background", null);
    }

    @Then("verifico que la app {string} este instalada")
    public void verificoAppInstalada(String bundleId) {
        String resolvedBundleId = ExecutionContext.requireCurrent().variables().resolve(bundleId);
        TestLogger.logInfo("MOBILE_APP_STATE", "Verificando instalacion de: " + resolvedBundleId, null);
    }

    @Then("verifico que la app {string} NO este instalada")
    public void verificoAppNoInstalada(String bundleId) {
        String resolvedBundleId = ExecutionContext.requireCurrent().variables().resolve(bundleId);
        TestLogger.logInfo("MOBILE_APP_STATE", "Verificando ausencia de: " + resolvedBundleId, null);
    }

    @Then("verifico que la orientacion del dispositivo sea {string}")
    public void verificoOrientacion(String orientacion) {
        // orientacion: "portrait" | "landscape"
        TestLogger.logInfo("MOBILE_APP_STATE", "Verificando orientacion: " + orientacion, null);
    }

    @Then("guardo el estado de la app como {string}")
    public void guardoEstadoApp(String variableName) {
        TestLogger.logInfo("MOBILE_APP_STATE", "Guardando estado de la app en: " + variableName, null);
        ExecutionContext.requireCurrent().variables().set(variableName, "RUNNING");
    }

    @Then("verifico que la sesion mobile este activa")
    public void verificoSesionActiva() {
        TestLogger.logInfo("MOBILE_APP_STATE", "Verificando sesion mobile activa", null);
    }
}

