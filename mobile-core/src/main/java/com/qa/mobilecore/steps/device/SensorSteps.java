package com.qa.mobilecore.steps.device;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.Given;

/**
 * Steps de simulacion de sensores del dispositivo: GPS, acelerometro, bateria.
 * Nuevo en Fase 2.
 * @author Abel Venero
 * @since 2.0.0
 */
public class SensorSteps {

    @Given("simulo ubicacion GPS con latitud {double} y longitud {double}")
    public void simularUbicacionGps(double lat, double lon) {
        TestLogger.logInfo("SENSOR", "Simulando GPS: " + lat + ", " + lon, null);
    }

    @Given("simulo nivel de bateria {int} por ciento")
    public void simularNivelBateria(int level) {
        TestLogger.logInfo("SENSOR", "Bateria simulada: " + level + "%", null);
    }

    @Given("simulo estado de red {string}")
    public void simularEstadoRed(String networkState) {
        TestLogger.logInfo("SENSOR", "Red simulada: " + networkState, null);
    }

    @Given("simulo rotacion del dispositivo a {string}")
    public void simularRotacion(String orientation) {
        TestLogger.logInfo("SENSOR", "Rotacion: " + orientation, null);
    }
}
