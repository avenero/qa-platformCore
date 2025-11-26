package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Componente para manejo de modales/dialogs simples.
 */
public class ModalComponent extends BaseComponent {

    private By closeButtonLocator;
    private By headerLocator;

    public ModalComponent(WebDriver driver, By locator, By headerLocator, By closeButtonLocator) {
        super(driver, locator);
        this.headerLocator = headerLocator;
        this.closeButtonLocator = closeButtonLocator;
    }

    public boolean isOpen() {
        return isVisible();
    }

    public String getHeaderText() {
        try {
            WebElement header = WaitUtils.waitForElementToBeVisible(headerLocator);
            return header.getText();
        } catch (Exception e) {
            TestLogger.logDebug("MODAL_COMPONENT", "getHeaderText -> error leyendo header", null);
            return null;
        }
    }

    public void close() {
        try {
            WebElement btn = WaitUtils.waitForElementToBeClickable(closeButtonLocator);
            btn.click();
            TestLogger.logDebug("MODAL_COMPONENT", "close -> modal cerrado", null);
        } catch (Exception e) {
            TestLogger.logError("MODAL_COMPONENT", "close -> error cerrando modal: " + e.getMessage(), null);
            throw e;
        }
    }
}
