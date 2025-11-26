package com.scotia.qa.webcore.components;

import com.scotia.qa.webcore.driver.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link ButtonComponent}.
 * Describe comportamientos de click y click vía JavaScript.
 */
class ButtonComponentTest {

    private WebDriver mockDriver;
    private WebElement mockElement;

    @BeforeEach
    void setup() {
        // Crear un mock de WebDriver que implemente también JavascriptExecutor
        mockDriver = Mockito.mock(WebDriver.class, Mockito.withSettings().extraInterfaces(JavascriptExecutor.class));
        mockElement = Mockito.mock(WebElement.class);
        DriverManager.setDriver(mockDriver);

        // Simular que el elemento está visible y habilitado para que WaitUtils lo considere clickable
        when(mockElement.isDisplayed()).thenReturn(true);
        when(mockElement.isEnabled()).thenReturn(true);
    }

    /**
     * Verifica que {@link ButtonComponent#click()} invoque click() en el elemento.
     */
    @Test
    void click_invokesElementClick() {
        By locator = By.id("btn-test");
        when(mockDriver.findElement(locator)).thenReturn(mockElement);

        ButtonComponent btn = new ButtonComponent(mockDriver, locator);
        btn.click();

        verify(mockElement).click();
    }

    /**
     * Verifica que {@link ButtonComponent#clickJs()} ejecute un click mediante JavaScript.
     */
    @Test
    void clickJs_executesJavascriptClick() {
        By locator = By.id("btn-test");
        when(mockDriver.findElement(locator)).thenReturn(mockElement);

        ButtonComponent btn = new ButtonComponent(mockDriver, locator);
        btn.clickJs();

        verify((JavascriptExecutor) mockDriver).executeScript(eq("arguments[0].click();"), eq(mockElement));
    }
}
