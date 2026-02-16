package com.scotia.qa.common.driver;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.logging.TestLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;

/**
 * Gestor simplificado de WebDrivers con estrategia dual.
 *
 * <p>Implementa una estrategia simple y robusta con 2 opciones:</p>
 * <ol>
 *   <li><strong>Local Path</strong> - Ruta configurada manualmente (recomendado para desarrollo)</li>
 *   <li><strong>Artifactory</strong> - Descarga desde repositorio corporativo (CI/CD)</li>
 * </ol>
 *
 * <p>Esta clase consolida toda la lógica de gestión de drivers, incluyendo descarga,
 * extracción, detección de OS, y reintentos automáticos.</p>
 *
 * <h3>Configuración:</h3>
 * <pre>
 * # config-scotia.properties
 * # Estrategia: local o artifactory
 * driver.strategy=local
 *
 * # Opción 1: LOCAL (desarrollo local)
 * driver.local.base.path=${DRIVER_LOCAL_PATH}
 *
 * # Opción 2: ARTIFACTORY (CI/CD)
 * driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
 * driver.artifactory.user=${ARTIFACTORY_USER}
 * driver.artifactory.token=${ARTIFACTORY_TOKEN}
 *
 * # Configuración de versión
 * driver.chrome.version=143.0.7499.41
 *
 * # .env.local
 * DRIVER_LOCAL_PATH=/Users/tu_usuario/drivers  (Mac/Linux) o C:/drivers (Windows)
 * ARTIFACTORY_BASE_URL=https://artifactory.corp.com/qa-drivers
 * ARTIFACTORY_USER=tu_usuario
 * ARTIFACTORY_TOKEN=tu_token
 * </pre>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>{@code
 * // Obtener driver (usa estrategia configurada)
 * Path driver = WebDriverManager.getDriver("chromedriver", "143.0.7499.41");
 * System.setProperty("webdriver.chrome.driver", driver.toString());
 *
 * // Usar con auto-detección de versión desde config
 * Path driver = WebDriverManager.getDriverFromConfig("chromedriver");
 * }</pre>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2025-12-16
 */
public class WebDriverManager {

    private static final ConfigManager config = ConfigManager.getInstance();

    /**
     * Obtiene el driver usando la estrategia configurada.
     *
     * <p>Estrategias soportadas:</p>
     * <ul>
     *   <li><strong>local</strong> - Busca en path local fijo configurado</li>
     *   <li><strong>artifactory</strong> - Descarga desde repositorio corporativo</li>
     * </ul>
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @param version Versión específica (ej: "143.0.7499.41")
     * @return Path al ejecutable del driver
     * @throws DriverNotFoundException Si no se encuentra con la estrategia configurada
     */
    public static Path getDriver(String driverName, String version) {
        String strategy = config.get("driver.strategy", "local").toLowerCase();

        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("🔍 Buscando %s %s usando estrategia: %s",
                driverName, version, strategy.toUpperCase()),
            Map.of("driver", driverName, "version", version, "strategy", strategy));

