package com.scotia.qa.webcore.driver;

import com.scotia.qa.common.logging.TestLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.URL;
import java.time.Duration;

/**
 * Factory para la creación y configuración de WebDrivers del Framework Scotia QA.
 *
 * <p><b>Soporta múltiples navegadores y modos de ejecución:</b></p>
 * <ul>
 *   <li><b>Local:</b> Usa WebDriverManager para gestión automática de drivers</li>
 *   <li><b>Grid:</b> Conecta a Selenium Grid/Hub remoto</li>
 * </ul>
 *
 * <p><b>Navegadores soportados:</b> Chrome, Firefox, Edge, Safari</p>
 */
public class WebDriverFactory {

    /**
     * Enum de tipos de navegador soportados.
     */
    public enum BrowserType {
        CHROME, FIREFOX, EDGE, SAFARI
    }

    /**
     * Enum de modos de ejecución.
     */
    public enum ExecutionMode {
        LOCAL,  // Ejecución local con WebDriverManager
        GRID    // Ejecución remota en Selenium Grid
    }

    /**
     * Configuración para crear drivers (Builder pattern).
     */
    public static class DriverConfig {
        private BrowserType browserType;
        private ExecutionMode executionMode;
        private boolean headless;
        private String gridHubUrl;
        private String proxyUrl;
        private boolean acceptInsecureCerts;

        public DriverConfig(BrowserType browserType) {
            this.browserType = browserType;
            this.executionMode = ExecutionMode.LOCAL;
            this.headless = false;
            this.acceptInsecureCerts = true;
        }

        public DriverConfig withHeadless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public DriverConfig withGrid(String gridHubUrl) {
            this.executionMode = ExecutionMode.GRID;
            this.gridHubUrl = gridHubUrl;
            return this;
        }

        public DriverConfig withProxy(String proxyUrl) {
            this.proxyUrl = proxyUrl;
            return this;
        }

        public DriverConfig withAcceptInsecureCerts(boolean accept) {
            this.acceptInsecureCerts = accept;
            return this;
        }

        // Getters
        public BrowserType getBrowserType() { return browserType; }
        public ExecutionMode getExecutionMode() { return executionMode; }
        public boolean isHeadless() { return headless; }
        public String getGridHubUrl() { return gridHubUrl; }
        public String getProxyUrl() { return proxyUrl; }
        public boolean isAcceptInsecureCerts() { return acceptInsecureCerts; }
    }

