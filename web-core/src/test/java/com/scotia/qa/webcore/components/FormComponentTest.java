package com.scotia.qa.webcore.components;

import com.scotia.qa.webcore.driver.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link FormComponent}.
 * Verifica el llenado de campos y la acción de submit.
 */
class FormComponentTest {
    private WebDriver mockDriver;
    private WebElement formEl;
    private WebElement inputEl;
    private WebElement submitBtn;

    @BeforeEach
    void setup() {
        mockDriver = Mockito.mock(WebDriver.class);
        formEl = Mockito.mock(WebElement.class);
        inputEl = Mockito.mock(WebElement.class);
        submitBtn = Mockito.mock(WebElement.class);
        DriverManager.setDriver(mockDriver);

        when(inputEl.isDisplayed()).thenReturn(true);
    }

    /**
     * Verifica que {@link FormComponent#fillFields(Map)} ponga los valores en los inputs.
     */
    @Test
    void fillFields_setsValues() {
        By form = By.id("f");
        By inputLocator = By.id("i1");
        when(mockDriver.findElement(inputLocator)).thenReturn(inputEl);

        FormComponent fc = new FormComponent(mockDriver, form);
        Map<By, String> fields = new HashMap<>();
        fields.put(inputLocator, "v1");
        fc.fillFields(fields);

        verify(inputEl).clear();
        verify(inputEl).sendKeys("v1");
    }

    /**
     * Verifica que {@link FormComponent#submit(By)} cliquee el botón de submit.
     */
    @Test
    void submit_clicksButton() {
        By form = By.id("f");
        By submitLocator = By.id("btn");
        when(mockDriver.findElement(submitLocator)).thenReturn(submitBtn);
        when(submitBtn.isDisplayed()).thenReturn(true);
        when(submitBtn.isEnabled()).thenReturn(true);

        FormComponent fc = new FormComponent(mockDriver, form);
        fc.submit(submitLocator);

        verify(submitBtn).click();
    }
}
