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
     * Configuración genérica y robusta de drivers con estrategia de fallback.
     *
     * <p><b>Estrategia de fallback (en orden):</b></p>
     * <ol>
     *   <li><b>Driver manual:</b> System Property configurado (ej: -Dwebdriver.chrome.driver=...)</li>
     *   <li><b>WebDriverManager online:</b> Intenta descargar desde internet (con timeout reducido)</li>
     *   <li><b>Cache local:</b> Busca en ~/.cache/selenium/ si descarga falla</li>
     *   <li><b>PATH del sistema:</b> Busca en directorios comunes del SO</li>
     *   <li><b>Error descriptivo:</b> Mensaje claro con instrucciones de solución</li>
     * </ol>
     *
     * <p><b>Soporta:</b> Windows, Mac, Linux | Con/sin internet | Con/sin proxy | Con/sin firewall</p>
     *
     * @param driverName Nombre del ejecutable (chromedriver, geckodriver, msedgedriver)
     * @param propertyName System Property key (webdriver.chrome.driver, webdriver.gecko.driver, etc.)
     * @param browserName Nombre del navegador para logs (Chrome, Firefox, Edge)
     */
    private static void setupDriver(String driverName, String propertyName, String browserName) {
        // Configurar System Properties de red para WebDriverManager
        configureNetworkProperties();
        // FALLBACK 1: ¿Hay driver manual configurado?
        String manualDriverPath = System.getProperty(propertyName);
        if (manualDriverPath != null && !manualDriverPath.isEmpty()) {
            java.io.File manualDriver = new java.io.File(manualDriverPath);
            if (manualDriver.exists() && manualDriver.canExecute()) {
                TestLogger.logInfo("WEB_DRIVER_FACTORY",
                    String.format("✅ Usando %s manual: %s", driverName, manualDriverPath), null);
                return;
            } else {
                TestLogger.logWarning("WEB_DRIVER_FACTORY",
                    String.format("⚠️ Driver manual configurado pero no válido: %s", manualDriverPath), null);
            }
        }

        // FALLBACK 2: Intentar WebDriverManager (con timeout reducido y configuración robusta)
        try {
            TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("🔄 Intentando configurar %s con WebDriverManager...", driverName), null);

            WebDriverManager wdm = getWebDriverManager(driverName);

            // Configurar cache local PRIMERO (prioridad máxima)
            String cachePath = getDefaultCachePath();
            wdm.cachePath(cachePath);

            // Verificar si ya existe en cache antes de intentar descargar
            java.io.File cacheDir = new java.io.File(cachePath);
            if (cacheDir.exists()) {
                TestLogger.logInfo("WEB_DRIVER_FACTORY",
                    String.format("📁 Cache encontrado en: %s", cachePath), null);
                wdm.ttl(0);  // Usar cache sin verificar versión online
            }

            // Configurar proxy si existe (usar configuración del sistema)
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort");

            // Si no hay proxy en System Properties, intentar con variables de entorno
            if (proxyHost == null) {
                proxyHost = System.getenv("HTTP_PROXY_HOST");
                proxyPort = System.getenv("HTTP_PROXY_PORT");
            }

            if (proxyHost != null && proxyPort != null) {
                String proxyUrl = proxyHost + ":" + proxyPort;
                TestLogger.logInfo("WEB_DRIVER_FACTORY",
                    String.format("🔧 Configurando proxy: %s", proxyUrl), null);
                wdm.proxy(proxyUrl);
            }

            // Configuración ULTRA ROBUSTA para ambientes corporativos
            wdm.timeout(10)  // Timeout AGRESIVO: 10 segundos (default: 60s)
               .avoidReadReleaseFromRepository()  // Evitar consultas lentas a repositorios
               .avoidBrowserDetection()  // No detectar navegador (ahorra 2-3 segundos)
               .avoidExport();  // No exportar variables de entorno

            TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("⏱️ Timeout configurado: 10 segundos (máximo)"), null);

            wdm.setup();

            TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("✅ %s configurado correctamente vía WebDriverManager", driverName), null);
            return;

        } catch (Exception e) {
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            TestLogger.logWarning("WEB_DRIVER_FACTORY",
                String.format("⚠️ WebDriverManager falló para %s: %s", driverName, errorDetail), null);
            TestLogger.logWarning("WEB_DRIVER_FACTORY",
                "💡 Intentando fallbacks (cache local → PATH sistema → error descriptivo)", null);
        }

        // FALLBACK 3: Buscar en cache local
        String cacheDriver = findDriverInCache(driverName);
        if (cacheDriver != null) {
            System.setProperty(propertyName, cacheDriver);
            TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("✅ Usando %s desde cache: %s", driverName, cacheDriver), null);
            return;
        }

        // FALLBACK 4: Buscar en PATH del sistema
        String systemDriver = findDriverInSystemPath(driverName);
        if (systemDriver != null) {
            System.setProperty(propertyName, systemDriver);
            TestLogger.logInfo("WEB_DRIVER_FACTORY",
                String.format("✅ Usando %s desde PATH: %s", driverName, systemDriver), null);
            return;
        }

        // FALLBACK 5: Todo falló → Error descriptivo con soluciones
        String errorMsg = buildDriverNotFoundError(driverName, propertyName, browserName);
        TestLogger.logError("WEB_DRIVER_FACTORY", errorMsg, null);
        throw new RuntimeException(errorMsg);
    }

    /**
     * Configura System Properties de red para que WebDriverManager use timeouts razonables.
     * Esto evita que WebDriverManager espere 85+ segundos antes de fallar.
     */
    private static void configureNetworkProperties() {
        // Solo configurar si no están ya establecidos
        if (System.getProperty("sun.net.client.defaultConnectTimeout") == null) {
            System.setProperty("sun.net.client.defaultConnectTimeout", "10000");  // 10 segundos
        }
        if (System.getProperty("sun.net.client.defaultReadTimeout") == null) {
            System.setProperty("sun.net.client.defaultReadTimeout", "10000");  // 10 segundos
        }

        // Configurar timeouts para Apache HttpClient (usado por WebDriverManager)
        if (System.getProperty("http.connection.timeout") == null) {
            System.setProperty("http.connection.timeout", "10000");  // 10 segundos
        }
        if (System.getProperty("http.socket.timeout") == null) {
            System.setProperty("http.socket.timeout", "10000");  // 10 segundos
        }

        TestLogger.logDebug("WEB_DRIVER_FACTORY",
            "⚙️ Timeouts de red configurados: 10 segundos (conexión y lectura)", null);
    }

    /**
     * Obtiene el WebDriverManager correspondiente según el nombre del driver.
     */
    private static WebDriverManager getWebDriverManager(String driverName) {
        switch (driverName.toLowerCase()) {
            case "chromedriver":
                return WebDriverManager.chromedriver();
            case "geckodriver":
                return WebDriverManager.firefoxdriver();
            case "msedgedriver":
                return WebDriverManager.edgedriver();
            default:
                throw new IllegalArgumentException("Driver no soportado: " + driverName);
        }
    }

    /**
     * Obtiene la ruta del cache multiplataforma (~/.cache/selenium/).
     */
    private static String getDefaultCachePath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return userHome + "\\.cache\\selenium";
        } else {
            return userHome + "/.cache/selenium";
        }
    }

    /**
     * Busca el driver en el cache local de Selenium.
     * Soporta: Windows, Mac, Linux.
     *
     * @return Ruta completa del driver si se encuentra, null si no existe
     */
    private static String findDriverInCache(String driverName) {
        String cachePath = getDefaultCachePath();
        java.io.File cacheDir = new java.io.File(cachePath);

        if (!cacheDir.exists() || !cacheDir.isDirectory()) {
            return null;
        }

        // Buscar recursivamente en subdirectorios
        return searchDriverRecursively(cacheDir, driverName);
    }

    /**
     * Busca el driver recursivamente en un directorio.
     */
    private static String searchDriverRecursively(java.io.File dir, String driverName) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return null;

        String os = System.getProperty("os.name").toLowerCase();
        String driverFileName = driverName;
        if (os.contains("win")) {
            driverFileName += ".exe";
        }

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                String found = searchDriverRecursively(file, driverName);
                if (found != null) return found;
            } else if (file.getName().equals(driverFileName) && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
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
     * Construye mensaje de error descriptivo con soluciones específicas por SO.
     */
    private static String buildDriverNotFoundError(String driverName, String propertyName, String browserName) {
        String os = System.getProperty("os.name").toLowerCase();
        String downloadUrl = getDriverDownloadUrl(browserName);

        StringBuilder error = new StringBuilder();
        error.append(String.format("\n\n❌ No se pudo configurar %s automáticamente.\n\n", driverName));
        error.append("🔥 CAUSA PROBABLE: Firewall/Proxy corporativo bloqueando descarga.\n\n");
        error.append("📋 SOLUCIONES:\n\n");

        // Solución 1: Descarga manual (específica por SO)
        error.append("1️⃣ DESCARGA MANUAL (Recomendado):\n");
        error.append(String.format("   ➤ Descargar desde: %s\n", downloadUrl));
        error.append(String.format("   ➤ Versión debe coincidir con %s instalado\n", browserName));

        if (os.contains("win")) {
            error.append(String.format("   ➤ Extraer %s.exe a: C:\\webdrivers\\\n", driverName));
            error.append(String.format("   ➤ Agregar property: -D%s=C:\\webdrivers\\%s.exe\n\n", propertyName, driverName));
        } else if (os.contains("mac")) {
            error.append(String.format("   ➤ Extraer %s a: /usr/local/bin/\n", driverName));
            error.append(String.format("   ➤ Dar permisos: chmod +x /usr/local/bin/%s\n", driverName));
            error.append(String.format("   ➤ O agregar property: -D%s=/usr/local/bin/%s\n\n", propertyName, driverName));
        } else {
            error.append(String.format("   ➤ Extraer %s a: /usr/local/bin/\n", driverName));
            error.append(String.format("   ➤ Dar permisos: sudo chmod +x /usr/local/bin/%s\n", driverName));
            error.append(String.format("   ➤ O agregar property: -D%s=/usr/local/bin/%s\n\n", propertyName, driverName));
        }

        // Solución 2: Configurar proxy
        error.append("2️⃣ CONFIGURAR PROXY (Si tienes proxy corporativo):\n");
        error.append("   ➤ Agregar: -Dhttp.proxyHost=proxy.empresa.com\n");
        error.append("   ➤ Agregar: -Dhttp.proxyPort=8080\n");
        error.append("   ➤ Ejecutar tests: ./gradlew test -Dhttp.proxyHost=... -Dhttp.proxyPort=...\n\n");

        // Solución 3: Cache local
        error.append("3️⃣ USAR CACHE LOCAL (Si ya descargaste antes):\n");
        error.append(String.format("   ➤ Buscar en: %s\n", getDefaultCachePath()));
        error.append(String.format("   ➤ Copiar %s a ubicación manual (opción 1)\n\n", driverName));

        // Solución 4: Contacto
        error.append("4️⃣ PEDIR AYUDA:\n");
        error.append("   ➤ Contactar al equipo de QA o Infra\n");
        error.append(String.format("   ➤ Solicitar %s preconfigurado\n\n", driverName));

        return error.toString();
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
