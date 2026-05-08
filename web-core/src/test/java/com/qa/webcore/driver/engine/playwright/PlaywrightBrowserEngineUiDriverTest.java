package com.qa.webcore.driver.engine.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.qa.common.driver.ElementLocator;
import com.qa.common.driver.UiDriver;
import com.qa.common.runtime.ExecutionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PlaywrightBrowserEngine — UiDriver contract")
class PlaywrightBrowserEngineUiDriverTest {

    private Page page;
    private PlaywrightBrowserEngine engine;

    @BeforeEach
    void setUp() {
        page = mock(Page.class);
        engine = new PlaywrightBrowserEngine(page);
    }

    // =========================================================================
    // Driver identity
    // =========================================================================

    @Nested
    @DisplayName("Driver identity")
    class DriverIdentity {

        @Test
        @DisplayName("getType() devuelve WEB")
        void getType_returnsWeb() {
            assertThat(engine.getType()).isEqualTo(UiDriver.DriverType.WEB);
        }

        @Test
        @DisplayName("PlaywrightBrowserEngine implementa UiDriver")
        void implementsUiDriver() {
            assertThat(engine).isInstanceOf(UiDriver.class);
        }
    }

    // =========================================================================
    // Selector mapping — click(ElementLocator) como proxy de toPlaywrightSelector
    // =========================================================================

    @Nested
    @DisplayName("Selector mapping via click(ElementLocator)")
    class SelectorMapping {

        @Test
        @DisplayName("CSS — valor directo, sin prefijo")
        void css_usesValueDirectly() {
            Locator locator = mock(Locator.class);
            when(page.locator("#submit")).thenReturn(locator);
            engine.click(ElementLocator.css("#submit"));
            verify(locator).click();
        }

        @Test
        @DisplayName("XPATH sin prefijo — agrega xpath=")
        void xpath_addsPrefixWhenMissing() {
            Locator locator = mock(Locator.class);
            when(page.locator("xpath=//button[@type='submit']")).thenReturn(locator);
            engine.click(ElementLocator.xpath("//button[@type='submit']"));
            verify(locator).click();
        }

        @Test
        @DisplayName("XPATH ya prefijado — no duplica xpath=")
        void xpath_alreadyPrefixed_noDuplicate() {
            Locator locator = mock(Locator.class);
            when(page.locator("xpath=//div")).thenReturn(locator);
            engine.click(ElementLocator.xpath("xpath=//div"));
            verify(locator).click();
        }

        @Test
        @DisplayName("ID — agrega prefijo #")
        void id_hashPrefix() {
            Locator locator = mock(Locator.class);
            when(page.locator("#username")).thenReturn(locator);
            engine.click(ElementLocator.id("username"));
            verify(locator).click();
        }

        @Test
        @DisplayName("ACCESSIBILITY_ID — genera [data-testid='...']")
        void accessibilityId_dataTestidAttribute() {
            Locator locator = mock(Locator.class);
            when(page.locator("[data-testid='login-btn']")).thenReturn(locator);
            engine.click(ElementLocator.accessibilityId("login-btn"));
            verify(locator).click();
        }

        @Test
        @DisplayName("TEXT — agrega prefijo text=")
        void text_textPrefix() {
            Locator locator = mock(Locator.class);
            when(page.locator("text=Iniciar sesion")).thenReturn(locator);
            engine.click(ElementLocator.text("Iniciar sesion"));
            verify(locator).click();
        }

        @Test
        @DisplayName("ROLE — agrega prefijo role=")
        void role_rolePrefix() {
            Locator locator = mock(Locator.class);
            when(page.locator("role=button")).thenReturn(locator);
            engine.click(ElementLocator.role("button"));
            verify(locator).click();
        }

        @Test
        @DisplayName("NAME — genera [name='...']")
        void name_nameAttribute() {
            Locator locator = mock(Locator.class);
            when(page.locator("[name='email']")).thenReturn(locator);
            engine.click(ElementLocator.name("email"));
            verify(locator).click();
        }

        @Test
        @DisplayName("CLASS_NAME — agrega prefijo .")
        void className_dotPrefix() {
            Locator locator = mock(Locator.class);
            when(page.locator(".btn-primary")).thenReturn(locator);
            engine.click(ElementLocator.className("btn-primary"));
            verify(locator).click();
        }

