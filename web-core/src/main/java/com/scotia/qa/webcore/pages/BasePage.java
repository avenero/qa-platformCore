package com.scotia.qa.webcore.pages;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.components.BaseComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Clase base para Page Objects del framework Scotia QA.
 *
 * Proporciona funcionalidades comunes para todas las páginas siguiendo el patrón Page Object Model.
 * Usa TestLogger (de common) para logging unificado.
 *
 * @author Abel Venero
 * @version 1.1.0
 */
public abstract class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver no puede ser null");
        }
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    protected void waitForElementToBeVisible(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected void waitForElementToBeClickable(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void waitForElementToBeVisible(By locator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void clickElement(WebElement element) {
        waitForElementToBeClickable(element);
        element.click();
        TestLogger.logDebug("WEB_PAGE", "Elemento clickeado: " + element.toString(), null);
    }

    protected void sendKeysToElement(WebElement element, String text) {
        waitForElementToBeVisible(element);
        element.clear();
        element.sendKeys(text);
        TestLogger.logDebug("WEB_PAGE", "Texto '" + text + "' enviado al elemento", null);
    }

    protected String getElementText(WebElement element) {
        waitForElementToBeVisible(element);
        String text = element.getText();
        TestLogger.logDebug("WEB_PAGE", "Texto obtenido del elemento: " + text, null);
        return text;
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    // === NUEVOS MÉTODOS PARA COMPONENTES (Compatibilidad hacia adelante) ===

    /**
     * Devuelve el WebDriver asociado a esta página.
     * Sirve para inyectar en componentes y para migración gradual.
     */
    protected WebDriver getDriver() {
        return this.driver;
    }

    /**
     * Crea un componente dado su tipo y un localizador By. La implementación
     * usa reflexión simple y asume que el componente tiene un constructor (WebDriver, By).
     *
     * @param componentClass Clase del componente a instanciar
     * @param locator Localizador By del componente
     * @param <T> Tipo del componente
     * @return instancia del componente
     */
    protected <T extends BaseComponent> T createComponent(Class<T> componentClass, By locator) {
        try {
            return componentClass.getConstructor(WebDriver.class, By.class).newInstance(this.driver, locator);
        } catch (Exception e) {
            TestLogger.logError("WEB_PAGE", "Error creando componente " + componentClass.getSimpleName() + ": " + e.getMessage(), null);
            throw new RuntimeException(e);
        }
    }
}
