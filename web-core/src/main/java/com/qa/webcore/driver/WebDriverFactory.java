package com.qa.webcore.driver;

import com.qa.common.logging.TestLogger;
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
 * Factory para la creación y configuración de WebDrivers del QA Automation Framework.
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
        private final BrowserType browserType;
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
        public BrowserType getBrowserType() {
            return browserType;
        }

        public ExecutionMode getExecutionMode() {
            return executionMode;
        }

        public boolean isHeadless() {
            return headless;
        }

        public String getGridHubUrl() {
            return gridHubUrl;
        }

        public String getProxyUrl() {
            return proxyUrl;
        }

        public boolean isAcceptInsecureCerts() {
            return acceptInsecureCerts;
        }
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
        com.qa.common.config.ConfigManager configManager =
            com.qa.common.config.ConfigManager.getInstance();
        String driverStrategy = configManager.get("driver.strategy", "auto");

        TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("Creando WebDriver: %s (execution=%s, driver.strategy=%s)",
                    config.getBrowserType(), config.getExecutionMode(), driverStrategy), null);

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
        return switch (config.getBrowserType()) {
            case CHROME -> createLocalChromeDriver(config);
            case FIREFOX -> createLocalFirefoxDriver(config);
            case EDGE -> createLocalEdgeDriver(config);
            case SAFARI -> createLocalSafariDriver(config);
            default -> throw new IllegalArgumentException("Browser no soportado: " + config.getBrowserType());
        };
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
            // Usar URI.toURL() en lugar del constructor deprecado URL(String)
            URL hubUrl = new java.net.URI(config.getGridHubUrl()).toURL();

            return switch (config.getBrowserType()) {
                case CHROME -> createGridChromeDriver(hubUrl, config);
                case FIREFOX -> createGridFirefoxDriver(hubUrl, config);
                case EDGE -> createGridEdgeDriver(hubUrl, config);
                case SAFARI -> createGridSafariDriver(hubUrl, config);
                default -> throw new IllegalArgumentException("Browser no soportado: " + config.getBrowserType());
            };
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
     * Configuración genérica y robusta de drivers con estrategia de fallback.
     *
     * <p><b>Estrategia de fallback (en orden):</b></p>
     * <ol>
     *   <li><b>Driver manual:</b> System Property configurado (ej: -Dwebdriver.chrome.driver=...)</li>
     *   <li><b>WebDriverManager Framework:</b> LOCAL PATH → CACHÉ → ARTIFACTORY</li>
     *   <li><b>PATH del sistema:</b> Busca en directorios comunes del SO</li>
     *   <li><b>Error descriptivo:</b> Mensaje claro con instrucciones de solución</li>
     * </ol>
     *
     * <p><b>Soporta:</b> Windows, Mac, Linux | Artifactory corporativo | Drivers locales</p>
     *
     * @param driverName Nombre del ejecutable (chromedriver, geckodriver, msedgedriver)
     * @param propertyName System Property key (webdriver.chrome.driver, webdriver.gecko.driver, etc.)
     * @param browserName Nombre del navegador para logs (Chrome, Firefox, Edge)
     */
    private static void setupDriver(String driverName, String propertyName, String browserName) {
        // Obtener ConfigManager del framework
        com.qa.common.config.ConfigManager config = com.qa.common.config.ConfigManager.getInstance();

        // FALLBACK 1: ¿Hay driver manual configurado via System Property?
        String manualDriverPath = System.getProperty(propertyName);
        if (manualDriverPath != null && !manualDriverPath.isEmpty()) {
            java.io.File manualDriver = new java.io.File(manualDriverPath);
            if (manualDriver.exists() && manualDriver.canExecute()) {
                TestLogger.logInfo("WEB_DRIVER_FACTORY",
                        String.format("✅ Usando %s manual (System Property): %s", driverName, manualDriverPath), null);
                return;
            } else {
                TestLogger.logWarning("WEB_DRIVER_FACTORY",
                        String.format("⚠️ Driver manual configurado pero no válido: %s", manualDriverPath), null);
            }
        }

        // FALLBACK 2: Buscar en PATH del sistema
        TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("🔍 Buscando %s en PATH del sistema...", driverName), null);

        String systemDriver = findDriverInSystemPath(driverName);
        if (systemDriver != null) {
            System.setProperty(propertyName, systemDriver);
            TestLogger.logInfo("WEB_DRIVER_FACTORY",
                    String.format("✅ Usando %s desde PATH del sistema: %s", driverName, systemDriver), null);
            return;
        }

        // TODO: Todos los fallbacks fallaron → Error descriptivo con soluciones
        String errorMsg = buildDriverNotFoundError(driverName, propertyName, browserName);
        TestLogger.logError("WEB_DRIVER_FACTORY", errorMsg, null);
        throw new RuntimeException(errorMsg);
    }


    /**
     * Busca el driver en directorios comunes del PATH del sistema.
     * Soporta: Windows, Mac, Linux.
     *
     * @return Ruta completa del driver si se encuentra, null si no existe
     */
    private static String findDriverInSystemPath(String driverName) {
        String os = System.getProperty("os.name").toLowerCase();
        String driverFileName = driverName;
        if (os.contains("win")) {
            driverFileName += ".exe";
        }

        // Directorios comunes según el SO
        String[] commonPaths;
        if (os.contains("win")) {
            commonPaths = new String[]{
                    "C:\\webdrivers\\",
                    "C:\\Program Files\\webdrivers\\",
                    "C:\\selenium\\drivers\\",
                    System.getenv("LOCALAPPDATA") + "\\Programs\\webdrivers\\"
            };
        } else if (os.contains("mac")) {
            commonPaths = new String[]{
                    "/usr/local/bin/",
                    "/usr/bin/",
                    "/opt/homebrew/bin/",
                    System.getProperty("user.home") + "/webdrivers/"
            };
        } else {
            // Linux
            commonPaths = new String[]{
                    "/usr/local/bin/",
                    "/usr/bin/",
                    "/opt/selenium/drivers/",
                    System.getProperty("user.home") + "/webdrivers/"
            };
        }

        for (String path : commonPaths) {
            java.io.File driver = new java.io.File(path + driverFileName);
            if (driver.exists() && driver.canExecute()) {
                return driver.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Construye mensaje de error descriptivo simplificado.
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, etc.)
     * @param propertyName System property key
     * @param browserName Nombre del navegador
     */
    private static String buildDriverNotFoundError(String driverName, String propertyName, String browserName) {
        String downloadUrl = getDriverDownloadUrl(browserName);

        return String.format(
            "No se pudo configurar %s. Verifica: (1) driver.strategy en config, " +
            "(2) DRIVER_LOCAL_PATH o ARTIFACTORY_BASE_URL, (3) Descarga manual desde %s",
            driverName, downloadUrl);
    }

    /**
     * Construye mensaje de error para estrategia LOCAL.
     */
    private static String buildDriverNotFoundErrorLocal(String driverName, String propertyName, String browserName, String errorDetail) {
        String downloadUrl = getDriverDownloadUrl(browserName);
        return String.format(
            "ESTRATEGIA LOCAL: %s no encontrado. Error: %s. Descarga desde: %s",
            driverName, errorDetail, downloadUrl);
    }

    /**
     * Construye mensaje de error para estrategia ARTIFACTORY.
     */
    private static String buildDriverNotFoundErrorArtifactory(String driverName, String propertyName, String browserName, String errorDetail) {
        return String.format(
            "ESTRATEGIA ARTIFACTORY: Error descargando %s: %s. Verifica URL base y conectividad",
            driverName, errorDetail);
    }

    /**
     * Obtiene la URL de descarga oficial del driver según el navegador.
     */
    private static String getDriverDownloadUrl(String browserName) {
        return switch (browserName.toLowerCase()) {
            case "chrome" -> "https://googlechromelabs.github.io/chrome-for-testing/";
            case "firefox" -> "https://github.com/mozilla/geckodriver/releases";
            case "edge" -> "https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/";
            default -> "https://www.selenium.dev/documentation/webdriver/getting_started/install_drivers/";
        };
    }

    /**
     * Configura timeouts y maximiza ventana del driver.
     * <p>
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
