package com.qa.mobilecore.helper;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
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
import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
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
 * <h2>Gestión del driver</h2>
 * <p>El {@link AppiumDriver} se gestiona internamente a través de {@link MobileDriverFactory}:
 * <ul>
 *   <li>{@link #initSession(DeviceDescriptor)} — configura el dispositivo en la factory y
 *       fuerza la creación inmediata del driver.</li>
 *   <li>{@link #driver()} — siempre delega a {@link MobileDriverFactory#getOrCreateDriver()}
 *       (lazy, idempotente).</li>
 *   <li>{@link #quitSession()} — libera el dispositivo del pool y cierra el driver
 *       a través de la factory.</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @since 2.2.0 driver() delega a MobileDriverFactory.getOrCreateDriver() (lazy)
 */
public class MobileHelper {

    private final ConfigManager       config;
    private final MobileDriverFactory mobileDriverFactory;

    /** Descriptor del dispositivo asignado a esta sesión (null hasta initSession). */
    private DeviceDescriptor assignedDevice;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor por defecto: crea una nueva {@link MobileDriverFactory}.
     * Usado por el registry cuando {@code MobilePlugin} no inyecta una factory.
     */
    public MobileHelper() {
        this(new MobileDriverFactory());
    }

    /**
     * Constructor con inyección de factory.
     * Usado por {@code MobilePlugin.registerServices()} para compartir la misma
     * instancia de factory entre el plugin y el helper dentro del mismo escenario.
     *
     * @param mobileDriverFactory factory de drivers para este escenario (no puede ser null)
     */
    public MobileHelper(MobileDriverFactory mobileDriverFactory) {
        if (mobileDriverFactory == null) {
            throw new IllegalArgumentException("MobileDriverFactory no puede ser null");
        }
        this.mobileDriverFactory = mobileDriverFactory;
        this.config              = ConfigManager.getInstance();
    }

    // =========================================================================
    // Gestión de sesión
    // =========================================================================

    /**
     * Inicializa la sesión Appium para el dispositivo dado.
     *
     * <p>Configura el dispositivo en la factory y fuerza la creación del driver.
     * Si el driver ya fue creado (llamada redundante), retorna sin hacer nada adicional.
     *
     * <p>Invocado por un step de configuración (p.ej. "Dado que configuro el dispositivo...").
     *
     * @param device descriptor del dispositivo a usar
     */
    public void initSession(DeviceDescriptor device) {
        this.assignedDevice = device;
        mobileDriverFactory.setDevice(device);

        // getOrCreateDriver() verifica el servidor Appium, crea el driver y actualiza el ThreadLocal.
        mobileDriverFactory.getOrCreateDriver();

        TestLogger.logInfo("MOBILE_HELPER",
            "Sesion iniciada para dispositivo: " + device.getId(), null);
    }

    /**
     * Cierra la sesión Appium y libera el dispositivo en el pool.
     *
     * <p>Es idempotente: puede llamarse varias veces sin efecto secundario.
     * Invocado desde {@code MobilePlugin.onScenarioEnd()} (modo plugin) o desde
     * {@code MobileHooksSteps.afterScenario()} (modo standalone).
     */
    public void quitSession() {
        // 1. Liberar el dispositivo del pool de ejecución
        if (assignedDevice != null) {
            DevicePool.getInstance().release(assignedDevice.getId());
            TestLogger.logInfo("MOBILE_HELPER",
                "Dispositivo liberado del pool: " + assignedDevice.getId(), null);
            assignedDevice = null;
        }
        // 2. Cerrar el driver vía factory (idempotente si ya fue cerrado)
        mobileDriverFactory.quitIfCreated();
        // 3. Safety-net: limpiar ThreadLocal aunque factory ya haya llamado quit()
        MobileDriverManager.quitDriverSafely();
    }

    /**
     * Obtiene el driver activo. Si no existe aún, lo crea de forma lazy.
     *
     * <p>Siempre delega a {@link MobileDriverFactory#getOrCreateDriver()}, que es
     * thread-safe y garantiza una única instancia por escenario.
     *
     * @return AppiumDriver activo para este escenario
     * @throws com.qa.mobilecore.driver.MobileDriverInitializationException si no puede crear sesión
     */
    public AppiumDriver driver() {
        return mobileDriverFactory.getOrCreateDriver();
    }

    /**
     * @return {@code true} si hay una sesión Appium activa (driver creado y no cerrado)
     */
    public boolean hasActiveSession() {
        return mobileDriverFactory.isDriverCreated();
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
    // Nuevos métodos — v2.2.1 (requeridos por steps genéricos)
    // =========================================================================

    /**
     * Espera hasta que un elemento sea visible (timeout por defecto 15s).
     */
    public void waitForVisible(String locator) {
        WebDriverWait wait = new WebDriverWait(driver(), Duration.ofSeconds(15));
        wait.until(ExpectedConditions.visibilityOf(findElement(locator)));
        TestLogger.logInfo("MOBILE_HELPER", "Elemento visible: " + locator, null);
    }

    /**
     * Espera hasta que un texto aparezca en pantalla.
     */
    public void waitForText(String text) {
        WebDriverWait wait = new WebDriverWait(driver(), Duration.ofSeconds(15));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(@text,'" + text.replace("'", "\\'") + "')]")));
        TestLogger.logInfo("MOBILE_HELPER", "Texto visible: " + text, null);
    }

    /**
     * Presiona una tecla de sistema del dispositivo (Back, Home, Recent).
     */
    public void pressSystemKey(String key) {
        AppiumDriver appiumDriver = driver();
        switch (key.toUpperCase().trim()) {
            case "BACK" -> appiumDriver.navigate().back();
            case "HOME" -> ((io.appium.java_client.android.AndroidDriver) appiumDriver)
                    .pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.HOME));
            case "RECENT", "APP_SWITCH" -> ((io.appium.java_client.android.AndroidDriver) appiumDriver)
                    .pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.APP_SWITCH));
            default -> throw new IllegalArgumentException(
                "Tecla de sistema no soportada: " + key + ". Usar: BACK, HOME, RECENT");
        }
        TestLogger.logInfo("MOBILE_HELPER", "Tecla de sistema presionada: " + key, null);
    }

    /**
     * Hace tap en coordenadas absolutas de la pantalla.
     */
    public void tapAt(int x, int y) {
        GestureHelper.tapAt(driver(), x, y);
        TestLogger.logInfo("MOBILE_HELPER", "Tap en coordenadas (" + x + ", " + y + ")", null);
    }

    // =========================================================================
    // Resolución de dispositivo desde config (uso interno y desde steps GIVEN)
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
}