    /**
     * Constructor privado - Factory pattern.
     */
    private WebDriverFactory() {
        // Utility class
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS PRINCIPALES
    // =========================================================================

    /**
     * Crea un WebDriver con configuración simple (local, no headless).
     */
    public static WebDriver createDriver(BrowserType browserType) {
        return createDriver(browserType, false);
    }

    /**
     * Crea un WebDriver con configuración básica.
     */
    public static WebDriver createDriver(BrowserType browserType, boolean headless) {
        DriverConfig config = new DriverConfig(browserType).withHeadless(headless);
        return createDriver(config);
    }

    /**
     * Crea un WebDriver con configuración completa.
     */
    public static WebDriver createDriver(DriverConfig config) {
        TestLogger.logInfo("WEB_DRIVER_FACTORY",
            "Creando WebDriver: " + config.getBrowserType() + " en modo " + config.getExecutionMode(), null);

        WebDriver driver;

        if (config.getExecutionMode() == ExecutionMode.LOCAL) {
            driver = createLocalDriver(config);
        } else {
            driver = createGridDriver(config);
        }

        configureDriver(driver);

        TestLogger.logInfo("WEB_DRIVER_FACTORY",
            "✅ WebDriver creado exitosamente: " + config.getBrowserType(), null);

        return driver;
    }

    // =========================================================================
    // CREACIÓN DE DRIVERS LOCALES
    // =========================================================================

    private static WebDriver createLocalDriver(DriverConfig config) {
        switch (config.getBrowserType()) {
            case CHROME:
                return createLocalChromeDriver(config);
            case FIREFOX:
                return createLocalFirefoxDriver(config);
            case EDGE:
                return createLocalEdgeDriver(config);
            case SAFARI:
                return createLocalSafariDriver(config);
            default:
                throw new IllegalArgumentException("Browser no soportado: " + config.getBrowserType());
        }
    }

    private static WebDriver createLocalChromeDriver(DriverConfig config) {
        setupChromeDriver();

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());
        options.addArguments("--no-sandbox");
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");

        if (config.isHeadless()) {
            options.addArguments("--headless");
        }

        if (config.getProxyUrl() != null) {
            options.setProxy(createProxy(config.getProxyUrl()));
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Chrome local configurado (headless: " + config.isHeadless() + ")", null);

        return new ChromeDriver(options);
    }

    /**
     * Configuración robusta y multiplataforma de ChromeDriver con fallbacks.
     * Funciona en: Windows, Mac, Linux.
     * Soporta: Ambientes con/sin internet, con/sin proxy, con/sin firewall corporativo.
     */
    private static void setupChromeDriver() {
        setupDriver("chromedriver", "webdriver.chrome.driver", "Chrome");
    }

    private static WebDriver createLocalFirefoxDriver(DriverConfig config) {
        setupFirefoxDriver();

        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());

        if (config.isHeadless()) {
            options.addArguments("--headless");
        }

        if (config.getProxyUrl() != null) {
            options.setProxy(createProxy(config.getProxyUrl()));
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Firefox local configurado (headless: " + config.isHeadless() + ")", null);

        return new FirefoxDriver(options);
    }

    /**
     * Configuración robusta y multiplataforma de FirefoxDriver (geckodriver) con fallbacks.
     * Funciona en: Windows, Mac, Linux.
     */
    private static void setupFirefoxDriver() {
        setupDriver("geckodriver", "webdriver.gecko.driver", "Firefox");
    }

    private static WebDriver createLocalEdgeDriver(DriverConfig config) {
        setupEdgeDriver();

        EdgeOptions options = new EdgeOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());

        if (config.isHeadless()) {
            options.addArguments("--headless");
        }

        if (config.getProxyUrl() != null) {
            options.setProxy(createProxy(config.getProxyUrl()));
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Edge local configurado (headless: " + config.isHeadless() + ")", null);

        return new EdgeDriver(options);
    }

    /**
     * Configuración robusta y multiplataforma de EdgeDriver (msedgedriver) con fallbacks.
     * Funciona en: Windows, Mac, Linux.
     */
    private static void setupEdgeDriver() {
        setupDriver("msedgedriver", "webdriver.edge.driver", "Edge");
    }

    private static WebDriver createLocalSafariDriver(DriverConfig config) {
        // Safari no soporta headless ni proxy configurado programáticamente
        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Safari local (headless y proxy no soportados en Safari)", null);

        return new SafariDriver();
    }

    // =========================================================================
    // CREACIÓN DE DRIVERS PARA SELENIUM GRID
    // =========================================================================

    private static WebDriver createGridDriver(DriverConfig config) {
        if (config.getGridHubUrl() == null || config.getGridHubUrl().isEmpty()) {
            throw new IllegalArgumentException("Grid Hub URL es requerida para modo GRID");
        }

        try {
            URL hubUrl = new URL(config.getGridHubUrl());

            switch (config.getBrowserType()) {
                case CHROME:
                    return createGridChromeDriver(hubUrl, config);
                case FIREFOX:
                    return createGridFirefoxDriver(hubUrl, config);
                case EDGE:
                    return createGridEdgeDriver(hubUrl, config);
                case SAFARI:
                    return createGridSafariDriver(hubUrl, config);
                default:
                    throw new IllegalArgumentException("Browser no soportado: " + config.getBrowserType());
            }
        } catch (Exception e) {
            TestLogger.logError("WEB_DRIVER_FACTORY",
                "Error conectando a Selenium Grid: " + config.getGridHubUrl(), null);
            throw new RuntimeException("Error inicializando Selenium Grid", e);
        }
    }

    private static WebDriver createGridChromeDriver(URL hubUrl, DriverConfig config) {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());
        options.addArguments("--no-sandbox");
        options.addArguments("--remote-allow-origins=*");

        if (config.isHeadless()) {
            options.addArguments("--headless");
        }

        if (config.getProxyUrl() != null) {
            options.setProxy(createProxy(config.getProxyUrl()));
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Chrome Grid configurado en: " + hubUrl, null);

        return new RemoteWebDriver(hubUrl, options);
    }

    private static WebDriver createGridFirefoxDriver(URL hubUrl, DriverConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());

        if (config.isHeadless()) {
            options.addArguments("--headless");
        }

        if (config.getProxyUrl() != null) {
            options.setProxy(createProxy(config.getProxyUrl()));
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Firefox Grid configurado en: " + hubUrl, null);

        return new RemoteWebDriver(hubUrl, options);
    }

    private static WebDriver createGridEdgeDriver(URL hubUrl, DriverConfig config) {
        EdgeOptions options = new EdgeOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());

        if (config.isHeadless()) {
            options.addArguments("--headless");
        }

        if (config.getProxyUrl() != null) {
            options.setProxy(createProxy(config.getProxyUrl()));
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Edge Grid configurado en: " + hubUrl, null);

        return new RemoteWebDriver(hubUrl, options);
    }

    private static WebDriver createGridSafariDriver(URL hubUrl, DriverConfig config) {
        SafariOptions options = new SafariOptions();
        options.setUseTechnologyPreview(true);
        options.setCapability("browserName", "safari");
        options.setCapability("platformName", "MAC");

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Safari Grid configurado en: " + hubUrl, null);

        return new RemoteWebDriver(hubUrl, options);
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    /**
     * Crea un objeto Proxy para configuración de navegadores.
     */
    private static Proxy createProxy(String proxyUrl) {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyUrl);
        proxy.setFtpProxy(proxyUrl);
        proxy.setSslProxy(proxyUrl);
        proxy.setNoProxy("*.bns,localhost,127.0.0.1");

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "Proxy configurado: " + proxyUrl, null);

        return proxy;
    }

