package com.scotia.qa.webcore.components;

import com.scotia.qa.common.logging.TestLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Componente para interactuar con tablas HTML (thead/tbody).
 */
public class TableComponent extends BaseComponent {

    private By rowLocator = By.cssSelector("tbody tr");

    public TableComponent(WebDriver driver, By locator) {
        super(driver, locator);
    }

    public List<WebElement> getRows() {
        WebElement table = getElement();
        return table.findElements(rowLocator);
    }

    public List<String> getHeaders() {
        WebElement table = getElement();
        List<WebElement> headers = table.findElements(By.cssSelector("thead th"));
        List<String> names = new ArrayList<>();
        for (WebElement h : headers) {
            names.add(h.getText());
        }
        return names;
    }

    public WebElement findRowByCellText(String text) {
        for (WebElement row : getRows()) {
            if (row.getText().contains(text)) {
                return row;
            }
        }
        return null;
    }

    public void clickCell(int rowIndex, int colIndex) {
        List<WebElement> rows = getRows();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Fila fuera de rango: " + rowIndex);
        }
        WebElement row = rows.get(rowIndex);
        List<WebElement> cells = row.findElements(By.cssSelector("td,th"));
        if (colIndex < 0 || colIndex >= cells.size()) {
            throw new IndexOutOfBoundsException("Columna fuera de rango: " + colIndex);
        }
        cells.get(colIndex).click();
        TestLogger.logDebug("TABLE_COMPONENT", "clickCell -> fila:" + rowIndex + " col:" + colIndex, null);
    }
}

