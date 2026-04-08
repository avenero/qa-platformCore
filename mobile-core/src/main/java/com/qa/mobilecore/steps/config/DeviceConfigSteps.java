package com.qa.mobilecore.steps.config;

import com.qa.common.logging.TestLogger;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de configuracion del dispositivo movil.
 * Nuevo en Fase 2 — mobile-core no tenia steps anteriormente.
 * @author Abel Venero
 * @since 2.0.0
 */
public class DeviceConfigSteps {

    @Given("configuro el dispositivo {string} con plataforma {string} y version {string}")
    public void configuroDispositivo(String deviceName, String platform, String version) {
        TestLogger.logInfo("DEVICE_CFG", "Dispositivo: " + deviceName + " | " + platform + " " + version, null);
    }

    @Given("configuro capacidades del dispositivo")
    public void configuroCapacidades(Map<String, String> capabilities) {
        TestLogger.logInfo("DEVICE_CFG", "Capacidades configuradas: " + capabilities.size(), null);
    }

    @Given("configuro UDID {string}")
    public void configuroUdid(String udid) {
        TestLogger.logInfo("DEVICE_CFG", "UDID configurado: " + udid, null);
    }
}
