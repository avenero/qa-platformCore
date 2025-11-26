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
 * Tests unitarios para {@link TableComponent}.
 */
public class TableComponentTest {

    private WebDriver mockDriver;
    private WebElement tableEl;
    private WebElement row1;
    private WebElement cell11;

    @BeforeEach
    void setup() {
        mockDriver = Mockito.mock(WebDriver.class);
        tableEl = Mockito.mock(WebElement.class);
        row1 = Mockito.mock(WebElement.class);
        cell11 = Mockito.mock(WebElement.class);
        DriverManager.setDriver(mockDriver);

        when(tableEl.isDisplayed()).thenReturn(true);
        when(mockDriver.findElement(By.id("t"))).thenReturn(tableEl);
    }

    @Test
    void getHeaders_returnsHeaderTexts() {
        WebElement th1 = Mockito.mock(WebElement.class);
        when(th1.getText()).thenReturn("H1");
        when(tableEl.findElements(By.cssSelector("thead th"))).thenReturn(Arrays.asList(th1));

        TableComponent t = new TableComponent(mockDriver, By.id("t"));
        List<String> headers = t.getHeaders();
        assertEquals(1, headers.size());
        assertEquals("H1", headers.get(0));
    }

    @Test
    void clickCell_clicksSpecificCell() {
        when(tableEl.findElements(By.cssSelector("tbody tr"))).thenReturn(Arrays.asList(row1));
        when(row1.findElements(By.cssSelector("td,th"))).thenReturn(Arrays.asList(cell11));

        TableComponent t = new TableComponent(mockDriver, By.id("t"));
        t.clickCell(0, 0);

        verify(cell11).click();
    }
}
