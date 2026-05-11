package com.qa.mobilecore.driver;

import com.qa.common.api.driver.ElementLocator;
import com.qa.common.api.driver.UiDriver;
import com.qa.common.api.runtime.ExecutionConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import io.appium.java_client.remote.SupportsContextSwitching;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests unitarios para {@link AppiumEngine}.
 *
 * <p>Usa el constructor package-private {@code AppiumEngine(AppiumDriver)} para inyectar
 * mocks y evitar la inicialización real de Appium.
 *
 * @since 3.0.0
 */
class AppiumEngineTest {

    // =========================================================================
    // DriverIdentity
    // =========================================================================

    @Nested
    class DriverIdentity {

        @Test
        void getType_returnsMobile() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThat(engine.getType()).isEqualTo(UiDriver.DriverType.MOBILE);
        }

        @Test
        void implementsUiDriver() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThat(engine).isInstanceOf(UiDriver.class);
        }
    }

    // =========================================================================
    // SelectorMapping
    // =========================================================================

    @Nested
    class SelectorMapping {

        @Test
        void click_cssLocator_bySelector() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(By.cssSelector("#btn"))).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.css("#btn"));

            verify(el).click();
        }

        @Test
        void click_xpathLocator() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(By.xpath("//button[@id='login']"))).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.xpath("//button[@id='login']"));

            verify(el).click();
        }

        @Test
        void click_accessibilityId_appiumBy() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(AppiumBy.accessibilityId("login_btn"))).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.accessibilityId("login_btn"));

            verify(el).click();
        }

        @Test
        void click_resourceId_appiumUiAutomator() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            By expected = AppiumBy.androidUIAutomator(
                "new UiSelector().resourceId(\"com.app:id/btn\")");
            when(mockDriver.findElement(expected)).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.resourceId("com.app:id/btn"));

            verify(el).click();
        }

        @Test
        void click_idLocator() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(By.id("submit"))).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.id("submit"));

            verify(el).click();
        }

        @Test
        void click_nameLocator() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(By.name("username"))).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.name("username"));

            verify(el).click();
        }

        @Test
        void click_classNameLocator() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(By.className("android.widget.Button"))).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.className("android.widget.Button"));

            verify(el).click();
        }

        @Test
        void click_textLocator_xpathCrossplatform() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            String text = "Login";
            By expected = By.xpath(
                "//*[@text='Login' or @label='Login' or @value='Login']");
            when(mockDriver.findElement(expected)).thenReturn(el);

            new AppiumEngine(mockDriver).click(ElementLocator.text(text));

            verify(el).click();
        }

        @Test
        void click_roleLocator_throwsUnsupported() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() -> engine.click(ElementLocator.role("button")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ROLE");
        }

        @Test
        void click_nullLocator_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() -> engine.click(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // TypeWithLocator
    // =========================================================================

    @Nested
    class TypeWithLocator {

        @Test
        void type_clearsAndSendsKeys() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            WebElement el = mock(WebElement.class);
            when(mockDriver.findElement(By.id("field"))).thenReturn(el);

            new AppiumEngine(mockDriver).type(ElementLocator.id("field"), "hello");

            verify(el).clear();
            verify(el).sendKeys("hello");
        }

        @Test
        void type_nullText_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() -> engine.type(ElementLocator.id("field"), null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // WaitForWithLocator
    // =========================================================================

    @Nested
    class WaitForWithLocator {

        @Test
        void waitFor_nullTimeout_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() -> engine.waitFor(ElementLocator.id("el"), null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void waitFor_zeroTimeout_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() -> engine.waitFor(ElementLocator.id("el"), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
        }

        @Test
        void waitFor_negativeTimeout_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() ->
                    engine.waitFor(ElementLocator.id("el"), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // ScreenshotWithPath
    // =========================================================================

    @Nested
    class ScreenshotWithPath {

        @Test
        void screenshot_copiesFileToDestination(@TempDir Path tempDir) throws Exception {
            // Create a real temp file with content to copy
            File srcFile = tempDir.resolve("source.png").toFile();
            srcFile.createNewFile();

            AppiumDriver mockDriver = mock(AppiumDriver.class,
                withSettings().extraInterfaces(TakesScreenshot.class));
            when(((TakesScreenshot) mockDriver).getScreenshotAs(any())).thenReturn(srcFile);

            Path dest = tempDir.resolve("output.png");
            new AppiumEngine(mockDriver).screenshot(dest);

            assertThat(dest).exists();
        }

        @Test
        void screenshot_nullDestination_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class,
                withSettings().extraInterfaces(TakesScreenshot.class));
            AppiumEngine engine = new AppiumEngine(mockDriver);

            assertThatThrownBy(() -> engine.screenshot(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void screenshot_createsParentDirectories(@TempDir Path tempDir) throws Exception {
            File srcFile = tempDir.resolve("source.png").toFile();
            srcFile.createNewFile();

            AppiumDriver mockDriver = mock(AppiumDriver.class,
                withSettings().extraInterfaces(TakesScreenshot.class));
            when(((TakesScreenshot) mockDriver).getScreenshotAs(any())).thenReturn(srcFile);

            Path dest = tempDir.resolve("nested").resolve("deep").resolve("output.png");
            new AppiumEngine(mockDriver).screenshot(dest);

            assertThat(dest).exists();
        }
    }

    // =========================================================================
    // GetCurrentLocation
    // =========================================================================

    @Nested
    class GetCurrentLocation {

        @Test
        void getCurrentLocation_nativeContext_returnsContext() {
            AppiumDriver mockDriver = mock(AppiumDriver.class,
                withSettings().extraInterfaces(SupportsContextSwitching.class));
            when(((SupportsContextSwitching) mockDriver).getContext()).thenReturn("NATIVE_APP");

            String location = new AppiumEngine(mockDriver).getCurrentLocation();

            assertThat(location).isEqualTo("NATIVE_APP");
        }

        @Test
        void getCurrentLocation_webviewContext_returnsUrl() {
            AppiumDriver mockDriver = mock(AppiumDriver.class,
                withSettings().extraInterfaces(SupportsContextSwitching.class));
            when(((SupportsContextSwitching) mockDriver).getContext()).thenReturn("WEBVIEW_com.myapp");
            when(mockDriver.getCurrentUrl()).thenReturn("https://example.com/page");

            String location = new AppiumEngine(mockDriver).getCurrentLocation();

            assertThat(location).isEqualTo("https://example.com/page");
        }

        @Test
        void getCurrentLocation_nonContextDriver_returnsCurrentUrl() {
            // Plain AppiumDriver — does NOT implement SupportsContextSwitching
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            when(mockDriver.getCurrentUrl()).thenReturn("https://example.com/fallback");

            String location = new AppiumEngine(mockDriver).getCurrentLocation();

            assertThat(location).isEqualTo("https://example.com/fallback");
        }
    }

    // =========================================================================
    // OpenCloseLifecycle
    // =========================================================================

    @Nested
    class OpenCloseLifecycle {

        @Test
        void open_nullConfig_throws() {
            AppiumEngine engine = new AppiumEngine();

            assertThatThrownBy(() -> engine.open(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void open_alreadyInitialized_throws() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            AppiumEngine engine = new AppiumEngine(mockDriver);
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            assertThatThrownBy(() -> engine.open(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está inicializado");
        }

        @Test
        void close_uninitEngine_doesNotThrow() {
            AppiumEngine engine = new AppiumEngine();

            // Must not throw any exception
            engine.close();
        }

        @Test
        void noArgConstructor_driverIsNull_isNotActive() {
            AppiumEngine engine = new AppiumEngine();

            // Driver is null — close() should be a no-op
            engine.close();

            // requireDriver() must throw ISE
            assertThatThrownBy(() -> engine.click(ElementLocator.id("any")))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
