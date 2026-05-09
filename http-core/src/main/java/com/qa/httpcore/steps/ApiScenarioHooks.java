package com.qa.httpcore.steps;

import com.qa.httpcore.implementations.BaseAuthenticationManager;
import com.qa.httpcore.implementations.BaseHttpClient;
import com.qa.httpcore.interfaces.AuthenticationService;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;

import io.cucumber.java.Before;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hook de Cucumber que garantiza que los servicios API estén disponibles
 * en el {@link ServiceRegistry} antes de cada escenario que use api-core.
 *
 * <p><b>Modo Engine</b> ({@link com.qa.common.runtime.CucumberRuntimeEngine}):
 * {@link com.qa.httpcore.plugin.ApiPlugin} ya registró los servicios →
 * este hook detecta que {@link HttpClient} existe y <em>no hace nada</em>.</p>
 *
 * <p><b>Modo Standalone</b> (JUnit directo sin Engine):
 * {@link com.qa.common.cucumber.hooks.ScenarioExecutionHooks} crea el
 * {@link ExecutionContext} con un registry vacío → este hook registra los tres
 * servicios compartiendo la <em>misma</em> instancia de {@link HttpClient}, de modo
 * que todas las step classes vean el mismo estado de la petición:
 * <ul>
 *   <li>{@link HttpClient} — cliente HTTP usado por HeaderSteps, ExecutionSteps, etc.</li>
 *   <li>{@link AuthenticationService} — gestor de tokens/autenticación.</li>
 *   <li>{@link ApiHelper} — fachada de alto nivel usada por UrlConfigSteps, etc.</li>
 * </ul>
 *
 * <p>Este hook debe estar en un paquete incluido en el <em>glue path</em> del test.
 * Al estar en {@code com.qa.httpcore.steps} —el mismo paquete que las step definitions—
 * se descubre automáticamente sin configuración adicional.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see com.qa.httpcore.plugin.ApiPlugin
 * @see com.qa.common.cucumber.hooks.ScenarioExecutionHooks
 */
public class ApiScenarioHooks {

    private static final Logger LOG = LoggerFactory.getLogger(ApiScenarioHooks.class);

    /**
     * Bootstrapea los servicios API en el {@link ServiceRegistry} si todavía no fueron
     * registrados (modo standalone).
     *
     * <p>El orden {@code 10} garantiza que se ejecuta DESPUÉS de
     * {@link com.qa.common.cucumber.hooks.ScenarioExecutionHooks} (order = 0),
     * que es quien activa el {@link ExecutionContext}.
     */
    @Before(order = 10)
    public void ensureApiServicesRegistered() {
        ExecutionContext.current().ifPresent(ctx -> {
            ServiceRegistry registry = ctx.registry();

            // Guard: si HttpClient ya está registrado (engine mode o ejecución previa),
            // no sobreescribir — los servicios ya fueron configurados por ApiPlugin.
            if (registry.isRegistered(HttpClient.class)) {
                return;
            }

            // Standalone mode: crear y registrar los tres servicios API compartiendo
            // la MISMA instancia de HttpClient para que el estado (host, headers, body)
            // sea visible para todas las step classes del escenario.
            HttpClient httpClient = new BaseHttpClient();
            registry.registerInstance(HttpClient.class, httpClient);
            registry.registerInstance(AuthenticationService.class,
                    new BaseAuthenticationManager(httpClient));
            registry.registerInstance(ApiHelper.class, new ApiHelper(httpClient));

            LOG.debug("[ApiScenarioHooks] Servicios API registrados en modo standalone: "
                    + "HttpClient, AuthenticationService, ApiHelper");
        });
    }
}

