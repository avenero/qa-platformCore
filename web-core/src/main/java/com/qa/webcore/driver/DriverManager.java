package com.qa.webcore.driver;

import com.qa.common.logging.TestLogger;
import org.openqa.selenium.WebDriver;

/**
 * Gestor singleton de WebDriver usando ThreadLocal para soporte de ejecución paralela.
 *
 * <p>Proporciona una instancia única de WebDriver por thread, permitiendo ejecuciones
 * paralelas de tests sin conflictos entre threads.</p>
 */
public class DriverManager {

    /**
     * ThreadLocal para almacenar instancia de WebDriver por thread.
     * Permite ejecución paralela de tests sin conflictos.
     */
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    /**
     * Constructor privado para prevenir instanciación.
     * Esta clase solo tiene métodos estáticos.
     */
    private DriverManager() {
        // Utility class - no instantiation
    }

    /**
     * Establece el WebDriver para el thread actual.
     * Mantener este método para compatibilidad con código existente.
     *
     * @param webDriver Instancia de WebDriver a establecer
     * @throws IllegalArgumentException si webDriver es null
     */
    public static void setDriver(WebDriver webDriver) {
        setDriver(webDriver, true);
    }

    /**
     * Establece el WebDriver para el thread actual con opción para evitar reemplazo accidental.
     *
     * @param webDriver Instancia de WebDriver a establecer
     * @param replace Si true, reemplaza el driver existente. Si false y ya existe, lanza IllegalStateException.
     */
    public static void setDriver(WebDriver webDriver, boolean replace) {
        if (webDriver == null) {
            TestLogger.logError("DRIVER_MANAGER",
                "Intento de establecer un WebDriver null", null);
            throw new IllegalArgumentException("WebDriver no puede ser null");
        }

        WebDriver current = driver.get();
        if (current != null && !replace) {
            String error = "WebDriver ya inicializado para thread: " + Thread.currentThread().getName();
            TestLogger.logError("DRIVER_MANAGER", error, null);
            throw new IllegalStateException(error);
        }

        driver.set(webDriver);
        TestLogger.logInfo("DRIVER_MANAGER",
            "WebDriver establecido para thread: " + Thread.currentThread().getName(), null);
    }

    /**
     * Obtiene el WebDriver del thread actual.
     *
     * @return WebDriver del thread actual
     * @throws IllegalStateException si no se ha inicializado el driver
     */
    public static WebDriver getDriver() {
        WebDriver currentDriver = driver.get();

        if (currentDriver == null) {
            String errorMsg = "WebDriver no inicializado para thread: " +
                Thread.currentThread().getName() +
                ". Llama a DriverManager.setDriver() primero.";
            TestLogger.logError("DRIVER_MANAGER", errorMsg, null);
            throw new IllegalStateException(errorMsg);
        }

        return currentDriver;
    }

    /**
     * Obtiene el WebDriver del thread actual o null si no está inicializado.
     *
     * @return WebDriver o null
     */
    public static WebDriver getDriverOrNull() {
        return driver.get();
    }

    /**
     * Cierra y limpia el WebDriver del thread actual.
     *
     * <p>Ejecuta driver.quit() para cerrar todas las ventanas y finalizar
     * la sesión del navegador, luego limpia la referencia del ThreadLocal.</p>
     */
    public static void quitDriver() {
        WebDriver currentDriver = driver.get();

        if (currentDriver != null) {
            try {
                currentDriver.quit();
                TestLogger.logInfo("DRIVER_MANAGER",
                    "WebDriver cerrado exitosamente para thread: " +
                    Thread.currentThread().getName(), null);
            } catch (Exception e) {
                TestLogger.logError("DRIVER_MANAGER",
                    "Error al cerrar WebDriver: " + e.getMessage(), null);
            } finally {
                driver.remove(); // Limpiar ThreadLocal
            }
        } else {
            TestLogger.logWarning("DRIVER_MANAGER",
                "Intento de cerrar WebDriver que no está inicializado", null);
        }
    }

    /**
     * Verifica si el driver está inicializado para el thread actual.
     *
     * @return true si el driver está inicializado, false en caso contrario
     */
    public static boolean isDriverInitialized() {
        return driver.get() != null;
    }

    /**
     * Cierra el driver actual sin lanzar excepciones.
     * Útil para hooks de limpieza en Cucumber.
     */
    public static void quitDriverSafely() {
        try {
            quitDriver();
        } catch (Exception e) {
            TestLogger.logWarning("DRIVER_MANAGER",
                "Error silenciado al cerrar driver: " + e.getMessage(), null);
        }
    }
}
