package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Componente para botones y elementos clicables.
 */
public class ButtonComponent extends BaseComponent {

    public ButtonComponent(WebDriver driver, By locator) {
        super(driver, locator);
    }

    public void click() {
        WebElement el = getElementForClick();
        el.click();
        TestLogger.logDebug("BUTTON_COMPONENT", "click -> " + locator, null);
    }

    public void clickJs() {
        try {
            WebElement el = getElement();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", el);
            TestLogger.logDebug("BUTTON_COMPONENT", "clickJs -> " + locator, null);
        } catch (Exception e) {
            TestLogger.logError("BUTTON_COMPONENT", "clickJs -> error ejecutando JS click: " + e.getMessage(), null);
            throw e;
        }
    }

    @Override
    public String getText() {
        return super.getText();
    }
}

