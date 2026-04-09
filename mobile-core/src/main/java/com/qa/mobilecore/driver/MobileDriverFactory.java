package com.qa.mobilecore.driver;

import com.qa.common.logging.TestLogger;
import com.qa.mobilecore.model.DeviceDescriptor;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Factory para crear instancias de {@link AppiumDriver}.
 *
 * <p>Soporta Android (UiAutomator2) e iOS (XCUITest) con las APIs modernas de Appium 8+.
 * No utiliza {@code DesiredCapabilities} ni {@code TouchAction} (deprecados).
 *
 * <p>El ciclo de vida del driver (ThreadLocal, quit) es responsabilidad de
 * {@link MobileDriverManager}. Esta clase solo crea drivers, no los gestiona.
 *
 * <p><b>Análogo a:</b> {@code WebDriverFactory} de {@code web-core}.
 *
 * <p><b>Modos de ejecución:</b>
 * <ul>
 *   <li><b>Local / Emulador:</b> Appium en {@code localhost:port}, AVD o dispositivo conectado</li>
 *   <li><b>Remote Grid:</b> URL del hub (BrowserStack, Sauce Labs, Docker/K8s interno)</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class MobileDriverFactory {

    private MobileDriverFactory() {}

    // =========================================================================
    // API principal — crear desde DeviceDescriptor (uso normal)
    // =========================================================================

    /**
     * Crea un {@link AppiumDriver} a partir de un {@link DeviceDescriptor} y las
     * propiedades de la app extraídas de {@link DriverConfig}.
     *
     * <p>Este es el método de entrada principal, invocado por {@code MobileHelper}.
     *
     * @param device descriptor del dispositivo (plataforma, udid, server URL)
     * @param driverConfig configuración de la app y timeouts
     * @return AppiumDriver listo para usar
     */
    public static AppiumDriver create(DeviceDescriptor device, DriverConfig driverConfig) {
        TestLogger.logInfo("MOBILE_FACTORY",
            "Creando AppiumDriver: " + device, null);

        URL serverUrl = toUrl(device.getAppiumServerUrl());

        AppiumDriver driver = device.isAndroid()
            ? createAndroidDriver(device, driverConfig, serverUrl)
            : createIOSDriver(device, driverConfig, serverUrl);

        configureDriver(driver, driverConfig);

        TestLogger.logInfo("MOBILE_FACTORY",
            "AppiumDriver creado exitosamente para: " + device.getId(), null);
        return driver;
    }

    // =========================================================================
    // Configuración de la sesión (DriverConfig — Builder pattern)
    // =========================================================================

    /**
     * Configuración de sesión Appium.
     * Análogo a {@code WebDriverFactory.DriverConfig} de {@code web-core}.
     */
    public static final class DriverConfig {

        private String appPath       = "";
        private String appPackage    = "";
        private String appActivity   = "";
        private String bundleId      = "";
        private boolean autoLaunch   = true;
        private boolean noReset      = false;
        private int implicitWaitSec  = 10;

        public DriverConfig() {}

        public DriverConfig withAppPath(String path) {
            this.appPath = path != null ? path : "";
            return this;
        }

        public DriverConfig withAppPackage(String pkg) {
            this.appPackage = pkg != null ? pkg : "";
            return this;
        }

        public DriverConfig withAppActivity(String activity) {
            this.appActivity = activity != null ? activity : "";
            return this;
        }

        public DriverConfig withBundleId(String bundleId) {
            this.bundleId = bundleId != null ? bundleId : "";
            return this;
        }

        public DriverConfig withAutoLaunch(boolean autoLaunch) {
            this.autoLaunch = autoLaunch;
            return this;
        }

        public DriverConfig withNoReset(boolean noReset) {
            this.noReset = noReset;
            return this;
        }

        public DriverConfig withImplicitWait(int seconds) {
            this.implicitWaitSec = seconds;
            return this;
        }

        public String getAppPath()      { return appPath; }
        public String getAppPackage()   { return appPackage; }
        public String getAppActivity()  { return appActivity; }
        public String getBundleId()     { return bundleId; }
        public boolean isAutoLaunch()   { return autoLaunch; }
        public boolean isNoReset()      { return noReset; }
        public int getImplicitWaitSec() { return implicitWaitSec; }
    }

    // =========================================================================
    // Creación interna — Android
    // =========================================================================

    private static AndroidDriver createAndroidDriver(
            DeviceDescriptor device, DriverConfig cfg, URL serverUrl) {

        UiAutomator2Options options = new UiAutomator2Options()
            .setPlatformName("Android")
            .setAutomationName("UiAutomator2");

        if (device.getDeviceName() != null && !device.getDeviceName().isBlank()) {
            options.setDeviceName(device.getDeviceName());
        }
        if (device.getPlatformVersion() != null && !device.getPlatformVersion().isBlank()) {
            options.setPlatformVersion(device.getPlatformVersion());
        }
        if (device.getUdid() != null && !device.getUdid().isBlank()) {
            options.setUdid(device.getUdid());
        }

        // App: se puede indicar por path completo o por package+activity (app ya instalada)
        if (cfg.getAppPath() != null && !cfg.getAppPath().isBlank()) {
            options.setApp(cfg.getAppPath());
        } else if (!cfg.getAppPackage().isBlank() && !cfg.getAppActivity().isBlank()) {
            options.setAppPackage(cfg.getAppPackage());
            options.setAppActivity(cfg.getAppActivity());
        }

        options.setCapability("autoLaunch", cfg.isAutoLaunch());
        options.setCapability("noReset", cfg.isNoReset());

        TestLogger.logInfo("MOBILE_FACTORY",
            "Creando AndroidDriver (UiAutomator2) en: " + serverUrl, null);
        return new AndroidDriver(serverUrl, options);
    }

    // =========================================================================
    // Creación interna — iOS
    // =========================================================================

    private static IOSDriver createIOSDriver(
            DeviceDescriptor device, DriverConfig cfg, URL serverUrl) {

        XCUITestOptions options = new XCUITestOptions()
            .setPlatformName("iOS")
            .setAutomationName("XCUITest");

        if (device.getDeviceName() != null && !device.getDeviceName().isBlank()) {
            options.setDeviceName(device.getDeviceName());
        }
        if (device.getPlatformVersion() != null && !device.getPlatformVersion().isBlank()) {
            options.setPlatformVersion(device.getPlatformVersion());
        }
        if (device.getUdid() != null && !device.getUdid().isBlank()) {
            options.setUdid(device.getUdid());
        }

        if (cfg.getAppPath() != null && !cfg.getAppPath().isBlank()) {
            options.setApp(cfg.getAppPath());
        } else if (!cfg.getBundleId().isBlank()) {
            options.setBundleId(cfg.getBundleId());
        }

        options.setCapability("autoLaunch", cfg.isAutoLaunch());
        options.setCapability("noReset", cfg.isNoReset());

        TestLogger.logInfo("MOBILE_FACTORY",
            "Creando IOSDriver (XCUITest) en: " + serverUrl, null);
        return new IOSDriver(serverUrl, options);
    }

    // =========================================================================
    // Configuración post-creación
    // =========================================================================

    private static void configureDriver(AppiumDriver driver, DriverConfig cfg) {
        try {
            driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(cfg.getImplicitWaitSec()));
            TestLogger.logInfo("MOBILE_FACTORY",
                "Driver configurado (implicit wait: " + cfg.getImplicitWaitSec() + "s)", null);
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_FACTORY",
                "No se pudo configurar timeout del driver: " + e.getMessage(), null);
        }
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private static URL toUrl(String urlStr) {
        try {
            return URI.create(urlStr).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "URL de Appium inválida: '" + urlStr + "'. " +
                "Formato esperado: http://localhost:4723", e);
        }
    }
}
