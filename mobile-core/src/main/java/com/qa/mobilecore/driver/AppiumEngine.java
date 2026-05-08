package com.qa.mobilecore.driver;

import com.qa.common.driver.ElementLocator;
import com.qa.common.driver.Gesture;
import com.qa.common.driver.UiDriver;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.mobilecore.appium.AppiumServerManager;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.helper.GestureHelper;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import io.appium.java_client.remote.SupportsContextSwitching;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Objects;

/**
 * Implementación de {@link UiDriver} para automatización mobile con Appium.
 *
 * <p>Wraps {@link AppiumDriver} (Appium 8+ / W3C Actions API) exponiendo el
 * contrato unificado de {@link UiDriver}. Delega a {@link GestureHelper} para
 * gestos táctiles y a {@link MobileDriverFactory} para la creación de sesiones.
 *
 * <h2>Ciclo de vida</h2>
 * <pre>
 * new AppiumEngine()           → driver es null (no inicializado)
 * open(config)                 → crea sesión Appium via MobileDriverFactory.create()
 * click / type / waitFor / … → operaciones con el driver activo
 * close()                      → cierra sesión; driver = null
 * </pre>
 *
 * <h2>Uso típico desde un CorePlugin</h2>
 * <pre>
 * UiDriver driver = new AppiumEngine();
 * driver.open(executionConfig);
 * try {
 *     driver.click(ElementLocator.accessibilityId("login_button"));
 * } finally {
 *     driver.close();
 * }
 * </pre>
 *
 * @author Abel Venero
 * @since 3.0.0
 * @see UiDriver
 * @see GestureHelper
 * @see MobileDriverFactory
 */
public final class AppiumEngine implements UiDriver {

