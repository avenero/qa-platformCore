package com.qa.mobilecore.driver;

import com.qa.common.logging.TestLogger;
import io.appium.java_client.AppiumDriver;

/**
 * Gestor de ciclo de vida del {@link AppiumDriver} usando ThreadLocal.
 *
 * <p>Garantiza aislamiento total entre ejecuciones paralelas: cada thread
 * (cada escenario en paralelo) tiene su propia instancia de {@code AppiumDriver},
 * sin ningún estado compartido.
 *
 * <p>Análogo exacto a {@code DriverManager} de {@code web-core}, adaptado para Appium.
 *
 * <p><b>Ciclo de vida por escenario:</b>
 * <pre>
 * MobileHooksSteps.@Before → MobileHelper.getOrCreateDriver() → MobileDriverManager.setDriver(d)
 *   [steps del escenario]  → MobileDriverManager.getDriver()
 * MobileHooksSteps.@After  → MobileDriverManager.quitDriverSafely()
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class MobileDriverManager {

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private MobileDriverManager() {}

    /**
     * Establece el driver para el thread actual.
     *
     * @param driver instancia de AppiumDriver a asignar
     * @throws IllegalArgumentException si driver es null
     */
    public static void setDriver(AppiumDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("AppiumDriver no puede ser null");
        }
        DRIVER.set(driver);
        TestLogger.logInfo("APPIUM_DRIVER_MGR",
            "AppiumDriver asignado al hilo: " + Thread.currentThread().getName(), null);
    }

    /**
     * Obtiene el driver del thread actual.
     *
     * @return AppiumDriver activo
     * @throws IllegalStateException si el driver no fue inicializado en este thread
     */
    public static AppiumDriver getDriver() {
        AppiumDriver d = DRIVER.get();
        if (d == null) {
            throw new IllegalStateException(
                "AppiumDriver no inicializado en el hilo: " + Thread.currentThread().getName() +
                ". Asegurate de tener un step 'Dado que configuro el dispositivo movil como...' " +
                "antes de interactuar con la app.");
        }
        return d;
    }

    /**
     * Retorna el driver o null si no fue inicializado. Útil para verificaciones condicionales.
     */
    public static AppiumDriver getDriverOrNull() {
        return DRIVER.get();
    }

    /**
     * @return true si el driver está inicializado en el thread actual
     */
    public static boolean isInitialized() {
        return DRIVER.get() != null;
    }

    /**
     * Cierra la sesión Appium y limpia el ThreadLocal.
     *
     * @throws RuntimeException si ocurre un error al cerrar la sesión
     */
    public static void quitDriver() {
        AppiumDriver d = DRIVER.get();
        if (d != null) {
            try {
                d.quit();
                TestLogger.logInfo("APPIUM_DRIVER_MGR",
                    "AppiumDriver cerrado en hilo: " + Thread.currentThread().getName(), null);
            } catch (Exception e) {
                TestLogger.logError("APPIUM_DRIVER_MGR",
                    "Error al cerrar AppiumDriver: " + e.getMessage(), null);
                throw new RuntimeException("Error cerrando AppiumDriver", e);
            } finally {
                DRIVER.remove();
            }
        }
    }

    /**
     * Cierra la sesión Appium sin propagar excepciones.
     * Recomendado para hooks {@code @After} de Cucumber.
     */
    public static void quitDriverSafely() {
        try {
            quitDriver();
        } catch (Exception e) {
            TestLogger.logWarning("APPIUM_DRIVER_MGR",
                "Error silenciado al cerrar AppiumDriver: " + e.getMessage(), null);
        } finally {
            DRIVER.remove();
        }
    }
}
