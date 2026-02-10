package com.scotia.qa.mobilecore.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
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
     * Crea un driver móvil con DesiredCapabilities (legacy).
     *
     * @deprecated Usar versiones con UiAutomator2Options o XCUITestOptions.
     *             Mantenido para backward compatibility.
     */
    @Deprecated
    public static AppiumDriver createDriver(Platform platform, DesiredCapabilities capabilities, String appiumUrl) {
        logger.warn("⚠️ DesiredCapabilities está deprecado. Migrar a UiAutomator2Options/XCUITestOptions");

        AppiumDriver driver;

        try {
            URL serverUrl = new URL(appiumUrl);

            switch (platform) {
                case ANDROID:
                    driver = createAndroidDriver(capabilities, serverUrl);
                    break;
                case IOS:
                    driver = createIOSDriver(capabilities, serverUrl);
                    break;
                default:
                    throw new IllegalArgumentException("Plataforma no soportada: " + platform);
            }

            configureDriver(driver);
            logger.info("AppiumDriver inicializado para plataforma: {}", platform);
            return driver;

        } catch (MalformedURLException e) {
            throw new RuntimeException("URL de Appium inválida: " + appiumUrl, e);
        }
    }

    /**
     * Crea AndroidDriver con DesiredCapabilities (legacy).
     *
     * @deprecated Usar {@link #createAndroidDriver(UiAutomator2Options, URL)}
     */
    @Deprecated
    private static AndroidDriver createAndroidDriver(DesiredCapabilities capabilities, URL serverUrl) {
        // Capacidades específicas para Android
        capabilities.setCapability("platformName", "Android");

        // Convertir a UiAutomator2Options para Appium 8+
        UiAutomator2Options options = new UiAutomator2Options();
        capabilities.asMap().forEach(options::setCapability);

        return new AndroidDriver(serverUrl, options);
    }

    /**
     * Crea IOSDriver con DesiredCapabilities (legacy).
     *
     * @deprecated Usar {@link #createIOSDriver(XCUITestOptions, URL)}
     */
    @Deprecated
    private static IOSDriver createIOSDriver(DesiredCapabilities capabilities, URL serverUrl) {
        // Capacidades específicas para iOS
        capabilities.setCapability("platformName", "iOS");

        // Convertir a XCUITestOptions para Appium 8+
        XCUITestOptions options = new XCUITestOptions();
        capabilities.asMap().forEach(options::setCapability);

        return new IOSDriver(serverUrl, options);
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
    // MÉTODOS HELPER LEGACY (DesiredCapabilities) - DEPRECADOS
    // =========================================================================

    /**
     * @deprecated Usar {@link #getAndroidOptions(String, String)}
     */
    @Deprecated
    public static DesiredCapabilities getAndroidCapabilities(String deviceName, String appPath) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName", deviceName);
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("app", appPath);
        capabilities.setCapability("automationName", "UiAutomator2");
        return capabilities;
    }

    /**
     * @deprecated Usar {@link #getIOSOptions(String, String)}
     */
    @Deprecated
    public static DesiredCapabilities getIOSCapabilities(String deviceName, String appPath) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName", deviceName);
        capabilities.setCapability("platformName", "iOS");
        capabilities.setCapability("app", appPath);
        capabilities.setCapability("automationName", "XCUITest");
        return capabilities;
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

