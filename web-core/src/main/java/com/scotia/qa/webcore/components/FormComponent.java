package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * Componente para formularios que orquesta múltiples campos.
 * Simplifica acciones como poblar campos por nombre y submit.
 */
public class FormComponent extends BaseComponent {

    public FormComponent(WebDriver driver, By locator) {
        super(driver, locator);
    }

    /**
     * Rellena campos en base a un mapa de localizadores dentro del form.
     * @param fields mapa de By->valor
     */
    public void fillFields(Map<By, String> fields) {
        for (Map.Entry<By, String> e : fields.entrySet()) {
            By fieldLocator = e.getKey();
            String value = e.getValue();
            WebElement input = WaitUtils.waitForElementToBeVisible(fieldLocator);
            input.clear();
            input.sendKeys(value);
            TestLogger.logDebug("FORM_COMPONENT", "fillFields -> " + fieldLocator + " = '" + value + "'", null);
        }
    }

    public void submit(By submitButtonLocator) {
        WebElement btn = WaitUtils.waitForElementToBeClickable(submitButtonLocator);
        btn.click();
        TestLogger.logDebug("FORM_COMPONENT", "submit -> form submit ejecutado", null);
    }
}

