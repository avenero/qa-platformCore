package com.qa.mobilecore.steps.device;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Steps de simulación de sensores y condiciones del dispositivo.
 *
 * <p>Fase BDD: GIVEN (antes de la acción) / WHEN (durante el flujo).
 *
 * <p><b>Viabilidad por plataforma:</b>
 * <ul>
 *   <li>GPS: Android (setLocation API) y iOS (executeScript mobile:updateLocation)</li>
 *   <li>Red: Android via mobile:setConnectivity; iOS requiere Network Link Conditioner</li>
 *   <li>Modo avión: Android via mobile:setConnectivity; iOS no soportado en simulador</li>
 *   <li>Rotación: soportada en ambas plataformas</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class SensorSteps {

    // =========================================================================
    // GPS
    // =========================================================================

    @Given("simulo ubicacion GPS con latitud {double} y longitud {double}")
    public void simularUbicacionGps(double lat, double lon) {
        mobile().setGpsLocation(lat, lon);
        TestLogger.logInfo("SENSOR", "GPS simulado: " + lat + ", " + lon, null);
    }

    @StepDef(value = "mobile.sensor.legacy-configuro-gps",
             deprecated = true, replacedBy = "mobile.sensor#simularUbicacionGps")
    @Given("configuro la ubicacion GPS a latitud {double} y longitud {double}")
    public void configuroUbicacionGps(double lat, double lon) {
        simularUbicacionGps(lat, lon);
    }

    // =========================================================================
    // Red
    // =========================================================================

    @Given("simulo estado de red {string}")
    public void simularEstadoRed(String networkState) {
        String resolved = ctx().variables().resolve(networkState);
        mobile().setNetworkCondition(resolved);
        TestLogger.logInfo("SENSOR", "Red simulada: " + resolved, null);
    }

    @StepDef(value = "mobile.sensor.legacy-configuro-red",
             deprecated = true, replacedBy = "mobile.sensor#simularEstadoRed")
    @Given("configuro el estado de la conexion de red como {string}")
    public void configuroEstadoRed(String condition) {
        simularEstadoRed(condition);
    }

    // =========================================================================
    // Modo avión
    // =========================================================================

    @When("cambio el modo avion a {string}")
    public void cambioModoAvion(String estado) {
        String resolved = ctx().variables().resolve(estado).toLowerCase();
        boolean on = resolved.equals("activado") || resolved.equals("on") || resolved.equals("true");
        mobile().toggleAirplaneMode(on);
        TestLogger.logInfo("SENSOR", "Modo avion: " + (on ? "activado" : "desactivado"), null);
    }

    // =========================================================================
    // Orientación (via sensor de rotación)
    // =========================================================================

    @Given("simulo rotacion del dispositivo a {string}")
    public void simularRotacion(String orientation) {
        String resolved = ctx().variables().resolve(orientation).toLowerCase();
        switch (resolved) {
            case "portrait"  -> mobile().rotatePortrait();
            case "landscape" -> mobile().rotateLandscape();
            default -> throw new IllegalArgumentException(
                "Orientacion no reconocida: '" + resolved + "'. Valores: portrait | landscape");
        }
        TestLogger.logInfo("SENSOR", "Rotacion simulada: " + resolved, null);
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
