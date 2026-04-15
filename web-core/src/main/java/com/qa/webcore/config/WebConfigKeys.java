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
 * web.browser=chrome
 * web.headless=false
 * web.base.url=https://app.example.com
 * web.grid.enabled=false
 * web.grid.url=http://selenium-grid:4444/wd/hub
 * web.page.load.timeout.sec=30
 * web.explicit.wait.sec=10
 * driver.strategy=auto
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
     * Tipo de navegador a usar.
     * Valores válidos: {@code chrome}, {@code firefox}, {@code edge}, {@code safari}
     * (case-insensitive).
     * <br>Default: {@code chrome}.
     */
    public static final String BROWSER = "web.browser";

    /**
     * Activa el modo headless del navegador.
     * Valores: {@code true} | {@code false}.
     * <br>Default: {@code false}.
     */
    public static final String HEADLESS = "web.headless";

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
    // Selenium Grid
    // =========================================================================

    /**
     * Activa la ejecución remota en Selenium Grid.
     * Valores: {@code true} | {@code false}.
     * <br>Default: {@code false} (ejecución local).
     * <br>Requiere que {@link #GRID_URL} esté configurada cuando es {@code true}.
     */
    public static final String GRID_ENABLED = "web.grid.enabled";

    /**
     * URL del hub de Selenium Grid.
     * <br>Ejemplo: {@code http://selenium-grid:4444/wd/hub}.
     * Compatible con Selenium Grid 3, Grid 4 y cloud providers (BrowserStack, Sauce Labs).
     */
    public static final String GRID_URL = "web.grid.url";

    // =========================================================================
    // Driver
    // =========================================================================

    /**
     * Estrategia de resolución del WebDriver binario.
     * Valores: {@code auto} | {@code local} | {@code artifactory}.
     * <br>Default: {@code auto}.
     *
     * <p>Esta clave no lleva prefijo {@code web.*} por compatibilidad histórica
     * y porque aplica a infraestructura de driver en general.
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
     * Timeout de explicit wait en segundos (para {@code WebDriverWait} y {@code WaitUtils}).
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
