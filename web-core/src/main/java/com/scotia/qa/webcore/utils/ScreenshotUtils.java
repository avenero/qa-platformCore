package com.scotia.qa.webcore.utils;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilidades para captura de screenshots en tests web.
 *
 * <p>Proporciona métodos para capturar screenshots de diferentes tipos:
 * viewport actual, página completa (full page), o elementos específicos.</p>
 *
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>Screenshot de viewport (visible area)</li>
 *   <li>Screenshot de página completa (con scroll automático)</li>
 *   <li>Screenshot de elementos específicos</li>
 *   <li>Nombres de archivo con timestamp automático</li>
 *   <li>Integración con TestLogger para evidencias</li>
 * </ul>
 *
 * <p><b>Ubicación por defecto:</b> {@code target/screenshots/}</p>
 *
 * @author Abel Venero
 * @version 1.1.0
 */
public class ScreenshotUtils {

    private static final String DEFAULT_SCREENSHOTS_DIR = "target/screenshots/";
    private static final String TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss_SSS";

    /**
     * Constructor privado para prevenir instanciación.
     */
    private ScreenshotUtils() {
        // Utility class
    }

    /**
     * Crea el directorio de screenshots si no existe.
     *
     * @param directory Ruta del directorio
     * @return Path del directorio creado
     */
    private static Path createScreenshotDirectory(String directory) {
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                TestLogger.logInfo("SCREENSHOT_UTILS",
                    "Directorio de screenshots creado: " + directory, null);
            }
            return path;
        } catch (IOException e) {
            TestLogger.logError("SCREENSHOT_UTILS",
                "Error creando directorio de screenshots: " + e.getMessage(), null);
            throw new RuntimeException("No se pudo crear directorio de screenshots", e);
        }
    }

    /**
     * Genera nombre de archivo con timestamp.
     *
     * @param baseName Nombre base del archivo
     * @return Nombre de archivo con timestamp
     */
    private static String generateFilename(String baseName) {
        String timestamp = new SimpleDateFormat(TIMESTAMP_PATTERN).format(new Date());
        String sanitizedName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return sanitizedName + "_" + timestamp + ".png";
    }

    /**
     * Captura screenshot del viewport actual (área visible).
     *
     * <p>Usa el método nativo de Selenium. Rápido pero solo captura lo visible.</p>
     *
     * @param screenshotName Nombre descriptivo del screenshot
     * @return Path del archivo creado
     */
    public static String takeScreenshot(String screenshotName) {
        try {
            WebDriver driver = DriverManager.getDriver();
            TakesScreenshot screenshot = (TakesScreenshot) driver;

            File srcFile = screenshot.getScreenshotAs(OutputType.FILE);
            String filename = generateFilename(screenshotName);
            Path directory = createScreenshotDirectory(DEFAULT_SCREENSHOTS_DIR);
            Path destPath = directory.resolve(filename);

            Files.copy(srcFile.toPath(), destPath);

            TestLogger.logInfo("SCREENSHOT_UTILS",
                "✅ Screenshot capturado: " + destPath, null);

            return destPath.toString();
        } catch (Exception e) {
            TestLogger.logError("SCREENSHOT_UTILS",
                "Error capturando screenshot: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Captura screenshot de página completa con scroll automático.
     *
     * <p>Usa AShot para hacer scroll y capturar toda la página.
     * Más lento que {@link #takeScreenshot(String)} pero captura todo el contenido.</p>
     *
     * @param screenshotName Nombre descriptivo del screenshot
     * @return Path del archivo creado
     */
    public static String takeFullPageScreenshot(String screenshotName) {
        try {
            WebDriver driver = DriverManager.getDriver();

            // AShot con estrategia de scroll para capturar página completa
            Screenshot screenshot = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(100))
                .takeScreenshot(driver);

            String filename = generateFilename(screenshotName + "_fullpage");
            Path directory = createScreenshotDirectory(DEFAULT_SCREENSHOTS_DIR);
            Path destPath = directory.resolve(filename);

            ImageIO.write(screenshot.getImage(), "PNG", destPath.toFile());

            TestLogger.logInfo("SCREENSHOT_UTILS",
                "✅ Screenshot de página completa capturado: " + destPath, null);

            return destPath.toString();
        } catch (Exception e) {
            TestLogger.logError("SCREENSHOT_UTILS",
                "Error capturando screenshot de página completa: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Captura screenshot de un elemento específico.
     *
     * @param element WebElement a capturar
     * @param screenshotName Nombre descriptivo del screenshot
     * @return Path del archivo creado
     */
    public static String takeElementScreenshot(WebElement element, String screenshotName) {
        try {
            WebDriver driver = DriverManager.getDriver();

            // AShot para capturar solo el elemento
            Screenshot screenshot = new AShot()
                .takeScreenshot(driver, element);

            String filename = generateFilename(screenshotName + "_element");
            Path directory = createScreenshotDirectory(DEFAULT_SCREENSHOTS_DIR);
            Path destPath = directory.resolve(filename);

            ImageIO.write(screenshot.getImage(), "PNG", destPath.toFile());

            TestLogger.logInfo("SCREENSHOT_UTILS",
                "✅ Screenshot de elemento capturado: " + destPath, null);

            return destPath.toString();
        } catch (Exception e) {
            TestLogger.logError("SCREENSHOT_UTILS",
                "Error capturando screenshot de elemento: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Captura screenshot con directorio personalizado.
     *
     * @param screenshotName Nombre descriptivo del screenshot
     * @param customDirectory Directorio personalizado
     * @return Path del archivo creado
     */
    public static String takeScreenshotToDirectory(String screenshotName, String customDirectory) {
        try {
            WebDriver driver = DriverManager.getDriver();
            TakesScreenshot screenshot = (TakesScreenshot) driver;

            File srcFile = screenshot.getScreenshotAs(OutputType.FILE);
            String filename = generateFilename(screenshotName);
            Path directory = createScreenshotDirectory(customDirectory);
            Path destPath = directory.resolve(filename);

            Files.copy(srcFile.toPath(), destPath);

            TestLogger.logInfo("SCREENSHOT_UTILS",
                "✅ Screenshot capturado en directorio custom: " + destPath, null);

            return destPath.toString();
        } catch (Exception e) {
            TestLogger.logError("SCREENSHOT_UTILS",
                "Error capturando screenshot en directorio custom: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Captura screenshot silenciosamente (sin lanzar excepciones).
     * Útil para hooks de Cucumber donde no queremos que un error de screenshot rompa el test.
     *
     * @param screenshotName Nombre descriptivo del screenshot
     * @return Path del archivo creado, o null si falló
     */
    public static String takeScreenshotSafely(String screenshotName) {
        try {
            return takeScreenshot(screenshotName);
        } catch (Exception e) {
            TestLogger.logWarning("SCREENSHOT_UTILS",
                "Screenshot falló silenciosamente: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Obtiene screenshot como byte array (útil para reportes).
     *
     * @return byte[] del screenshot, o null si falla
     */
    public static byte[] getScreenshotAsBytes() {
        try {
            WebDriver driver = DriverManager.getDriver();
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            return screenshot.getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            TestLogger.logError("SCREENSHOT_UTILS",
                "Error obteniendo screenshot como bytes: " + e.getMessage(), null);
            return new byte[0];
        }
    }
}

