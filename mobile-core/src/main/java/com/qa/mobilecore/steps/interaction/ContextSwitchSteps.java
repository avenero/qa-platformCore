package com.qa.mobilecore.steps.interaction;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.When;

/**
 * Steps de cambio de contexto entre nativo y WebView.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ContextSwitchSteps {

    @When("cambio al contexto nativo")
    public void cambioAlContextoNativo() {
        TestLogger.logInfo("CTX_SWITCH", "Cambiando a contexto NATIVE", null);
    }

    @When("cambio al contexto WebView {string}")
    public void cambioAlContextoWebView(String webViewId) {
        TestLogger.logInfo("CTX_SWITCH", "Cambiando a WebView: " + webViewId, null);
    }

    @When("listo los contextos disponibles")
    public void listoContextosDisponibles() {
        TestLogger.logInfo("CTX_SWITCH", "Listando contextos disponibles", null);
    }
}
