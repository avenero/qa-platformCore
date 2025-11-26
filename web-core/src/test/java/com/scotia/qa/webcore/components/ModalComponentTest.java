package com.scotia.qa.webcore.components;

import com.scotia.qa.webcore.driver.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link ModalComponent}.
 * Comprueba comportamiento de visibilidad y cierre del modal.
 */
class ModalComponentTest {
    private WebDriver mockDriver;
    private WebElement modalRoot;
    private WebElement header;
    private WebElement closeBtn;

    @BeforeEach
    void setup() {
        mockDriver = Mockito.mock(WebDriver.class);
        modalRoot = Mockito.mock(WebElement.class);
        header = Mockito.mock(WebElement.class);
        closeBtn = Mockito.mock(WebElement.class);
        DriverManager.setDriver(mockDriver);
    }

    /**
     * Verifica que {@link ModalComponent#isOpen()} refleje la visibilidad del root.
     */
    @Test
    void isOpen_reflectsVisibility() {
        By root = By.id("modal");
        when(mockDriver.findElement(root)).thenReturn(modalRoot);
        when(modalRoot.isDisplayed()).thenReturn(true);

        ModalComponent m = new ModalComponent(mockDriver, root, By.cssSelector(".header"), By.cssSelector(".close"));
        assertTrue(m.isOpen());
    }

    /**
     * Verifica que {@link ModalComponent#close()} cliquee el botón de cierre.
     */
    @Test
    void close_clicksCloseButton() {
        By root = By.id("modal");
        By close = By.cssSelector(".close");
        when(mockDriver.findElement(root)).thenReturn(modalRoot);
        when(mockDriver.findElement(close)).thenReturn(closeBtn);
        when(closeBtn.isDisplayed()).thenReturn(true);
        when(closeBtn.isEnabled()).thenReturn(true);

        ModalComponent m = new ModalComponent(mockDriver, root, By.cssSelector(".header"), close);
        m.close();

        verify(closeBtn).click();
    }
}
