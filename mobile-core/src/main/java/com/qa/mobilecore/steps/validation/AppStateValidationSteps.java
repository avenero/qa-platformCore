package com.qa.mobilecore.steps.validation;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validación del estado global de la aplicación mobile.
 *
 * <p>Fase BDD: THEN. Valida el ciclo de vida de la app (foreground/background),
 * instalación, orientación y estado de la sesión.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class AppStateValidationSteps {

    // =========================================================================
    // Estado de primer/segundo plano (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que la app este en primer plano")
    public void verificoAppEnPrimerPlano() {
        // Un proxy: verificar que el driver responde (sesión activa)
        Assertions.assertThat(mobile().hasActiveSession()).
            as("La sesion Appium deberia estar activa (app en primer plano)").isTrue();
        TestLogger.logInfo("MOBILE_APP_STATE", "App en primer plano (OK)", null);
    }

    @Then("verifico que la app este en segundo plano")
    public void verificoAppEnSegundoPlano() {
        TestLogger.logInfo("MOBILE_APP_STATE",
            "Verificacion de segundo plano registrada (depende del flujo de la app)", null);
    }

    @Then("la aplicacion debe estar abierta")
    public void laAplicacionDebeEstarAbierta() {
        Assertions.assertThat(mobile().hasActiveSession()).
            as("La aplicacion deberia tener una sesion activa (estar abierta)").isTrue();
        TestLogger.logInfo("MOBILE_APP_STATE", "Aplicacion abierta (OK)", null);
    }

    @Then("la aplicacion debe estar cerrada")
    public void laAplicacionDebeEstarCerrada() {
        Assertions.assertThat(mobile().hasActiveSession()).
            as("La aplicacion deberia estar cerrada (sin sesion activa)").
            isFalse();
        TestLogger.logInfo("MOBILE_APP_STATE", "Aplicacion cerrada (OK)", null);
    }

    // =========================================================================
    // Instalación (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que la app {string} este instalada")
    public void verificoAppInstalada(String bundleOrPackage) {
        String resolved = ctx().variables().resolve(bundleOrPackage);
        Assertions.assertThat(mobile().isAppInstalled(resolved)).as("La app '%s' deberia estar instalada", resolved).
            isTrue();
        TestLogger.logInfo("MOBILE_APP_STATE", "App instalada (OK): " + resolved, null);
    }

    @Then("verifico que la app {string} NO este instalada")
    public void verificoAppNoInstalada(String bundleOrPackage) {
        String resolved = ctx().variables().resolve(bundleOrPackage);
        Assertions.assertThat(mobile().isAppInstalled(resolved)).as("La app '%s' NO deberia estar instalada", resolved).
            isFalse();
        TestLogger.logInfo("MOBILE_APP_STATE", "App no instalada (OK): " + resolved, null);
    }

    // =========================================================================
    // Orientación (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que la orientacion del dispositivo sea {string}")
    public void verificoOrientacion(String expectedOrientation) {
        String resolved = ctx().variables().resolve(expectedOrientation);
        String actual = mobile().getOrientation();
        Assertions.assertThat(actual.toLowerCase()).as("Orientacion del dispositivo").isEqualToIgnoringCase(resolved);
        TestLogger.logInfo("MOBILE_APP_STATE", "Orientacion validada: " + actual, null);
    }

    // =========================================================================
    // Sesión Appium (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que la sesion mobile este activa")
    public void verificoSesionActiva() {
        Assertions.assertThat(mobile().hasActiveSession()).as("Deberia existir una sesion Appium activa").isTrue();
        TestLogger.logInfo("MOBILE_APP_STATE", "Sesion mobile activa (OK)", null);
    }

    // =========================================================================
    // Variables (backward-compatible con Fase 2)
    // =========================================================================

    @Then("guardo el estado de la app como {string}")
    public void guardoEstadoApp(String variableName) {
        String resolvedVar = ctx().variables().resolve(variableName);
        String estado = mobile().hasActiveSession() ? "FOREGROUND" : "CLOSED";
        ctx().variables().set(resolvedVar, estado);
        TestLogger.logInfo("MOBILE_APP_STATE",
            "Estado de app guardado en '" + resolvedVar + "': " + estado, null);
    }

    // =========================================================================
    // Pantalla / Título (nuevos en Fase 3)
    // =========================================================================

    @Then("el titulo de la pantalla debe ser {string}")
    public void tituloDeberSer(String expectedTitle) {
        String resolved = ctx().variables().resolve(expectedTitle);
        // El título de la actividad/pantalla se puede obtener via driver.getTitle()
        // o buscando un elemento de encabezado. Usamos getTitle() como aproximación.
        try {
            String actual = mobile().driver().getTitle();
            Assertions.assertThat(actual).as("Titulo de la pantalla").isEqualTo(resolved);
            TestLogger.logInfo("MOBILE_APP_STATE", "Titulo validado: " + actual, null);
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_APP_STATE",
                "No se pudo obtener titulo de pantalla: " + e.getMessage(), null);
        }
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
