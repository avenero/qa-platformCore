package com.scotia.qa.common.driver;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.logging.TestLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Base64;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Gestor de descarga de WebDrivers desde Artifactory.
 *
 * <p>Permite descargar drivers (chromedriver, geckodriver, edgedriver) desde un
 * repositorio Artifactory corporativo, con caché local para evitar descargas repetidas.</p>
 *
 * <h3>Configuración requerida:</h3>
 * <pre>
 * # config-scotia.properties
 * driver.strategy=artifactory
 * driver.artifactory.enabled=true
 * driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
 * driver.artifactory.user=${ARTIFACTORY_USER}
 * driver.artifactory.token=${ARTIFACTORY_TOKEN}
 * driver.chrome.version=114.0.5735.90
 * driver.cache.enabled=true
 * driver.cache.dir=${user.home}/.qa-drivers
 *
 * # .env.local
 * ARTIFACTORY_BASE_URL=https://artifactory.scotia.com/artifactory/qa-drivers
 * ARTIFACTORY_USER=tu_usuario
 * ARTIFACTORY_TOKEN=tu_token
 * CHROMEDRIVER_VERSION=114.0.5735.90
 * </pre>
 *
 * <h3>Estructura en Artifactory:</h3>
 * <pre>
 * artifactory.scotia.com/artifactory/qa-drivers/
 * ├── chromedriver/
 * │   ├── 114.0.5735.90/
 * │   │   ├── linux64/chromedriver.zip
 * │   │   ├── mac64/chromedriver.zip
 * │   │   └── win32/chromedriver.zip
 * │   └── 115.0.5790.98/...
 * ├── geckodriver/
 * │   └── 0.33.0/...
 * └── edgedriver/
 *     └── 114.0.1823.37/...
 * </pre>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>{@code
 * // Obtener driver (descarga si no existe en caché)
 * Path driverPath = ArtifactoryDriverManager.getDriver("chromedriver", "114.0.5735.90");
 * System.setProperty("webdriver.chrome.driver", driverPath.toString());
 *
 * // O usar con auto-detección de versión desde config
 * Path driver = ArtifactoryDriverManager.getDriverFromConfig("chromedriver");
 * }</pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2025-12-05
 */
public class ArtifactoryDriverManager {

    private static final ConfigManager config = ConfigManager.getInstance();

    /**
     * Obtiene el driver especificado desde Artifactory o caché local.
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @param version Versión específica del driver (ej: "114.0.5735.90")
     * @return Path al ejecutable del driver
     * @throws IOException Si falla la descarga o extracción
     */
    public static Path getDriver(String driverName, String version) throws IOException {
        // 1. Verificar si existe en caché
        Path cachedDriver = checkCache(driverName, version);
        if (cachedDriver != null) {
            TestLogger.logInfo("DRIVER_MANAGER",
                String.format("✓ Driver encontrado en caché: %s %s", driverName, version),
                Map.of("path", cachedDriver.toString()));
            return cachedDriver;
        }

        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("⬇️  Descargando driver desde Artifactory: %s %s", driverName, version),
            null);

        // 2. Construir URL de Artifactory
        String artifactoryUrl = buildArtifactoryUrl(driverName, version);

        // 3. Descargar driver
        Path downloadedZip = downloadFromArtifactory(artifactoryUrl, driverName, version);

        // 4. Extraer y guardar en caché
        Path extractedDriver = extractAndCache(downloadedZip, driverName, version);

        TestLogger.logInfo("DRIVER_MANAGER",
            String.format("✓ Driver descargado y cacheado: %s %s", driverName, version),
            Map.of("path", extractedDriver.toString()));