    /** Driver activo. volatile para visibilidad inmediata entre hilos. */
    volatile AppiumDriver driver;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor de ciclo de vida — el driver se crea en {@link #open(ExecutionConfig)}.
     */
    public AppiumEngine() {
        this.driver = null;
    }

    /**
     * Constructor package-private para inyección en tests.
     *
     * <p><b>Solo para uso en tests unitarios.</b> Inyecta un mock de {@link AppiumDriver}
     * evitando la inicialización real de Appium.
     *
     * @param driver mock o stub de AppiumDriver; no puede ser null
     */
    AppiumEngine(AppiumDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver no puede ser null");
    }

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Lee las propiedades de {@code config} usando {@link MobileConfigKeys},
     * garantiza que el servidor Appium está activo y crea la sesión.
     *
     * @throws NullPointerException     si {@code config} es null
     * @throws IllegalStateException    si el driver ya está inicializado
     */
    @Override
    public void open(ExecutionConfig config) {
        Objects.requireNonNull(config, "ExecutionConfig no puede ser null");
        if (driver != null) {
            throw new IllegalStateException(
                "AppiumEngine ya está inicializado. Llame a close() antes de open().");
        }

        DeviceDescriptor device = buildDevice(config);
        MobileDriverFactory.DriverConfig driverConfig = buildDriverConfig(config);

        boolean autoStart = Boolean.parseBoolean(
            config.getProperty(MobileConfigKeys.APPIUM_AUTO_START, "false"));
        int startupTimeout = Integer.parseInt(
            config.getProperty(MobileConfigKeys.APPIUM_STARTUP_TIMEOUT_SEC, "30"));

        AppiumServerManager.ensureRunning(device.getAppiumServerUrl(), autoStart, startupTimeout);

        this.driver = MobileDriverFactory.create(device, driverConfig);
        MobileDriverManager.setDriver(this.driver);

        TestLogger.logInfo("APPIUM_ENGINE",
            "AppiumEngine abierto para dispositivo: " + device.getId(), null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cierra la sesión Appium liberando el driver. Idempotente: no lanza
     * excepción si el driver no estaba inicializado. Los errores de cierre
     * se absorben y se loguean como WARNING.
     */
    @Override
    public void close() {
        if (driver != null) {
            try {
                driver.quit();
                TestLogger.logInfo("APPIUM_ENGINE", "AppiumDriver cerrado correctamente", null);
            } catch (Exception e) {
                TestLogger.logWarning("APPIUM_ENGINE",
                    "Aviso al cerrar AppiumDriver: " + e.getMessage(), null);
            } finally {
                this.driver = null;
                MobileDriverManager.quitDriverSafely();
            }
        }
    }

    // =========================================================================
    // Interacción con elementos
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException  si {@code locator} es null
     * @throws IllegalStateException si el driver no está inicializado
     */
    @Override
    public void click(ElementLocator locator) {
        Objects.requireNonNull(locator, "ElementLocator no puede ser null");
        requireDriver().findElement(toBy(locator)).click();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Limpia el campo antes de escribir.
     *
     * @throws NullPointerException  si {@code locator} o {@code text} son null
     * @throws IllegalStateException si el driver no está inicializado
     */
    @Override
    public void type(ElementLocator locator, String text) {
        Objects.requireNonNull(locator, "ElementLocator no puede ser null");
        Objects.requireNonNull(text, "text no puede ser null");
        WebElement el = requireDriver().findElement(toBy(locator));
        el.clear();
        el.sendKeys(text);
    }

    // =========================================================================
    // Esperas
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Usa {@link WebDriverWait} con {@link ExpectedConditions#elementToBeClickable}.
     *
     * @throws NullPointerException     si {@code locator} o {@code timeout} son null
     * @throws IllegalArgumentException si {@code timeout} no es positivo
     * @throws IllegalStateException    si el driver no está inicializado
     */
    @Override
    public void waitFor(ElementLocator locator, Duration timeout) {
        Objects.requireNonNull(locator, "ElementLocator no puede ser null");
        Objects.requireNonNull(timeout, "timeout no puede ser null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException(
                "timeout debe ser positivo, pero fue: " + timeout);
        }
        new WebDriverWait(requireDriver(), timeout)
            .until(ExpectedConditions.elementToBeClickable(toBy(locator)));
    }

    // =========================================================================
    // Capturas
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Usa {@link TakesScreenshot#getScreenshotAs(OutputType)} de Appium y
     * copia el archivo al destino indicado, creando los directorios padre si no existen.
     *
     * @throws NullPointerException  si {@code destination} es null
     * @throws RuntimeException      si ocurre un error de I/O al copiar el archivo
     * @throws IllegalStateException si el driver no está inicializado
     */
    @Override
    public void screenshot(Path destination) {
        Objects.requireNonNull(destination, "destination no puede ser null");
        java.io.File src = ((TakesScreenshot) requireDriver()).getScreenshotAs(OutputType.FILE);
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(src.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            TestLogger.logInfo("APPIUM_ENGINE", "Screenshot guardado en: " + destination, null);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar screenshot en: " + destination, e);
        }
    }

    // =========================================================================
    // Gestos (mobile)
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Delega a {@link GestureHelper#execute(AppiumDriver, Gesture)}.
     *
     * @throws NullPointerException  si {@code gesture} es null
     * @throws IllegalStateException si el driver no está inicializado
     */
    @Override
    public void performGesture(Gesture gesture) {
        Objects.requireNonNull(gesture, "gesture no puede ser null");
        GestureHelper.execute(requireDriver(), gesture);
    }

    // =========================================================================
    // Navegación y contexto
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Si el driver soporta context-switching (Appium hybrid apps):
     * <ul>
     *   <li>Contexto {@code WEBVIEW_*} → retorna la URL actual del WebView</li>
     *   <li>Contexto nativo → retorna el nombre del contexto (ej: {@code "NATIVE_APP"})</li>
     * </ul>
     * En caso contrario retorna {@code driver.getCurrentUrl()}.
     */
    @Override
    public String getCurrentLocation() {
        AppiumDriver d = requireDriver();
        if (d instanceof SupportsContextSwitching ctx) {
            String context = ctx.getContext();
            if (context != null && context.startsWith("WEBVIEW_")) {
                return d.getCurrentUrl();
            }
            return context;
        }
        return d.getCurrentUrl();
    }

    // =========================================================================
    // Identidad
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    public DriverType getType() {
        return DriverType.MOBILE;
    }

    // =========================================================================
    // Privados — utilidades internas
    // =========================================================================

    /**
     * Retorna el driver activo o lanza {@link IllegalStateException} si no está inicializado.
     */
    private AppiumDriver requireDriver() {
        AppiumDriver d = this.driver;
        if (d == null) {
            throw new IllegalStateException(
                "AppiumEngine no está inicializado. Llame a open(ExecutionConfig) primero.");
        }
        return d;
    }

    /**
     * Traduce un {@link ElementLocator} al {@link By} correspondiente para Appium.
     *
     * @param locator locator a traducir
     * @return By de Selenium/Appium
     * @throws UnsupportedOperationException si la estrategia es ROLE (exclusiva de web)
     */
    private static By toBy(ElementLocator locator) {
        String value = locator.value();
        return switch (locator.strategy()) {
            case CSS             -> By.cssSelector(value);
            case XPATH           -> By.xpath(value);
            case ACCESSIBILITY_ID -> AppiumBy.accessibilityId(value);
            case RESOURCE_ID     -> AppiumBy.androidUIAutomator(
                "new UiSelector().resourceId(\"%s\")".formatted(value));
            case ID              -> By.id(value);
            case NAME            -> By.name(value);
            case CLASS_NAME      -> By.className(value);
            case TEXT            -> By.xpath(
                "//*[@text='%s' or @label='%s' or @value='%s']".formatted(value, value, value));
            case ROLE            -> throw new UnsupportedOperationException(
                "ROLE es exclusivo de web — no soportado en AppiumEngine");
        };
    }

    /**
     * Construye un {@link DeviceDescriptor} a partir de las propiedades de la configuración.
     */
    private static DeviceDescriptor buildDevice(ExecutionConfig config) {
        String typeStr   = config.getProperty(MobileConfigKeys.DEVICE_TYPE, "ANDROID_EMULATOR");
        String platform  = config.getProperty(MobileConfigKeys.PLATFORM, "Android");
        String version   = config.getProperty(MobileConfigKeys.PLATFORM_VERSION, "");
        String name      = config.getProperty(MobileConfigKeys.DEVICE_NAME, "");
        String udid      = config.getProperty(MobileConfigKeys.UDID, "");
        String serverUrl = config.getProperty(MobileConfigKeys.APPIUM_SERVER_URL, "http://localhost:4723");
        String deviceId  = config.getProperty(MobileConfigKeys.DEVICE_ID, "device-default");

        DeviceType type;
        try {
            type = DeviceType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            type = platform.equalsIgnoreCase("iOS")
                ? DeviceType.IOS_SIMULATOR
                : DeviceType.ANDROID_EMULATOR;
        }

        return DeviceDescriptor.builder(deviceId, type)
            .platformName(platform)
            .platformVersion(version)
            .deviceName(name)
            .udid(udid.isBlank() ? null : udid)
            .appiumServerUrl(serverUrl)
            .build();
    }

    /**
     * Construye un {@link MobileDriverFactory.DriverConfig} a partir de las propiedades de la configuración.
     */
    private static MobileDriverFactory.DriverConfig buildDriverConfig(ExecutionConfig config) {
        return new MobileDriverFactory.DriverConfig()
            .withAppPath(config.getProperty(MobileConfigKeys.APP_PATH, ""))
            .withAppPackage(config.getProperty(MobileConfigKeys.APP_PACKAGE, ""))
            .withAppActivity(config.getProperty(MobileConfigKeys.APP_ACTIVITY, ""))
            .withBundleId(config.getProperty(MobileConfigKeys.BUNDLE_ID, ""))
            .withAutoLaunch(Boolean.parseBoolean(
                config.getProperty(MobileConfigKeys.APP_AUTO_LAUNCH, "true")))
            .withNoReset(Boolean.parseBoolean(
                config.getProperty(MobileConfigKeys.APP_NO_RESET, "false")))
            .withImplicitWait(Integer.parseInt(
                config.getProperty(MobileConfigKeys.IMPLICIT_WAIT_SEC, "10")));
    }
}
