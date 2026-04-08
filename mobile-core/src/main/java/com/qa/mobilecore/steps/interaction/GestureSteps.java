package com.qa.mobilecore.steps.interaction;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.When;

/**
 * Steps de gestos touch: tap, long press, swipe, pinch, zoom.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class GestureSteps {

    @When("toco el elemento {string}")
    public void tocoElElemento(String locator) {
        TestLogger.logInfo("GESTURE", "Tap en: " + locator, null);
    }

    @When("mantengo presionado el elemento {string} por {int} segundos")
    public void mantengoPresionadoElElemento(String locator, int seconds) {
        TestLogger.logInfo("GESTURE", "Long press " + seconds + "s en: " + locator, null);
    }

    @When("deslizo hacia {string}")
    public void deslizoCiaDireccion(String direction) {
        TestLogger.logInfo("GESTURE", "Swipe hacia: " + direction, null);
    }

    @When("deslizo en el elemento {string} hacia {string}")
    public void deslizarEnElemento(String locator, String direction) {
        TestLogger.logInfo("GESTURE", "Swipe en " + locator + " hacia " + direction, null);
    }

    @When("hago pinch sobre el elemento {string}")
    public void hagoPinch(String locator) {
        TestLogger.logInfo("GESTURE", "Pinch en: " + locator, null);
    }

    @When("hago zoom sobre el elemento {string}")
    public void hagoZoom(String locator) {
        TestLogger.logInfo("GESTURE", "Zoom en: " + locator, null);
    }

    @When("hago scroll mobile hasta el elemento {string}")
    public void hagoScrollHastaElemento(String locator) {
        TestLogger.logInfo("GESTURE", "Scroll hasta: " + locator, null);
    }
}
