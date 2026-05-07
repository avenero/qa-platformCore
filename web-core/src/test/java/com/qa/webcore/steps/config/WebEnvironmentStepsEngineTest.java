package com.qa.webcore.steps.config;

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

@DisplayName("WebEnvironmentSteps with BrowserEngine")
class WebEnvironmentStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("seteo cookie usa evaluate cuando hay ExecutionContext activo")
    void seteoCookieUsaEvaluate() {
        RecordingEnvironmentEngine engine = new RecordingEnvironmentEngine();
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext.builder().registry(registry).build().activate();

        new WebEnvironmentSteps().seteoCookie("token", "abc123");

        assertThat(engine.evaluateCalls.get()).isEqualTo(1);
        assertThat(engine.lastScript).contains("document.cookie");
    }

    private static final class RecordingEnvironmentEngine implements BrowserEngine {
        private final AtomicInteger evaluateCalls = new AtomicInteger();
        private String lastScript;

        @Override public Object evaluate(String script, Object... args) { evaluateCalls.incrementAndGet(); lastScript = script; return null; }
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
    }
}
