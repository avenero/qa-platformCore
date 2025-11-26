package com.scotia.qa.webcore.implementations.driver;

import com.scotia.qa.webcore.driver.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class DriverManagerTest {

    @AfterEach
    void cleanup() {
        // Asegurar limpieza entre tests
        try {
            DriverManager.quitDriverSafely();
        } catch (Exception ignored) {}
    }

    @Test
    void setDriver_null_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DriverManager.setDriver(null));
    }

    @Test
    void setDriver_and_getDriver_returnsSameInstance() {
        WebDriver mock = Mockito.mock(WebDriver.class);
        DriverManager.setDriver(mock);

        WebDriver obtained = DriverManager.getDriver();
        assertSame(mock, obtained);
    }

    @Test
    void quitDriver_callsQuit_and_clearsThreadLocal() {
        WebDriver mock = Mockito.mock(WebDriver.class);
        DriverManager.setDriver(mock);

        DriverManager.quitDriver();

        Mockito.verify(mock).quit();
        assertFalse(DriverManager.isDriverInitialized());
    }
}

