package com.scotia.qa.webcore.implementations.driver;

import com.scotia.qa.webcore.driver.WebDriverFactory;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class WebDriverFactoryTest {

    @Test
    void createDriver_local_chrome_returnsDriver() {
        WebDriver driver = WebDriverFactory.createDriver(WebDriverFactory.BrowserType.CHROME, true);
        assertNotNull(driver);
        // No cerramos el driver explícitamente acá porque puede abrir una ventana en CI.
        driver.quit();
    }

    @Test
    void createDriver_invalidGridUrl_throwsRuntimeException() {
        WebDriverFactory.DriverConfig cfg = new WebDriverFactory.DriverConfig(WebDriverFactory.BrowserType.CHROME)
            .withGrid("http://invalid-host:4444/wd/hub");

        assertThrows(RuntimeException.class, () -> WebDriverFactory.createDriver(cfg));
    }
}

