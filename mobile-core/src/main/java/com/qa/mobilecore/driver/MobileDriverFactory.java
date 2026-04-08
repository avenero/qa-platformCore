package com.qa.mobilecore.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;

/**
 * Factory para la creación y configuración de AppiumDrivers.
 * Maneja la inicialización de drivers para Android e iOS.
 *
 * <p><b>Actualizado a Appium 8+ API con backward compatibility.</b></p>
 */
public class MobileDriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(MobileDriverFactory.class);

    public enum Platform {
        ANDROID, IOS
    }

    /**
     * Crea AndroidDriver con UiAutomator2Options (Appium 8+).
     *
     * @param options Opciones de UiAutomator2
     * @param serverUrl URL del servidor Appium
     * @return AndroidDriver configurado
     */
    public static AndroidDriver createAndroidDriver(UiAutomator2Options options, URL serverUrl) {
        logger.info("✅ Creando AndroidDriver con UiAutomator2Options (Appium 8+)");
        options.setPlatformName("Android");
        return new AndroidDriver(serverUrl, options);
    }

    /**
     * Crea IOSDriver con XCUITestOptions (Appium 8+).
     *
     * @param options Opciones de XCUITest
     * @param serverUrl URL del servidor Appium
     * @return IOSDriver configurado
     */
    public static IOSDriver createIOSDriver(XCUITestOptions options, URL serverUrl) {
        logger.info("✅ Creando IOSDriver con XCUITestOptions (Appium 8+)");
        options.setPlatformName("iOS");
        return new IOSDriver(serverUrl, options);
    }

    private static void configureDriver(AppiumDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }


    // =========================================================================
    // MÉTODOS HELPER MODERNOS (Appium 8+) - RECOMENDADOS
    // =========================================================================

    /**
     * Obtiene opciones configuradas para Android (Appium 8+).
     *
     * @param deviceName Nombre del dispositivo
     * @param appPath Ruta de la aplicación
     * @return UiAutomator2Options configurado
     */
    public static UiAutomator2Options getAndroidOptions(String deviceName, String appPath) {
        UiAutomator2Options options = new UiAutomator2Options()
            .setDeviceName(deviceName)
            .setApp(appPath)
            .setPlatformName("Android")
            .setAutomationName("UiAutomator2");

        logger.debug("✅ UiAutomator2Options creado para dispositivo: {}", deviceName);
        return options;
    }

    /**
     * Obtiene opciones configuradas para iOS (Appium 8+).
     *
     * @param deviceName Nombre del dispositivo
     * @param appPath Ruta de la aplicación
     * @return XCUITestOptions configurado
     */
    public static XCUITestOptions getIOSOptions(String deviceName, String appPath) {
        XCUITestOptions options = new XCUITestOptions()
            .setDeviceName(deviceName)
            .setApp(appPath)
            .setPlatformName("iOS")
            .setAutomationName("XCUITest");

        logger.debug("✅ XCUITestOptions creado para dispositivo: {}", deviceName);
        return options;
    }
}

