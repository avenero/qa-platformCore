package com.qa.mobilecore.steps.config;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;

import java.util.Map;

/**
 * Steps de configuración del dispositivo mobile.
 *
 * <p>Componente padre: {@code mobile.device.config}
 * ({@link com.qa.mobilecore.components.DeviceConfigComponent}).
 * Fase BDD: GIVEN.
 *
 * <p>Establece plataforma, tipo de dispositivo, orientación y URL de Appium antes de
 * iniciar la sesión. Los steps son genéricos y reutilizables en cualquier proyecto
 * que use mobile-core.
 *
 * <p>Todos los steps canónicos llevan {@link StepDef} con ID explícito. El formato es
 * {@code mobile.device.config.{sub-id}}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DeviceConfigSteps {

    // =========================================================================
    // Plataforma y tipo de dispositivo
    // =========================================================================

    /**
     * Configura la plataforma del dispositivo ({@code "android"} o {@code "ios"}).
     * Es el step principal de configuración de plataforma.
     */
    @StepDef(value = "mobile.device.config.platform",
             displayName = "Configurar plataforma móvil")
    @Given("configuro el dispositivo movil como {string}")
    public void configuroDispositivoMovilComo(String platform) {
        String resolved = ctx().variables().resolve(platform);
        mobile().setPlatform(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Plataforma: " + resolved, null);
    }

    /**
     * Selecciona el dispositivo físico o virtual por su ID de dispositivo (UDID en iOS,
     * serial en Android).
     */
    @StepDef(value = "mobile.device.config.device-id",
             displayName = "Seleccionar dispositivo por ID")
    @Given("selecciono el dispositivo movil con id {string}")
    public void seleccionoDispositivoConId(String deviceId) {
        String resolved = ctx().variables().resolve(deviceId);
        mobile().setDeviceId(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Device ID seleccionado: " + resolved, null);
    }

    /**
     * Configura la versión de la plataforma del sistema operativo móvil
     * (ej: {@code "14.0"} para iOS, {@code "13"} para Android).
     */
    @StepDef(value = "mobile.device.config.platform-version",
             displayName = "Configurar versión de plataforma")
    @Given("configuro la version de plataforma movil {string}")
    public void configuroPlatformVersion(String version) {
        String resolved = ctx().variables().resolve(version);
        mobile().setPlatformVersion(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Version de plataforma: " + resolved, null);
    }

    // =========================================================================
    // Tipo de dispositivo — emulador / físico / simulador
    // =========================================================================

    /**
     * Configura que la app se ejecute en un emulador Android.
     */
    @StepDef(value = "mobile.device.config.emulator",
             displayName = "Ejecutar en emulador Android")
    @Given("configuro que la app se ejecute en un emulador")
    public void configuroEmulador() {
        mobile().setDeviceType("ANDROID_EMULATOR");
        TestLogger.logInfo("DEVICE_CFG", "Target: emulador Android", null);
    }

    /**
     * Configura que la app se ejecute en un dispositivo físico Android.
     */
    @StepDef(value = "mobile.device.config.physical",
             displayName = "Ejecutar en dispositivo físico Android")
    @Given("configuro que la app se ejecute en un dispositivo fisico")
    public void configuroDispositivoFisico() {
        mobile().setDeviceType("ANDROID_PHYSICAL");
        TestLogger.logInfo("DEVICE_CFG", "Target: dispositivo fisico Android", null);
    }

    /**
     * Configura que la app se ejecute en un simulador iOS.
     */
    @StepDef(value = "mobile.device.config.ios-simulator",
             displayName = "Ejecutar en simulador iOS")
    @Given("configuro que la app se ejecute en un simulador de iOS")
    public void configuroSimuladorIOS() {
        mobile().setDeviceType("IOS_SIMULATOR");
        TestLogger.logInfo("DEVICE_CFG", "Target: simulador iOS", null);
    }

    // =========================================================================
    // Servidor Appium
    // =========================================================================

    /**
     * Configura la URL del servidor Appium al que se conectará el driver.
     * Por defecto suele ser {@code "http://localhost:4723"}.
     */
    @StepDef(value = "mobile.device.config.appium-server",
             displayName = "Configurar URL del servidor Appium")
    @Given("configuro el servidor de Appium en {string}")
    public void configuroServidorAppium(String url) {
        String resolved = ctx().variables().resolve(url);
        mobile().setAppiumServerUrl(resolved);
        TestLogger.logInfo("DEVICE_CFG", "Appium server: " + resolved, null);
    }

    // =========================================================================
    // Orientación
    // =========================================================================

    /**
     * Configura la orientación de pantalla del dispositivo
     * ({@code "portrait"} o {@code "landscape"}).
     */
    @StepDef(value = "mobile.device.config.orientation",
             displayName = "Configurar orientación de pantalla")
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
    // Capacidades genéricas (DataTable)
    // =========================================================================

    /**
     * Configura múltiples capacidades Appium de una vez mediante una DataTable
     * ({@code | clave | valor |}).
     */
    @StepDef(value = "mobile.device.config.capabilities",
             displayName = "Configurar capacidades del dispositivo (DataTable)")
    @Given("configuro capacidades del dispositivo")
    public void configuroCapacidades(Map<String, String> capabilities) {
        capabilities.forEach((k, v) -> {
            String resolved = ctx().variables().resolve(v);
            System.setProperty(k, resolved);
        });
        TestLogger.logInfo("DEVICE_CFG", "Capacidades configuradas: " + capabilities.size(), null);
    }

    /**
     * Configura el UDID del dispositivo iOS directamente.
     */
    @StepDef(value = "mobile.device.config.udid",
             displayName = "Configurar UDID del dispositivo")
    @Given("configuro UDID {string}")
    public void configuroUdid(String udid) {
        String resolved = ctx().variables().resolve(udid);
        System.setProperty(MobileConfigKeys.UDID, resolved);
        TestLogger.logInfo("DEVICE_CFG", "UDID: " + resolved, null);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private ExecutionContext ctx() {
        return ExecutionContext.requireCurrent();
    }

    private MobileHelper mobile() {
        return ctx().service(MobileHelper.class);
    }
}
