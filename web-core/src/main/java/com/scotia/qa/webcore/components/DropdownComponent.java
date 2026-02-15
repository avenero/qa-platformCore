package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Componente para select / dropdowns nativos.
 * Nota: para dropdowns custom basados en divs, usar metodologías específicas.
 */
public class DropdownComponent extends BaseComponent {

    public DropdownComponent(WebDriver driver, By locator) {
        super(driver, locator);
    }

    private Select getSelect() {
        WebElement el = getElement();
        return new Select(el);
    }

    public void selectByVisibleText(String text) {
        // Implementación manual para compatibilidad con mocks en tests
        WebElement el = getElement();
        List<WebElement> options = el.findElements(By.tagName("option"));
        for (WebElement opt : options) {
            try {
                if (text.equals(opt.getText())) {
                    opt.click();
                    TestLogger.logDebug("DROPDOWN_COMPONENT", "selectByVisibleText -> '" + text + "' en " + locator, null);
                    return;
                }
            } catch (Exception e) {
                TestLogger.logError("DROPDOWN_COMPONENT", "Error seleccionando por texto: " + e.getMessage(), null);
                throw e;
            }
        }
        throw new IllegalArgumentException("Opción no encontrada: " + text);
    }

    public void selectByValue(String value) {
        WebElement el = getElement();
        List<WebElement> options = el.findElements(By.tagName("option"));
        for (WebElement opt : options) {
            try {
                // Usar getDomAttribute() en lugar de getAttribute() (deprecado en Selenium 4+)
                if (value.equals(opt.getDomAttribute("value"))) {
                    opt.click();
                    TestLogger.logDebug("DROPDOWN_COMPONENT", "selectByValue -> '" + value + "' en " + locator, null);
                    return;
                }
            } catch (Exception e) {
                TestLogger.logError("DROPDOWN_COMPONENT", "Error seleccionando por value: " + e.getMessage(), null);
                throw e;
            }
        }
        throw new IllegalArgumentException("Opción no encontrada por value: " + value);
    }

    public List<String> getAllOptions() {
        return getSelect().getOptions().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public String getSelectedOption() {
        return getSelect().getFirstSelectedOption().getText();
    }
}
