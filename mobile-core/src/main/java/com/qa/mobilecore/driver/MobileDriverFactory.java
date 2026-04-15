package com.qa.mobilecore.driver;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.mobilecore.appium.AppiumServerManager;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Factory para crear y gestionar el ciclo de vida del {@link AppiumDriver} por escenario.
 *
 * <p>Combina el patrón factory (creación de sesiones Appium) con la gestión del ciclo
 * de vida: una instancia de esta clase vive un escenario BDD y se registra en el
 * {@link com.qa.common.runtime.ServiceRegistry} por {@link com.qa.mobilecore.plugin.MobilePlugin}.
 *
 * <h2>Ciclo de vida</h2>
 * <pre>
 * MobilePlugin.registerServices()      → registra MobileDriverFactory (lazy)
 * [primer step mobile]                 → driver() → getOrCreateDriver() → crea AppiumDriver
 * MobilePlugin.onScenarioEnd()         → quitIfCreated()  → cierra AppiumDriver
 * </pre>
 *
 * <h2>Estrategia de creación del dispositivo</h2>
 * <ol>
 *   <li>Si {@link #setDevice(DeviceDescriptor)} fue llamado (p.ej. desde
 *       {@code MobileHelper.initSession()}), usa ese dispositivo.</li>
 *   <li>Si no, resuelve el dispositivo desde {@code ConfigManager}
 *       (prioridad: ExecutionConfig → System Properties → env → archivos).</li>
 * </ol>
 *
 * <p>{@link #getOrCreateDriver()} es thread-safe (synchronized) y lazy: el driver
 * se crea solo cuando se necesita por primera vez en el escenario.
 *
 * <p>El método estático {@link #create(DeviceDescriptor, DriverConfig)} se mantiene
 * por compatibilidad con llamadas directas existentes.
 *
 * <p>Soporta Android (UiAutomator2) e iOS (XCUITest) con las APIs modernas de Appium 8+.
 * No utiliza {@code DesiredCapabilities} ni {@code TouchAction} (deprecados).
 *
 * <p><b>Modos de ejecución:</b>
 * <ul>
 *   <li><b>Local / Emulador:</b> Appium en {@code localhost:port}, AVD o dispositivo conectado</li>
 *   <li><b>Remote Grid:</b> URL del hub (BrowserStack, Sauce Labs, Docker/K8s interno)</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @since 2.2.0 Refactorizado a clase de instancia con ciclo de vida por escenario
 */
public class MobileDriverFactory {

    private final ConfigManager config;

    /** Driver activo para este escenario. volatile para visibilidad inmediata entre hilos. */
    private volatile AppiumDriver currentDriver;

    /**
     * Dispositivo configurado explícitamente (p.ej. via {@link #setDevice}).
     * Tiene prioridad sobre la resolución automática desde config.
     */
    private volatile DeviceDescriptor configuredDevice;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Crea una nueva instancia que lee configuración de {@link ConfigManager}.
     * Invocado por el supplier lazy de {@link com.qa.common.runtime.ServiceRegistry}.
     */
    public MobileDriverFactory() {
        this.config = ConfigManager.getInstance();
    }

    // =========================================================================
    // API de ciclo de vida — instancia
    // =========================================================================

    /**
     * Retorna el driver activo o lo crea si aún no existe (lazy singleton por escenario).
     *
     * <p>Thread-safe: synchronized para evitar creación doble en ejecuciones paralelas.
     *
     * @return AppiumDriver activo para este escenario
     * @throws MobileDriverInitializationException si no es posible iniciar la sesión Appium
     * @since 2.2.0
     */
    public synchronized AppiumDriver getOrCreateDriver() {
        if (currentDriver != null) {
            return currentDriver;
        }

        DeviceDescriptor device = (configuredDevice != null)
                ? configuredDevice
                : resolveDeviceFromConfig();

        DriverConfig driverConfig = buildDriverConfigFromConfig();

        boolean autoStart      = config.getBoolean(MobileConfigKeys.APPIUM_AUTO_START, false);
        int     startupTimeout = config.getInt(MobileConfigKeys.APPIUM_STARTUP_TIMEOUT_SEC, 30);
        AppiumServerManager.ensureRunning(device.getAppiumServerUrl(), autoStart, startupTimeout);

        TestLogger.logInfo("MOBILE_FACTORY",
                "Creando AppiumDriver lazy para: " + device.getId(), null);

        try {
            currentDriver = create(device, driverConfig);
        } catch (MobileDriverInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new MobileDriverInitializationException(
                    "No se pudo iniciar la sesión Appium para el dispositivo '"
                    + device.getId() + "': " + e.getMessage(), e);
        }

        // Actualizar ThreadLocal para compatibilidad con código que usa MobileDriverManager
        MobileDriverManager.setDriver(currentDriver);

        TestLogger.logInfo("MOBILE_FACTORY",
                "AppiumDriver creado y asignado al hilo: " + Thread.currentThread().getName(), null);
        return currentDriver;
    }

    /**
     * Cierra el driver activo y limpia la referencia interna.
     *
     * <p>Es idempotente: si el driver ya fue cerrado o nunca se creó, no lanza excepción.
     * Los errores de cierre (p.ej. sesión ya terminada en el servidor) se absorben y se loguean.
     *
     * @since 2.2.0
     */
    public synchronized void quitIfCreated() {
        if (currentDriver == null) {
            TestLogger.logInfo("MOBILE_FACTORY",
                    "quitIfCreated: no hay AppiumDriver activo, nada que cerrar", null);
            return;
        }
        try {
            currentDriver.quit();
            TestLogger.logInfo("MOBILE_FACTORY", "AppiumDriver cerrado correctamente", null);
        } catch (Exception e) {
            TestLogger.logWarning("MOBILE_FACTORY",
                    "Aviso al cerrar AppiumDriver (sesión posiblemente ya terminada): "
                    + e.getMessage(), null);
        } finally {
            currentDriver    = null;
            configuredDevice = null;
        }
    }

    /**
     * @return {@code true} si el driver fue creado y aún no fue cerrado
     * @since 2.2.0
     */
    public synchronized boolean isDriverCreated() {
        return currentDriver != null;
    }

    /**
     * Establece el dispositivo a usar en la próxima llamada a {@link #getOrCreateDriver()}.
     *
     * <p>Debe llamarse antes de {@link #getOrCreateDriver()} para que tenga efecto.
     * Llamado típicamente por {@code MobileHelper.initSession(DeviceDescriptor)}.
     *
     * @param device descriptor del dispositivo; puede ser null para resetear a config
     * @since 2.2.0
     */
    public synchronized void setDevice(DeviceDescriptor device) {
        this.configuredDevice = device;
        TestLogger.logInfo("MOBILE_FACTORY",
                "Dispositivo configurado: " + (device != null ? device.getId() : "null"), null);
    }

    /**
     * Inyecta un driver externamente. Uso exclusivo en tests unitarios del mismo paquete.
     *
     * @param driver driver a inyectar (puede ser un mock/stub)
     */
    synchronized void injectDriver(AppiumDriver driver) {
        this.currentDriver = driver;
    }

    // =========================================================================
    // API estática — crear desde DeviceDescriptor (uso directo / backward compat)
    // =========================================================================

    /**
     * Crea un {@link AppiumDriver} a partir de un {@link DeviceDescriptor} y las
     * propiedades de la app extraídas de {@link DriverConfig}.
     *
     * <p>Este método permanece estático para compatibilidad con llamadas directas
     * existentes que no pasan por el ciclo de vida de la instancia.
     *
     * @param device      descriptor del dispositivo (plataforma, udid, server URL)
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
    // Resolución de configuración (instancia)
    // =========================================================================

    private DeviceDescriptor resolveDeviceFromConfig() {
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
                ? DeviceType.IOS_SIMULATOR
                : DeviceType.ANDROID_EMULATOR;
        }

        return DeviceDescriptor.builder(id, type)
                .platformName(platform)
                .platformVersion(version)
                .deviceName(name)
                .udid(udid.isBlank() ? null : udid)
                .appiumServerUrl(url)
                .build();
    }

    private DriverConfig buildDriverConfigFromConfig() {
        return new DriverConfig()
                .withAppPath(config.get(MobileConfigKeys.APP_PATH, ""))
                .withAppPackage(config.get(MobileConfigKeys.APP_PACKAGE, ""))
                .withAppActivity(config.get(MobileConfigKeys.APP_ACTIVITY, ""))
                .withBundleId(config.get(MobileConfigKeys.BUNDLE_ID, ""))
                .withAutoLaunch(config.getBoolean(MobileConfigKeys.APP_AUTO_LAUNCH, true))
                .withNoReset(config.getBoolean(MobileConfigKeys.APP_NO_RESET, false))
                .withImplicitWait(config.getInt(MobileConfigKeys.IMPLICIT_WAIT_SEC, 10));
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
