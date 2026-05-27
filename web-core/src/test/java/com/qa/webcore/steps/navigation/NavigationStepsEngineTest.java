package com.qa.webcore.steps.navigation;


import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.ServiceRegistry;
import com.qa.webcore.driver.engine.BrowserElement;
import com.qa.webcore.driver.engine.BrowserEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NavigationSteps with BrowserEngine")
class NavigationStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("actualizo URL usa navigateTo + waitForLoadState")
    void actualizoUrlUsaEngine() {
        RecordingEngine engine = new RecordingEngine();
        activateContext(engine);

        new NavigationSteps().actualizoUrlEnElNavegador("https://example.com");

        assertThat(engine.navigateCalls.get()).isEqualTo(1);
        assertThat(engine.waitLoadStateCalls.get()).isEqualTo(1);
        assertThat(engine.lastUrl).isEqualTo("https://example.com");
        assertThat(engine.lastLoadState).isEqualTo("load");
    }

    @Test
    @DisplayName("navego hacia adelante usa evaluate history.forward")
    void forwardUsaEvaluate() {
        RecordingEngine engine = new RecordingEngine();
        activateContext(engine);

        new NavigationSteps().navegoHaciaAdelante();

        assertThat(engine.evaluateCalls.get()).isEqualTo(1);
        assertThat(engine.lastScript).contains("history.forward");
    }

    private static void activateContext(BrowserEngine engine) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext.builder().registry(registry).build().activate();
    }

    private static final class RecordingEngine implements BrowserEngine {
        private final AtomicInteger navigateCalls = new AtomicInteger();
        private final AtomicInteger waitLoadStateCalls = new AtomicInteger();
        private final AtomicInteger evaluateCalls = new AtomicInteger();
        private String lastUrl;
        private String lastLoadState;
        private String lastScript;

        @Override public void navigateTo(String url) { navigateCalls.incrementAndGet(); lastUrl = url; }
        @Override public void waitForLoadState(String state, int timeoutMs) { waitLoadStateCalls.incrementAndGet(); lastLoadState = state; }
        @Override public Object evaluate(String script, Object... args) { evaluateCalls.incrementAndGet(); lastScript = script; return null; }
        @Override public void refresh() {}
        @Override public void back() {}
        @Override public BrowserElement find(String selector) { throw new UnsupportedOperationException(); }
        @Override public List<BrowserElement> findAll(String selector) { return List.of(); }
        @Override public boolean isPresent(String selector) { return false; }
        @Override public void click(String selector) {}
        @Override public void type(String selector, String text) {}
        @Override public void clear(String selector) {}
        @Override public void selectOption(String selector, String value) {}
        @Override public void hover(String selector) {}
        @Override public void doubleClick(String selector) {}
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
        @Override public void waitForVisible(String selector, int timeoutMs) {}
        @Override public void waitForText(String selector, String text, int timeoutMs) {}
        @Override public void waitForHidden(String selector, int timeoutMs) {}
        @Override public void waitForUrl(String urlPattern, int timeoutMs) {}
        @Override public BrowserElement findInFrame(String frameSelector, String elementSelector) { throw new UnsupportedOperationException(); }
        @Override public void clickInFrame(String frameSelector, String elementSelector) {}
        @Override public byte[] screenshot() { return new byte[0]; }
        @Override public byte[] screenshotElement(String selector) { return new byte[0]; }
        @Override public void scrollIntoView(String selector) {}
        @Override public void switchToNewWindow() {}
        @Override public String getWindowTitle() { return ""; }
        @Override public void closeCurrentWindow() {}
        @Override public String getAlertText() { return ""; }
        @Override public void acceptAlert() {}
        @Override public void dismissAlert() {}
        @Override public boolean isActive() { return true; }
        @Override public void uploadFile(String selector, String filePath) {}
        @Override public void open(com.qa.common.api.runtime.ExecutionConfig config) {}
        @Override public void close() {}
    }
}
