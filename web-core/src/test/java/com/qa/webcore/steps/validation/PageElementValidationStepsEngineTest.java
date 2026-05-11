package com.qa.webcore.steps.validation;


import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.internal.runtime.ServiceRegistry;
import com.qa.webcore.driver.engine.BrowserElement;
import com.qa.webcore.driver.engine.BrowserEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Page/Element validation steps with BrowserEngine")
class PageElementValidationStepsEngineTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("ElementValidation usa isPresent y find().getText()")
    void elementValidationUsesEngine() {
        RecordingValidationEngine engine = new RecordingValidationEngine();
        engine.present = true;
        engine.text = "Hola Mundo";
        activateContext(engine);

        ElementValidationSteps steps = new ElementValidationSteps();
        steps.verificoSiExisteElElemento("#greeting");
        steps.verificoQueElTextoEnSea("#greeting", "Hola Mundo");

        assertThat(engine.isPresentCalls.get()).isGreaterThanOrEqualTo(1);
        assertThat(engine.findCalls.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("PageValidation usa getCurrentUrl y texto de body")
    void pageValidationUsesEngine() {
        RecordingValidationEngine engine = new RecordingValidationEngine();
        engine.currentUrl = "https://example.com/dashboard";
        engine.text = "Bienvenido dashboard";
        activateContext(engine);

        PageValidationSteps steps = new PageValidationSteps();
        steps.validoUrl("https://example.com/dashboard");
        steps.validoQueExistaElTexto("dashboard");

        assertThat(engine.urlCalls.get()).isEqualTo(1);
        assertThat(engine.findCalls.get()).isGreaterThanOrEqualTo(1);
    }

    private static void activateContext(BrowserEngine engine) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerInstance(BrowserEngine.class, engine);
        ExecutionContext.builder().registry(registry).build().activate();
    }

    private static final class RecordingValidationEngine implements BrowserEngine {
        private final AtomicInteger isPresentCalls = new AtomicInteger();
        private final AtomicInteger findCalls = new AtomicInteger();
        private final AtomicInteger urlCalls = new AtomicInteger();
        private boolean present;
        private String text = "";
        private String currentUrl = "";

        @Override public boolean isPresent(String selector) { isPresentCalls.incrementAndGet(); return present; }
        @Override public BrowserElement find(String selector) {
            findCalls.incrementAndGet();
            return new BrowserElement() {
                @Override public String getText() { return text; }
                @Override public String getAttribute(String name) { return ""; }
                @Override public boolean isVisible() { return true; }
                @Override public boolean isEnabled() { return true; }
                @Override public boolean isSelected() { return false; }
                @Override public void click() {}
                @Override public void type(String text) {}
            };
        }
        @Override public String getCurrentUrl() { urlCalls.incrementAndGet(); return currentUrl; }
        @Override public List<BrowserElement> findAll(String selector) { return List.of(); }
        @Override public void click(String selector) {}
        @Override public void type(String selector, String text) {}
        @Override public void clear(String selector) {}
        @Override public void selectOption(String selector, String value) {}
        @Override public void hover(String selector) {}
        @Override public void doubleClick(String selector) {}
        @Override public void navigateTo(String url) {}
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
        @Override public void open(com.qa.common.api.runtime.ExecutionConfig config) {}
        @Override public void close() {}
    }
}
