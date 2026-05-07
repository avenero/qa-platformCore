package com.qa.webcore.driver.engine.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.qa.webcore.driver.engine.BrowserElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PlaywrightBrowserEngine")
class PlaywrightBrowserEngineTest {

    private Page page;
    private PlaywrightBrowserEngine engine;

    @BeforeEach
    void setUp() {
        page = mock(Page.class);
        engine = new PlaywrightBrowserEngine(page);
    }

    @Nested
    @DisplayName("Locators")
    class Locators {

        @Test
        @DisplayName("find normaliza xpath sin prefijo y retorna BrowserElement")
        void findNormalizaXpath() {
            Locator locator = mock(Locator.class);
            Locator first = mock(Locator.class);
            when(page.locator("xpath=//div[@id='main']")).thenReturn(locator);
            when(locator.first()).thenReturn(first);

            BrowserElement element = engine.find("//div[@id='main']");

            assertThat(element).isNotNull();
            verify(page, times(1)).locator("xpath=//div[@id='main']");
            verify(locator, times(1)).first();
        }

        @Test
        @DisplayName("findAll arma lista con nth segun count")
        void findAllArmaLista() {
            Locator collection = mock(Locator.class);
            Locator item0 = mock(Locator.class);
            Locator item1 = mock(Locator.class);
            when(page.locator(".item")).thenReturn(collection);
            when(collection.count()).thenReturn(2);
            when(collection.nth(0)).thenReturn(item0);
            when(collection.nth(1)).thenReturn(item1);

            List<BrowserElement> elements = engine.findAll(".item");

            assertThat(elements).hasSize(2);
            verify(collection, times(1)).count();
            verify(collection, times(1)).nth(0);
            verify(collection, times(1)).nth(1);
        }

        @Test
        @DisplayName("isPresent retorna true cuando count es mayor a cero")
        void isPresent() {
            Locator locator = mock(Locator.class);
            when(page.locator("#status")).thenReturn(locator);
            when(locator.count()).thenReturn(1);

            assertThat(engine.isPresent("#status")).isTrue();
        }
    }

    @Nested
    @DisplayName("Esperas")
    class Esperas {

        @Test
        @DisplayName("waitForVisible delega a waitForSelector con timeout")
        void waitForVisible() {
            engine.waitForVisible("#login", 1_500);

            verify(page, times(1))
                    .waitForSelector(any(String.class), any(Page.WaitForSelectorOptions.class));
        }

        @Test
        @DisplayName("waitForText hace polling hasta encontrar el texto")
        void waitForTextHacePolling() {
            Locator locator = mock(Locator.class);
            when(page.locator("#message")).thenReturn(locator);
            when(locator.textContent()).thenReturn("processing", "still processing", "done");

            engine.waitForText("#message", "done", 1_500);

            verify(locator, atLeast(3)).textContent();
        }

        @Test
        @DisplayName("waitForText falla por timeout si nunca aparece texto")
        void waitForTextTimeout() {
            Locator locator = mock(Locator.class);
            when(page.locator("#message")).thenReturn(locator);
            when(locator.textContent()).thenReturn("processing");

            assertThatThrownBy(() -> engine.waitForText("#message", "done", 250))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Timeout esperando texto");
        }

        @Test
        @DisplayName("waitForUrl delega en waitForURL")
        void waitForUrl() {
            engine.waitForUrl("**/dashboard", 2_000);
            verify(page, times(1))
                    .waitForURL(any(String.class), any(Page.WaitForURLOptions.class));
        }

        @Test
        @DisplayName("waitForLoadState valida valor inválido")
        void waitForLoadStateInvalido() {
            assertThatThrownBy(() -> engine.waitForLoadState("bad-state", 2_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Load state invalido");
        }
    }

    @Nested
    @DisplayName("Frames")
    class Frames {

        @Test
        @DisplayName("findInFrame usa frameLocator según el diseño C-07")
        void findInFrame() {
            FrameLocator frameLocator = mock(FrameLocator.class);
            Locator locator = mock(Locator.class);
            Locator first = mock(Locator.class);
            when(page.frameLocator("iframe#payment")).thenReturn(frameLocator);
            when(frameLocator.locator("#pay")).thenReturn(locator);
            when(locator.first()).thenReturn(first);

            BrowserElement element = engine.findInFrame("iframe#payment", "#pay");

            assertThat(element).isNotNull();
            verify(page, times(1)).frameLocator("iframe#payment");
            verify(frameLocator, times(1)).locator("#pay");
            verify(locator, times(1)).first();
        }

        @Test
        @DisplayName("clickInFrame hace click sobre locator dentro del frame")
        void clickInFrame() {
            FrameLocator frameLocator = mock(FrameLocator.class);
            Locator locator = mock(Locator.class);
            when(page.frameLocator("iframe#payment")).thenReturn(frameLocator);
            when(frameLocator.locator("#confirm")).thenReturn(locator);

            engine.clickInFrame("iframe#payment", "#confirm");

            verify(locator, times(1)).click();
        }
    }

    @Nested
    @DisplayName("Ventanas")
    class Ventanas {

        @Test
        @DisplayName("switchToNewWindow cambia a la última page del contexto")
        void switchToNewWindow() {
            BrowserContext context = mock(BrowserContext.class);
            Page secondPage = mock(Page.class);
            when(page.context()).thenReturn(context);
            when(context.pages()).thenReturn(List.of(page, secondPage));

            engine.switchToNewWindow();

            when(secondPage.title()).thenReturn("Popup");
            assertThat(engine.getWindowTitle()).isEqualTo("Popup");
        }
    }
}
