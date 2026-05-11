package com.qa.webcore.steps.interaction;


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

@DisplayName("Alert and DragDrop steps with BrowserEngine")
class AlertDragDropStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("AlertSteps delega accept y dismiss al engine")
    void alertDelegates() {
        RecordingInteractionEngine engine = new RecordingInteractionEngine();
        activateContext(engine);
        AlertSteps steps = new AlertSteps();

        steps.aceptoLaAlerta();
        steps.rechazoLaAlerta();

        assertThat(engine.acceptAlertCalls.get()).isEqualTo(1);
        assertThat(engine.dismissAlertCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("DragDropSteps usa evaluate para drag/drop y resize")
    void dragDropUsesEvaluate() {
        RecordingInteractionEngine engine = new RecordingInteractionEngine();
        activateContext(engine);
        DragDropSteps steps = new DragDropSteps();

        steps.arrastroElementoHacia("#source", "#target");
        steps.redimensionoElemento("#box", 400, 220);

        assertThat(engine.evaluateCalls.get()).isEqualTo(2);
        assertThat(engine.lastScript).contains("style.height");
    }

    private static void activateContext(BrowserEngine engine) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext.builder().registry(registry).build().activate();
    }

    private static final class RecordingInteractionEngine implements BrowserEngine {
        private final AtomicInteger acceptAlertCalls = new AtomicInteger();
        private final AtomicInteger dismissAlertCalls = new AtomicInteger();
        private final AtomicInteger evaluateCalls = new AtomicInteger();
        private String lastScript;

        @Override public void acceptAlert() { acceptAlertCalls.incrementAndGet(); }
        @Override public void dismissAlert() { dismissAlertCalls.incrementAndGet(); }
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
        @Override public boolean isActive() { return true; }
        @Override public void open(com.qa.common.api.runtime.ExecutionConfig config) {}
        @Override public void close() {}
    }
}