    // =========================================================================
    // CONFIGURACIÓN ROBUSTA DE DRIVERS (MULTIPLATAFORMA)
    // =========================================================================

    /**
     * Configuración simplificada de drivers usando common/WebDriverManager.
     *
     * <p><b>Estrategia dual:</b></p>
     * <ol>
     *   <li><b>LOCAL:</b> Busca en path configurado (driver.local.base.path)</li>
     *   <li><b>ARTIFACTORY:</b> Descarga desde repositorio corporativo</li>
     * </ol>
     *
     * @param driverName Nombre del ejecutable (chromedriver, geckodriver, msedgedriver)
     * @param propertyName System Property key (webdriver.chrome.driver, webdriver.gecko.driver, etc.)
     * @param browserName Nombre del navegador para logs (Chrome, Firefox, Edge)
     */
    private static void setupDriver(String driverName, String propertyName, String browserName) {
        // ESTRATEGIA 1: ¿Hay driver manual configurado via System Property?
        String manualDriverPath = System.getProperty(propertyName);
        if (manualDriverPath != null && !manualDriverPath.isEmpty()) {
            java.io.File manualDriver = new java.io.File(manualDriverPath);
            if (manualDriver.exists() && manualDriver.canExecute()) {
                TestLogger.logDebug("WEB_DRIVER_FACTORY",
                    String.format("Usando %s manual: %s", driverName, manualDriverPath), null);
                return;
            }
        }

        // ESTRATEGIA 2: Usar WebDriverManager del framework
        try {
            java.nio.file.Path driverPath = com.scotia.qa.common.driver.WebDriverManager.getDriverFromConfig(driverName);

            if (driverPath != null && java.nio.file.Files.exists(driverPath)) {
                System.setProperty(propertyName, driverPath.toString());
                TestLogger.logDebug("WEB_DRIVER_FACTORY",
                    String.format("%s configurado: %s", driverName, driverPath), null);
                return;
            }

        } catch (com.scotia.qa.common.driver.WebDriverManager.DriverNotFoundException e) {
            // Error conciso del WebDriverManager
            String errorMsg = String.format("❌ %s no encontrado.\n" +
                "Solución: Verifica config-scotia.properties:\n" +
                "  - driver.strategy=local (o artifactory)\n" +
                "  - driver.local.base.path=${DRIVER_LOCAL_PATH}\n" +
                "  - driver.chrome.version=143.0.7499.41\n" +
                "Descarga: %s",
                driverName,
                getDriverDownloadUrl(browserName));

            TestLogger.logError("WEB_DRIVER_FACTORY", errorMsg, null);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("❌ Error configurando %s: %s", driverName, e.getMessage());
            TestLogger.logError("WEB_DRIVER_FACTORY", errorMsg, null);
            throw new RuntimeException(errorMsg, e);
        }
    }


    /**
     * Obtiene la URL de descarga oficial del driver según el navegador.
     */
    private static String getDriverDownloadUrl(String browserName) {
        switch (browserName.toLowerCase()) {
            case "chrome":
                return "https://googlechromelabs.github.io/chrome-for-testing/";
            case "firefox":
                return "https://github.com/mozilla/geckodriver/releases";
            case "edge":
                return "https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/";
            default:
                return "https://www.selenium.dev/documentation/webdriver/getting_started/install_drivers/";
        }
    }

    /**
     * Configura timeouts y maximiza ventana del driver.
     *
     * NOTA: evitamos combinar implicit waits con explicit waits para
     * evitar efectos secundarios en esperas. Por defecto establecemos
     * implicit wait a 0 y recomendamos usar WebDriverWait/WaitUtils.
     */
    private static void configureDriver(WebDriver driver) {
        try {
            // Deshabilitar implicit wait para evitar interacción con ExpectedConditions
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            try {
                driver.manage().window().maximize();
            } catch (Exception e) {
                // Algunos entornos (headless, remote) pueden lanzar excepciones al maximizar
                TestLogger.logWarning("WEB_DRIVER_FACTORY",
                    "No se pudo maximizar ventana del driver: " + e.getMessage(), null);
            }

            TestLogger.logDebug("WEB_DRIVER_FACTORY",
                "Timeouts configurados (implicit: 0s, pageLoad: 30s)", null);
        } catch (Exception e) {
            TestLogger.logError("WEB_DRIVER_FACTORY",
                "Error configurando driver: " + e.getMessage(), null);
        }
    }
}
