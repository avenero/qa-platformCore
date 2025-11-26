package com.scotia.qa.webcore.components;

import com.scotia.qa.webcore.driver.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link DropdownComponent}.
 * Verifica extracción de opciones y selección por texto.
 */
class DropdownComponentTest {
    private WebDriver mockDriver;
    private WebElement mockSelectElement;
    private WebElement option1;
    private WebElement option2;

    @BeforeEach
    void setup() {
        mockDriver = Mockito.mock(WebDriver.class);
        mockSelectElement = Mockito.mock(WebElement.class);
        option1 = Mockito.mock(WebElement.class);
        option2 = Mockito.mock(WebElement.class);

        DriverManager.setDriver(mockDriver);

        // Make the select element appear visible for WaitUtils
        when(mockSelectElement.isDisplayed()).thenReturn(true);
        // Ensure the element has tagName 'select' so org.openqa.selenium.support.ui.Select works
        when(mockSelectElement.getTagName()).thenReturn("select");
    }

    /**
     * Verifica que {@link DropdownComponent#getAllOptions()} retorne los textos de las opciones.
     */
    @Test
    void getAllOptions_returnsTexts() {
        By locator = By.id("sel");
        when(mockDriver.findElement(locator)).thenReturn(mockSelectElement);
        when(option1.getText()).thenReturn("One");
        when(option2.getText()).thenReturn("Two");
        when(mockSelectElement.findElements(By.tagName("option"))).thenReturn(Arrays.asList(option1, option2));

        DropdownComponent dd = new DropdownComponent(mockDriver, locator);
        List<String> opts = dd.getAllOptions();

        assertEquals(2, opts.size());
        assertTrue(opts.contains("One"));
        assertTrue(opts.contains("Two"));
    }

    /**
     * Verifica que {@link DropdownComponent#selectByVisibleText(String)} haga click en la opción que coincide.
     */
    @Test
    void selectByVisibleText_clicksMatchingOption() {
        By locator = By.id("sel");
        when(mockDriver.findElement(locator)).thenReturn(mockSelectElement);
        when(option1.getText()).thenReturn("One");
        when(option2.getText()).thenReturn("Two");
        when(mockSelectElement.findElements(By.tagName("option"))).thenReturn(Arrays.asList(option1, option2));

        DropdownComponent dd = new DropdownComponent(mockDriver, locator);
        dd.selectByVisibleText("Two");

        verify(option2).click();
    }
}
