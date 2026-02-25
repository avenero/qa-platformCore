package com.scotia.qa.common.driver;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.ssl.SSLUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
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
 * ARTIFACTORY_BASE_URL=<a href="https://artifactory.corp.com/qa-drivers">...</a>
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

        return switch (strategy) {
            case "local" -> getDriverFromLocalStrategy(driverName, version);
            case "artifactory" -> getDriverFromArtifactoryStrategy(driverName, version);
            default -> throw new DriverNotFoundException(
                    String.format("Estrategia '%s' no válida. Valores permitidos: 'local', 'artifactory'",
                            strategy));
        };
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
            String.format("Driver %s no encontrado en LOCAL PATH: %s/%s/%s",
                driverName, basePath, driverName, getExecutableName(driverName)));
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
                String.format("Error descargando %s desde Artifactory: %s (URL: %s)",
                    driverName, e.getMessage(), baseUrl));
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
        // 1. Construir URL de Artifactory
        String artifactoryUrl = buildArtifactoryUrl(driverName, version);

        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("📥 Descargando %s desde Artifactory...", driverName),
            Map.of("url", artifactoryUrl));

        // 2. Descargar driver ejecutable directo
        return downloadDriverExecutable(artifactoryUrl, driverName);
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
        // VALIDACIÓN DE CACHE: Verificar si ya existe antes de descargar
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".cache", "qa-drivers");
        String executableName = getExecutableName(driverName);
        Path cachedDriverPath = cacheDir.resolve(executableName);

        if (Files.exists(cachedDriverPath)) {
            long cachedSize = Files.size(cachedDriverPath);
            boolean isExecutable = Files.isExecutable(cachedDriverPath);

            // Validar que sea un driver válido (>100KB y ejecutable)
            if (cachedSize > 100_000 && isExecutable) {
                TestLogger.logInfo("DRIVER_MANAGER",
                    String.format("✅ Driver encontrado en cache: %s (%.1f MB) - Reutilizando",
                        driverName, cachedSize / (1024.0 * 1024.0)),
                    Map.of("path", cachedDriverPath.toString(), "size", cachedSize + " bytes"));
                return cachedDriverPath;
            } else {
                TestLogger.logWarning("DRIVER_MANAGER",
                    String.format("⚠️ Driver en cache inválido (size=%d, executable=%s) - Descargando nuevo",
                        cachedSize, isExecutable),
                    null);
            }
        }

        // No existe en cache o está corrupto → Descargar
        String user = config.get("driver.artifactory.user");
        String token = config.get("driver.artifactory.token");

        boolean useAuth = (user != null && !user.isEmpty() && token != null && !token.isEmpty());

        int timeout = config.getInt("driver.artifactory.timeout", 60) * 1000;
        int maxRetries = config.getInt("driver.artifactory.retry.max", 3);
        boolean retryEnabled = config.getBoolean("driver.artifactory.retry.enabled", true);

        IOException lastException = null;
        int attempt = 0;

        while (attempt < (retryEnabled ? maxRetries : 1)) {
            attempt++;
            try {
                HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(url).toURL().openConnection();

                // Configurar SSL usando el truststore del framework (si es HTTPS)
                if (conn instanceof HttpsURLConnection) {
                    SSLContext sslContext = SSLUtils.loadFrameworkSSLContext();
                    if (sslContext != null) {
                        ((HttpsURLConnection) conn).setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                }

                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                conn.setRequestProperty("User-Agent", "Scotia-QA-Framework/2.0.0");

                // Autenticación Basic (solo si hay credenciales)
                if (useAuth) {
                    String auth = user + ":" + token;
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                    conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
                }

                int status = conn.getResponseCode();

                if (status == 200) {
                    // Validar Content-Type (debe ser binario, NO text/html)
                    String contentType = conn.getContentType();
                    TestLogger.logDebug("DRIVER_MANAGER",
                        String.format("Response: HTTP %d, Content-Type: %s", status, contentType),
                        null);

                    if (contentType != null && contentType.contains("text/html")) {
                        throw new IOException(String.format(
                            "Artifactory retornó HTML en lugar de binario. " +
                            "URL probablemente incorrecta o acceso denegado.\n" +
                            "URL: %s\nContent-Type: %s",
                            url, contentType));
                    }

                    // Descargar ejecutable (sobrescribir el que ya existe en cache)
                    Files.createDirectories(cacheDir);

                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, cachedDriverPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // VALIDACIÓN CRÍTICA: Verificar tamaño del archivo
                    long fileSize = Files.size(cachedDriverPath);
                    if (fileSize < 100_000) {  // 100KB mínimo - drivers reales pesan >20MB
                        String errorContent = Files.readString(cachedDriverPath).substring(0, Math.min(500, (int)fileSize));
                        throw new IOException(String.format(
                            "Archivo descargado muy pequeño (%d bytes). Probablemente es HTML de error.\n" +
                            "URL: %s\n" +
                            "Contenido: %s...",
                            fileSize, url, errorContent));
                    }

                    // Hacer ejecutable (Unix/Mac/Linux)
                    boolean executable = cachedDriverPath.toFile().setExecutable(true, false);
                    if (!executable) {
                        TestLogger.logWarning("DRIVER_MANAGER",
                            "⚠️ No se pudieron establecer permisos de ejecución", null);
                    }

                    TestLogger.logDebug("DRIVER_MANAGER",
                        String.format("Driver descargado exitosamente (%d MB)",
                            fileSize / (1024 * 1024)),
                        null);

                    return cachedDriverPath;

                } else if (status == 401) {
                    throw new IOException(
                        "Credenciales de Artifactory inválidas (HTTP 401). " +
                        "Verifica ARTIFACTORY_USER y ARTIFACTORY_TOKEN");
                } else if (status == 404) {
                    throw new IOException(String.format(
                            """
                                    Driver no encontrado en Artifactory (HTTP 404).
                                    URL: %s
                                    Verifica que el driver existe en esa ruta en Artifactory.""", url));
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

