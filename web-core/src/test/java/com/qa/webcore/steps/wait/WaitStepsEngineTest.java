package com.qa.webcore.steps.wait;


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

@DisplayName("WaitSteps with BrowserEngine")
class WaitStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("esperar visible usa waitForVisible")
    void waitVisibleUsesEngine() {
        RecordingWaitEngine engine = new RecordingWaitEngine();
        activateContext(engine);

        new WaitSteps().esperarHastaQueElementoEsteVisible("#login");

        assertThat(engine.waitVisibleCalls.get()).isEqualTo(1);
        assertThat(engine.lastSelector).isEqualTo("#login");
    }

    @Test
    @DisplayName("esperar habilitado hace polling con find().isEnabled()")
    void waitEnabledUsesFindPolling() {
        RecordingWaitEngine engine = new RecordingWaitEngine();
        engine.enabledSequence = new boolean[]{false, true};
        activateContext(engine);

        new WaitSteps().esperarHastaQueElementoEsteHabilitado("#submit");

        assertThat(engine.findCalls.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("esperar texto visible usa waitForText en body")
    void waitTextUsesEngine() {
        RecordingWaitEngine engine = new RecordingWaitEngine();
        activateContext(engine);

        new WaitSteps().esperarHastaQueElTextoSeaVisible("Bienvenido");

        assertThat(engine.waitTextCalls.get()).isEqualTo(1);
        assertThat(engine.lastSelector).isEqualTo("body");
        assertThat(engine.lastText).isEqualTo("Bienvenido");
    }

    private static void activateContext(BrowserEngine engine) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext.builder().registry(registry).build().activate();
    }

    private static final class RecordingWaitEngine implements BrowserEngine {
        private final AtomicInteger waitVisibleCalls = new AtomicInteger();
        private final AtomicInteger waitTextCalls = new AtomicInteger();
        private final AtomicInteger findCalls = new AtomicInteger();
        private String lastSelector;
        private String lastText;
        private boolean[] enabledSequence = new boolean[]{true};
        private int enabledIndex = 0;

        @Override
        public BrowserElement find(String selector) {
            findCalls.incrementAndGet();
            return new BrowserElement() {
                @Override public String getText() { return ""; }
                @Override public String getAttribute(String name) { return ""; }
                @Override public boolean isVisible() { return true; }
                @Override public boolean isEnabled() {
                    int index = Math.min(enabledIndex, enabledSequence.length - 1);
                    boolean value = enabledSequence[index];
                    enabledIndex++;
                    return value;
                }
                @Override public boolean isSelected() { return false; }
                @Override public void click() {}
                @Override public void type(String text) {}
            };
        }

        @Override
        public void waitForVisible(String selector, int timeoutMs) {
            waitVisibleCalls.incrementAndGet();
            lastSelector = selector;
        }

        @Override
        public void waitForText(String selector, String text, int timeoutMs) {
            waitTextCalls.incrementAndGet();
            lastSelector = selector;
            lastText = text;
        }

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
        @Override public void waitForHidden(String selector, int timeoutMs) {}
        @Override public void waitForUrl(String urlPattern, int timeoutMs) {}
        @Override public void waitForLoadState(String state, int timeoutMs) {}
        @Override public BrowserElement findInFrame(String frameSelector, String elementSelector) { throw new UnsupportedOperationException(); }
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
        @Override public void uploadFile(String selector, String filePath) {}
        @Override public void open(com.qa.common.api.runtime.ExecutionConfig config) {}
        @Override public void close() {}
    }
}
