package com.scotia.qa.common.driver;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.logging.TestLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Gestor inteligente de WebDrivers con estrategia de triple fallback.
 *
 * <p>Implementa una estrategia completa y autónoma de triple fallback para obtener WebDrivers:</p>
 * <ol>
 *   <li><strong>Local Path Fijo</strong> - Ruta configurada manualmente por el desarrollador</li>
 *   <li><strong>Caché Local</strong> - Drivers descargados previamente (automático)</li>
 *   <li><strong>Artifactory</strong> - Descarga desde repositorio corporativo con autenticación (último recurso)</li>
 * </ol>
 *
 * <p>Esta clase consolida toda la lógica de gestión de drivers, incluyendo descarga,
 * extracción, caché, detección de OS, y reintentos automáticos.</p>
 *
 * <h3>Configuración:</h3>
 * <pre>
 * # config-scotia.properties
 * driver.strategy=fallback
 * driver.local.enabled=true
 * driver.local.base.path=${DRIVER_LOCAL_PATH}
 * driver.local.strict=false
 * driver.cache.enabled=true
 * driver.cache.dir=${user.home}/.qa-drivers
 * driver.cache.ttl=30
 * driver.artifactory.enabled=true
 * driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
 * driver.logging.show.strategy=true
 *
 * # .env.local
 * DRIVER_LOCAL_PATH=/Users/tu_usuario/drivers
 * ARTIFACTORY_BASE_URL=https://artifactory.corp.com/qa-drivers
 * </pre>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>{@code
 * // Obtener driver con fallback automático
 * Path driver = WebDriverManager.getDriver("chromedriver", "114.0.5735.90");
 * System.setProperty("webdriver.chrome.driver", driver.toString());
 *
 * // Usar con auto-detección de versión
 * Path driver = WebDriverManager.getDriverFromConfig("chromedriver");
 *
 * // Limpiar caché
 * WebDriverManager.clearCache();
 * }</pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2025-12-08
 */
public class WebDriverManager {

    private static final ConfigManager config = ConfigManager.getInstance();

