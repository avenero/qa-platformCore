package com.qa.webcore.steps.navigation;

import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.webcore.driver.engine.BrowserElement;
import com.qa.webcore.driver.engine.BrowserEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FrameSteps with BrowserEngine")
class FrameStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("cambio frame por css usa findInFrame y guarda selector en VariableStore")
    void cambioFrameCss() {
        RecordingFrameEngine engine = new RecordingFrameEngine();
        ExecutionContext ctx = activateContext(engine);

        new FrameSteps().seleccionoElIframeConAtributoCss("iframe#main");

        assertThat(engine.findInFrameCalls.get()).isEqualTo(1);
        assertThat(engine.lastFrameSelector).isEqualTo("iframe#main");
        assertThat(ctx.variables().get("web.frame.current.selector", String.class))
                .contains("iframe#main");
    }

    @Test
    @DisplayName("cambio frame por path xpath normaliza selector")
    void cambioFramePathXpath() {
        RecordingFrameEngine engine = new RecordingFrameEngine();
        activateContext(engine);

        new FrameSteps().cambioIFramePath("//iframe[@id='billing']");

        assertThat(engine.lastFrameSelector).isEqualTo("xpath=//iframe[@id='billing']");
    }

    @Test
    @DisplayName("inicializo iframe principal limpia selector activo")
    void limpiarFrameActivo() {
        RecordingFrameEngine engine = new RecordingFrameEngine();
        ExecutionContext ctx = activateContext(engine);
        ctx.variables().set("web.frame.current.selector", "iframe#temp");

        new FrameSteps().inicializarIframePrincipal();

        assertThat(ctx.variables().has("web.frame.current.selector")).isFalse();
    }

    private static ExecutionContext activateContext(BrowserEngine engine) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
        ctx.activate();
        return ctx;
    }

    private static final class RecordingFrameEngine implements BrowserEngine {
        private final AtomicInteger findInFrameCalls = new AtomicInteger();
        private String lastFrameSelector;

        @Override
        public BrowserElement findInFrame(String frameSelector, String elementSelector) {
            findInFrameCalls.incrementAndGet();
            lastFrameSelector = frameSelector;
            return new BrowserElement() {
                @Override public String getText() { return ""; }
                @Override public String getAttribute(String name) { return ""; }
                @Override public boolean isVisible() { return true; }
                @Override public boolean isEnabled() { return true; }
                @Override public boolean isSelected() { return false; }
                @Override public void click() {}
                @Override public void type(String text) {}
            };
        }

        @Override public BrowserElement find(String selector) { throw new UnsupportedOperationException(); }
        @Override public List<BrowserElement> findAll(String selector) { return List.of(); }
        @Override public boolean isPresent(String selector) { return false; }
        @Override public void click(String selector) {}
        @Override public void type(String selector, String text) {}
        @Override public void clear(String selector) {}
        @Override public void selectOption(String selector, String value) {}
        @Override public void hover(String selector) {}
        @Override public void doubleClick(String selector) {}
        @Override public void navigateTo(String url) {}
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
        @Override public void back() {}
        @Override public void refresh() {}
        @Override public void waitForVisible(String selector, int timeoutMs) {}
        @Override public void waitForText(String selector, String text, int timeoutMs) {}
        @Override public void waitForHidden(String selector, int timeoutMs) {}
        @Override public void waitForUrl(String urlPattern, int timeoutMs) {}
        @Override public void waitForLoadState(String state, int timeoutMs) {}
        @Override public void clickInFrame(String frameSelector, String elementSelector) {}
        @Override public byte[] screenshot() { return new byte[0]; }
        @Override public byte[] screenshotElement(String selector) { return new byte[0]; }
        @Override public Object evaluate(String script, Object... args) { return null; }
        @Override public void scrollIntoView(String selector) {}
        @Override public void switchToNewWindow() {}
        @Override public String getWindowTitle() { return ""; }
        @Override public void closeCurrentWindow() {}
        @Override public String getAlertText() { return ""; }
        @Override public void acceptAlert() {}
        @Override public void dismissAlert() {}
        @Override public boolean isActive() { return true; }
        @Override public void open(com.qa.common.runtime.ExecutionConfig config) {}
        @Override public void close() {}
    }
}
