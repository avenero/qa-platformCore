package com.qa.webcore.driver.engine.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.qa.common.api.driver.ElementLocator;
import com.qa.common.api.driver.UiDriver;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.webcore.config.WebConfigKeys;
import com.qa.webcore.driver.engine.BrowserElement;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.driver.playwright.PlaywrightManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Implementación de {@link BrowserEngine} sobre Playwright-Java.
 */
public final class PlaywrightBrowserEngine implements BrowserEngine {

    private static final long WAIT_POLL_INTERVAL_MS = 100L;

    private volatile Page currentPage;

    /** Constructor para uso directo con una {@link Page} ya inicializada (inyección en tests). */
    public PlaywrightBrowserEngine(Page page) {
        this.currentPage = Objects.requireNonNull(page, "page no puede ser null");
    }

    /** Constructor de ciclo de vida — requiere llamar a {@link #open(ExecutionConfig)} antes de usar. */
    public PlaywrightBrowserEngine() {
        this.currentPage = null;
    }

    // =========================================================================
    // UiDriver lifecycle
    // =========================================================================

    @Override
    public void open(ExecutionConfig config) {
        Objects.requireNonNull(config, "config no puede ser null");
        if (currentPage != null) {
            throw new IllegalStateException(
                "PlaywrightBrowserEngine ya está inicializado. Ejecuta close() antes de abrir.");
        }
        String browser = config.getProperty(WebConfigKeys.BROWSER, "chromium");
        boolean headless = Boolean.parseBoolean(
            config.getProperty(WebConfigKeys.BROWSER_HEADLESS, "true"));
        PlaywrightManager.initSuite(browser, headless);
        PlaywrightManager.startScenario();
        this.currentPage = PlaywrightManager.getPage();
    }

    @Override
    public void close() {
        try {
            PlaywrightManager.endScenario();
        } finally {
            this.currentPage = null;
        }
    }

    @Override
    public BrowserElement find(String selector) {
        return wrap(currentPage().locator(normalizeSelector(selector)).first());
    }

