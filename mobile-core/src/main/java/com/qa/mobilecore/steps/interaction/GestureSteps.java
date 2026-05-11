package com.qa.mobilecore.steps.interaction;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.When;

/**
 * Steps de gestos táctiles: tap, long press, swipe, scroll, pinch, zoom.
 *
 * <p>Fase BDD: WHEN. Usa {@link com.qa.mobilecore.helper.GestureHelper} con
 * la W3C Actions API de Appium 8+ (sin TouchAction deprecado).
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class GestureSteps {

    private static final long MILLIS_PER_SECOND = 1000L;

    // =========================================================================
    // Tap
    // =========================================================================

    @When("toco el elemento {string}")
    public void tocoElElemento(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().tap(resolved);
        TestLogger.logInfo("GESTURE", "Tap en: " + resolved, null);
    }

    @When("hago doble tap en el elemento {string}")
    public void hagoDobleTap(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().doubleTap(resolved);
        TestLogger.logInfo("GESTURE", "Doble tap en: " + resolved, null);
    }

    // =========================================================================
    // Long Press
    // =========================================================================

    @When("mantengo presionado el elemento {string} por {int} segundos")
    public void mantengoPresionadoPorSegundos(String locator, int seconds) {
        String resolved = ctx().variables().resolve(locator);
        mobile().longPress(resolved, seconds * MILLIS_PER_SECOND);
        TestLogger.logInfo("GESTURE", "Long press " + seconds + "s en: " + resolved, null);
    }

    @When("hago tap largo en el elemento {string} por {int} milisegundos")
    public void hagoTapLargoPorMilisegundos(String locator, int millis) {
        String resolved = ctx().variables().resolve(locator);
        mobile().longPress(resolved, millis);
        TestLogger.logInfo("GESTURE", "Long press " + millis + "ms en: " + resolved, null);
    }

    // =========================================================================
    // Swipe
    // =========================================================================

    @When("hago swipe hacia {string}")
    public void hagoSwipeHacia(String direction) {
        String resolved = ctx().variables().resolve(direction);
        mobile().swipe(resolved);
        TestLogger.logInfo("GESTURE", "Swipe hacia: " + resolved, null);
    }

    @When("deslizo en el elemento {string} hacia {string}")
    public void deslizarEnElemento(String locator, String direction) {
        String resolvedLoc = ctx().variables().resolve(locator);
        String resolvedDir = ctx().variables().resolve(direction);
        // El swipe desde el elemento hacia una dirección equivale a un swipe global
        // restringido al área del elemento — usamos el swipe genérico de pantalla completa
        mobile().swipe(resolvedDir);
        TestLogger.logInfo("GESTURE", "Swipe en " + resolvedLoc + " hacia " + resolvedDir, null);
    }

    @When("hago swipe desde el elemento {string} hasta el elemento {string}")
    public void hagoSwipeEntreElementos(String fromLocator, String toLocator) {
        String resolvedFrom = ctx().variables().resolve(fromLocator);
        String resolvedTo   = ctx().variables().resolve(toLocator);
        mobile().swipeBetween(resolvedFrom, resolvedTo);
        TestLogger.logInfo("GESTURE", "Swipe de " + resolvedFrom + " a " + resolvedTo, null);
    }

    // =========================================================================
    // Scroll
    // =========================================================================

    @When("hago scroll mobile hasta el elemento {string}")
    public void hagoScrollHastaElemento(String locator) {
        String resolved = ctx().variables().resolve(locator);
        // Scroll genérico hacia arriba hasta encontrar el elemento
        for (int i = 0; i < 5; i++) {
            if (mobile().elementExists(resolved)) {
                break;
            }
            mobile().swipe("arriba");
        }
        TestLogger.logInfo("GESTURE", "Scroll hasta elemento: " + resolved, null);
    }

    @When("hago scroll hasta que el texto {string} sea visible")
    public void hagoScrollHastaTexto(String text) {
        String resolved = ctx().variables().resolve(text);
        mobile().scrollToText(resolved);
        TestLogger.logInfo("GESTURE", "Scroll hasta texto: " + resolved, null);
    }

    // =========================================================================
    // Pinch y Zoom
    // =========================================================================

    @When("hago pinch sobre el elemento {string}")
    public void hagoPinch(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().pinch(resolved);
        TestLogger.logInfo("GESTURE", "Pinch en: " + resolved, null);
    }

    @When("hago zoom sobre el elemento {string}")
    public void hagoZoom(String locator) {
        String resolved = ctx().variables().resolve(locator);
        mobile().zoom(resolved);
        TestLogger.logInfo("GESTURE", "Zoom en: " + resolved, null);
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Gestures v2.2.1
    // =========================================================================

    @When("hago tap en las coordenadas {int}, {int}")
    public void hagoTapEnCoordenadas(int x, int y) {
        mobile().tapAt(x, y);
        TestLogger.logInfo("GESTURE", "Tap en coordenadas (" + x + ", " + y + ")", null);
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
