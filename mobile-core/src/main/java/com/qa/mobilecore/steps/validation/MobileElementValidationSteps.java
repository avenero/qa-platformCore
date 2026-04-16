package com.qa.mobilecore.steps.validation;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.appium.java_client.AppiumDriver;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Steps de validación de elementos nativos mobile.
 *
 * <p>Fase BDD: THEN. Valida visibilidad, texto, estado y cantidad de elementos.
 * Usa AssertJ para mensajes de fallo descriptivos.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class MobileElementValidationSteps {

    // =========================================================================
    // Visibilidad (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que el elemento mobile {string} este visible")
    public void verificoElementoVisible(String locator) {
        String resolved = ctx().variables().resolve(locator);
        Assertions.assertThat(mobile().isVisible(resolved))
            .as("El elemento mobile '%s' deberia ser visible", resolved)
            .isTrue();
        TestLogger.logInfo("MOBILE_VAL", "Visible (OK): " + resolved, null);
    }

    @Then("verifico que el elemento mobile {string} NO este visible")
    public void verificoElementoNoVisible(String locator) {
        String resolved = ctx().variables().resolve(locator);
        Assertions.assertThat(mobile().isVisible(resolved))
            .as("El elemento mobile '%s' NO deberia ser visible", resolved)
            .isFalse();
        TestLogger.logInfo("MOBILE_VAL", "No visible (OK): " + resolved, null);
    }

    @Then("deberia ver el texto {string} en la pantalla")
    public void deberiaVerTextoEnPantalla(String text) {
        String resolved = ctx().variables().resolve(text);
        boolean exists = mobile().elementExists("text:" + resolved);
        Assertions.assertThat(exists)
            .as("El texto '%s' deberia ser visible en pantalla", resolved)
            .isTrue();
        TestLogger.logInfo("MOBILE_VAL", "Texto en pantalla (OK): " + resolved, null);
    }

    @Then("no deberia ver el texto {string} en la pantalla")
    public void noDeberiaVerTextoEnPantalla(String text) {
        String resolved = ctx().variables().resolve(text);
        boolean exists = mobile().elementExists("text:" + resolved);
        Assertions.assertThat(exists)
            .as("El texto '%s' NO deberia ser visible en pantalla", resolved)
            .isFalse();
        TestLogger.logInfo("MOBILE_VAL", "Texto ausente (OK): " + resolved, null);
    }

    // =========================================================================
    // Texto (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que el texto del elemento mobile {string} sea {string}")
    public void verificoTextoElemento(String locator, String expectedText) {
        String resolvedLocator  = ctx().variables().resolve(locator);
        String resolvedExpected = ctx().variables().resolve(expectedText);
        String actual = mobile().getText(resolvedLocator);
        Assertions.assertThat(actual)
            .as("Texto del elemento mobile '%s'", resolvedLocator)
            .isEqualTo(resolvedExpected);
        TestLogger.logInfo("MOBILE_VAL",
            "Texto validado en '" + resolvedLocator + "': " + actual, null);
    }

    @Then("verifico que el campo mobile {string} contenga el texto {string}")
    public void verificoCampoContenga(String locator, String text) {
        String resolvedLocator = ctx().variables().resolve(locator);
        String resolvedText    = ctx().variables().resolve(text);
        String actual = mobile().getText(resolvedLocator);
        Assertions.assertThat(actual)
            .as("Campo mobile '%s' deberia contener '%s'", resolvedLocator, resolvedText)
            .contains(resolvedText);
        TestLogger.logInfo("MOBILE_VAL",
            "Contenido validado en '" + resolvedLocator + "': " + resolvedText, null);
    }

    // =========================================================================
    // Estado (backward-compatible con Fase 2)
    // =========================================================================

    @Then("verifico que el elemento mobile {string} este habilitado")
    public void verificoElementoHabilitado(String locator) {
        String resolved = ctx().variables().resolve(locator);
        Assertions.assertThat(mobile().isEnabled(resolved))
            .as("El elemento mobile '%s' deberia estar habilitado", resolved)
            .isTrue();
        TestLogger.logInfo("MOBILE_VAL", "Habilitado (OK): " + resolved, null);
    }

    @Then("verifico que el elemento mobile {string} este deshabilitado")
    public void verificoElementoDeshabilitado(String locator) {
        String resolved = ctx().variables().resolve(locator);
        Assertions.assertThat(mobile().isEnabled(resolved))
            .as("El elemento mobile '%s' deberia estar deshabilitado", resolved)
            .isFalse();
        TestLogger.logInfo("MOBILE_VAL", "Deshabilitado (OK): " + resolved, null);
    }

    // =========================================================================
    // Atributos
    // =========================================================================

    @Then("el atributo {string} del elemento mobile {string} debe ser {string}")
    public void atributoElementoMobileDebeSer(String attribute, String locator, String expected) {
        String resolvedAttr    = ctx().variables().resolve(attribute);
        String resolvedLocator = ctx().variables().resolve(locator);
        String resolvedExpected = ctx().variables().resolve(expected);
        String actual = mobile().getAttribute(resolvedLocator, resolvedAttr);
        Assertions.assertThat(actual)
            .as("Atributo '%s' del elemento mobile '%s'", resolvedAttr, resolvedLocator)
            .isEqualTo(resolvedExpected);
        TestLogger.logInfo("MOBILE_VAL",
            "Atributo '" + resolvedAttr + "' validado: " + actual, null);
    }

    // =========================================================================
    // Listas
    // =========================================================================

    @Then("la lista {string} debe tener {int} elementos")
    public void listaDebeTenerElementos(String listLocator, int expectedCount) {
        String resolved = ctx().variables().resolve(listLocator);
        List<WebElement> items = mobile().findElements(resolved);
        Assertions.assertThat(items)
            .as("La lista '%s' deberia tener %d elementos", resolved, expectedCount)
            .hasSize(expectedCount);
        TestLogger.logInfo("MOBILE_VAL",
            "Lista '" + resolved + "' tiene " + items.size() + " elementos (OK)", null);
    }

    @Then("la lista {string} debe contener un elemento con texto {string}")
    public void listaDebeContenerElementoConTexto(String listLocator, String text) {
        String resolvedList = ctx().variables().resolve(listLocator);
        String resolvedText = ctx().variables().resolve(text);
        List<WebElement> items = mobile().findElements(resolvedList);
        boolean found = items.stream()
            .anyMatch(el -> el.getText().contains(resolvedText));
        Assertions.assertThat(found)
            .as("La lista '%s' deberia contener un elemento con texto '%s'",
                resolvedList, resolvedText)
            .isTrue();
        TestLogger.logInfo("MOBILE_VAL",
            "Lista contiene '" + resolvedText + "' (OK)", null);
    }

    // =========================================================================
    // Screenshot (backward-compatible con Fase 2)
    // =========================================================================

    @Then("tomo screenshot mobile")
    public void tomoScreenshot() {
        try {
            AppiumDriver driver = mobile().driver();
            if (driver instanceof TakesScreenshot ts) {
                ts.getScreenshotAs(OutputType.BASE64);
                TestLogger.logInfo("MOBILE_VAL", "Screenshot capturado", null);
            }
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_VAL",
                "No se pudo capturar screenshot: " + e.getMessage(), null);
        }
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Validación v2.2.1
    // =========================================================================

    /**
     * Verifica la cantidad de elementos que coinciden con un locator.
     */
    @Then("verifico que existan {int} elementos {string}")
    public void verificoCantidadElementos(int expectedCount, String locator) {
        String resolved = ctx().variables().resolve(locator);
        List<WebElement> items = mobile().findElements(resolved);
        Assertions.assertThat(items)
            .as("Cantidad de elementos '%s'", resolved)
            .hasSize(expectedCount);
        TestLogger.logInfo("MOBILE_VAL",
            "Cantidad de '" + resolved + "' = " + items.size() + " (OK)", null);
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
