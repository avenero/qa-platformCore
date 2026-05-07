package com.qa.webcore.driver.engine;

import java.util.List;

/**
 * Puerto de automatización web independiente del motor subyacente.
 */
public interface BrowserEngine {

    // Localizacion
    BrowserElement find(String selector);
    List<BrowserElement> findAll(String selector);
    boolean isPresent(String selector);

    // Acciones
    void click(String selector);
    void type(String selector, String text);
    void clear(String selector);
    void selectOption(String selector, String value);
    void hover(String selector);
    void doubleClick(String selector);

    // Navegacion
    void navigateTo(String url);
    String getCurrentUrl();
    String getTitle();
    void back();
    void refresh();

    // Esperas
    void waitForVisible(String selector, int timeoutMs);
    void waitForText(String selector, String text, int timeoutMs);
    void waitForHidden(String selector, int timeoutMs);
    void waitForUrl(String urlPattern, int timeoutMs);
    void waitForLoadState(String state, int timeoutMs);

    // Frames
    BrowserElement findInFrame(String frameSelector, String elementSelector);
    void clickInFrame(String frameSelector, String elementSelector);

    // Capturas
    byte[] screenshot();
    byte[] screenshotElement(String selector);

    // JavaScript
    Object evaluate(String script, Object... args);
    void scrollIntoView(String selector);

    // Ventanas
    void switchToNewWindow();
    String getWindowTitle();
    void closeCurrentWindow();

    // Alertas
    String getAlertText();
    void acceptAlert();
    void dismissAlert();

    // Lifecycle
    boolean isActive();
}
