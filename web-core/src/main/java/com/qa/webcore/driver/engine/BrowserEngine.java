package com.qa.webcore.driver.engine;

import com.qa.common.api.driver.ElementLocator;
import com.qa.common.api.driver.UiDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Puerto de automatización web independiente del motor subyacente.
 *
 * <p>Extiende {@link UiDriver} para participar en el contrato unificado
 * de drivers de UI. Los métodos abstractos {@code open(ExecutionConfig)} y
 * {@code close()} deben ser implementados por la clase concreta
 * ({@link com.qa.webcore.driver.engine.playwright.PlaywrightBrowserEngine}).
 *
 * <p>Los seis métodos puente ({@code click}, {@code type}, {@code waitFor},
 * {@code screenshot}, {@code getCurrentLocation}, {@code getType}) tienen
 * implementaciones {@code default} que delegan a la API string existente vía
 * {@link #toSelector(ElementLocator)}. Las implementaciones concretas pueden
 * sobrescribirlos directamente con Playwright para mayor rendimiento.
 */
public interface BrowserEngine extends UiDriver {

    // =========================================================================
    // Localización
    // =========================================================================

    BrowserElement find(String selector);
    List<BrowserElement> findAll(String selector);
    boolean isPresent(String selector);

    // =========================================================================
    // Acciones (string-based API)
    // =========================================================================

    void click(String selector);
    void type(String selector, String text);
    void clear(String selector);
    void selectOption(String selector, String value);
    void hover(String selector);
    void doubleClick(String selector);

    // =========================================================================
    // Navegación
    // =========================================================================

    void navigateTo(String url);
    String getCurrentUrl();
    String getTitle();
    void back();
    void refresh();

    // =========================================================================
    // Esperas
    // =========================================================================

    void waitForVisible(String selector, int timeoutMs);
    void waitForText(String selector, String text, int timeoutMs);
    void waitForHidden(String selector, int timeoutMs);
    void waitForUrl(String urlPattern, int timeoutMs);
    void waitForLoadState(String state, int timeoutMs);

    // =========================================================================
    // Frames
    // =========================================================================

    BrowserElement findInFrame(String frameSelector, String elementSelector);
    void clickInFrame(String frameSelector, String elementSelector);

    // =========================================================================
    // Capturas
    // =========================================================================

    byte[] screenshot();
    byte[] screenshotElement(String selector);

    // =========================================================================
    // JavaScript
    // =========================================================================

    Object evaluate(String script, Object... args);
    void scrollIntoView(String selector);

    // =========================================================================
    // Ventanas
    // =========================================================================

    void switchToNewWindow();
    String getWindowTitle();
    void closeCurrentWindow();

    // =========================================================================
    // Alertas
    // =========================================================================

    String getAlertText();
    void acceptAlert();
    void dismissAlert();

    // =========================================================================
    // Lifecycle (string-based)
    // =========================================================================

    boolean isActive();

    // =========================================================================
    // UiDriver bridge defaults — delegan a la API string vía toSelector()
    // Las implementaciones concretas pueden sobrescribir para ir directo a Playwright.
    // =========================================================================

    @Override
    default void click(ElementLocator locator) {
        click(toSelector(locator));
    }

    @Override
    default void type(ElementLocator locator, String text) {
        type(toSelector(locator), text);
    }

    @Override
    default void waitFor(ElementLocator locator, Duration timeout) {
        Objects.requireNonNull(timeout, "timeout no puede ser null");
        long ms = timeout.toMillis();
        if (ms <= 0) {
            throw new IllegalArgumentException("timeout debe ser positivo");
        }
        waitForVisible(toSelector(locator), (int) Math.min(ms, Integer.MAX_VALUE));
    }

    @Override
    default void screenshot(Path destination) {
        Objects.requireNonNull(destination, "destination no puede ser null");
        byte[] bytes = screenshot();
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(destination, bytes);
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar la captura en: " + destination, ex);
        }
    }

    @Override
    default String getCurrentLocation() {
        return getCurrentUrl();
    }

    @Override
    default DriverType getType() {
        return DriverType.WEB;
    }

    // =========================================================================
    // Selector helper (private, Java 9+)
    // =========================================================================

    /**
     * Convierte un {@link ElementLocator} al selector string que Playwright entiende.
     *
     * <p>RESOURCE_ID es exclusivo de mobile y lanza {@link UnsupportedOperationException}.
     */
    private static String toSelector(ElementLocator locator) {
        Objects.requireNonNull(locator, "locator no puede ser null");
        return switch (locator.strategy()) {
            case CSS              -> locator.value();
            case XPATH            -> locator.value().startsWith("xpath=")
                                     ? locator.value() : "xpath=" + locator.value();
            case ID               -> "#" + locator.value();
            case ACCESSIBILITY_ID -> "[data-testid='" + locator.value() + "']";
            case TEXT             -> "text=" + locator.value();
            case ROLE             -> "role=" + locator.value();
            case NAME             -> "[name='" + locator.value() + "']";
            case CLASS_NAME       -> "." + locator.value();
            case RESOURCE_ID      -> throw new UnsupportedOperationException(
                                     "RESOURCE_ID es exclusivo de mobile — no soportado en BrowserEngine");
        };
    }
}