    @Override
    public List<BrowserElement> findAll(String selector) {
        Locator locator = currentPage().locator(normalizeSelector(selector));
        int count = locator.count();
        List<BrowserElement> elements = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            elements.add(wrap(locator.nth(i)));
        }
        return List.copyOf(elements);
    }

    @Override
    public boolean isPresent(String selector) {
        return currentPage().locator(normalizeSelector(selector)).count() > 0;
    }

    @Override
    public void click(String selector) {
        currentPage().locator(normalizeSelector(selector)).click();
    }

    @Override
    public void type(String selector, String text) {
        currentPage().locator(normalizeSelector(selector)).fill(text);
    }

    @Override
    public void clear(String selector) {
        currentPage().locator(normalizeSelector(selector)).clear();
    }

    @Override
    public void selectOption(String selector, String value) {
        currentPage().locator(normalizeSelector(selector)).selectOption(value);
    }

    @Override
    public void hover(String selector) {
        currentPage().locator(normalizeSelector(selector)).hover();
    }

    @Override
    public void doubleClick(String selector) {
        currentPage().locator(normalizeSelector(selector)).dblclick();
    }

    @Override
    public void navigateTo(String url) {
        currentPage().navigate(requireNonBlank(url, "url no puede ser null/vacia"));
    }

    @Override
    public String getCurrentUrl() {
        return currentPage().url();
    }

    @Override
    public String getTitle() {
        return currentPage().title();
    }

    @Override
    public void back() {
        currentPage().goBack();
    }

    @Override
    public void refresh() {
        currentPage().reload();
    }

    @Override
    public void waitForVisible(String selector, int timeoutMs) {
        currentPage().waitForSelector(
                normalizeSelector(selector),
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(asPlaywrightTimeout(timeoutMs))
        );
    }

    @Override
    public void waitForText(String selector, String text, int timeoutMs) {
        String normalizedSelector = normalizeSelector(selector);
        String expectedText = requireNonBlank(text, "text no puede ser null/vacio");
        Locator locator = currentPage().locator(normalizedSelector);

        waitUntil(timeoutMs, () -> {
            try {
                String actual = locator.textContent();
                return actual != null && actual.contains(expectedText);
            } catch (RuntimeException ignored) {
                return false;
            }
        }, "Timeout esperando texto '" + expectedText + "' en selector '" + normalizedSelector + "'");
    }

    @Override
    public void waitForHidden(String selector, int timeoutMs) {
        currentPage().waitForSelector(
                normalizeSelector(selector),
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(asPlaywrightTimeout(timeoutMs))
        );
    }

    @Override
    public void waitForUrl(String urlPattern, int timeoutMs) {
        currentPage().waitForURL(
                requireNonBlank(urlPattern, "urlPattern no puede ser null/vacio"),
                new Page.WaitForURLOptions().setTimeout(asPlaywrightTimeout(timeoutMs))
        );
    }

    @Override
    public void waitForLoadState(String state, int timeoutMs) {
        currentPage().waitForLoadState(
                parseLoadState(state),
                new Page.WaitForLoadStateOptions().setTimeout(asPlaywrightTimeout(timeoutMs))
        );
    }

    @Override
    public BrowserElement findInFrame(String frameSelector, String elementSelector) {
        FrameLocator frame = currentPage().frameLocator(normalizeSelector(frameSelector));
        return wrap(frame.locator(normalizeSelector(elementSelector)).first());
    }

    @Override
    public void clickInFrame(String frameSelector, String elementSelector) {
        FrameLocator frame = currentPage().frameLocator(normalizeSelector(frameSelector));
        frame.locator(normalizeSelector(elementSelector)).click();
    }

    @Override
    public byte[] screenshot() {
        return currentPage().screenshot();
    }

    @Override
    public byte[] screenshotElement(String selector) {
        return currentPage().locator(normalizeSelector(selector)).screenshot();
    }

    @Override
    public Object evaluate(String script, Object... args) {
        String nonBlankScript = requireNonBlank(script, "script no puede ser null/vacio");
        if (args == null || args.length == 0) {
            return currentPage().evaluate(nonBlankScript);
        }
        if (args.length == 1) {
            return currentPage().evaluate(nonBlankScript, args[0]);
        }
        return currentPage().evaluate(nonBlankScript, Arrays.asList(args));
    }

    @Override
    public void scrollIntoView(String selector) {
        currentPage().locator(normalizeSelector(selector)).scrollIntoViewIfNeeded();
    }

    @Override
    public void switchToNewWindow() {
        BrowserContext context = currentPage().context();
        List<Page> pages = context.pages();
        if (pages.size() < 2) {
            throw new IllegalStateException("No hay una nueva ventana disponible para cambiar.");
        }
        this.currentPage = pages.get(pages.size() - 1);
    }

    @Override
    public String getWindowTitle() {
        return currentPage().title();
    }

    @Override
    public void closeCurrentWindow() {
        Page page = currentPage();
        BrowserContext context = page.context();
        page.close();
        List<Page> pages = context.pages();
        if (pages.isEmpty()) {
            throw new IllegalStateException("No quedan ventanas activas luego de cerrar la actual.");
        }
        this.currentPage = pages.get(pages.size() - 1);
    }

    @Override
    public String getAlertText() {
        throw new UnsupportedOperationException(
                "Playwright gestiona dialogs de forma asincrona."
                        + " Se implementa en la fase de migracion de AlertComponent.");
    }

    @Override
    public void acceptAlert() {
        throw new UnsupportedOperationException(
                "Playwright gestiona dialogs de forma asincrona."
                        + " Se implementa en la fase de migracion de AlertComponent.");
    }

    @Override
    public void dismissAlert() {
        throw new UnsupportedOperationException(
                "Playwright gestiona dialogs de forma asincrona."
                        + " Se implementa en la fase de migracion de AlertComponent.");
    }

    @Override
    public boolean isActive() {
        return currentPage != null;
    }

    // =========================================================================
    // UiDriver — ElementLocator overrides (directos a Playwright, sin puente string)
    // =========================================================================

    @Override
    public void click(ElementLocator locator) {
        currentPage().locator(toPlaywrightSelector(locator)).click();
    }

    @Override
    public void type(ElementLocator locator, String text) {
        currentPage().locator(toPlaywrightSelector(locator)).fill(text);
    }

    @Override
    public void waitFor(ElementLocator locator, Duration timeout) {
        Objects.requireNonNull(timeout, "timeout no puede ser null");
        long ms = timeout.toMillis();
        if (ms <= 0) {
            throw new IllegalArgumentException("timeout debe ser positivo");
        }
        currentPage().waitForSelector(
            toPlaywrightSelector(locator),
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout((double) ms)
        );
    }

    @Override
    public void screenshot(Path destination) {
        Objects.requireNonNull(destination, "destination no puede ser null");
        byte[] bytes = screenshot();
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(destination, bytes);
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar la captura en: " + destination, ex);
        }
    }

    @Override
    public String getCurrentLocation() {
        return getCurrentUrl();
    }

    @Override
    public UiDriver.DriverType getType() {
        return UiDriver.DriverType.WEB;
    }

    private PlaywrightBrowserElement wrap(Locator locator) {
        return new PlaywrightBrowserElement(locator);
    }

    private Page currentPage() {
        Page page = this.currentPage;
        if (page == null) {
            throw new IllegalStateException("No hay Page activa en PlaywrightBrowserEngine.");
        }
        return page;
    }

    private static String toPlaywrightSelector(ElementLocator locator) {
        Objects.requireNonNull(locator, "locator no puede ser null");
        return switch (locator.strategy()) {
            case CSS              -> locator.value();
            case XPATH            -> locator.value().startsWith("xpath=")
                                     ? locator.value() : "xpath=" + locator.value();
            case ID               -> "#" + locator.value();
            case ACCESSIBILITY_ID -> "[data-testid='" + locator.value() + "']";
            case TEXT             -> "text=" + locator.value();
            case ROLE             -> "role=" + locator.value();
            case NAME             -> "[name='" + locator.value() + "']";
            case CLASS_NAME       -> "." + locator.value();
            case RESOURCE_ID      -> throw new UnsupportedOperationException(
                                     "RESOURCE_ID es exclusivo de mobile — no soportado en PlaywrightBrowserEngine");
        };
    }

    private static String normalizeSelector(String selector) {
        String value = requireNonBlank(selector, "selector no puede ser null/vacio").trim();
        if (value.startsWith("xpath=")) {
            return value;
        }
        if (value.startsWith("//") || value.startsWith("(//")) {
            return "xpath=" + value;
        }
        return value;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static double asPlaywrightTimeout(int timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs debe ser > 0");
        }
        return timeoutMs;
    }

    private static LoadState parseLoadState(String state) {
        String normalized = requireNonBlank(state, "state no puede ser null/vacio")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "load" -> LoadState.LOAD;
            case "domcontentloaded" -> LoadState.DOMCONTENTLOADED;
            case "networkidle" -> LoadState.NETWORKIDLE;
            default -> throw new IllegalArgumentException(
                    "Load state invalido: " + state + ". Valores validos: load|domcontentloaded|networkidle");
        };
    }

    private static void waitUntil(int timeoutMs, BooleanSupplier condition, String timeoutMessage) {
        long timeout = validatedTimeoutMillis(timeoutMs);
        long deadlineNanos = System.nanoTime() + Duration.ofMillis((long) timeout).toNanos();
        while (System.nanoTime() <= deadlineNanos) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(WAIT_POLL_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Wait interrumpido.", ex);
            }
        }
        throw new IllegalStateException(timeoutMessage);
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private static long validatedTimeoutMillis(int timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs debe ser > 0");
        }
        return timeoutMs;
    }
}