    /**
     * Obtiene el driver usando estrategia de fallback.
     *
     * <p>Orden de búsqueda:</p>
     * <ol>
     *   <li>Local Path Fijo (si driver.local.enabled=true)</li>
     *   <li>Caché Local (si driver.cache.enabled=true)</li>
     *   <li>Artifactory (si driver.artifactory.enabled=true)</li>
     * </ol>
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @param version Versión específica (ej: "114.0.5735.90")
     * @return Path al ejecutable del driver
     * @throws DriverNotFoundException Si no se encuentra en ninguna fuente
     */
    public static Path getDriver(String driverName, String version) {
        boolean showStrategy = config.getBoolean("driver.logging.show.strategy", true);

        // ESTRATEGIA 1: Local Path Fijo
        if (config.getBoolean("driver.local.enabled", true)) {
            try {
                Path localPath = getDriverFromLocalPath(driverName, version);
                if (localPath != null) {
                    if (showStrategy) {
                        TestLogger.logInfo("DRIVER_MANAGER",
                            String.format("✓ Usando driver desde LOCAL PATH: %s %s",
                                driverName, version),
                            Map.of("strategy", "local-path", "path", localPath.toString()));
                    }
                    return localPath;
                }

                // Modo strict: falla si no existe en local
                if (config.getBoolean("driver.local.strict", false)) {
                    throw new DriverNotFoundException(
                        String.format("driver.local.strict=true pero %s %s no existe en %s",
                            driverName, version, config.get("driver.local.base.path")));
                }
            } catch (IOException e) {
                TestLogger.logWarning("DRIVER_MANAGER",
                    "No se pudo acceder a LOCAL PATH, intentando caché...",
                    Map.of("error", e.getMessage()));
            }
        }

        // ESTRATEGIA 2: Caché Local
        if (config.getBoolean("driver.cache.enabled", true)) {
            Path cachedPath = getDriverFromCache(driverName, version);
            if (cachedPath != null) {
                if (showStrategy) {
                    TestLogger.logInfo("DRIVER_MANAGER",
                        String.format("✓ Usando driver desde CACHÉ: %s %s",
                            driverName, version),
                        Map.of("strategy", "cache", "path", cachedPath.toString()));
                }
                return cachedPath;
            }
        }

        // ESTRATEGIA 3: Artifactory
        if (config.getBoolean("driver.artifactory.enabled", true)) {
            try {
                Path downloadedPath = downloadFromArtifactory(driverName, version);
                if (showStrategy) {
                    TestLogger.logInfo("DRIVER_MANAGER",
                        String.format("✓ Driver descargado desde ARTIFACTORY: %s %s",
                            driverName, version),
                        Map.of("strategy", "artifactory", "path", downloadedPath.toString()));
                }
                return downloadedPath;
            } catch (IOException e) {
                TestLogger.logError("DRIVER_MANAGER",
                    "Falló descarga desde Artifactory",
                    Map.of("error", e.getMessage()));
            }
        }

        // Todas las estrategias fallaron
        throw new DriverNotFoundException(
            String.format("No se pudo obtener %s %s desde ninguna fuente. " +
                "Verifica: (1) driver.local.base.path, (2) caché en ~/.qa-drivers, " +
                "(3) credenciales de Artifactory", driverName, version));
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
     * <p>Estructura esperada: {base.path}/{driver-name}/{version}/{executable}</p>
     * <p>Ejemplo: ~/drivers/chromedriver/114.0.5735.90/chromedriver</p>
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver si existe, null si no existe
     * @throws IOException Si hay error accediendo al filesystem
     */
    private static Path getDriverFromLocalPath(String driverName, String version) throws IOException {
        String basePath = config.get("driver.local.base.path");
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }

        // Resolver variables de entorno y home
        basePath = resolvePathVariables(basePath);

        // Construir path esperado
        String executableName = getExecutableName(driverName);
        Path driverPath = Paths.get(basePath, driverName, version, executableName);

        if (Files.exists(driverPath) && Files.isExecutable(driverPath)) {
            return driverPath;
        }

        return null;
    }

    /**
     * Busca driver en caché local.
     *
     * <p>Verifica si el driver existe en el directorio de caché y no ha expirado.</p>
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver si existe y es válido, null en caso contrario
     */
    private static Path getDriverFromCache(String driverName, String version) {
        String cacheDir = config.get("driver.cache.dir",
            System.getProperty("user.home") + "/.qa-drivers");

        // Resolver variables
        cacheDir = resolvePathVariables(cacheDir);

        Path driverPath = Paths.get(cacheDir, driverName, version,
            getExecutableName(driverName));

        if (Files.exists(driverPath) && Files.isExecutable(driverPath)) {
            // Verificar expiración
            if (isCacheExpired(driverPath)) {
                TestLogger.logWarning("DRIVER_MANAGER",
                    "Driver en caché expirado, se descargará nuevo",
                    Map.of("path", driverPath.toString()));
                return null;
            }
            return driverPath;
        }

        return null;
    }

    /**
     * Descarga driver desde Artifactory con autenticación.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver descargado y cacheado
     * @throws IOException Si falla la descarga
     */
    private static Path downloadFromArtifactory(String driverName, String version) throws IOException {
        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("⬇️  Descargando driver desde Artifactory: %s %s", driverName, version),
            null);

        // 1. Construir URL de Artifactory
        String artifactoryUrl = buildArtifactoryUrl(driverName, version);

        // 2. Descargar driver
        Path downloadedZip = downloadDriverZip(artifactoryUrl, driverName, version);