        switch (strategy) {
            case "local":
                return getDriverFromLocalStrategy(driverName, version);

            case "artifactory":
                return getDriverFromArtifactoryStrategy(driverName, version);

            default:
                throw new DriverNotFoundException(
                    String.format("Estrategia '%s' no válida. Valores permitidos: 'local', 'artifactory'",
                        strategy));
        }
    }

    /**
     * Obtiene driver usando estrategia LOCAL.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver local
     * @throws DriverNotFoundException Si no se encuentra en path local
     */
    private static Path getDriverFromLocalStrategy(String driverName, String version) {
        try {
            Path localPath = findDriverInLocalPath(driverName, version);
            if (localPath != null) {
                TestLogger.logInfo("DRIVER_MANAGER",
                    String.format("✅ Driver encontrado en LOCAL PATH: %s %s",
                        driverName, version),
                    Map.of("strategy", "local", "path", localPath.toString()));
                return localPath;
            }
        } catch (IOException e) {
            throw new DriverNotFoundException(
                String.format("Error buscando %s %s en LOCAL PATH: %s",
                    driverName, version, e.getMessage()));
        }

        // No encontrado → error descriptivo
        String basePath = config.get("driver.local.base.path", "NO_CONFIGURADO");
        throw new DriverNotFoundException(
            String.format("❌ Driver %s %s no encontrado en LOCAL PATH: %s\n\n" +
                "📋 SOLUCIÓN:\n" +
                "1. Descargar driver desde: %s\n" +
                "2. Copiar a: %s/%s/%s/%s\n" +
                "3. Verificar permisos de ejecución",
                driverName, version, basePath,
                getDriverDownloadUrl(driverName),
                basePath, driverName, version, getExecutableName(driverName)));
    }

    /**
     * Obtiene driver usando estrategia ARTIFACTORY.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver descargado
     * @throws DriverNotFoundException Si falla la descarga
     */
    private static Path getDriverFromArtifactoryStrategy(String driverName, String version) {
        try {
            Path downloadedPath = downloadFromArtifactory(driverName, version);
            TestLogger.logInfo("DRIVER_MANAGER",
                String.format("✅ Driver descargado desde ARTIFACTORY: %s %s",
                    driverName, version),
                Map.of("strategy", "artifactory", "path", downloadedPath.toString()));
            return downloadedPath;
        } catch (IOException e) {
            String baseUrl = config.get("driver.artifactory.base.url", "NO_CONFIGURADO");
            throw new DriverNotFoundException(
                String.format("❌ Error descargando %s %s desde Artifactory: %s\n\n" +
                    "📋 SOLUCIÓN:\n" +
                    "1. Verificar URL: %s\n" +
                    "2. Verificar credenciales (ARTIFACTORY_USER, ARTIFACTORY_TOKEN)\n" +
                    "3. Verificar conectividad de red\n" +
                    "4. Considerar cambiar a estrategia 'local'",
                    driverName, version, e.getMessage(), baseUrl));
        }
    }

    /**
     * Obtiene el driver leyendo la versión desde configuración.
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @return Path al ejecutable del driver
     * @throws DriverNotFoundException Si falla la obtención o no se encuentra configuración
     */
    public static Path getDriverFromConfig(String driverName) {
        String versionKey = String.format("driver.%s.version",
            driverName.replace("driver", ""));

        String version = config.get(versionKey);
        if (version == null || version.isEmpty()) {
            throw new DriverNotFoundException(
                String.format("Versión de %s no configurada. Verifica %s en config-scotia.properties",
                    driverName, versionKey));
        }

        return getDriver(driverName, version);
    }

    /**
     * Busca driver en path local fijo configurado por el desarrollador.
     *
     * <p>Soporta tres estructuras de directorios:</p>
     * <ul>
     *   <li>Estructura versionada: {@code base-path/chromedriver/114.0.5735.90/chromedriver}</li>
     *   <li>Estructura de driver: {@code base-path/chromedriver/chromedriver} (cualquier versión)</li>
     *   <li>Estructura plana: {@code base-path/chromedriver} (directamente el ejecutable)</li>
     * </ul>
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @param version Versión específica del driver
     * @return Path al ejecutable si existe, null si no se encuentra
     * @throws IOException Si hay error accediendo al filesystem
     */
    private static Path findDriverInLocalPath(String driverName, String version) throws IOException {
        String basePath = config.get("driver.local.base.path");

        if (basePath == null || basePath.isEmpty()) {
            TestLogger.logDebug("DRIVER_MANAGER",
                "driver.local.base.path no configurado, omitiendo búsqueda en path local",
                null);
            return null;
        }

        // Resolver variables de entorno
        basePath = resolvePathVariables(basePath);
        Path baseDir = Paths.get(basePath);

        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            TestLogger.logWarning("DRIVER_MANAGER",
                "Path local no existe o no es un directorio",
                Map.of("path", basePath));
            return null;
        }

        String executableName = getExecutableName(driverName);

        // Estrategia 1: Buscar en estructura versionada (base-path/driver/version/executable)
        Path versionedPath = baseDir.resolve(driverName).resolve(version).resolve(executableName);
        if (Files.exists(versionedPath) && Files.isExecutable(versionedPath)) {
            TestLogger.logDebug("DRIVER_MANAGER",
                "Driver encontrado en estructura versionada",
                Map.of("path", versionedPath.toString()));
            return versionedPath;
        }

        // Estrategia 2: Buscar en carpeta de driver sin versión (base-path/driver/executable)
        Path driverDirPath = baseDir.resolve(driverName).resolve(executableName);
        if (Files.exists(driverDirPath) && Files.isExecutable(driverDirPath)) {
            TestLogger.logDebug("DRIVER_MANAGER",
                "Driver encontrado en carpeta de driver",
                Map.of("path", driverDirPath.toString()));
            return driverDirPath;
        }

        // Estrategia 3: Buscar en estructura plana (base-path/executable directamente)
        Path flatPath = baseDir.resolve(executableName);
        if (Files.exists(flatPath) && Files.isExecutable(flatPath)) {
            TestLogger.logDebug("DRIVER_MANAGER",
                "Driver encontrado en estructura plana",
                Map.of("path", flatPath.toString()));
            return flatPath;
        }

        TestLogger.logDebug("DRIVER_MANAGER",
            "Driver no encontrado en path local",
            Map.of(
                "basePath", basePath,
                "driverName", driverName,
                "version", version,
                "executable", executableName
            ));

        return null;
    }

    /**
     * Descarga driver desde Artifactory con autenticación.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver descargado
     * @throws IOException Si falla la descarga
     */
    private static Path downloadFromArtifactory(String driverName, String version) throws IOException {
        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("⬇️  Descargando driver desde Artifactory: %s %s", driverName, version),
            null);

        // 1. Construir URL de Artifactory
        String artifactoryUrl = buildArtifactoryUrl(driverName, version);

        // 2. Descargar driver ejecutable directo (NO ZIP - estructura nueva de Artifactory)
        Path downloadedDriver = downloadDriverExecutable(artifactoryUrl, driverName);

        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("✅ Driver descargado: %s %s", driverName, version),
            Map.of("path", downloadedDriver.toString()));

        return downloadedDriver;
    }

    /**
     * Descarga el driver ejecutable directamente desde Artifactory (sin ZIP).
     *
     * <p>Nuevo método para estructura de Artifactory corporativo que almacena
     * ejecutables directos en lugar de archivos ZIP.</p>
     *
     * @param url URL completa del driver en Artifactory
     * @param driverName Nombre del driver
     * @return Path al driver descargado en ~/.cache/qa-drivers/
     * @throws IOException Si falla la descarga
     */
    private static Path downloadDriverExecutable(String url, String driverName) throws IOException {
        String user = config.get("driver.artifactory.user");
        String token = config.get("driver.artifactory.token");

        if (user == null || token == null || user.isEmpty() || token.isEmpty()) {
            throw new IOException(
                "Credenciales de Artifactory no configuradas. " +
                "Verifica ARTIFACTORY_USER y ARTIFACTORY_TOKEN en .env.local");
        }

        int timeout = config.getInt("driver.artifactory.timeout", 60) * 1000;
        int maxRetries = config.getInt("driver.artifactory.retry.max", 3);
        boolean retryEnabled = config.getBoolean("driver.artifactory.retry.enabled", true);

        IOException lastException = null;
        int attempt = 0;

        while (attempt < (retryEnabled ? maxRetries : 1)) {
            attempt++;
            try {
                HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                conn.setRequestProperty("User-Agent", "Scotia-QA-Framework/2.0.0");

                // Autenticación Basic
                String auth = user + ":" + token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

                int status = conn.getResponseCode();

                if (status == 200) {
                    // Crear directorio de cache (~/.cache/qa-drivers/)
                    Path cacheDir = Paths.get(System.getProperty("user.home"), ".cache", "qa-drivers");
                    Files.createDirectories(cacheDir);

                    // Descargar ejecutable directamente
                    String executableName = getExecutableName(driverName);
                    Path driverPath = cacheDir.resolve(executableName);

                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, driverPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Hacer ejecutable (Unix/Mac/Linux)
                    boolean executable = driverPath.toFile().setExecutable(true, false);
                    if (!executable) {
                        TestLogger.logWarning("DRIVER_MANAGER",
                            "⚠️ No se pudieron establecer permisos de ejecución", null);
                    }

                    TestLogger.logInfo("DRIVER_MANAGER",
                        String.format("✓ Driver descargado exitosamente (intento %d/%d)", attempt, maxRetries),
                        Map.of(
                            "size", Files.size(driverPath) + " bytes",
                            "path", driverPath.toString(),
                            "executable", executableName
                        ));

                    return driverPath;

                } else if (status == 401) {
                    throw new IOException(
                        "Credenciales de Artifactory inválidas (HTTP 401). " +
                        "Verifica ARTIFACTORY_USER y ARTIFACTORY_TOKEN");
                } else if (status == 404) {
                    throw new IOException(String.format(
                        "Driver no encontrado en Artifactory (HTTP 404).\n" +
                        "URL: %s\n" +
                        "Verifica que el driver existe en esa ruta en Artifactory.", url));
                } else {
                    throw new IOException(String.format("Error descargando driver (HTTP %d)", status));
                }

            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries && retryEnabled) {
                    TestLogger.logWarning("DRIVER_MANAGER",
                        String.format("⚠️ Reintentando descarga (%d/%d): %s",
                            attempt, maxRetries, e.getMessage()),
                        null);
                    try {
                        Thread.sleep(2000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    break;
                }
            }
        }

        throw new IOException(
            String.format("Descarga fallida después de %d intentos. URL: %s", attempt, url),
            lastException);
    }

    /**
     * Construye la URL completa de Artifactory para descargar el driver.
     *
     * <p>Formato usado en Artifactory corporativo:</p>
     * <pre>
     * {base-url}/external/qa-drivers/{driver}-{os}/{driver}[.exe]
     * </pre>
     *
     * <p>Ejemplos de URLs generadas:</p>
     * <ul>
     *   <li>Windows: .../external/qa-drivers/chromedriver-win/chromedriver.exe</li>
     *   <li>Mac: .../external/qa-drivers/chromedriver-mac/chromedriver</li>
     *   <li>Linux: .../external/qa-drivers/geckodriver-linux/geckodriver</li>
     * </ul>
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @param version Versión del driver (solo para logging, NO se usa en URL)
     * @return URL completa de descarga
     */
    private static String buildArtifactoryUrl(String driverName, String version) {
        String baseUrl = config.get("driver.artifactory.base.url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException(
                "driver.artifactory.base.url no configurado. Verifica config-scotia.properties y .env.local");
        }

        String os = detectOSForArtifactory();  // "win", "mac", "linux"
        String executableName = getExecutableName(driverName);  // "chromedriver.exe" o "chromedriver"

        // Formato REAL de Artifactory corporativo (sin versión en ruta, sin ZIP):
        // {base-url}/external/qa-drivers/{driver}-{os}/{driver}[.exe]
        String url = String.format("%s/external/qa-drivers/%s-%s/%s",
            baseUrl.replaceAll("/$", ""),
            driverName,      // "chromedriver", "geckodriver", "edgedriver"
            os,              // "win", "mac", "linux"
            executableName); // "chromedriver.exe" (Windows) o "chromedriver" (Mac/Linux)

        TestLogger.logDebug("DRIVER_MANAGER",
            String.format("📍 URL Artifactory generada: %s", url),
            Map.of("driver", driverName, "os", os, "executable", executableName));

        return url;
    }

    /**
     * Detecta el sistema operativo y retorna el sufijo para URLs de Artifactory.
     * Formato usado en Artifactory: chromedriver-{os}
     *
     * <p>Ejemplos de rutas generadas:</p>
     * <ul>
     *   <li>Windows: external/qa-drivers/chromedriver-win/chromedriver.exe</li>
     *   <li>Mac: external/qa-drivers/chromedriver-mac/chromedriver</li>
     *   <li>Linux: external/qa-drivers/chromedriver-linux/chromedriver</li>
     * </ul>
     *
     * @return "win", "mac", o "linux" (sufijo para nomenclatura Artifactory)
     */
    private static String detectOSForArtifactory() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "win";      // chromedriver-win
        } else if (os.contains("mac")) {
            return "mac";      // chromedriver-mac (Intel y ARM usan el mismo)
        } else {
            return "linux";    // chromedriver-linux
        }
    }


    /**
     * Obtiene el nombre del ejecutable según OS.
     *
     * @param driverName Nombre del driver
     * @return Nombre del ejecutable con extensión si es Windows
     */
    private static String getExecutableName(String driverName) {
        boolean isWindows = System.getProperty("os.name")
            .toLowerCase().contains("win");

        return switch (driverName) {
            case "chromedriver" -> isWindows ? "chromedriver.exe" : "chromedriver";
            case "geckodriver" -> isWindows ? "geckodriver.exe" : "geckodriver";
            case "edgedriver" -> isWindows ? "msedgedriver.exe" : "msedgedriver";
            default -> throw new IllegalArgumentException("Driver desconocido: " + driverName);
        };
    }

    /**
     * Obtiene la URL de descarga oficial del driver.
     *
     * @param driverName Nombre del driver
     * @return URL de descarga oficial
     */
    private static String getDriverDownloadUrl(String driverName) {
        return switch (driverName) {
            case "chromedriver" -> "https://googlechromelabs.github.io/chrome-for-testing/";
            case "geckodriver" -> "https://github.com/mozilla/geckodriver/releases";
            case "edgedriver" -> "https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/";
            default -> "https://www.selenium.dev/documentation/webdriver/getting_started/install_drivers/";
        };
    }

    /**
     * Resuelve variables de entorno y paths especiales.
     *
     * <p>Soporta:</p>
     * <ul>
     *   <li>${user.home} - Home del usuario</li>
     *   <li>~ - Home del usuario (Unix)</li>
     *   <li>${env.VAR} - Variable de entorno</li>
     * </ul>
     *
     * @param path Path con posibles variables
     * @return Path resuelto
     */
    private static String resolvePathVariables(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // Resolver ${user.home}
        path = path.replace("${user.home}", System.getProperty("user.home"));

        // Resolver ~ (solo al inicio)
        if (path.startsWith("~")) {
            path = System.getProperty("user.home") + path.substring(1);
        }

        // Resolver ${env.VAR}
        int startIdx = path.indexOf("${env.");
        while (startIdx != -1) {
            int endIdx = path.indexOf("}", startIdx);
            if (endIdx != -1) {
                String varName = path.substring(startIdx + 6, endIdx);
                String varValue = System.getenv(varName);
                if (varValue != null) {
                    path = path.replace("${env." + varName + "}", varValue);
                }
            }
            startIdx = path.indexOf("${env.", endIdx);
        }

        return path;
    }

    /**
     * Excepción lanzada cuando no se encuentra el driver en ninguna fuente.
     */
    public static class DriverNotFoundException extends RuntimeException {
        public DriverNotFoundException(String message) {
            super(message);
        }

        public DriverNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

