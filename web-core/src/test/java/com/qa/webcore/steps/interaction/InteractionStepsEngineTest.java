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

@DisplayName("Interaction steps with BrowserEngine")
class InteractionStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("ClickSteps delega click/hover/doubleClick al engine")
    void clickStepsDeleganAlEngine() {
        RecordingInteractionEngine engine = new RecordingInteractionEngine();
        activateContext(engine);
        ClickSteps steps = new ClickSteps();

        steps.presionoElBoton("#save");
        steps.situoElCursorDelMouseSobreElElemento("#save");
        steps.hagoDobleClicEnElElemento("#save");

        assertThat(engine.clickCalls.get()).isEqualTo(1);
        assertThat(engine.hoverCalls.get()).isEqualTo(1);
        assertThat(engine.doubleClickCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("InputSteps delega type y clear al engine")
    void inputStepsDeleganAlEngine() {
        RecordingInteractionEngine engine = new RecordingInteractionEngine();
        activateContext(engine);
        InputSteps steps = new InputSteps();

        steps.ingresoElTextoEnElElemento("abel", "#username");
        steps.limpioElCampo("#username");

        assertThat(engine.typeCalls.get()).isEqualTo(1);
        assertThat(engine.clearCalls.get()).isEqualTo(1);
        assertThat(engine.lastSelector).isEqualTo("#username");
    }

    @Test
    @DisplayName("SelectSteps delega selectOption al engine")
    void selectStepsDeleganAlEngine() {
        RecordingInteractionEngine engine = new RecordingInteractionEngine();
        activateContext(engine);
        SelectSteps steps = new SelectSteps();

        steps.seleccionoElTextoEnElCombobox("AR", "#country");

        assertThat(engine.selectOptionCalls.get()).isEqualTo(1);
        assertThat(engine.lastSelector).isEqualTo("#country");
        assertThat(engine.lastValue).isEqualTo("AR");
    }

    private static void activateContext(BrowserEngine engine) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext.builder().registry(registry).build().activate();
    }

    private static final class RecordingInteractionEngine implements BrowserEngine {
        private final AtomicInteger clickCalls = new AtomicInteger();
        private final AtomicInteger hoverCalls = new AtomicInteger();
        private final AtomicInteger doubleClickCalls = new AtomicInteger();
        private final AtomicInteger typeCalls = new AtomicInteger();
        private final AtomicInteger clearCalls = new AtomicInteger();
        private final AtomicInteger selectOptionCalls = new AtomicInteger();
        private String lastSelector;
        private String lastValue;

        @Override public void click(String selector) { clickCalls.incrementAndGet(); lastSelector = selector; }
        @Override public void hover(String selector) { hoverCalls.incrementAndGet(); lastSelector = selector; }
        @Override public void doubleClick(String selector) { doubleClickCalls.incrementAndGet(); lastSelector = selector; }
        @Override public void type(String selector, String text) { typeCalls.incrementAndGet(); lastSelector = selector; lastValue = text; }
        @Override public void clear(String selector) { clearCalls.incrementAndGet(); lastSelector = selector; }
        @Override public void selectOption(String selector, String value) { selectOptionCalls.incrementAndGet(); lastSelector = selector; lastValue = value; }
        @Override public boolean isPresent(String selector) { return true; }
        @Override public BrowserElement find(String selector) { throw new UnsupportedOperationException(); }
        @Override public List<BrowserElement> findAll(String selector) { return List.of(); }
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