        // 3. Extraer y guardar en caché
        Path extractedDriver = extractAndCache(downloadedZip, driverName, version);

        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("✓ Driver descargado y cacheado: %s %s", driverName, version),
            Map.of("path", extractedDriver.toString()));

        return extractedDriver;
    }

    /**
     * Construye la URL completa de Artifactory para descargar el driver.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return URL completa de descarga
     */
    private static String buildArtifactoryUrl(String driverName, String version) {
        String baseUrl = config.get("driver.artifactory.base.url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException(
                "driver.artifactory.base.url no configurado. Verifica config-scotia.properties y .env.local");
        }

        String os = detectOS();

        // Formato: {base-url}/{driver}/{version}/{os}/{driver}.zip
        return String.format("%s/%s/%s/%s/%s.zip",
            baseUrl.replaceAll("/$", ""),
            driverName,
            version,
            os,
            driverName);
    }

    /**
     * Descarga el archivo zip del driver desde Artifactory.
     *
     * @param url URL de descarga
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al archivo zip descargado
     * @throws IOException Si falla la descarga
     */
    private static Path downloadDriverZip(String url, String driverName, String version) throws IOException {
        String user = config.get("driver.artifactory.user");
        String token = config.get("driver.artifactory.token");

        if (user == null || token == null || user.isEmpty() || token.isEmpty()) {
            throw new IOException(
                "Credenciales de Artifactory no configuradas. Verifica ARTIFACTORY_USER y ARTIFACTORY_TOKEN en .env.local");
        }

        int timeout = config.getInt("driver.artifactory.timeout", 60) * 1000;
        int maxRetries = config.getInt("driver.artifactory.retry.max", 3);
        boolean retryEnabled = config.getBoolean("driver.artifactory.retry.enabled", true);

        IOException lastException = null;
        int attempt = 0;

        while (attempt < (retryEnabled ? maxRetries : 1)) {
            attempt++;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                conn.setRequestProperty("User-Agent", "Scotia-QA-Framework/1.0.0");

                // Autenticación Basic
                String auth = user + ":" + token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

                int status = conn.getResponseCode();

                if (status == 200) {
                    Path tempDir = Files.createTempDirectory("driver-download-");
                    Path zipPath = tempDir.resolve(driverName + "-" + version + ".zip");

                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    TestLogger.logInfo("DRIVER_MANAGER",
                        String.format("✓ Driver descargado (intento %d/%d)", attempt, maxRetries),
                        Map.of("size", Files.size(zipPath) + " bytes"));

                    return zipPath;
                } else if (status == 401) {
                    throw new IOException("Credenciales de Artifactory inválidas (HTTP 401)");
                } else if (status == 404) {
                    throw new IOException(String.format("Driver no encontrado en Artifactory (HTTP 404): %s", url));
                } else {
                    throw new IOException(String.format("Error descargando driver (HTTP %d)", status));
                }

            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries && retryEnabled) {
                    TestLogger.logWarning("DRIVER_MANAGER",
                        String.format("⚠️ Reintentando descarga (%d/%d): %s", attempt, maxRetries, e.getMessage()),
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

        throw new IOException("Descarga fallida después de " + attempt + " intentos", lastException);
    }

    /**
     * Extrae el driver del zip y lo guarda en caché.
     *
     * @param zipPath Path al archivo zip descargado
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al ejecutable extraído
     * @throws IOException Si falla la extracción
     */
    private static Path extractAndCache(Path zipPath, String driverName, String version) throws IOException {
        String cacheDir = config.get("driver.cache.dir",
            System.getProperty("user.home") + "/.qa-drivers");

        cacheDir = resolvePathVariables(cacheDir);
        Path targetDir = Paths.get(cacheDir, driverName, version);
        Files.createDirectories(targetDir);

        Path extractedDriver = null;
        String executableName = getExecutableName(driverName);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = Paths.get(entry.getName()).getFileName().toString();

                if (!entry.isDirectory() && fileName.equalsIgnoreCase(executableName)) {
                    extractedDriver = targetDir.resolve(executableName);
                    Files.copy(zis, extractedDriver, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
            }
        }

        if (extractedDriver == null || !Files.exists(extractedDriver)) {
            throw new IOException(String.format("No se encontró %s dentro del zip", executableName));
        }

        // Hacer ejecutable (Unix/Linux/Mac)
        try {
            boolean success = extractedDriver.toFile().setExecutable(true, false);
            if (!success) {
                TestLogger.logWarning("DRIVER_MANAGER",
                    "⚠️ No se pudieron establecer permisos de ejecución", null);
            }
        } catch (Exception e) {
            TestLogger.logWarning("DRIVER_MANAGER",
                "⚠️ Error estableciendo permisos: " + e.getMessage(), null);
        }

        // Limpiar zip temporal
        try {
            Files.deleteIfExists(zipPath);
            Files.deleteIfExists(zipPath.getParent());
        } catch (Exception ignored) {}

        return extractedDriver;
    }

    /**
     * Detecta el sistema operativo y retorna el identificador para Artifactory.
     *
     * @return "linux64", "mac64", "mac_arm64", o "win32"
     */
    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win")) {
            return "win32";
        } else if (os.contains("mac")) {
            return arch.contains("aarch64") || arch.contains("arm") ? "mac_arm64" : "mac64";
        } else {
            return "linux64";
        }
    }

    /**
     * Verifica si el caché expiró según TTL configurado.
     *
     * @param driverPath Path al driver en caché
     * @return true si expiró, false si sigue válido
     */
    private static boolean isCacheExpired(Path driverPath) {
        int ttlDays = config.getInt("driver.cache.ttl", 30);
        if (ttlDays == 0) {
            return false; // Nunca expira
        }

        try {
            long lastModified = Files.getLastModifiedTime(driverPath).toMillis();
            long now = System.currentTimeMillis();
            long daysSinceDownload = (now - lastModified) / (1000L * 60L * 60L * 24L);
            return daysSinceDownload > ttlDays;
        } catch (IOException e) {
            TestLogger.logWarning("DRIVER_MANAGER",
                "No se pudo verificar fecha de caché, asumiendo válido",
                Map.of("path", driverPath.toString()));
            return false; // En caso de error, no expirar
        }
    }

    /**
     * Limpia el caché de drivers.
     *
     * <p>Elimina todos los drivers descargados en el directorio de caché.</p>
     *
     * @throws IOException Si falla la eliminación
     */
    public static void clearCache() throws IOException {
        String cacheDir = config.get("driver.cache.dir",
            System.getProperty("user.home") + "/.qa-drivers");

        cacheDir = resolvePathVariables(cacheDir);
        Path cachePath = Paths.get(cacheDir);

        if (Files.exists(cachePath)) {
            try (var paths = Files.walk(cachePath)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            TestLogger.logWarning("DRIVER_MANAGER",
                                "No se pudo eliminar: " + path,
                                Map.of("error", e.getMessage()));
                        }
                    });
            }

            TestLogger.logInfo("DRIVER_MANAGER",
                "Caché de drivers limpiado",
                Map.of("path", cachePath.toString()));
        }
    }

    /**
     * Limpia el caché de un driver específico.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver (opcional, null para todas las versiones)
     * @throws IOException Si falla la eliminación
     */
    public static void clearCacheFor(String driverName, String version) throws IOException {
        String cacheDir = config.get("driver.cache.dir",
            System.getProperty("user.home") + "/.qa-drivers");

        cacheDir = resolvePathVariables(cacheDir);

        Path targetPath = version != null
            ? Paths.get(cacheDir, driverName, version)
            : Paths.get(cacheDir, driverName);

        if (Files.exists(targetPath)) {
            try (var paths = Files.walk(targetPath)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            TestLogger.logWarning("DRIVER_MANAGER",
                                "No se pudo eliminar: " + path,
                                Map.of("error", e.getMessage()));
                        }
                    });
            }

            TestLogger.logInfo("DRIVER_MANAGER",
                String.format("Caché limpiado para %s %s",
                    driverName, version != null ? version : "(todas las versiones)"),
                Map.of("path", targetPath.toString()));
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

