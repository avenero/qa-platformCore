package com.qa.webcore.driver.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PlaywrightManager — PoC scope suite/escenario")
class PlaywrightManagerTest {

    @AfterEach
    void tearDown() {
        PlaywrightManager.resetForTests();
    }

    @Test
    @DisplayName("initSuite es idempotente y crea recursos suite una sola vez")
    void initSuiteEsIdempotente() {
        Playwright playwright = mock(Playwright.class);
        Browser browser = mock(Browser.class);
        AtomicInteger initCalls = new AtomicInteger(0);

        PlaywrightManager.setSuiteInitializerForTesting((browserName, headless) -> {
            initCalls.incrementAndGet();
            return new PlaywrightManager.SuiteResources(playwright, browser);
        });

        PlaywrightManager.initSuite("chromium", true);
        PlaywrightManager.initSuite("chromium", true);

        assertThat(PlaywrightManager.isSuiteInitialized()).isTrue();
        assertThat(initCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("initSuite con config distinta falla si la suite ya fue inicializada")
    void initSuiteConConfigDistintaFalla() {
        Playwright playwright = mock(Playwright.class);
        Browser browser = mock(Browser.class);
        PlaywrightManager.setSuiteInitializerForTesting((browserName, headless) ->
                new PlaywrightManager.SuiteResources(playwright, browser));

        PlaywrightManager.initSuite("chromium", true);

        assertThatThrownBy(() -> PlaywrightManager.initSuite("firefox", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya inicializada");
    }

    @Test
    @DisplayName("startScenario crea contexto/page por hilo y endScenario limpia recursos")
    void startYEndScenarioGestionanScopeCorrecto() {
        Playwright playwright = mock(Playwright.class);
        Browser browser = mock(Browser.class);
        BrowserContext context = mock(BrowserContext.class);
        Page page = mock(Page.class);

        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(context);
        when(context.newPage()).thenReturn(page);

        PlaywrightManager.setSuiteInitializerForTesting((browserName, headless) ->
                new PlaywrightManager.SuiteResources(playwright, browser));

        PlaywrightManager.initSuite("chromium", true);
        PlaywrightManager.startScenario();

        assertThat(PlaywrightManager.getPage()).isSameAs(page);

        PlaywrightManager.endScenario();

        verify(page, times(1)).close();
        verify(context, times(1)).close();
        assertThatThrownBy(PlaywrightManager::getPage)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay Page activa");
    }

    @Test
    @DisplayName("startScenario falla si la suite no fue inicializada")
    void startScenarioSinSuiteFalla() {
        assertThatThrownBy(PlaywrightManager::startScenario)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no inicializada");
    }

    @Test
    @DisplayName("startScenario doble en el mismo hilo falla para evitar leaks")
    void startScenarioDobleFalla() {
        Playwright playwright = mock(Playwright.class);
        Browser browser = mock(Browser.class);
        BrowserContext context = mock(BrowserContext.class);
        Page page = mock(Page.class);

        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(context);
        when(context.newPage()).thenReturn(page);

        PlaywrightManager.setSuiteInitializerForTesting((browserName, headless) ->
                new PlaywrightManager.SuiteResources(playwright, browser));
        PlaywrightManager.initSuite("chromium", true);
        PlaywrightManager.startScenario();

        assertThatThrownBy(PlaywrightManager::startScenario)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un escenario Playwright activo");
    }

    @Test
    @DisplayName("dos escenarios secuenciales crean context/page distintos")
    void dosEscenariosSecuencialesAislados() {
        Playwright playwright = mock(Playwright.class);
        Browser browser = mock(Browser.class);
        BrowserContext context1 = mock(BrowserContext.class);
        BrowserContext context2 = mock(BrowserContext.class);
        Page page1 = mock(Page.class);
        Page page2 = mock(Page.class);

        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(context1, context2);
        when(context1.newPage()).thenReturn(page1);
        when(context2.newPage()).thenReturn(page2);

        PlaywrightManager.setSuiteInitializerForTesting((browserName, headless) ->
                new PlaywrightManager.SuiteResources(playwright, browser));
        PlaywrightManager.initSuite("chromium", true);

        PlaywrightManager.startScenario();
        assertThat(PlaywrightManager.getPage()).isSameAs(page1);
        PlaywrightManager.endScenario();

        PlaywrightManager.startScenario();
        assertThat(PlaywrightManager.getPage()).isSameAs(page2);
        PlaywrightManager.endScenario();

        verify(browser, times(2)).newContext(any(Browser.NewContextOptions.class));
        verify(context1, times(1)).close();
        verify(context2, times(1)).close();
    }

    @Test
    @DisplayName("closeSuite cierra browser y playwright suite-scoped")
    void closeSuiteCierraRecursosSuite() {
        Playwright playwright = mock(Playwright.class);
        Browser browser = mock(Browser.class);

        PlaywrightManager.setSuiteInitializerForTesting((browserName, headless) ->
                new PlaywrightManager.SuiteResources(playwright, browser));

        PlaywrightManager.initSuite("chromium", true);
        PlaywrightManager.closeSuite();

        verify(browser, times(1)).close();
        verify(playwright, times(1)).close();
        assertThat(PlaywrightManager.isSuiteInitialized()).isFalse();
    }

    @Test
    @DisplayName("PoC headless real (opt-in) — chromium inicia en modo CI")
    @EnabledIfSystemProperty(named = "playwright.headless.compatibility", matches = "true")
    void pocHeadlessRealOptIn() {
        PlaywrightManager.useDefaultInitializerForTesting();

        PlaywrightManager.initSuite("chromium", true);
        PlaywrightManager.startScenario();

        assertThat(PlaywrightManager.getPage()).isNotNull();
    }
}
