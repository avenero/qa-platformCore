package com.qa.apicore.plugin;

import com.qa.apicore.components.ApiAuthComponent;
import com.qa.apicore.components.ApiCookieComponent;
import com.qa.apicore.components.ApiExecutionComponent;
import com.qa.apicore.components.ApiHeaderComponent;
import com.qa.apicore.components.ApiParameterComponent;
import com.qa.apicore.components.ApiPerformanceComponent;
import com.qa.apicore.components.ApiRequestBodyComponent;
import com.qa.apicore.components.ApiResponseBodyComponent;
import com.qa.apicore.components.ApiResponseHeaderComponent;
import com.qa.apicore.components.ApiSecurityComponent;
import com.qa.apicore.components.ApiStatusCodeComponent;
import com.qa.apicore.components.ApiUrlComponent;
import com.qa.apicore.implementations.BaseAuthenticationManager;
import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.AuthenticationService;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.runtime.CorePlugin;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Plugin de API para el runtime de ejecución BDD.
 *
 * <p>Registra los servicios HTTP ({@link HttpClient}, {@link AuthenticationService},
 * {@link ApiHelper}) y declara los 12 componentes de steps API organizados por
 * responsabilidad (configuración, ejecución, validación).
 *
 * <p>Se descubre automáticamente vía Java SPI desde:
 * {@code META-INF/services/com.qa.common.runtime.CorePlugin}
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiPlugin implements CorePlugin {

    private static final Logger log = LoggerFactory.getLogger(ApiPlugin.class);

    @Override
    public String getName() {
        return "api";
    }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@api", "@rest", "@http", "@service");
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        log.debug("[ApiPlugin] Registrando servicios HTTP...");

        // HttpClient — inicialización lazy para no crear el cliente si el escenario no lo usa
        registry.registerLazy(HttpClient.class, BaseHttpClient::new);

        // AuthenticationService — depende del HttpClient, resolución lazy en cadena
        registry.registerLazy(AuthenticationService.class, () -> {
            HttpClient httpClient = registry.require(HttpClient.class);
            return new BaseAuthenticationManager(httpClient);
        });

        // ApiHelper — facade que envuelve HttpClient con lógica de steps
        registry.registerLazy(ApiHelper.class, () -> {
            HttpClient httpClient = registry.require(HttpClient.class);
            return new ApiHelper(httpClient);
        });

        log.info("[ApiPlugin] Servicios registrados: HttpClient, AuthenticationService, ApiHelper");
    }

    @Override
    public void onScenarioStart(ExecutionContext context) {
        log.debug("[ApiPlugin] onScenarioStart — reiniciando estado HTTP para escenario");
        context.registry().get(HttpClient.class)
                .ifPresent(HttpClient::clearRequestData);
    }

    @Override
    public void onScenarioEnd(ExecutionContext context) {
        log.debug("[ApiPlugin] onScenarioEnd — limpiando cliente HTTP");
        context.registry().get(HttpClient.class)
                .ifPresent(HttpClient::reset);
    }

    /**
     * Declara los 12 componentes de steps API (Fase 2).
     *
     * <p>Cada componente apunta a su clase específica de steps
     * según {@code DISENO-STEPS-POR-COMPONENTES.md §3}.
     */
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                // GIVEN — Configuración
                new ApiUrlComponent(),
                new ApiAuthComponent(),
                new ApiHeaderComponent(),
                new ApiCookieComponent(),
                new ApiParameterComponent(),
                new ApiRequestBodyComponent(),
                // WHEN — Ejecución
                new ApiExecutionComponent(),
                // THEN — Validación
                new ApiStatusCodeComponent(),
                new ApiResponseBodyComponent(),
                new ApiResponseHeaderComponent(),
                new ApiPerformanceComponent(),
                new ApiSecurityComponent()
        );
    }

    // -------------------------------------------------------------------------
    // (helper component() eliminado — en Fase 2 se usan clases concretas)
    // -------------------------------------------------------------------------
}

