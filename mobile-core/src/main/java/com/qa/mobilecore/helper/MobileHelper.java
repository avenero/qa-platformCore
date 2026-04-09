package com.qa.mobilecore.helper;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.mobilecore.appium.AppiumServerManager;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.driver.MobileDriverFactory;
import com.qa.mobilecore.driver.MobileDriverManager;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;
import com.qa.mobilecore.pool.DevicePool;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.SupportsContextSwitching;
import io.appium.java_client.remote.SupportsRotation;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fachada principal de la capa mobile-core.
 *
 * <p>Centraliza toda la interacción con Appium para los steps BDD.
 * Análogo a {@code WebHelper} de {@code web-core}.
 *
 * <p>Se registra en el {@link com.qa.common.runtime.ServiceRegistry} por {@code MobilePlugin}
 * y se accede desde los steps vía:
 * <pre>
 * MobileHelper mobile = ExecutionContext.requireCurrent().service(MobileHelper.class);
 * </pre>
 *
 * <p><b>Gestión del driver:</b> el {@link AppiumDriver} se crea de forma lazy
 * (primera vez que un step lo necesita). El ciclo de vida completo es:
 * <ol>
 *   <li>{@link #initSession(DeviceDescriptor)} — crea el driver y lo registra en
 *       {@link MobileDriverManager}</li>
 *   <li>Los steps llaman a {@link #driver()} para obtener el driver ya creado</li>
 *   <li>{@link #quitSession()} — cierra el driver al finalizar el escenario</li>
 * </ol>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class MobileHelper {

    private final ConfigManager config;

    /** Descriptor del dispositivo asignado a esta sesión (null hasta initSession). */
    private DeviceDescriptor assignedDevice;

    /** Config de driver construida a partir de ExecutionConfig/properties. */
    private MobileDriverFactory.DriverConfig driverConfig;

    public MobileHelper() {
        this.config = ConfigManager.getInstance();
    }

    // =========================================================================
    // Gestión de sesión
    // =========================================================================

    /**
     * Inicializa la sesión Appium para el dispositivo dado.
     * Invocado por {@code MobilePlugin.onScenarioStart()} o por un step de configuración.
     *
     * @param device descriptor del dispositivo a usar
     */
    public void initSession(DeviceDescriptor device) {
        this.assignedDevice = device;
        this.driverConfig   = buildDriverConfig();

        boolean autoStart   = config.getBoolean(MobileConfigKeys.APPIUM_AUTO_START, false);
        int startupTimeout  = config.getInt(MobileConfigKeys.APPIUM_STARTUP_TIMEOUT_SEC, 30);

        AppiumServerManager.ensureRunning(device.getAppiumServerUrl(), autoStart, startupTimeout);

        AppiumDriver d = MobileDriverFactory.create(device, driverConfig);
        MobileDriverManager.setDriver(d);

        TestLogger.logInfo("MOBILE_HELPER",
            "Sesion iniciada para dispositivo: " + device.getId(), null);
    }

    /**
     * Cierra la sesión Appium y libera el dispositivo en el pool.
     */
    public void quitSession() {
        if (assignedDevice != null) {
            DevicePool.getInstance().release(assignedDevice.getId());
            TestLogger.logInfo("MOBILE_HELPER",
                "Dispositivo liberado del pool: " + assignedDevice.getId(), null);
        }
        MobileDriverManager.quitDriverSafely();
    }

    /**
     * Obtiene el driver activo. Lanza excepción si la sesión no fue iniciada.
     */
    public AppiumDriver driver() {
        return MobileDriverManager.getDriver();
    }

    /**
     * @return true si hay una sesión Appium activa
     */
    public boolean hasActiveSession() {
        return MobileDriverManager.isInitialized();
    }

    // =========================================================================
    // Configuración de dispositivo (desde steps)
    // =========================================================================

    /**
     * Configura la plataforma del dispositivo para la próxima sesión.
     * Solo tiene efecto antes de que se llame a {@link #initSession(DeviceDescriptor)}.
     */
    public void setPlatform(String platform) {
        TestLogger.logInfo("MOBILE_HELPER", "Plataforma configurada: " + platform, null);
        System.setProperty(MobileConfigKeys.PLATFORM, platform.toUpperCase());
    }

    public void setDeviceId(String deviceId) {
        System.setProperty(MobileConfigKeys.DEVICE_ID, deviceId);
        TestLogger.logInfo("MOBILE_HELPER", "Device ID configurado: " + deviceId, null);
    }

    public void setDeviceType(String type) {
        System.setProperty(MobileConfigKeys.DEVICE_TYPE, type.toUpperCase());
        TestLogger.logInfo("MOBILE_HELPER", "Tipo de dispositivo: " + type, null);
    }

    public void setPlatformVersion(String version) {
        System.setProperty(MobileConfigKeys.PLATFORM_VERSION, version);
        TestLogger.logInfo("MOBILE_HELPER", "Version de plataforma: " + version, null);
    }

    public void setAppiumServerUrl(String url) {
        System.setProperty(MobileConfigKeys.APPIUM_SERVER_URL, url);
        TestLogger.logInfo("MOBILE_HELPER", "Appium server URL: " + url, null);
    }

    // =========================================================================
    // App Management
    // =========================================================================

    public void setAppPath(String path) {
        System.setProperty(MobileConfigKeys.APP_PATH, path);
        TestLogger.logInfo("MOBILE_HELPER", "App path: " + path, null);
    }

    public void setAppPackage(String pkg) {
        System.setProperty(MobileConfigKeys.APP_PACKAGE, pkg);
        TestLogger.logInfo("MOBILE_HELPER", "App package: " + pkg, null);
    }

    public void setAppActivity(String activity) {
        System.setProperty(MobileConfigKeys.APP_ACTIVITY, activity);
        TestLogger.logInfo("MOBILE_HELPER", "App activity: " + activity, null);
    }

    public void setBundleId(String bundleId) {
        System.setProperty(MobileConfigKeys.BUNDLE_ID, bundleId);
        TestLogger.logInfo("MOBILE_HELPER", "Bundle ID: " + bundleId, null);
    }

    public void installApp(String appPath) {
        ((InteractsWithApps) driver()).installApp(appPath);
        TestLogger.logInfo("MOBILE_HELPER", "App instalada desde: " + appPath, null);
    }

    public void removeApp(String bundleOrPackage) {
        ((InteractsWithApps) driver()).removeApp(bundleOrPackage);
        TestLogger.logInfo("MOBILE_HELPER", "App desinstalada: " + bundleOrPackage, null);
    }

    public void launchApp() {
        driver().executeScript("mobile:launchApp", Map.of());
        TestLogger.logInfo("MOBILE_HELPER", "App lanzada", null);
    }

    public void closeApp() {
        driver().executeScript("mobile:terminateApp",
            Map.of("bundleId", config.get(MobileConfigKeys.BUNDLE_ID, "")));
        TestLogger.logInfo("MOBILE_HELPER", "App cerrada", null);
    }

    public void restartApp() {
        closeApp();
        launchApp();
        TestLogger.logInfo("MOBILE_HELPER", "App reiniciada", null);
    }

    public void backgroundApp(int seconds) {
        ((InteractsWithApps) driver()).runAppInBackground(java.time.Duration.ofSeconds(seconds));
        TestLogger.logInfo("MOBILE_HELPER", "App en background por " + seconds + "s", null);
    }

    public void activateApp(String bundleOrPackage) {
        ((InteractsWithApps) driver()).activateApp(bundleOrPackage);
        TestLogger.logInfo("MOBILE_HELPER", "App activada: " + bundleOrPackage, null);
    }

    public boolean isAppInstalled(String bundleOrPackage) {
        return ((InteractsWithApps) driver()).isAppInstalled(bundleOrPackage);
    }

    // =========================================================================
    // Context Switching
    // =========================================================================

    public void switchToNative() {
        ((SupportsContextSwitching) driver()).context("NATIVE_APP");
        TestLogger.logInfo("MOBILE_HELPER", "Contexto cambiado a NATIVE_APP", null);
    }

    public void switchToWebView(String webViewId) {
        ((SupportsContextSwitching) driver()).context(webViewId);
        TestLogger.logInfo("MOBILE_HELPER", "Contexto cambiado a: " + webViewId, null);
    }

    public Set<String> getContexts() {
        return ((SupportsContextSwitching) driver()).getContextHandles();
    }

    // =========================================================================
    // Orientación
    // =========================================================================

    public void rotatePortrait() {
        ((SupportsRotation) driver()).rotate(ScreenOrientation.PORTRAIT);
        TestLogger.logInfo("MOBILE_HELPER", "Orientacion: portrait", null);
    }

    public void rotateLandscape() {
        ((SupportsRotation) driver()).rotate(ScreenOrientation.LANDSCAPE);
        TestLogger.logInfo("MOBILE_HELPER", "Orientacion: landscape", null);
    }

    public String getOrientation() {
        return ((SupportsRotation) driver()).getOrientation().name();
    }

    // =========================================================================
    // Permisos
    // =========================================================================

    public void grantPermission(String permission) {
        if (driver() instanceof AndroidDriver androidDriver) {
            try {
                String pkg = config.get(MobileConfigKeys.APP_PACKAGE, "");
                if (!pkg.isBlank()) {
                    androidDriver.executeScript("mobile:shell",
                        Map.of("command", "pm grant " + pkg + " " + permission));
                    TestLogger.logInfo("MOBILE_HELPER",
                        "Permiso concedido (ADB): " + permission, null);
                    return;
                }
            } catch (Exception e) {
                TestLogger.logWarning("MOBILE_HELPER",
                    "No se pudo conceder permiso via ADB: " + e.getMessage(), null);
            }
        }
        TestLogger.logInfo("MOBILE_HELPER", "Conceder permiso: " + permission, null);
    }

    public void denyPermission(String permission) {
        if (driver() instanceof AndroidDriver androidDriver) {
            try {
                String pkg = config.get(MobileConfigKeys.APP_PACKAGE, "");
                if (!pkg.isBlank()) {
                    androidDriver.executeScript("mobile:shell",
                        Map.of("command", "pm revoke " + pkg + " " + permission));
                    TestLogger.logInfo("MOBILE_HELPER",
                        "Permiso revocado (ADB): " + permission, null);
                    return;
                }
            } catch (Exception e) {
                TestLogger.logWarning("MOBILE_HELPER",
                    "No se pudo revocar permiso via ADB: " + e.getMessage(), null);
            }
        }
        TestLogger.logInfo("MOBILE_HELPER", "Denegar permiso: " + permission, null);
    }

    public void acceptSystemDialog() {
        try {
            driver().switchTo().alert().accept();
            TestLogger.logInfo("MOBILE_HELPER", "Dialogo del sistema aceptado", null);
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_HELPER",
                "No se encontro dialogo de sistema para aceptar", null);
        }
    }

    public void dismissSystemDialog() {
        try {
            driver().switchTo().alert().dismiss();
            TestLogger.logInfo("MOBILE_HELPER", "Dialogo del sistema rechazado", null);
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_HELPER",
                "No se encontro dialogo de sistema para rechazar", null);
        }
    }

    // =========================================================================
    // Notificaciones
    // =========================================================================

    public void openNotificationCenter() {
        if (driver() instanceof AndroidDriver androidDriver) {
            androidDriver.openNotifications();
        } else {
            driver().executeScript("mobile:swipe",
                Map.of("direction", "down", "startX", 200, "startY", 0, "endX", 200, "endY", 300));
        }
        TestLogger.logInfo("MOBILE_HELPER", "Centro de notificaciones abierto", null);
    }

    // =========================================================================
    // Sensores (plataforma-específicos)
    // =========================================================================

    public void setGpsLocation(double lat, double lon) {
        // mobile:setLocation funciona tanto en Android (UiAutomator2) como en iOS (XCUITest)
        try {
            driver().executeScript("mobile:setLocation",
                Map.of("latitude", lat, "longitude", lon, "altitude", 0.0));
            TestLogger.logInfo("MOBILE_HELPER", "GPS: " + lat + ", " + lon, null);
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_HELPER",
                "No se pudo establecer GPS: " + e.getMessage(), null);
        }
    }

    public void setNetworkCondition(String condition) {
        try {
            boolean online = condition.equalsIgnoreCase("online");
            if (driver() instanceof AndroidDriver) {
                driver().executeScript("mobile:setConnectivity",
                    Map.of("wifi", online, "data", online));
            }
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_HELPER",
                "No se pudo cambiar red a '" + condition + "': " + e.getMessage(), null);
        }
        TestLogger.logInfo("MOBILE_HELPER", "Red: " + condition, null);
    }

    public void toggleAirplaneMode(boolean on) {
        if (driver() instanceof AndroidDriver) {
            try {
                driver().executeScript("mobile:setConnectivity",
                    Map.of("airplaneMode", on));
            } catch (Exception e) {
                TestLogger.logWarning("MOBILE_HELPER",
                    "No se pudo cambiar modo avion: " + e.getMessage(), null);
            }
        }
        TestLogger.logInfo("MOBILE_HELPER", "Modo avion: " + (on ? "activado" : "desactivado"), null);
    }

    // =========================================================================
    // Elementos nativos
    // =========================================================================

    public WebElement findElement(String locator) {
        return ElementLocatorHelper.find(driver(), locator);
    }

    public List<WebElement> findElements(String locator) {
        return ElementLocatorHelper.findAll(driver(), locator);
    }

    public boolean elementExists(String locator) {
        return ElementLocatorHelper.exists(driver(), locator);
    }

    public void tap(String locator) {
        GestureHelper.tap(driver(), findElement(locator));
    }

    public void doubleTap(String locator) {
        GestureHelper.doubleTap(driver(), findElement(locator));
    }

    public void longPress(String locator, long durationMs) {
        GestureHelper.longPress(driver(), findElement(locator), durationMs);
    }

    public void type(String locator, String text) {
        WebElement el = findElement(locator);
        el.clear();
        el.sendKeys(text);
        TestLogger.logInfo("MOBILE_HELPER", "Texto ingresado en: " + locator, null);
    }

    public void clearField(String locator) {
        findElement(locator).clear();
        TestLogger.logInfo("MOBILE_HELPER", "Campo limpiado: " + locator, null);
    }

    public String getText(String locator) {
        return findElement(locator).getText();
    }

    public String getAttribute(String locator, String attribute) {
        return findElement(locator).getAttribute(attribute);
    }

    public boolean isVisible(String locator) {
        try {
            return findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEnabled(String locator) {
        return findElement(locator).isEnabled();
    }

    // =========================================================================
    // Gestos (delegados)
    // =========================================================================

    public void swipe(String direction) {
        GestureHelper.swipe(driver(), direction);
    }

    public void swipeBetween(String fromLocator, String toLocator) {
        GestureHelper.swipeBetween(driver(), findElement(fromLocator), findElement(toLocator));
    }

    public void scrollToText(String text) {
        GestureHelper.scrollToText(driver(), text);
    }

    public void pinch(String locator) {
        GestureHelper.pinch(driver(), findElement(locator));
    }

    public void zoom(String locator) {
        GestureHelper.zoom(driver(), findElement(locator));
    }

    // =========================================================================
    // Resolución de dispositivo y config
    // =========================================================================

    /**
     * Resuelve el dispositivo a usar: primero busca en el pool por ID configurado,
     * si no existe lo construye desde las propiedades de configuración y lo registra.
     */
    public DeviceDescriptor resolveDevice() {
        String preferredId = config.get(MobileConfigKeys.DEVICE_ID);

        DevicePool pool = DevicePool.getInstance();

        if (!pool.hasDevices()) {
            DeviceDescriptor device = buildDeviceFromConfig();
            pool.register(device);
            TestLogger.logInfo("MOBILE_HELPER",
                "Dispositivo construido desde config y registrado: " + device, null);
        }

        return pool.acquire(preferredId);
    }

    private DeviceDescriptor buildDeviceFromConfig() {
        String typeStr  = config.get(MobileConfigKeys.DEVICE_TYPE, "ANDROID_EMULATOR");
        String platform = config.get(MobileConfigKeys.PLATFORM, "Android");
        String version  = config.get(MobileConfigKeys.PLATFORM_VERSION, "");
        String name     = config.get(MobileConfigKeys.DEVICE_NAME, "");
        String udid     = config.get(MobileConfigKeys.UDID, "");
        String url      = config.get(MobileConfigKeys.APPIUM_SERVER_URL, "http://localhost:4723");
        String id       = config.get(MobileConfigKeys.DEVICE_ID, "device-default");

        DeviceType type;
        try {
            type = DeviceType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            type = platform.equalsIgnoreCase("iOS")
                ? DeviceType.IOS_SIMULATOR : DeviceType.ANDROID_EMULATOR;
        }

        return DeviceDescriptor.builder(id, type)
            .platformName(platform)
            .platformVersion(version)
            .deviceName(name)
            .udid(udid.isBlank() ? null : udid)
            .appiumServerUrl(url)
            .build();
    }

    private MobileDriverFactory.DriverConfig buildDriverConfig() {
        return new MobileDriverFactory.DriverConfig()
            .withAppPath(config.get(MobileConfigKeys.APP_PATH, ""))
            .withAppPackage(config.get(MobileConfigKeys.APP_PACKAGE, ""))
            .withAppActivity(config.get(MobileConfigKeys.APP_ACTIVITY, ""))
            .withBundleId(config.get(MobileConfigKeys.BUNDLE_ID, ""))
            .withAutoLaunch(config.getBoolean(MobileConfigKeys.APP_AUTO_LAUNCH, true))
            .withNoReset(config.getBoolean(MobileConfigKeys.APP_NO_RESET, false))
            .withImplicitWait(config.getInt(MobileConfigKeys.IMPLICIT_WAIT_SEC, 10));
    }
}
