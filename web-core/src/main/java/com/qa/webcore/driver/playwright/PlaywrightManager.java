package com.qa.webcore.driver.playwright;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.qa.common.internal.config.ConfigManager;

import java.util.Locale;
import java.util.Objects;

/**
 * Gestor de ciclo de vida para recursos Playwright con scopes suite/escenario.
 *
 * <p>Scopes aplicados:
 * <ul>
 *   <li>Suite: {@link Playwright} y {@link Browser}</li>
 *   <li>Escenario (thread-local): {@link BrowserContext} y {@link Page}</li>
 * </ul>
 */
public final class PlaywrightManager {

    private static final Object INIT_LOCK = new Object();
    private static final int VIEWPORT_WIDTH = 1920;
    private static final int VIEWPORT_HEIGHT = 1080;
    private static final ThreadLocal<BrowserContext> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<APIRequestContext> SCENARIO_API_CTX = new ThreadLocal<>();

    private static volatile Playwright playwright;
    private static volatile Browser browser;
    private static volatile String suiteBrowserName;
    private static volatile boolean suiteHeadless;
    private static volatile SuiteInitializer suiteInitializer = new DefaultSuiteInitializer();
    private static volatile APIRequestContext suiteApiCtxStandalone;

    private PlaywrightManager() {
        throw new UnsupportedOperationException("Clase utilitaria - no instanciable");
    }

    public static void initSuite(String browserName, boolean headless) {
        String normalizedBrowser = normalizeBrowserName(browserName);
        if (isSuiteInitialized()) {
            validateRequestedSuiteConfig(normalizedBrowser, headless);
            return;
        }
        synchronized (INIT_LOCK) {
            if (isSuiteInitialized()) {
                validateRequestedSuiteConfig(normalizedBrowser, headless);
                return;
            }
            SuiteResources resources = suiteInitializer.create(normalizedBrowser, headless);
            playwright = resources.playwright;
            browser = resources.browser;
            suiteBrowserName = normalizedBrowser;
            suiteHeadless = headless;
        }
    }

