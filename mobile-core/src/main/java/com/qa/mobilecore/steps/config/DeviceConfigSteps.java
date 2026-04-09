package com.qa.mobilecore.steps.config;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;

import java.util.Map;

/**
 * Steps de configuración del dispositivo mobile.
 *
 * <p>Fase BDD: GIVEN. Establece plataforma, tipo de dispositivo, orientación
 * y URL de Appium antes de iniciar la sesión.
 *
 * <p>Los steps no conocen negocio: son genéricos y reutilizables en cualquier
 * proyecto que use mobile-core.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DeviceConfigSteps {

    // =========================================================================
    // Plataforma y tipo
    // =========================================================================

    @Given("configuro el dispositivo movil como {string}")
    public void configuroDispositivoMovilComo(String platform) {
        String resolved = ctx().variables().resolve(platform);
        mobile().setPlatform(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Plataforma: " + resolved, null);
    }

    @Given("selecciono el dispositivo movil con id {string}")
    public void seleccionoDispositivoConId(String deviceId) {
        String resolved = ctx().variables().resolve(deviceId);
        mobile().setDeviceId(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Device ID seleccionado: " + resolved, null);
    }

    @Given("configuro la version de plataforma movil {string}")
    public void configuroPlatformVersion(String version) {
        String resolved = ctx().variables().resolve(version);
        mobile().setPlatformVersion(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Version de plataforma: " + resolved, null);
    }

    @Given("configuro que la app se ejecute en un emulador")
    public void configuroEmulador() {
        mobile().setDeviceType("ANDROID_EMULATOR");
        TestLogger.logInfo("DEVICE_CFG", "Target: emulador Android", null);
    }

    @Given("configuro que la app se ejecute en un dispositivo fisico")
    public void configuroDispositivoFisico() {
        mobile().setDeviceType("ANDROID_PHYSICAL");
        TestLogger.logInfo("DEVICE_CFG", "Target: dispositivo fisico Android", null);
    }

    @Given("configuro que la app se ejecute en un simulador de iOS")
    public void configuroSimuladorIOS() {
        mobile().setDeviceType("IOS_SIMULATOR");
        TestLogger.logInfo("DEVICE_CFG", "Target: simulador iOS", null);
    }

    @Given("configuro el servidor de Appium en {string}")
    public void configuroServidorAppium(String url) {
        String resolved = ctx().variables().resolve(url);
        mobile().setAppiumServerUrl(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Appium server: " + resolved, null);
    }

    // =========================================================================
    // Orientación
    // =========================================================================

    @Given("configuro la orientacion del dispositivo como {string}")
    public void configuroOrientacion(String orientacion) {
        String resolved = ctx().variables().resolve(orientacion).toLowerCase();
        switch (resolved) {
            case "portrait"   -> mobile().rotatePortrait();
            case "landscape"  -> mobile().rotateLandscape();
            default           -> throw new IllegalArgumentException(
                "Orientacion no reconocida: '" + resolved + "'. Valores: portrait | landscape");
        }
        TestLogger.logInfo("DEVICE_CFG", "Orientacion configurada: " + resolved, null);
    }

    // =========================================================================
    // Forma legacy (backward-compatible con Fase 2)
    // =========================================================================

    @Given("configuro el dispositivo {string} con plataforma {string} y version {string}")
    public void configuroDispositivo(String deviceName, String platform, String version) {
        String rPlatform = ctx().variables().resolve(platform);
        String rVersion  = ctx().variables().resolve(version);
        mobile().setPlatform(rPlatform);
        mobile().setPlatformVersion(rVersion);
        TestLogger.logInfo("DEVICE_CFG",
            "Dispositivo: " + deviceName + " | " + rPlatform + " " + rVersion, null);
    }

    @Given("configuro capacidades del dispositivo")
    public void configuroCapacidades(Map<String, String> capabilities) {
        capabilities.forEach((k, v) -> {
            String resolved = ctx().variables().resolve(v);
            System.setProperty(k, resolved);
        });
        TestLogger.logInfo("DEVICE_CFG", "Capacidades configuradas: " + capabilities.size(), null);
    }

    @Given("configuro UDID {string}")
    public void configuroUdid(String udid) {
        String resolved = ctx().variables().resolve(udid);
        com.qa.common.config.ConfigManager.getInstance();
        System.setProperty(com.qa.mobilecore.config.MobileConfigKeys.UDID, resolved);
        TestLogger.logInfo("DEVICE_CFG", "UDID: " + resolved, null);
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
