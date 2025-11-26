package com.scotia.qa.webcore.components;

import com.scotia.qa.webcore.driver.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para {@link InputComponent}.
 * Cada método verifica interacciones básicas con el input (set, get).
 */
class InputComponentTest {

    private WebDriver mockDriver;
    private WebElement mockElement;

    @BeforeEach
    void setup() {
        mockDriver = Mockito.mock(WebDriver.class);
        mockElement = Mockito.mock(WebElement.class);

        // Establecer driver en DriverManager para que WaitUtils lo use
        DriverManager.setDriver(mockDriver);

        when(mockElement.isDisplayed()).thenReturn(true);
        when(mockElement.isEnabled()).thenReturn(true);
    }

    /**
     * Verifica que {@link InputComponent#setText(String)} invoque clear() y sendKeys() en el elemento.
     */
    @Test
    void setText_callsClearAndSendKeys() {
        By locator = By.id("input-test");

        // Simulamos que findElement devuelve nuestro mockElement
        when(mockDriver.findElement(locator)).thenReturn(mockElement);

        InputComponent input = new InputComponent(mockDriver, locator);
        input.setText("hello");

        verify(mockElement).clear();
        verify(mockElement).sendKeys("hello");
    }

    /**
     * Verifica que {@link InputComponent#getValue()} retorne el atributo "value" del elemento.
     */
    @Test
    void getValue_returnsAttributeValue() {
        By locator = By.id("input-test");
        when(mockDriver.findElement(locator)).thenReturn(mockElement);
        when(mockElement.getAttribute("value")).thenReturn("v1");

        InputComponent input = new InputComponent(mockDriver, locator);
        assertEquals("v1", input.getValue());
    }
}
