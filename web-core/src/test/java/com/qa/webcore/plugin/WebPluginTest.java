package com.qa.webcore.plugin;

import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebPlugin Playwright-only")
class WebPluginTest {

    private final WebPlugin plugin = new WebPlugin();

    @AfterEach
    void tearDown() {
        WebPlugin.resetLifecycleHooksForTesting();
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("Metadata del plugin — identidad v3.0.0")
    void metadataBasica() {
        assertThat(plugin.platformId()).isEqualTo("WEB");
        assertThat(plugin.displayName()).isEqualTo("Web Browser Testing");
        assertThat(plugin.getOrder()).isEqualTo(100);
        assertThat(plugin.getActivationTags()).containsExactlyInAnyOrder("@web", "@ui", "@browser", "@playwright");
    }

    @Test
    @DisplayName("describeCapabilities() retorna 3 browsers Playwright")
    void describeCapabilitiesRetorna3Browsers() {
        var report = plugin.describeCapabilities();
        assertThat(report.available()).isTrue();
        assertThat(report.platformId()).isEqualTo("WEB");
        assertThat(report.options()).extracting("id")
            .containsExactlyInAnyOrder("chromium", "firefox", "webkit");
    }

    @Test
    @DisplayName("registerServices registra WebHelper lazy")
    void registerServicesRegistraWebHelper() {
        ServiceRegistry registry = new ServiceRegistry();
        plugin.registerServices(registry, new ExecutionConfig.Builder().build());

        assertThat(registry.isRegistered(WebHelper.class)).isTrue();
        assertThat(registry.get(WebHelper.class)).isPresent();
    }

    @Test
    @DisplayName("onScenarioStart inicializa BrowserEngine Playwright")
    void onScenarioStartInicializaPlaywright() {
        plugin.setPlaywrightScenarioInitializerForTesting((ctx, browser, headless) ->
                ctx.registry().registerInstance(BrowserEngine.class, new NoOpBrowserEngine()));
        ExecutionContext context = ExecutionContext.builder()
                .registry(new ServiceRegistry())
                .config(new ExecutionConfig.Builder().property("playwright.browser", "chromium").build())
                .build();

        plugin.onScenarioStart(context);

        assertThat(context.registry().isRegistered(BrowserEngine.class)).isTrue();
    }

    @Test
    @DisplayName("onScenarioEnd ejecuta terminador")
    void onScenarioEndEjecutaTerminator() {
        AtomicBoolean called = new AtomicBoolean(false);
        plugin.setScenarioTerminatorForTesting(ctx -> called.set(true));
        ExecutionContext context = ExecutionContext.builder()
                .registry(new ServiceRegistry())
                .config(new ExecutionConfig.Builder().build())
                .build();

        plugin.onScenarioEnd(context);

        assertThat(called).isTrue();
    }

    @Test
    @DisplayName("getGluePackages incluye paquete raíz para hooks/variables")
    void getGluePackagesIncluyePaqueteRaiz() {
        assertThat(plugin.getGluePackages())
                .contains("com.qa.webcore.steps");
    }

    private static final class NoOpBrowserEngine implements BrowserEngine {
        @Override public com.qa.webcore.driver.engine.BrowserElement find(String selector) { throw new UnsupportedOperationException(); }
        @Override public java.util.List<com.qa.webcore.driver.engine.BrowserElement> findAll(String selector) { return java.util.List.of(); }
        @Override public boolean isPresent(String selector) { return false; }
        @Override public void click(String selector) { }
        @Override public void type(String selector, String text) { }
        @Override public void clear(String selector) { }
        @Override public void selectOption(String selector, String value) { }
        @Override public void hover(String selector) { }
        @Override public void doubleClick(String selector) { }
        @Override public void navigateTo(String url) { }
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
        @Override public void back() { }
        @Override public void refresh() { }
        @Override public void waitForVisible(String selector, int timeoutMs) { }
        @Override public void waitForText(String selector, String text, int timeoutMs) { }
        @Override public void waitForHidden(String selector, int timeoutMs) { }
        @Override public void waitForUrl(String urlPattern, int timeoutMs) { }
        @Override public void waitForLoadState(String state, int timeoutMs) { }
        @Override public com.qa.webcore.driver.engine.BrowserElement findInFrame(String frameSelector, String elementSelector) { throw new UnsupportedOperationException(); }
        @Override public void clickInFrame(String frameSelector, String elementSelector) { }
        @Override public byte[] screenshot() { return new byte[0]; }
        @Override public byte[] screenshotElement(String selector) { return new byte[0]; }
        @Override public Object evaluate(String script, Object... args) { return null; }
        @Override public void scrollIntoView(String selector) { }
        @Override public void switchToNewWindow() { }
        @Override public String getWindowTitle() { return ""; }
        @Override public void closeCurrentWindow() { }
        @Override public String getAlertText() { return ""; }
        @Override public void acceptAlert() { }
        @Override public void dismissAlert() { }
        @Override public boolean isActive() { return true; }
        @Override public void open(com.qa.common.runtime.ExecutionConfig config) {}
        @Override public void close() {}
    }
}
