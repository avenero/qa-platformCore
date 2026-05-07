package com.qa.webcore.config;

/**
 * Constantes de configuración para el módulo web-core.
 *
 * <p>Centraliza todas las claves que pueden configurarse vía:
 * <ol>
 *   <li>{@code ExecutionConfig.properties} — enviado por el Backend por ejecución (máxima prioridad)</li>
 *   <li>Sistema properties {@code -Dkey=value}</li>
 *   <li>Variables de entorno</li>
 *   <li>Archivos {@code config-app.properties} del proyecto</li>
 * </ol>
 *
 * <p><b>Ejemplo de uso en config-app.properties:</b>
 * <pre>
 * web.browser=chromium
 * web.headless=false
 * browser.engine=playwright
 * playwright.browser=chromium
 * web.base.url=https://app.example.com
 * web.page.load.timeout.sec=30
 * web.explicit.wait.sec=10
 * </pre>
 *
 * <p><b>Indicaciones para el Backend (BE):</b>
 * Enviar estas propiedades en el {@code ExecutionRequest.properties} para cada ejecución.
 * Las claves prefijadas con {@code web.*} son exclusivas de este módulo.
 * {@link #DRIVER_STRATEGY} es compartida con infraestructura de driver y no lleva prefijo {@code web.*}.
 *
 * <p><b>Claves de VariableStore ({@code *_RUNTIME_VAR}):</b>
 * Las constantes sufijadas con {@code _RUNTIME_VAR} son claves del {@code VariableStore},
 * no del sistema de configuración. Se usan para overrides dentro del escenario
 * (ej: step "configuro el driver del navegador {string}").
 *
 * @author Abel Venero
 * @since 2.2.0
 */
public final class WebConfigKeys {

    private WebConfigKeys() {
        throw new UnsupportedOperationException("Clase de constantes — no instanciable");
    }

    // =========================================================================
    // Navegador
    // =========================================================================

    /**
     * Tipo de navegador Playwright a usar.
     * Valores válidos: {@code chromium}, {@code firefox}, {@code webkit}
     * (case-insensitive).
     * <br>Default: {@code chromium}.
     */
    public static final String BROWSER = "web.browser";

    /**
     * Activa el modo headless del navegador.
     * Valores: {@code true} | {@code false}.
     * <br>Default: {@code false}.
     */
    public static final String BROWSER_HEADLESS = "web.headless";

    /**
     * Alias de compatibilidad para {@link #BROWSER_HEADLESS}.
     *
     * @deprecated Usar {@link #BROWSER_HEADLESS} en código nuevo.
     */
    @Deprecated(since = "2.3.0", forRemoval = false)
    public static final String HEADLESS = BROWSER_HEADLESS;

    // =========================================================================
    // Motor de automatización
    // =========================================================================

    /**
     * Motor de browser a usar.
     * Valor soportado en web-core v2.1.0+: {@code playwright}.
     */
    public static final String BROWSER_ENGINE = "browser.engine";

    /**
     * Browser de Playwright cuando {@link #BROWSER_ENGINE} = {@code playwright}.
     * Valores: {@code chromium} | {@code firefox} | {@code webkit}.
     * <br>Default: {@code chromium}.
     */
    public static final String PLAYWRIGHT_BROWSER = "playwright.browser";

    /**
     * Timeout por defecto para operaciones Playwright (milisegundos).
     * <br>Default sugerido: {@code 30000}.
     */
    public static final String PW_TIMEOUT_MS = "playwright.timeout.ms";

    /**
     * Directorio base de capturas Playwright.
     */
    public static final String PW_SCREENSHOT_DIR = "playwright.screenshots.dir";

    /**
     * URL base de la aplicación bajo prueba.
     * <br>Ejemplo: {@code https://app.example.com}, {@code http://localhost:8080}.
     */
    public static final String BASE_URL = "web.base.url";

    /**
     * URL del proxy HTTP para el navegador.
     * <br>Ejemplo: {@code http://proxy.corp.com:8080}.
     * <br>Si no se configura, el driver no usa proxy explícito.
     */
    public static final String PROXY_URL = "web.proxy.url";

    // =========================================================================
    // Legacy de infraestructura
    // =========================================================================

    /**
     * Clave legacy preservada por compatibilidad con configuraciones históricas.
     */
    public static final String GRID_ENABLED = "web.grid.enabled";

    /**
     * Clave legacy preservada por compatibilidad con configuraciones históricas.
     */
    public static final String GRID_URL = "web.grid.url";

    // =========================================================================
    // Legacy de driver
    // =========================================================================

    /**
     * Clave legacy preservada por compatibilidad histórica.
     */
    public static final String DRIVER_STRATEGY = "driver.strategy";

    // =========================================================================
    // Timeouts
    // =========================================================================

    /**
     * Timeout de carga de página en segundos.
     * <br>Default: {@code 30}.
     */
    public static final String PAGE_LOAD_TIMEOUT_SEC = "web.page.load.timeout.sec";

    /**
     * Timeout de explicit wait en segundos.
     * <br>Default: {@code 10}.
     */
    public static final String EXPLICIT_WAIT_SEC = "web.explicit.wait.sec";

    /**
     * Implicit wait del driver en segundos.
     * <br>Default: {@code 0} (deshabilitado — preferir explicit waits).
     * <br><b>Advertencia:</b> combinar implicit y explicit waits puede causar timeouts inesperados.
     */
    public static final String IMPLICIT_WAIT_SEC = "web.implicit.wait.sec";

    // =========================================================================
    // VariableStore — overrides por escenario
    // =========================================================================

    /**
     * Clave del {@code VariableStore} que almacena el tipo de navegador
     * seleccionado en tiempo de ejecución (override por escenario).
     *
     * <p>Escrita por steps de configuración de browser
     * (ej: "configuro el driver del navegador {string}").
     * Leída por {@code WebHooksSteps} con prioridad sobre {@link #BROWSER}.
     *
     * @see com.qa.webcore.steps.config.BrowserConfigSteps
     */
    public static final String BROWSER_RUNTIME_VAR = "web.browser.type";

    /**
     * Clave del {@code VariableStore} que almacena el override de modo headless
     * en tiempo de ejecución (override por escenario).
     *
     * <p>Escrita por steps de configuración de browser.
     * Leída por {@code WebHooksSteps} con prioridad sobre {@link #HEADLESS}.
     *
     * @see com.qa.webcore.steps.config.BrowserConfigSteps
     */
    public static final String HEADLESS_RUNTIME_VAR = "web.headless.override";

    // =========================================================================
    // Legacy
    // =========================================================================

    /**
     * Clave legacy para la URL de la aplicación (sin prefijo {@code web.*}).
     * Mantenida por compatibilidad con proyectos que usaban {@code host} directamente.
     *
     * @deprecated Usar {@link #BASE_URL} en proyectos nuevos.
     */
    @Deprecated(since = "2.2.0", forRemoval = false)
    public static final String HOST_LEGACY = "host";
}
