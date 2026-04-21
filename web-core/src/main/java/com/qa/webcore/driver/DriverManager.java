package com.qa.webcore.driver;

import com.qa.common.logging.TestLogger;
import org.openqa.selenium.WebDriver;

/**
 * Gestor singleton de WebDriver usando ThreadLocal para soporte de ejecucion paralela.
 *
 * <p>Proporciona una instancia unica de WebDriver por thread, permitiendo ejecuciones
 * paralelas de tests sin conflictos entre threads.</p>
 *
 * @author CuAleon Test Engineering
 * @since 1.0
 */
public class DriverManager {

    /**
     * ThreadLocal para almacenar instancia de WebDriver por thread.
     * Permite ejecucion paralela de tests sin conflictos.
     */
    private static final ThreadLocal<WebDriver> DRIVER_HOLDER = new ThreadLocal<>();

    /**
     * Constructor privado para prevenir instanciacion.
     * Esta clase solo tiene metodos estaticos.
     */
    private DriverManager() {
        // Utility class - no instantiation
    }

    /**
     * Establece el WebDriver para el thread actual.
     * Mantener este metodo para compatibilidad con codigo existente.
     *
     * @param webDriver Instancia de WebDriver a establecer
     * @throws IllegalArgumentException si webDriver es null
     */
    public static void setDriver(WebDriver webDriver) {
        setDriver(webDriver, true);
    }

    /**
     * Establece el WebDriver para el thread actual con opcion para evitar reemplazo accidental.
     *
     * @param webDriver Instancia de WebDriver a establecer
     * @param replace Si true, reemplaza el driver existente. Si false y ya existe, lanza
     *                IllegalStateException.
     */
    public static void setDriver(WebDriver webDriver, boolean replace) {
        if (webDriver == null) {
            TestLogger.logError("DRIVER_MANAGER",
                "Intento de establecer un WebDriver null", null);
            throw new IllegalArgumentException("WebDriver no puede ser null");
        }

        WebDriver current = DRIVER_HOLDER.get();
        if (current != null && !replace) {
            String error = "WebDriver ya inicializado para thread: " + Thread.currentThread().getName();
            TestLogger.logError("DRIVER_MANAGER", error, null);
            throw new IllegalStateException(error);
        }

        DRIVER_HOLDER.set(webDriver);
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
        WebDriver currentDriver = DRIVER_HOLDER.get();

        if (currentDriver == null) {
            String errorMsg = "WebDriver no inicializado para thread: "
                + Thread.currentThread().getName()
                + ". Llama a DriverManager.setDriver() primero.";
            TestLogger.logError("DRIVER_MANAGER", errorMsg, null);
            throw new IllegalStateException(errorMsg);
        }

        return currentDriver;
    }

    /**
     * Obtiene el WebDriver del thread actual o null si no esta inicializado.
     *
     * @return WebDriver o null
     */
    public static WebDriver getDriverOrNull() {
        return DRIVER_HOLDER.get();
    }

    /**
     * Cierra y limpia el WebDriver del thread actual.
     *
     * <p>Ejecuta driver.quit() para cerrar todas las ventanas y finalizar
     * la sesion del navegador, luego limpia la referencia del ThreadLocal.</p>
     */
    public static void quitDriver() {
        WebDriver currentDriver = DRIVER_HOLDER.get();

        if (currentDriver != null) {
            try {
                currentDriver.quit();
                TestLogger.logInfo("DRIVER_MANAGER",
                    "WebDriver cerrado exitosamente para thread: "
                    + Thread.currentThread().getName(), null);
            } catch (Exception e) {
                TestLogger.logError("DRIVER_MANAGER",
                    "Error al cerrar WebDriver: " + e.getMessage(), null);
            } finally {
                DRIVER_HOLDER.remove(); // Limpiar ThreadLocal
            }
        } else {
            TestLogger.logWarning("DRIVER_MANAGER",
                "Intento de cerrar WebDriver que no esta inicializado", null);
        }
    }

    /**
     * Verifica si el driver esta inicializado para el thread actual.
     *
     * @return true si el driver esta inicializado, false en caso contrario
     */
    public static boolean isDriverInitialized() {
        return DRIVER_HOLDER.get() != null;
    }

    /**
     * Cierra el driver actual sin lanzar excepciones.
     * Util para hooks de limpieza en Cucumber.
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
