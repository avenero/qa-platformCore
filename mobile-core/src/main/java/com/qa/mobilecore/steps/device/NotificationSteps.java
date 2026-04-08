package com.qa.mobilecore.steps.device;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps de interaccion con notificaciones push y del sistema.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NotificationSteps {

    @When("abro el panel de notificaciones")
    public void abroElPanelDeNotificaciones() {
        TestLogger.logInfo("NOTIFICATION", "Abriendo panel de notificaciones", null);
    }

    @When("toco la notificacion que contiene {string}")
    public void tocoLaNotificacion(String text) {
        TestLogger.logInfo("NOTIFICATION", "Tocando notificacion: " + text, null);
    }

    @When("descarto todas las notificaciones")
    public void descartarTodasLasNotificaciones() {
        TestLogger.logInfo("NOTIFICATION", "Descartando todas las notificaciones", null);
    }

    @Then("verifico que existe una notificacion con el texto {string}")
    public void verificoNotificacion(String text) {
        TestLogger.logInfo("NOTIFICATION", "Verificando notificacion: " + text, null);
    }
}