        return extractedDriver;
    }

    /**
     * Obtiene el driver leyendo la versión desde configuración.
     *
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @return Path al ejecutable del driver
     * @throws IOException Si falla la descarga o no se encuentra configuración
     */
    public static Path getDriverFromConfig(String driverName) throws IOException {
        String version = getVersionFromConfig(driverName);
        if (version == null || version.isEmpty()) {
            throw new IOException(String.format(
                "Versión de %s no configurada. Verifica driver.%s.version en config-scotia.properties",
                driverName, driverName.replace("driver", "")));
        }
        return getDriver(driverName, version);
    }

    /**
     * Verifica si el driver existe en caché local.
     *
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al driver si existe, null si no existe
     */
    private static Path checkCache(String driverName, String version) {
        if (!config.getBoolean("driver.cache.enabled", true)) {
            return null;
        }

        String cacheDir = config.get("driver.cache.dir", System.getProperty("user.home") + "/.qa-drivers");
        Path driverPath = Paths.get(cacheDir, driverName, version, getExecutableName(driverName));

        if (Files.exists(driverPath) && Files.isExecutable(driverPath)) {
            return driverPath;
        }

        return null;
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
        // Ejemplo: https://artifactory.scotia.com/artifactory/qa-drivers/chromedriver/114.0.5735.90/linux64/chromedriver.zip
        return String.format("%s/%s/%s/%s/%s.zip",
            baseUrl.replaceAll("/$", ""), // quitar / final si existe
            driverName,
            version,
            os,
            driverName);
    }

    /**
     * Descarga el driver desde Artifactory con autenticación.
     *
     * @param url URL de descarga
     * @param driverName Nombre del driver
     * @param version Versión del driver
     * @return Path al archivo zip descargado
     * @throws IOException Si falla la descarga
     */
    private static Path downloadFromArtifactory(String url, String driverName, String version) throws IOException {
        String user = config.get("driver.artifactory.user");
        String token = config.get("driver.artifactory.token");

        if (user == null || token == null || user.isEmpty() || token.isEmpty()) {
            throw new IOException(
                "Credenciales de Artifactory no configuradas. Verifica ARTIFACTORY_USER y ARTIFACTORY_TOKEN en .env.local");
        }

        int timeout = config.getInt("driver.artifactory.timeout", 60) * 1000; // a milisegundos
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
                    // Crear directorio temporal
                    Path tempDir = Files.createTempDirectory("driver-download-");
                    Path zipPath = tempDir.resolve(driverName + "-" + version + ".zip");

                    // Descargar
                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    TestLogger.logInfo("DRIVER_MANAGER",
                        String.format("✓ Driver descargado: %s (intento %d/%d)", driverName, attempt, maxRetries),
                        Map.of("size", Files.size(zipPath) + " bytes"));

                    return zipPath;
                } else if (status == 401) {
                    throw new IOException("Credenciales de Artifactory inválidas (HTTP 401). Verifica ARTIFACTORY_USER y ARTIFACTORY_TOKEN");
                } else if (status == 404) {
                    throw new IOException(String.format(
                        "Driver no encontrado en Artifactory (HTTP 404). Verifica que exista: %s", url));
                } else {
                    throw new IOException(String.format("Error descargando driver (HTTP %d): %s", status, url));
                }

            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries && retryEnabled) {
                    TestLogger.logWarning("DRIVER_MANAGER",
                        String.format("⚠️ Reintentando descarga (%d/%d): %s", attempt, maxRetries, e.getMessage()),
                        null);
                    try {
                        Thread.sleep(2000L * attempt); // backoff exponencial
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
        String cacheDir = config.get("driver.cache.dir", System.getProperty("user.home") + "/.qa-drivers");
        Path targetDir = Paths.get(cacheDir, driverName, version);
        Files.createDirectories(targetDir);

        Path extractedDriver = null;
        String executableName = getExecutableName(driverName);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = Paths.get(entry.getName()).getFileName().toString();

                // Buscar el ejecutable del driver (ignorar subdirectorios)
                if (!entry.isDirectory() && fileName.equalsIgnoreCase(executableName)) {
                    extractedDriver = targetDir.resolve(executableName);
                    Files.copy(zis, extractedDriver, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
            }
        }

        if (extractedDriver == null || !Files.exists(extractedDriver)) {
            throw new IOException(String.format(
                "No se encontró %s dentro del zip descargado", executableName));
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
                "⚠️ Error estableciendo permisos de ejecución: " + e.getMessage(), null);
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
            // M1/M2 chips (ARM) vs Intel
            return arch.contains("aarch64") || arch.contains("arm") ? "mac_arm64" : "mac64";
        } else {
            return "linux64";
        }
    }

    /**
     * Obtiene el nombre del ejecutable según el driver y SO.
     *
     * @param driverName Nombre del driver
     * @return Nombre del ejecutable (con .exe en Windows)
     */
    private static String getExecutableName(String driverName) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        return switch (driverName.toLowerCase()) {
            case "chromedriver" -> isWindows ? "chromedriver.exe" : "chromedriver";
            case "geckodriver" -> isWindows ? "geckodriver.exe" : "geckodriver";
            case "edgedriver", "msedgedriver" -> isWindows ? "msedgedriver.exe" : "msedgedriver";
            default -> isWindows ? driverName + ".exe" : driverName;
        };
    }

    /**
     * Obtiene la versión del driver desde configuración.
     *
     * @param driverName Nombre del driver
     * @return Versión configurada o null
     */
    private static String getVersionFromConfig(String driverName) {
        // Intenta leer de config: driver.chrome.version, driver.firefox.version, etc.
        String configKey = "driver." + driverName.replace("driver", "").toLowerCase() + ".version";
        String version = config.get(configKey);

        if (version == null || version.isEmpty()) {
            // Fallback: variable de entorno (ej: CHROMEDRIVER_VERSION)
            String envKey = driverName.toUpperCase() + "_VERSION";
            version = System.getenv(envKey);
        }

        return version;
    }

    /**
     * Limpia la caché de drivers (útil para testing o actualizaciones forzadas).
     *
     * @throws IOException Si falla la eliminación
     */
    public static void clearCache() throws IOException {
        String cacheDir = config.get("driver.cache.dir", System.getProperty("user.home") + "/.qa-drivers");
        Path cachePath = Paths.get(cacheDir);

        if (Files.exists(cachePath)) {
            try (var paths = Files.walk(cachePath)) {
                paths.sorted((a, b) -> -a.compareTo(b)) // orden inverso para eliminar archivos antes que directorios
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            TestLogger.logWarning("DRIVER_MANAGER",
                                "⚠️ No se pudo eliminar: " + path, null);
                        }
                    });
            }

            TestLogger.logInfo("DRIVER_MANAGER", "✓ Caché de drivers limpiada",
                Map.of("path", cacheDir));
        }
    }
}

