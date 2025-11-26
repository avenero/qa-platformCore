package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Componente para campos de texto (input / textarea).
 */
public class InputComponent extends BaseComponent {

    public InputComponent(WebDriver driver, By locator) {
        super(driver, locator);
    }

    public void setText(String text) {
        WebElement el = getElement();
        el.clear();
        el.sendKeys(text);
        TestLogger.logDebug("INPUT_COMPONENT", "setText -> '" + text + "' en " + locator, null);
    }

    public void appendText(String text) {
        WebElement el = getElement();
        el.sendKeys(text);
        TestLogger.logDebug("INPUT_COMPONENT", "appendText -> '" + text + "' en " + locator, null);
    }

    public void clear() {
        WebElement el = getElement();
        el.clear();
        TestLogger.logDebug("INPUT_COMPONENT", "clear -> " + locator, null);
    }

    public String getValue() {
        try {
            return getElement().getAttribute("value");
        } catch (Exception e) {
            TestLogger.logDebug("INPUT_COMPONENT", "getValue -> error leyendo value de: " + locator, null);
            return null;
        }
    }

    public void sendEnter() {
        WebElement el = getElement();
        el.sendKeys(Keys.ENTER);
        TestLogger.logDebug("INPUT_COMPONENT", "sendEnter -> " + locator, null);
    }

    public boolean isEmpty() {
        String v = getValue();
        return v == null || v.trim().isEmpty();
    }
}

