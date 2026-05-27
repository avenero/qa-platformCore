package com.qa.common.api.driver;

import com.qa.common.api.runtime.ExecutionConfig;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Contrato unificado para drivers de automatización de UI.
 *
 * <p>Abstrae las diferencias entre {@code web-core} (Playwright) y
 * {@code mobile-core} (Appium) exponiendo un conjunto de operaciones comunes
 * que cualquier capa de steps puede invocar sin acoplarse a la plataforma
 * subyacente.
 *
 * <h2>Jerarquía de implementación</h2>
 * <pre>
 *         ┌──────────────────────┐
 *         │  UiDriver (common)   │
 *         └──────────┬───────────┘
 *                    │ implementan
 *       ┌────────────┴────────────┐
 *       ▼                         ▼
 * BrowserEngine             AppiumEngine
 * (web-core)                (mobile-core)
 * </pre>
 *
 * <h2>Ciclo de vida</h2>
 * <ol>
 *   <li>El {@code CorePlugin} crea e invoca {@link #open(ExecutionConfig)} en
 *       {@code onSuiteStart} o {@code onScenarioStart}.</li>
 *   <li>Los steps del escenario usan {@code click()}, {@code type()}, etc.</li>
 *   <li>El {@code CorePlugin} invoca {@link #close()} en {@code onScenarioEnd}
 *       o {@code onSuiteEnd} — garantizado por el try-finally del engine.</li>
 * </ol>
 *
 * <h2>Operaciones exclusivas de mobile</h2>
 * <p>{@link #performGesture(Gesture)} tiene implementación por defecto que lanza
 * {@link UnsupportedOperationException}. Las implementaciones web no necesitan
 * sobrescribirlo; las mobile deben hacerlo obligatoriamente.
 *
 * @author Abel Venero
 * @since 3.0.0
 * @see ElementLocator
 * @see Gesture
 * @see DriverType
 */
public interface UiDriver extends AutoCloseable {

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    /**
     * Inicializa el driver con la configuración de ejecución.
     *
     * <p>Las implementaciones deben leer de {@code config} las propiedades
     * necesarias (browser target, device UDID, remote URL, etc.) y dejar el
     * driver listo para recibir comandos.
     *
     * @param config configuración inmutable de la ejecución, nunca null
     * @throws IllegalStateException si el driver ya está inicializado
     */
    void open(ExecutionConfig config);

    /**
     * Cierra y libera todos los recursos del driver.
     *
     * <p>Implementación garantizada de {@link AutoCloseable#close()}.
     * No lanza checked exceptions para facilitar el uso en bloques try-with-resources
     * y en lambdas del engine.
     */
    @Override
    void close();

    // =========================================================================
    // Interacción con elementos
    // =========================================================================

    /**
     * Hace click sobre el elemento identificado por el locator.
     *
     * @param locator locator del elemento, nunca null
     * @throws java.util.NoSuchElementException si el elemento no existe
     * @throws IllegalStateException si el driver no está inicializado
     */
    void click(ElementLocator locator);

    /**
     * Escribe texto en el elemento input identificado por el locator.
     *
     * <p>Las implementaciones deben limpiar el campo antes de escribir,
     * a menos que el contrato de la acción indique lo contrario.
     *
     * @param locator locator del elemento de entrada, nunca null
     * @param text    texto a escribir, nunca null (puede ser vacío para limpiar)
     * @throws java.util.NoSuchElementException si el elemento no existe
     */
    void type(ElementLocator locator, String text);

    // =========================================================================
    // Esperas
    // =========================================================================

    /**
     * Espera hasta que el elemento sea visible e interactuable.
     *
     * <p>Las implementaciones deben lanzar {@link java.util.concurrent.TimeoutException}
     * (o su equivalente sin checked) si el elemento no aparece dentro del timeout.
     *
     * @param locator locator del elemento a esperar, nunca null
     * @param timeout tiempo máximo de espera, nunca null ni negativo
     */
    void waitFor(ElementLocator locator, Duration timeout);

    // =========================================================================
    // Capturas
    // =========================================================================

    /**
     * Toma una captura de pantalla completa y la guarda en la ruta especificada.
     *
     * <p>El formato del archivo se infiere de la extensión del path (PNG por defecto).
     * Si el directorio padre no existe, las implementaciones pueden crearlo o lanzar
     * {@link java.io.IOException} envuelto en {@link RuntimeException}.
     *
     * @param destination ruta donde guardar la captura, nunca null
     */
    void screenshot(Path destination);

    // =========================================================================
    // Gestos (mobile)
    // =========================================================================

    /**
     * Ejecuta un gesto táctil.
     *
     * <p>La implementación por defecto lanza {@link UnsupportedOperationException}
     * con un mensaje descriptivo — los drivers web no necesitan sobrescribir este método.
     * Los drivers mobile ({@code AppiumEngine}) <b>deben</b> sobrescribir y delegarr
     * al {@code GestureHelper} de {@code mobile-core}.
     *
     * @param gesture gesto a ejecutar (Swipe, Pinch, LongPress), nunca null
     * @throws UnsupportedOperationException si la implementación no soporta gestos
     * @see Gesture
     */
    default void performGesture(Gesture gesture) {
        throw new UnsupportedOperationException(
            "Gestures not supported by " + getClass().getSimpleName()
            + ". Only mobile UiDriver implementations support gestures.");
    }

    // =========================================================================
    // Navegación y contexto
    // =========================================================================

    /**
     * Retorna la URL actual (web) o el contexto/actividad activa (mobile).
     *
     * <p>Web: URL completa (ej: {@code "https://app.example.com/dashboard"}).
     * Mobile: activity o bundle identifier activo (ej: {@code "com.myapp/.MainActivity"}).
     *
     * @return contexto de navegación actual, nunca null
     */
    String getCurrentLocation();

    // =========================================================================
    // Identidad
    // =========================================================================

    /**
     * Retorna el tipo de driver.
     *
     * @return {@link DriverType#WEB} para implementaciones Playwright/Selenium,
     *         {@link DriverType#MOBILE} para implementaciones Appium
     */
    DriverType getType();

    // =========================================================================
    // DriverType enum
    // =========================================================================

    /**
     * Tipo de driver de UI soportado por la plataforma.
     */
    enum DriverType {
        /** Driver de automatización web (Playwright, Selenium). */
        WEB,
        /** Driver de automatización mobile (Appium — Android/iOS). */
        MOBILE
    }
}
