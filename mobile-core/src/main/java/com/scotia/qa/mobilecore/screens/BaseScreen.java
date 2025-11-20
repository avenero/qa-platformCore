package com.scotia.qa.mobilecore.screens;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Clase base para Screen Objects en automatización móvil.
 * Proporciona funcionalidades comunes para todas las pantallas.
 */
public abstract class BaseScreen {

    protected static final Logger logger = LoggerFactory.getLogger(BaseScreen.class);
    protected AppiumDriver driver;
    protected WebDriverWait wait;

    public BaseScreen(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    protected void waitForElementToBeVisible(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected void waitForElementToBeClickable(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void tapElement(WebElement element) {
        waitForElementToBeClickable(element);
        element.click();
        logger.debug("Elemento tocado: {}", element);
    }

    protected void sendKeysToElement(WebElement element, String text) {
        waitForElementToBeVisible(element);
        element.clear();
        element.sendKeys(text);
        logger.debug("Texto '{}' enviado al elemento: {}", text, element);
    }

    protected String getElementText(WebElement element) {
        waitForElementToBeVisible(element);
        String text = element.getText();
        logger.debug("Texto obtenido del elemento: {}", text);
        return text;
    }

    protected boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void swipeUp() {
        // Implementación básica de swipe up
        int startX = driver.manage().window().getSize().getWidth() / 2;
        int startY = driver.manage().window().getSize().getHeight() * 3 / 4;
        int endY = driver.manage().window().getSize().getHeight() / 4;

        // Note: touchAction implementation would go here
        logger.debug("Swipe up realizado");
    }

    protected void swipeDown() {
        // Implementación básica de swipe down
        int startX = driver.manage().window().getSize().getWidth() / 2;
        int startY = driver.manage().window().getSize().getHeight() / 4;
        int endY = driver.manage().window().getSize().getHeight() * 3 / 4;

        // Note: touchAction implementation would go here
        logger.debug("Swipe down realizado");
    }
}
