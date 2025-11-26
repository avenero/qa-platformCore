package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Clase base para los componentes UI del framework.
 * Provee utilidades comunes (esperas, acceso al elemento, cheks básicos).
 */
public abstract class BaseComponent {
    protected final WebDriver driver;
    protected final By locator;

    protected static final int DEFAULT_WAIT_SECONDS = 15;

    public BaseComponent(WebDriver driver, By locator) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver no puede ser null");
        }
        if (locator == null) {
            throw new IllegalArgumentException("Locator no puede ser null");
        }
        this.driver = driver;
        this.locator = locator;
    }

    protected WebElement getElement() {
        return WaitUtils.waitForElementToBeVisible(locator);
    }

    protected WebElement getElementForClick() {
        return WaitUtils.waitForElementToBeClickable(locator);
    }

    public boolean isVisible() {
        try {
            return getElement().isDisplayed();
        } catch (Exception e) {
            TestLogger.logDebug("BASE_COMPONENT", "isVisible -> elemento no presente: " + locator, null);
            return false;
        }
    }

    public boolean isEnabled() {
        try {
            return getElement().isEnabled();
        } catch (Exception e) {
            TestLogger.logDebug("BASE_COMPONENT", "isEnabled -> elemento no presente: " + locator, null);
            return false;
        }
    }

    public String getText() {
        try {
            return getElement().getText();
        } catch (Exception e) {
            TestLogger.logDebug("BASE_COMPONENT", "getText -> error leyendo texto de: " + locator, null);
            return null;
        }
    }

    public By getLocator() {
        return this.locator;
    }
}

