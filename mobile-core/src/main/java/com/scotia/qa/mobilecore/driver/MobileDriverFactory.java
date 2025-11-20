package com.scotia.qa.mobilecore.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Factory para la creación y configuración de AppiumDrivers.
 * Maneja la inicialización de drivers para Android e iOS.
 */
public class MobileDriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(MobileDriverFactory.class);

    public enum Platform {
        ANDROID, IOS
    }

    public static AppiumDriver createDriver(Platform platform, DesiredCapabilities capabilities, String appiumUrl) {
        AppiumDriver driver;

        try {
            URL serverUrl = new URL(appiumUrl);

            switch (platform) {
                case ANDROID:
                    driver = createAndroidDriver(capabilities, serverUrl);
                    break;
                case IOS:
                    driver = createIOSDriver(capabilities, serverUrl);
                    break;
                default:
                    throw new IllegalArgumentException("Plataforma no soportada: " + platform);
            }

            configureDriver(driver);
            logger.info("AppiumDriver inicializado para plataforma: {}", platform);
            return driver;

        } catch (MalformedURLException e) {
            throw new RuntimeException("URL de Appium inválida: " + appiumUrl, e);
        }
    }

    private static AndroidDriver createAndroidDriver(DesiredCapabilities capabilities, URL serverUrl) {
        // Capacidades específicas para Android
        capabilities.setCapability("platformName", "Android");
        return new AndroidDriver(serverUrl, capabilities);
    }

    private static IOSDriver createIOSDriver(DesiredCapabilities capabilities, URL serverUrl) {
        // Capacidades específicas para iOS
        capabilities.setCapability("platformName", "iOS");
        return new IOSDriver(serverUrl, capabilities);
    }

    private static void configureDriver(AppiumDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    public static DesiredCapabilities getAndroidCapabilities(String deviceName, String appPath) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName", deviceName);
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("app", appPath);
        capabilities.setCapability("automationName", "UiAutomator2");
        return capabilities;
    }

    public static DesiredCapabilities getIOSCapabilities(String deviceName, String appPath) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName", deviceName);
        capabilities.setCapability("platformName", "iOS");
        capabilities.setCapability("app", appPath);
        capabilities.setCapability("automationName", "XCUITest");
        return capabilities;
    }
}

