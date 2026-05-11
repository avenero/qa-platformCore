package com.qa.mobilecore.steps.device;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

/**
 * Steps de interacción y validación de notificaciones del sistema.
 *
 * <p>Fase BDD: WHEN (acciones) / THEN (validaciones).
 * Nota: la verificación del contenido de notificaciones requiere acceso
 * al panel de notificaciones, lo cual está soportado nativamente en Android.
 * En iOS las notificaciones requieren configuración adicional de simctl.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class NotificationSteps {

    @When("abro el panel de notificaciones")
    public void abroElPanelDeNotificaciones() {
        mobile().openNotificationCenter();
        TestLogger.logInfo("NOTIFICATION", "Panel de notificaciones abierto", null);
    }

    @When("toco la notificacion que contiene {string}")
    public void tocoLaNotificacion(String text) {
        String resolved = ctx().variables().resolve(text);
        // Busca el elemento de notificación por texto y hace tap
        mobile().scrollToText(resolved);
        mobile().tap("text:" + resolved);
        TestLogger.logInfo("NOTIFICATION", "Notificacion tocada: " + resolved, null);
    }

    /**
     * Descarta notificaciones tocando el botón con el texto indicado.
     * Ejemplo: "Borrar todo", "Clear all", "Dismiss all".
     */
    @When("descarto las notificaciones con el boton {string}")
    public void descartarNotificacionesConBoton(String buttonText) {
        String resolved = ctx().variables().resolve(buttonText);
        mobile().tap("text:" + resolved);
        TestLogger.logInfo("NOTIFICATION", "Notificaciones descartadas via: " + resolved, null);
    }

    @Then("verifico que existe una notificacion con el texto {string}")
    public void verificoNotificacion(String text) {
        String resolved = ctx().variables().resolve(text);
        boolean exists = mobile().elementExists("text:" + resolved);
        Assertions.assertThat(exists).as("Deberia existir una notificacion con el texto: '%s'", resolved).isTrue();
        TestLogger.logInfo("NOTIFICATION", "Notificacion verificada: " + resolved, null);
    }

    @Then("no deberia existir una notificacion con el texto {string}")
    public void noDeberiaExistirNotificacion(String text) {
        String resolved = ctx().variables().resolve(text);
        boolean exists = mobile().elementExists("text:" + resolved);
        Assertions.assertThat(exists).as("NO deberia existir una notificacion con el texto: '%s'", resolved).isFalse();
        TestLogger.logInfo("NOTIFICATION", "Ausencia de notificacion verificada: " + resolved, null);
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
