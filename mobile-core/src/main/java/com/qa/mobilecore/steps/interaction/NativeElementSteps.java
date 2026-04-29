package com.qa.mobilecore.steps.interaction;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

/**
 * Steps de interacción con elementos nativos de la UI mobile.
 *
 * <p>Fase BDD: WHEN (acciones) / THEN (validaciones sobre elementos individuales).
 * Cubre escritura, limpieza, selección y verificaciones de estado/texto.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class NativeElementSteps {

    private static final long MILLIS_PER_SECOND = 1000L;

    // =========================================================================
    // Escritura e interacción (WHEN)
    // =========================================================================

    @When("ingreso el texto {string} en el campo nativo {string}")
    public void ingresarTextoNativo(String text, String locator) {
        String resolvedText   = ctx().variables().resolve(text);
        String resolvedLocator = ctx().variables().resolve(locator);
        mobile().type(resolvedLocator, resolvedText);
        TestLogger.logInfo("NATIVE_EL",
            "Texto ingresado en '" + resolvedLocator + "': " + resolvedText, null);
    }

    @When("presiono el boton nativo {string}")
    public void presionoBotonNativo(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().tap(resolved);
        TestLogger.logInfo("NATIVE_EL", "Tap en boton: " + resolved, null);
    }

    @When("limpio el campo nativo {string}")
    public void limpioElCampoNativo(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().clearField(resolved);
        TestLogger.logInfo("NATIVE_EL", "Campo limpiado: " + resolved, null);
    }

    @When("marco el switch {string}")
    public void marcoSwitch(String locator) {
        String resolved = ctx().variables().resolve(locator);
        if (!mobile().findElement(resolved).isSelected()) {
            mobile().tap(resolved);
        }
        TestLogger.logInfo("NATIVE_EL", "Switch activado: " + resolved, null);
    }

    @When("desmarco el switch {string}")
    public void desmarcoSwitch(String locator) {
        String resolved = ctx().variables().resolve(locator);
        if (mobile().findElement(resolved).isSelected()) {
            mobile().tap(resolved);
        }
        TestLogger.logInfo("NATIVE_EL", "Switch desactivado: " + resolved, null);
    }

    // =========================================================================
    // Validaciones de elementos (THEN)
    // =========================================================================

    @StepDef(value = "mobile.native-element.verifico-visible",
             displayName = "Verificar que elemento nativo sea visible")
    @Then("el elemento nativo {string} debe ser visible")
    public void elementoDebeSerVisible(String locator) {
        String resolved = ctx().variables().resolve(locator);
        boolean visible = mobile().isVisible(resolved);
        Assertions.assertThat(visible).as("El elemento '%s' deberia ser visible", resolved).isTrue();
        TestLogger.logInfo("NATIVE_EL", "Elemento visible: " + resolved, null);
    }

    @Then("el elemento nativo {string} no debe ser visible")
    public void elementoNoDebeSerVisible(String locator) {
        String resolved = ctx().variables().resolve(locator);
        boolean visible = mobile().isVisible(resolved);
        Assertions.assertThat(visible).as("El elemento '%s' NO deberia ser visible", resolved).isFalse();
        TestLogger.logInfo("NATIVE_EL", "Elemento no visible (OK): " + resolved, null);
    }

    @Then("el elemento {string} debe estar habilitado")
    public void elementoDebeEstarHabilitado(String locator) {
        String resolved = ctx().variables().resolve(locator);
        Assertions.assertThat(mobile().isEnabled(resolved)).as("El elemento '%s' deberia estar habilitado", resolved).
            isTrue();
        TestLogger.logInfo("NATIVE_EL", "Elemento habilitado (OK): " + resolved, null);
    }

    @Then("el elemento {string} debe estar deshabilitado")
    public void elementoDebeEstarDeshabilitado(String locator) {
        String resolved = ctx().variables().resolve(locator);
        Assertions.assertThat(mobile().isEnabled(resolved)).
            as("El elemento '%s' deberia estar deshabilitado", resolved).
            isFalse();
        TestLogger.logInfo("NATIVE_EL", "Elemento deshabilitado (OK): " + resolved, null);
    }

    @Then("el texto del elemento {string} debe ser {string}")
    public void textoElementoDebeSer(String locator, String expectedText) {
        String resolvedLocator = ctx().variables().resolve(locator);
        String resolvedExpected = ctx().variables().resolve(expectedText);
        String actual = mobile().getText(resolvedLocator);
        Assertions.assertThat(actual).as("Texto del elemento '%s'", resolvedLocator).isEqualTo(resolvedExpected);
        TestLogger.logInfo("NATIVE_EL",
            "Texto validado en '" + resolvedLocator + "': " + actual, null);
    }

    @Then("el texto del elemento {string} debe contener {string}")
    public void textoElementoDebeContener(String locator, String partialText) {
        String resolvedLocator = ctx().variables().resolve(locator);
        String resolvedPart    = ctx().variables().resolve(partialText);
        String actual = mobile().getText(resolvedLocator);
        Assertions.assertThat(actual).
            as("Texto del elemento '%s' deberia contener '%s'", resolvedLocator, resolvedPart).
            contains(resolvedPart);
        TestLogger.logInfo("NATIVE_EL",
            "Texto contiene (OK) en '" + resolvedLocator + "': " + resolvedPart, null);
    }

    @Then("el atributo {string} del elemento {string} debe ser {string}")
    public void atributoElementoDebeSer(String attribute, String locator, String expectedValue) {
        String resolvedAttr     = ctx().variables().resolve(attribute);
        String resolvedLocator  = ctx().variables().resolve(locator);
        String resolvedExpected = ctx().variables().resolve(expectedValue);
        String actual = mobile().getAttribute(resolvedLocator, resolvedAttr);
        Assertions.assertThat(actual).as("Atributo '%s' del elemento '%s'", resolvedAttr, resolvedLocator).
            isEqualTo(resolvedExpected);
        TestLogger.logInfo("NATIVE_EL",
            "Atributo '" + resolvedAttr + "' validado: " + actual, null);
    }

    // =========================================================================
    // Variables (GIVEN)
    // =========================================================================

    @Given("guardo el texto del elemento nativo {string} como {string}")
    public void guardoTextoNativo(String locator, String variable) {
        String resolvedLocator = ctx().variables().resolve(locator);
        String resolvedVar     = ctx().variables().resolve(variable);
        String text = mobile().getText(resolvedLocator);
        ctx().variables().set(resolvedVar, text);
        TestLogger.logInfo("NATIVE_EL",
            "Texto de '" + resolvedLocator + "' guardado como '" + resolvedVar + "': " + text, null);
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Interacción v2.2.1
    // =========================================================================

    /**
     * Espera hasta que el elemento sea visible (timeout configurable internamente).
     */
    @When("espero hasta que el elemento {string} sea visible")
    public void esperarElementoVisible(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().waitForVisible(resolved);
        TestLogger.logInfo("NATIVE_EL", "Elemento visible: " + resolved, null);
    }

    /**
     * Espera un número específico de segundos (hard wait, útil en mobile).
     */
    @When("espero {int} segundos en el dispositivo")
    public void esperarSegundos(int seconds) {
        try {
            Thread.sleep(seconds * MILLIS_PER_SECOND);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        TestLogger.logInfo("NATIVE_EL", "Esperó " + seconds + " segundos", null);
    }

    /**
     * Espera hasta que un texto aparezca en pantalla.
     */
    @When("espero hasta que el texto {string} aparezca en pantalla")
    public void esperarTextoEnPantalla(String text) {
        String resolved = ctx().variables().resolve(text);
        mobile().waitForText(resolved);
        TestLogger.logInfo("NATIVE_EL", "Texto visible: " + resolved, null);
    }

    /**
     * Presiona una tecla de sistema del dispositivo (Back, Home, Recent).
     */
    @When("presiono la tecla de sistema {string}")
    public void presionoTeclaSistema(String key) {
        String resolved = ctx().variables().resolve(key);
        mobile().pressSystemKey(resolved);
        TestLogger.logInfo("NATIVE_EL", "Tecla de sistema presionada: " + resolved, null);
    }

    /**
     * Guarda el valor de un atributo de un elemento como variable.
     */
    @Given("guardo el valor del atributo {string} del elemento nativo {string} como {string}")
    public void guardoAtributoComoVariable(String attribute, String locator, String variable) {
        String resolvedLocator = ctx().variables().resolve(locator);
        String resolvedAttr    = ctx().variables().resolve(attribute);
        String resolvedVar     = ctx().variables().resolve(variable);
        String value = mobile().getAttribute(resolvedLocator, resolvedAttr);
        ctx().variables().set(resolvedVar, value);
        TestLogger.logInfo("NATIVE_EL",
            "Atributo '" + resolvedAttr + "' guardado como '" + resolvedVar + "': " + value, null);
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