    public static void startScenario() {
        if (isScenarioActive()) {
            throw new IllegalStateException(
                    "Ya existe un escenario Playwright activo en este hilo."
                    + " Ejecuta endScenario() antes de iniciar otro.");
        }
        Browser activeBrowser = requireSuiteBrowser();
        BrowserContext context = activeBrowser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                .setIgnoreHTTPSErrors(true)
        );
        try {
            Page page = context.newPage();
            CONTEXT_HOLDER.set(context);
            PAGE_HOLDER.set(page);
        } catch (RuntimeException ex) {
            closeSilently(context);
            throw ex;
        }
    }

    public static Page getPage() {
        Page currentPage = PAGE_HOLDER.get();
        if (currentPage == null) {
            throw new IllegalStateException("No hay Page activa en este hilo. Ejecuta startScenario() primero.");
        }
        return currentPage;
    }

    public static void endScenario() {
        try {
            closeSilently(PAGE_HOLDER.get());
            closeSilently(CONTEXT_HOLDER.get());
        } finally {
            PAGE_HOLDER.remove();
            CONTEXT_HOLDER.remove();
            clearScenarioApiContext();
        }
    }

    public static void closeSuite() {
        synchronized (INIT_LOCK) {
            endScenario();
            closeStandaloneApiContext();
            closeSilently(browser);
            closeSilently(playwright);
            browser = null;
            playwright = null;
            suiteBrowserName = null;
        }
    }

    /**
     * Retorna el {@link APIRequestContext} activo para realizar peticiones HTTP desde Playwright.
     *
     * <ul>
     *   <li>Si hay un escenario web activo en este hilo, se devuelve el {@code request()}
     *       del {@link BrowserContext} actual; comparte cookies, storage y autenticación
     *       con el {@link Page}, permitiendo flujos mixtos web ↔ HTTP.</li>
     *   <li>Si no hay escenario activo, se devuelve un {@link APIRequestContext} standalone
     *       cacheado a nivel de suite, ideal para escenarios sólo-HTTP. Su {@code baseURL}
     *       se toma de la propiedad {@code base.url} (default {@code http://localhost}).</li>
     * </ul>
     */
    public static APIRequestContext getApiContext() {
        if (isScenarioActive()) {
            APIRequestContext ctx = SCENARIO_API_CTX.get();
            if (ctx == null) {
                ctx = requireScenarioContext().request();
                SCENARIO_API_CTX.set(ctx);
            }
            return ctx;
        }
        return getOrCreateStandaloneApiContext();
    }

    static void clearScenarioApiContext() {
        SCENARIO_API_CTX.remove();
    }

    static void closeStandaloneApiContext() {
        APIRequestContext ctx = suiteApiCtxStandalone;
        if (ctx != null) {
            try {
                ctx.dispose();
            } catch (RuntimeException ignored) {
                // Limpieza defensiva — no propagamos errores en el cierre de suite.
            }
            suiteApiCtxStandalone = null;
        }
    }

    private static APIRequestContext getOrCreateStandaloneApiContext() {
        APIRequestContext existing = suiteApiCtxStandalone;
        if (existing != null) {
            return existing;
        }
        synchronized (INIT_LOCK) {
            if (suiteApiCtxStandalone == null) {
                Playwright pw = ensurePlaywright();
                String baseUrl = ConfigManager.getInstance().get("base.url", "http://localhost");
                suiteApiCtxStandalone = pw.request().newContext(
                    new APIRequest.NewContextOptions().setBaseURL(baseUrl)
                );
            }
            return suiteApiCtxStandalone;
        }
    }

    private static Playwright ensurePlaywright() {
        Playwright pw = playwright;
        if (pw != null) {
            return pw;
        }
        synchronized (INIT_LOCK) {
            if (playwright == null) {
                playwright = Playwright.create();
            }
            return playwright;
        }
    }

    private static BrowserContext requireScenarioContext() {
        BrowserContext ctx = CONTEXT_HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "No hay BrowserContext activo en este hilo. Ejecuta startScenario() primero."
            );
        }
        return ctx;
    }

    public static boolean isSuiteInitialized() {
        return playwright != null && browser != null;
    }

    static boolean isScenarioActive() {
        return CONTEXT_HOLDER.get() != null || PAGE_HOLDER.get() != null;
    }

    static void setSuiteInitializerForTesting(SuiteInitializer initializer) {
        suiteInitializer = Objects.requireNonNull(initializer, "initializer no puede ser null");
    }

    static void useDefaultInitializerForTesting() {
        suiteInitializer = new DefaultSuiteInitializer();
    }

    static void resetForTests() {
        closeSuite();
        useDefaultInitializerForTesting();
    }

    private static Browser requireSuiteBrowser() {
        Browser currentBrowser = browser;
        if (currentBrowser == null) {
            throw new IllegalStateException("Suite Playwright no inicializada. Ejecuta initSuite() primero.");
        }
        return currentBrowser;
    }

    private static String normalizeBrowserName(String browserName) {
        if (browserName == null || browserName.isBlank()) {
            return "chromium";
        }
        return browserName.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateRequestedSuiteConfig(String browserName, boolean headless) {
        String currentBrowser = suiteBrowserName;
        if (currentBrowser == null) {
            return;
        }
        if (!currentBrowser.equals(browserName) || suiteHeadless != headless) {
            throw new IllegalStateException(
                "Playwright suite ya inicializada con browser=" + currentBrowser
                    + " y headless=" + suiteHeadless
                    + ". Cierra la suite antes de reinicializar con otra configuracion."
            );
        }
    }

    private static void closeSilently(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // No-op: limpieza defensiva del ciclo de vida.
            }
        }
    }

    @FunctionalInterface
    interface SuiteInitializer {
        SuiteResources create(String browserName, boolean headless);
    }

    static final class SuiteResources {
        private final Playwright playwright;
        private final Browser browser;

        SuiteResources(Playwright playwright, Browser browser) {
            this.playwright = Objects.requireNonNull(playwright, "playwright no puede ser null");
            this.browser = Objects.requireNonNull(browser, "browser no puede ser null");
        }
    }

    private static final class DefaultSuiteInitializer implements SuiteInitializer {
        @Override
        public SuiteResources create(String browserName, boolean headless) {
            Playwright playwrightInstance = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);
            Browser launchedBrowser = switch (browserName) {
                case "firefox" -> playwrightInstance.firefox().launch(options);
                case "webkit" -> playwrightInstance.webkit().launch(options);
                default -> playwrightInstance.chromium().launch(options);
            };
            return new SuiteResources(playwrightInstance, launchedBrowser);
        }
    }
}
