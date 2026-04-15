package com.qa.webcore.driver;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.config.WebConfigKeys;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;

/**
 * Factory para la creación y configuración de WebDrivers del QA Automation Framework.
 *
 * <h2>Estrategia de inicialización (orden de intentos)</h2>
 * <p>Para ejecución local, {@code createLocalXxxDriver()} sigue esta secuencia:
 * <ol>
 *   <li><b>System Property manual</b> — si {@code webdriver.xxx.driver} ya está configurada,
 *       se usa directamente sin intentar otras estrategias.</li>
 *   <li><b>Selenium Manager</b> (Selenium 4.6+, automático) — descarga y cachea el driver
 *       binario compatible con el navegador instalado. Requiere acceso a internet.</li>
 *   <li><b>PATH del sistema</b> — busca el binario del driver en directorios comunes
 *       de Windows/Mac/Linux. Ideal para CI/CD con drivers pre-instalados.</li>
 * </ol>
 * <p>Si los tres fallan se lanza {@link WebDriverInitializationException} con pasos claros
 * de resolución.
 *
 * <h2>Ciclo de vida vía ServiceRegistry</h2>
 * <p>Usar {@link #createDriver(DriverConfig, ExecutionContext)} para crear el driver y
 * registrarlo en el {@code ServiceRegistry} del contexto. Esto permite que
 * {@link com.qa.webcore.plugin.WebPlugin#onScenarioEnd} cierre el driver automáticamente
 * vía {@link #closeDriver(ExecutionContext)}.
 *
 * <h2>Navegadores soportados</h2>
 * <p>Chrome, Firefox, Edge, Safari — en modo Local y Grid (Selenium Grid 3/4, cloud providers).
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(WebDriverFactory.class);

    // =========================================================================
    // Enums
    // =========================================================================

    /** Tipos de navegador soportados. */
    public enum BrowserType {
        CHROME, FIREFOX, EDGE, SAFARI
    }

    /** Modos de ejecución del driver. */
    public enum ExecutionMode {
        LOCAL,  // Driver local con Selenium Manager / PATH
        GRID    // RemoteWebDriver contra Selenium Grid
    }

    // =========================================================================
    // DriverConfig — Builder pattern
    // =========================================================================

    /**
     * Configuración inmutable del driver (Builder pattern).
     *
     * <p>Construir vía: {@code new DriverConfig(BrowserType.CHROME).withHeadless(true)}.
     */
    public static class DriverConfig {
        private final BrowserType browserType;
        private ExecutionMode executionMode;
        private boolean headless;
        private String gridHubUrl;
        private String proxyUrl;
        private boolean acceptInsecureCerts;

        public DriverConfig(BrowserType browserType) {
            this.browserType       = browserType;
            this.executionMode     = ExecutionMode.LOCAL;
            this.headless          = false;
            this.acceptInsecureCerts = true;
        }

        public DriverConfig withHeadless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public DriverConfig withGrid(String gridHubUrl) {
            this.executionMode = ExecutionMode.GRID;
            this.gridHubUrl    = gridHubUrl;
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

        public BrowserType    getBrowserType()        { return browserType; }
        public ExecutionMode  getExecutionMode()      { return executionMode; }
        public boolean        isHeadless()            { return headless; }
        public String         getGridHubUrl()         { return gridHubUrl; }
        public String         getProxyUrl()           { return proxyUrl; }
        public boolean        isAcceptInsecureCerts() { return acceptInsecureCerts; }
    }

    // =========================================================================
    // Constructor privado — utility class
    // =========================================================================

    private WebDriverFactory() {}

    // =========================================================================
    // API PÚBLICA — Creación
    // =========================================================================

    /**
     * Crea un WebDriver local con configuración mínima (sin headless).
     *
     * @param browserType tipo de navegador
     * @return instancia lista para usar
     * @throws WebDriverInitializationException si no se puede inicializar el driver
     */
    public static WebDriver createDriver(BrowserType browserType) {
        return createDriver(browserType, false);
    }

    /**
     * Crea un WebDriver local con control de modo headless.
     *
     * @param browserType tipo de navegador
     * @param headless    {@code true} para modo sin UI
     * @return instancia lista para usar
     * @throws WebDriverInitializationException si no se puede inicializar el driver
     */
    public static WebDriver createDriver(BrowserType browserType, boolean headless) {
        return createDriver(new DriverConfig(browserType).withHeadless(headless));
    }

    /**
     * Crea un WebDriver con configuración completa.
     *
     * @param config configuración del driver
     * @return instancia lista para usar
     * @throws WebDriverInitializationException si no se puede inicializar el driver
     */
    public static WebDriver createDriver(DriverConfig config) {
        String driverStrategy = resolveDriverStrategy();
        log.info("[WebDriverFactory] Creando WebDriver: browser={} mode={} headless={} strategy={}",
                config.getBrowserType(), config.getExecutionMode(), config.isHeadless(), driverStrategy);

        WebDriver driver = (config.getExecutionMode() == ExecutionMode.LOCAL)
                ? createLocalDriver(config)
                : createGridDriver(config);

        configureDriver(driver);
        log.info("[WebDriverFactory] ✅ WebDriver listo: {}", config.getBrowserType());
        return driver;
    }

    /**
     * Crea un WebDriver, lo registra en el {@link ExecutionContext#registry()} y lo retorna.
     *
     * <p>Al estar en {@code ServiceRegistry}, {@link com.qa.webcore.plugin.WebPlugin}
     * puede cerrar el driver automáticamente en {@code onScenarioEnd} vía
     * {@link #closeDriver(ExecutionContext)}.
     *
     * @param config  configuración del driver
     * @param context contexto de la ejecución actual; si es {@code null} no se registra
     * @return instancia lista para usar
     * @throws WebDriverInitializationException si no se puede inicializar el driver
     * @since 2.2.0
     */
    public static WebDriver createDriver(DriverConfig config, ExecutionContext context) {
        WebDriver driver = createDriver(config);
        if (context != null) {
            context.registry().registerInstance(WebDriver.class, driver);
            log.debug("[WebDriverFactory] WebDriver registrado en ServiceRegistry del contexto");
        }
        return driver;
    }

    // =========================================================================
    // API PÚBLICA — Ciclo de vida
    // =========================================================================

    /**
     * Cierra el WebDriver registrado en el {@link ExecutionContext#registry()}.
     *
     * <p>Llamado por {@link com.qa.webcore.plugin.WebPlugin#onScenarioEnd} al finalizar
     * cada escenario. Es idempotente: si el driver ya fue cerrado o no estaba registrado,
     * no lanza excepción.
     *
     * @param context contexto del que extraer el driver; si es {@code null}, no hace nada
     * @since 2.2.0
     */
    public static void closeDriver(ExecutionContext context) {
        if (context == null) {
            log.debug("[WebDriverFactory] closeDriver() invocado con context=null — no-op");
            return;
        }
        context.registry().get(WebDriver.class).ifPresentOrElse(
            driver -> {
                try {
                    driver.quit();
                    log.info("[WebDriverFactory] ✅ WebDriver cerrado vía ServiceRegistry");
                } catch (Exception e) {
                    // Puede ocurrir si el driver ya fue cerrado (NoSuchSessionException).
                    // Absorbemos silenciosamente — el objetivo (driver cerrado) se cumplió.
                    log.warn("[WebDriverFactory] Aviso al cerrar WebDriver (puede ser cierre doble): {}",
                             e.getMessage());
                }
            },
            () -> log.debug("[WebDriverFactory] No hay WebDriver en ServiceRegistry — nada que cerrar")
        );
    }

    // =========================================================================
    // CREACIÓN LOCAL — estrategia de fallback por navegador
    // =========================================================================

    private static WebDriver createLocalDriver(DriverConfig config) {
        return switch (config.getBrowserType()) {
            case CHROME  -> createLocalChromeDriver(config);
            case FIREFOX -> createLocalFirefoxDriver(config);
            case EDGE    -> createLocalEdgeDriver(config);
            case SAFARI  -> createLocalSafariDriver(config);
        };
    }

    /**
     * Crea ChromeDriver con estrategia de fallback de 3 niveles:
     * <ol>
     *   <li>System Property manual ({@code webdriver.chrome.driver})</li>
     *   <li>Selenium Manager (automático en Selenium 4.6+)</li>
     *   <li>PATH del sistema (chromedriver en directorios comunes)</li>
     * </ol>
     */
    private static WebDriver createLocalChromeDriver(DriverConfig config) {
        ChromeOptions options = buildChromeOptions(config);

        // Estrategia 0: System Property configurada manualmente — respetar siempre
        String manualPath = System.getProperty("webdriver.chrome.driver");
        if (manualPath != null && !manualPath.isEmpty()) {
            log.debug("[WebDriverFactory] Usando System Property: webdriver.chrome.driver={}", manualPath);
            try {
                return new ChromeDriver(options);
            } catch (Exception e) {
                throw new WebDriverInitializationException(
                        "ChromeDriver via System Property falló (path=" + manualPath + "): " + e.getMessage(), e);
            }
        }

        // Estrategia 1: Selenium Manager (Selenium 4.6+, automático)
        try {
            WebDriver driver = new ChromeDriver(options);
            log.info("[WebDriverFactory] ✅ ChromeDriver inicializado vía Selenium Manager");
            return driver;
        } catch (Exception seleniumManagerEx) {
            log.warn("[WebDriverFactory] ⚠  Selenium Manager falló para Chrome: {}", seleniumManagerEx.getMessage());
        }

        // Estrategia 2: Buscar chromedriver en PATH del sistema
        String pathDriver = findInSystemPath("chromedriver");
        if (pathDriver != null) {
            System.setProperty("webdriver.chrome.driver", pathDriver);
            try {
                WebDriver driver = new ChromeDriver(options);
                log.info("[WebDriverFactory] ✅ ChromeDriver inicializado desde PATH: {}", pathDriver);
                return driver;
            } catch (Exception pathEx) {
                log.warn("[WebDriverFactory] ⚠  chromedriver en PATH también falló: {}", pathEx.getMessage());
            }
        }

        throw new WebDriverInitializationException(buildInitError("ChromeDriver", "chromedriver",
                "webdriver.chrome.driver", "https://googlechromelabs.github.io/chrome-for-testing/"));
    }

    /**
     * Crea FirefoxDriver con estrategia de fallback de 3 niveles:
     * <ol>
     *   <li>System Property manual ({@code webdriver.gecko.driver})</li>
     *   <li>Selenium Manager (automático en Selenium 4.6+)</li>
     *   <li>PATH del sistema (geckodriver en directorios comunes)</li>
     * </ol>
     */
    private static WebDriver createLocalFirefoxDriver(DriverConfig config) {
        FirefoxOptions options = buildFirefoxOptions(config);

        String manualPath = System.getProperty("webdriver.gecko.driver");
        if (manualPath != null && !manualPath.isEmpty()) {
            log.debug("[WebDriverFactory] Usando System Property: webdriver.gecko.driver={}", manualPath);
            try {
                return new FirefoxDriver(options);
            } catch (Exception e) {
                throw new WebDriverInitializationException(
                        "FirefoxDriver via System Property falló (path=" + manualPath + "): " + e.getMessage(), e);
            }
        }

        // Estrategia 1: Selenium Manager
        try {
            WebDriver driver = new FirefoxDriver(options);
            log.info("[WebDriverFactory] ✅ FirefoxDriver inicializado vía Selenium Manager");
            return driver;
        } catch (Exception seleniumManagerEx) {
            log.warn("[WebDriverFactory] ⚠  Selenium Manager falló para Firefox: {}", seleniumManagerEx.getMessage());
        }

        // Estrategia 2: PATH
        String pathDriver = findInSystemPath("geckodriver");
        if (pathDriver != null) {
            System.setProperty("webdriver.gecko.driver", pathDriver);
            try {
                WebDriver driver = new FirefoxDriver(options);
                log.info("[WebDriverFactory] ✅ FirefoxDriver inicializado desde PATH: {}", pathDriver);
                return driver;
            } catch (Exception pathEx) {
                log.warn("[WebDriverFactory] ⚠  geckodriver en PATH también falló: {}", pathEx.getMessage());
            }
        }

        throw new WebDriverInitializationException(buildInitError("FirefoxDriver", "geckodriver",
                "webdriver.gecko.driver", "https://github.com/mozilla/geckodriver/releases"));
    }

    /**
     * Crea EdgeDriver con estrategia de fallback de 3 niveles:
     * <ol>
     *   <li>System Property manual ({@code webdriver.edge.driver})</li>
     *   <li>Selenium Manager (automático en Selenium 4.6+)</li>
     *   <li>PATH del sistema (msedgedriver en directorios comunes)</li>
     * </ol>
     */
    private static WebDriver createLocalEdgeDriver(DriverConfig config) {
        EdgeOptions options = buildEdgeOptions(config);

        String manualPath = System.getProperty("webdriver.edge.driver");
        if (manualPath != null && !manualPath.isEmpty()) {
            log.debug("[WebDriverFactory] Usando System Property: webdriver.edge.driver={}", manualPath);
            try {
                return new EdgeDriver(options);
            } catch (Exception e) {
                throw new WebDriverInitializationException(
                        "EdgeDriver via System Property falló (path=" + manualPath + "): " + e.getMessage(), e);
            }
        }

        // Estrategia 1: Selenium Manager
        try {
            WebDriver driver = new EdgeDriver(options);
            log.info("[WebDriverFactory] ✅ EdgeDriver inicializado vía Selenium Manager");
            return driver;
        } catch (Exception seleniumManagerEx) {
            log.warn("[WebDriverFactory] ⚠  Selenium Manager falló para Edge: {}", seleniumManagerEx.getMessage());
        }

        // Estrategia 2: PATH
        String pathDriver = findInSystemPath("msedgedriver");
        if (pathDriver != null) {
            System.setProperty("webdriver.edge.driver", pathDriver);
            try {
                WebDriver driver = new EdgeDriver(options);
                log.info("[WebDriverFactory] ✅ EdgeDriver inicializado desde PATH: {}", pathDriver);
                return driver;
            } catch (Exception pathEx) {
                log.warn("[WebDriverFactory] ⚠  msedgedriver en PATH también falló: {}", pathEx.getMessage());
            }
        }

        throw new WebDriverInitializationException(buildInitError("EdgeDriver", "msedgedriver",
                "webdriver.edge.driver",
                "https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/"));
    }

    /**
     * Crea SafariDriver.
     *
     * <p>Safari solo funciona en macOS con {@code safaridriver} activado
     * ({@code safaridriver --enable}). No requiere binario externo ni Selenium Manager.
     * No soporta headless ni configuración de proxy programática.
     */
    private static WebDriver createLocalSafariDriver(DriverConfig config) {
        if (config.isHeadless()) {
            log.warn("[WebDriverFactory] Safari no soporta modo headless — se ignora la opción");
        }
        try {
            WebDriver driver = new SafariDriver();
            log.info("[WebDriverFactory] ✅ SafariDriver inicializado");
            return driver;
        } catch (Exception e) {
            throw new WebDriverInitializationException(
                    "No se pudo inicializar SafariDriver. " +
                    "Verifique: (1) ejecutar 'safaridriver --enable' en macOS, " +
                    "(2) habilitar 'Allow Remote Automation' en Safari → Develop.", e);
        }
    }

    // =========================================================================
    // CREACIÓN GRID — RemoteWebDriver
    // =========================================================================

    private static WebDriver createGridDriver(DriverConfig config) {
        if (config.getGridHubUrl() == null || config.getGridHubUrl().isEmpty()) {
            throw new WebDriverInitializationException(
                    "Grid Hub URL es requerida para ExecutionMode.GRID. " +
                    "Configure '" + WebConfigKeys.GRID_URL + "' en la configuración.");
        }
        try {
            URL hubUrl = new java.net.URI(config.getGridHubUrl()).toURL();
            WebDriver driver = switch (config.getBrowserType()) {
                case CHROME  -> new RemoteWebDriver(hubUrl, buildChromeOptions(config));
                case FIREFOX -> new RemoteWebDriver(hubUrl, buildFirefoxOptions(config));
                case EDGE    -> new RemoteWebDriver(hubUrl, buildEdgeOptions(config));
                case SAFARI  -> new RemoteWebDriver(hubUrl, buildSafariOptions());
            };
            log.info("[WebDriverFactory] ✅ RemoteWebDriver conectado a Grid: {}", config.getGridHubUrl());
            return driver;
        } catch (WebDriverInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDriverInitializationException(
                    "Error conectando a Selenium Grid (" + config.getGridHubUrl() + "): " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // OPCIONES DE NAVEGADOR — builders extraídos para reuso
    // =========================================================================

    private static ChromeOptions buildChromeOptions(DriverConfig config) {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());
        options.addArguments("--no-sandbox");
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        if (config.getProxyUrl() != null) {
            options.setProxy(buildProxy(config.getProxyUrl()));
        }
        log.debug("[WebDriverFactory] ChromeOptions: headless={} proxy={}", config.isHeadless(), config.getProxyUrl());
        return options;
    }

    private static FirefoxOptions buildFirefoxOptions(DriverConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());
        if (config.isHeadless()) {
            options.addArguments("--headless");
        }
        if (config.getProxyUrl() != null) {
            options.setProxy(buildProxy(config.getProxyUrl()));
        }
        return options;
    }

    private static EdgeOptions buildEdgeOptions(DriverConfig config) {
        EdgeOptions options = new EdgeOptions();
        options.setAcceptInsecureCerts(config.isAcceptInsecureCerts());
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        if (config.getProxyUrl() != null) {
            options.setProxy(buildProxy(config.getProxyUrl()));
        }
        return options;
    }

    private static SafariOptions buildSafariOptions() {
        SafariOptions options = new SafariOptions();
        options.setUseTechnologyPreview(true);
        return options;
    }

    // =========================================================================
    // UTILIDADES INTERNAS
    // =========================================================================

    /**
     * Busca el binario del driver en directorios comunes del SO.
     *
     * @param driverName nombre del ejecutable (chromedriver, geckodriver, msedgedriver)
     * @return ruta absoluta si se encontró, {@code null} si no
     */
    static String findInSystemPath(String driverName) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String fileName = os.contains("win") ? driverName + ".exe" : driverName;

        String[] paths;
        if (os.contains("win")) {
            paths = new String[]{
                    "C:\\webdrivers\\",
                    "C:\\Program Files\\webdrivers\\",
                    "C:\\selenium\\drivers\\",
                    System.getenv("LOCALAPPDATA") != null
                            ? System.getenv("LOCALAPPDATA") + "\\Programs\\webdrivers\\" : ""
            };
        } else if (os.contains("mac")) {
            paths = new String[]{
                    "/usr/local/bin/",
                    "/usr/bin/",
                    "/opt/homebrew/bin/",
                    System.getProperty("user.home") + "/webdrivers/"
            };
        } else {
            paths = new String[]{
                    "/usr/local/bin/",
                    "/usr/bin/",
                    "/opt/selenium/drivers/",
                    System.getProperty("user.home") + "/webdrivers/"
            };
        }

        for (String dir : paths) {
            if (dir == null || dir.isEmpty()) continue;
            java.io.File candidate = new java.io.File(dir + fileName);
            if (candidate.exists() && candidate.canExecute()) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    /** Lee la estrategia de driver desde ConfigManager (para logging). */
    private static String resolveDriverStrategy() {
        try {
            return com.qa.common.config.ConfigManager.getInstance()
                    .get(WebConfigKeys.DRIVER_STRATEGY, "auto");
        } catch (Exception e) {
            return "auto";
        }
    }

    /** Construye la configuración de proxy para las opciones del navegador. */
    private static Proxy buildProxy(String proxyUrl) {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyUrl);
        proxy.setFtpProxy(proxyUrl);
        proxy.setSslProxy(proxyUrl);
        proxy.setNoProxy("*.local,localhost,127.0.0.1");
        log.debug("[WebDriverFactory] Proxy configurado: {}", proxyUrl);
        return proxy;
    }

    /**
     * Construye el mensaje de error estándar cuando todos los intentos de inicialización fallan.
     *
     * @param driverClass  nombre de la clase de driver (ej: "ChromeDriver")
     * @param binaryName   nombre del ejecutable (ej: "chromedriver")
     * @param sysPropKey   clave de System Property (ej: "webdriver.chrome.driver")
     * @param downloadUrl  URL oficial de descarga del driver
     * @return mensaje descriptivo con pasos de resolución
     */
    private static String buildInitError(String driverClass, String binaryName,
                                         String sysPropKey, String downloadUrl) {
        return String.format(
                "No se pudo inicializar %s. Verifique:%n" +
                "  1. Selenium Manager: acceso a internet y versión de navegador compatible%n" +
                "  2. PATH del sistema: '%s' ejecutable y compatible con el navegador instalado%n" +
                "  3. Manual: -D%s=/ruta/al/%s%n" +
                "  Descarga oficial: %s",
                driverClass, binaryName, sysPropKey, binaryName, downloadUrl);
    }

    /**
     * Aplica timeouts base al driver recién creado.
     *
     * <p>Implicit wait en 0 para evitar interferencia con explicit waits.
     * Page load timeout en 30s como valor base (sobreescribible por {@code WaitUtils}).
     */
    private static void configureDriver(WebDriver driver) {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            try {
                driver.manage().window().maximize();
            } catch (Exception e) {
                log.warn("[WebDriverFactory] No se pudo maximizar ventana: {}", e.getMessage());
            }
            log.debug("[WebDriverFactory] Timeouts configurados (implicit=0s, pageLoad=30s)");
        } catch (Exception e) {
            log.error("[WebDriverFactory] Error configurando driver: {}", e.getMessage());
        }
    }
}
