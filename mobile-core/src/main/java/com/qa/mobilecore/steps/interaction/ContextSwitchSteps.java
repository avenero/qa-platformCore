package com.qa.mobilecore.steps.interaction;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

import java.util.Set;

/**
 * Steps de cambio de contexto entre nativo y WebView (apps híbridas).
 *
 * <p>Fase BDD: WHEN. Permite alternar entre contextos NATIVE_APP y WEBVIEW,
 * útil para apps híbridas (React Native, Cordova, Ionic, etc.).
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class ContextSwitchSteps {

    @When("cambio al contexto nativo")
    public void cambioAlContextoNativo() {
        mobile().switchToNative();
        TestLogger.logInfo("CTX_SWITCH", "Contexto cambiado a NATIVE_APP", null);
    }

    @When("cambio al contexto WebView {string}")
    public void cambioAlContextoWebView(String webViewId) {
        String resolved = ctx().variables().resolve(webViewId);
        mobile().switchToWebView(resolved);
        TestLogger.logInfo("CTX_SWITCH", "Contexto cambiado a WebView: " + resolved, null);
    }

    @When("cambio el contexto a {string}")
    public void cambioElContextoA(String contextName) {
        String resolved = ctx().variables().resolve(contextName);
        if ("NATIVE_APP".equalsIgnoreCase(resolved)) {
            mobile().switchToNative();
        } else {
            mobile().switchToWebView(resolved);
        }
        TestLogger.logInfo("CTX_SWITCH", "Contexto: " + resolved, null);
    }

    @When("listo los contextos disponibles")
    public void listoContextosDisponibles() {
        Set<String> contexts = mobile().getContexts();
        TestLogger.logInfo("CTX_SWITCH",
            "Contextos disponibles (" + contexts.size() + "): " + contexts, null);
    }

    @Then("guardo los contextos disponibles como {string}")
    public void guardoContextosDisponibles(String variable) {
        Set<String> contexts = mobile().getContexts();
        ctx().variables().set(ctx().variables().resolve(variable), contexts.toString());
        TestLogger.logInfo("CTX_SWITCH",
            "Contextos guardados en '" + variable + "': " + contexts, null);
    }

    @Then("deberia existir un contexto WebView disponible")
    public void deberiaExistirContextoWebView() {
        Set<String> contexts = mobile().getContexts();
        boolean hasWebView = contexts.stream().anyMatch(c -> c.toUpperCase().contains("WEBVIEW"));
        Assertions.assertThat(hasWebView).
            as("Deberia haber al menos un contexto WEBVIEW disponible. Contextos: " + contexts).isTrue();
        TestLogger.logInfo("CTX_SWITCH", "WebView context detectado (OK)", null);
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
