package com.qa.webcore.driver.engine;

/**
 * Abstracción de elemento web para desacoplar componentes BDD
 * de implementaciones concretas de automatización de navegador.
 */
public interface BrowserElement {

    String getText();

    String getAttribute(String name);

    boolean isVisible();

    boolean isEnabled();

    boolean isSelected();

    void click();

    void type(String text);
}
