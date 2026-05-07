package com.qa.webcore.driver.engine.playwright;

import com.microsoft.playwright.Locator;
import com.qa.webcore.driver.engine.BrowserElement;

import java.util.Objects;

/**
 * Adaptador de {@link Locator} al contrato {@link BrowserElement}.
 */
public final class PlaywrightBrowserElement implements BrowserElement {

    private final Locator locator;

    public PlaywrightBrowserElement(Locator locator) {
        this.locator = Objects.requireNonNull(locator, "locator no puede ser null");
    }

    @Override
    public String getText() {
        String text = locator.textContent();
        return text == null ? "" : text;
    }

    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    @Override
    public boolean isVisible() {
        return locator.isVisible();
    }

    @Override
    public boolean isEnabled() {
        return locator.isEnabled();
    }

    @Override
    public boolean isSelected() {
        return locator.isChecked();
    }

    @Override
    public void click() {
        locator.click();
    }

    @Override
    public void type(String text) {
        locator.fill(text);
    }
}