        @Test
        @DisplayName("RESOURCE_ID — lanza UnsupportedOperationException (solo mobile)")
        void resourceId_throwsUnsupported() {
            assertThatThrownBy(() -> engine.click(ElementLocator.resourceId("com.app:id/btn")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("RESOURCE_ID");
        }

        @Test
        @DisplayName("locator null — lanza NullPointerException")
        void nullLocator_throws() {
            assertThatThrownBy(() -> engine.click((ElementLocator) null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // type(ElementLocator, String)
    // =========================================================================

    @Nested
    @DisplayName("type(ElementLocator, String)")
    class TypeWithLocator {

        @Test
        @DisplayName("CSS locator — llama fill() con el texto provisto")
        void type_cssLocator_fillsText() {
            Locator locator = mock(Locator.class);
            when(page.locator("#email")).thenReturn(locator);
            engine.type(ElementLocator.css("#email"), "user@example.com");
            verify(locator).fill("user@example.com");
        }

        @Test
        @DisplayName("ID locator — selector con # correcto")
        void type_idLocator_hashPrefix() {
            Locator locator = mock(Locator.class);
            when(page.locator("#password")).thenReturn(locator);
            engine.type(ElementLocator.id("password"), "secret");
            verify(locator).fill("secret");
        }
    }

    // =========================================================================
    // waitFor(ElementLocator, Duration)
    // =========================================================================

    @Nested
    @DisplayName("waitFor(ElementLocator, Duration)")
    class WaitForWithLocator {

        @Test
        @DisplayName("CSS locator — invoca waitForSelector con estado VISIBLE")
        void waitFor_cssLocator_callsWaitForSelector() {
            when(page.waitForSelector(eq(".spinner"), any())).thenReturn(null);
            engine.waitFor(ElementLocator.css(".spinner"), Duration.ofSeconds(5));
            verify(page).waitForSelector(eq(".spinner"), any(Page.WaitForSelectorOptions.class));
        }

        @Test
        @DisplayName("timeout null — lanza NullPointerException")
        void waitFor_nullTimeout_throws() {
            assertThatThrownBy(() -> engine.waitFor(ElementLocator.css(".btn"), null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("timeout cero — lanza IllegalArgumentException")
        void waitFor_zeroTimeout_throws() {
            assertThatThrownBy(() -> engine.waitFor(ElementLocator.css(".btn"), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
        }
    }

    // =========================================================================
    // screenshot(Path)
    // =========================================================================

    @Nested
    @DisplayName("screenshot(Path)")
    class ScreenshotWithPath {

        @Test
        @DisplayName("guarda bytes en la ruta especificada")
        void screenshot_writesToPath(@TempDir Path tmpDir) throws Exception {
            Path dest = tmpDir.resolve("snap.png");
            when(page.screenshot()).thenReturn(new byte[]{10, 20, 30});
            engine.screenshot(dest);
            assertThat(Files.readAllBytes(dest)).containsExactly(10, 20, 30);
        }

        @Test
        @DisplayName("crea directorios padre si no existen")
        void screenshot_createsParentDirectories(@TempDir Path tmpDir) throws Exception {
            Path dest = tmpDir.resolve("sub/dir/snap.png");
            when(page.screenshot()).thenReturn(new byte[]{1});
            engine.screenshot(dest);
            assertThat(dest).exists();
        }

        @Test
        @DisplayName("destination null — lanza NullPointerException")
        void screenshot_nullDestination_throws() {
            assertThatThrownBy(() -> engine.screenshot((Path) null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // getCurrentLocation()
    // =========================================================================

    @Nested
    @DisplayName("getCurrentLocation()")
    class GetCurrentLocation {

        @Test
        @DisplayName("delega a getCurrentUrl()")
        void getCurrentLocation_returnsPageUrl() {
            when(page.url()).thenReturn("https://app.example.com/dashboard");
            assertThat(engine.getCurrentLocation()).isEqualTo("https://app.example.com/dashboard");
        }
    }

    // =========================================================================
    // open() / close() — guardianes sin PlaywrightManager real
    // =========================================================================

    @Nested
    @DisplayName("open() / close() lifecycle guardianes")
    class OpenCloseLifecycle {

        @Test
        @DisplayName("open(null) — lanza NullPointerException antes de tocar PlaywrightManager")
        void open_nullConfig_throws() {
            PlaywrightBrowserEngine uninit = new PlaywrightBrowserEngine();
            assertThatThrownBy(() -> uninit.open(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("open() sobre engine ya inicializado — lanza IllegalStateException")
        void open_alreadyInitialized_throws() {
            ExecutionConfig config = mock(ExecutionConfig.class);
            assertThatThrownBy(() -> engine.open(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está inicializado");
        }

        @Test
        @DisplayName("close() sobre engine sin Page activa — no lanza excepcion")
        void close_noActivePage_doesNotThrow() {
            PlaywrightBrowserEngine uninit = new PlaywrightBrowserEngine();
            assertThatCode(() -> uninit.close()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("no-arg constructor — isActive() retorna false")
        void noArgConstructor_isNotActive() {
            PlaywrightBrowserEngine uninit = new PlaywrightBrowserEngine();
            assertThat(uninit.isActive()).isFalse();
        }
    }
}
