package com.qa.httpcore.plugin;


import com.qa.common.spi.CorePlugin;
import com.qa.httpcore.components.ApiAuthComponent;
import com.qa.httpcore.components.ApiCookieComponent;
import com.qa.httpcore.components.ApiExecutionComponent;
import com.qa.httpcore.components.ApiHeaderComponent;
import com.qa.httpcore.components.ApiParameterComponent;
import com.qa.httpcore.components.ApiPerformanceComponent;
import com.qa.httpcore.components.ApiRequestBodyComponent;
import com.qa.httpcore.components.ApiResponseBodyComponent;
import com.qa.httpcore.components.ApiResponseHeaderComponent;
import com.qa.httpcore.components.ApiSecurityComponent;
import com.qa.httpcore.components.ApiStatusCodeComponent;
import com.qa.httpcore.components.ApiUrlComponent;
import com.qa.httpcore.execution.BaseUrlSource;
import com.qa.httpcore.execution.BaseUrlTracker;
import com.qa.httpcore.execution.ExecutionMeta;
import com.qa.httpcore.implementations.BaseAuthenticationManager;
import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.interfaces.AuthenticationService;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.driver.CapabilityDescriptor;
import com.qa.common.api.driver.CapabilityReport;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.internal.runtime.ServiceRegistry;

import com.qa.common.api.runtime.StepComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
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

    private static final Logger LOG = LoggerFactory.getLogger(ApiPlugin.class);

    /** Orden de registro del plugin API dentro del sistema de plugins. */
    private static final int PLUGIN_ORDER = 50;

    /**
     * Claves de propiedad que el BE puede usar para inyectar la base URL del ambiente.
     * El bootstrap las prueba en orden y usa la primera que tenga valor.
     */
    private static final List<String> BASE_URL_KEYS = List.of(
        "base.url",
        "api.base.url",
        "api.baseurl",
        "api.baseurl.qa"   // legacy — compatibilidad con proyectos que usan esta clave
    );

    /** Tracker activo por hilo — permite ejecución paralela sin tocar el ServiceRegistry. */
    private static final ThreadLocal<BaseUrlTracker> TRACKER = new ThreadLocal<>();

    @Override
    public String platformId() {
        return "HTTP";
    }

    @Override
    public String displayName() {
        return "HTTP API Testing";
    }

    @Override
    public CapabilityReport describeCapabilities() {
        return CapabilityReport.available("HTTP", List.of(
            new CapabilityDescriptor("rest",    "REST",    "HTTP/HTTPS endpoints with JSON/XML"),
            new CapabilityDescriptor("graphql", "GraphQL", "GraphQL query and mutation testing"),
            new CapabilityDescriptor("soap",    "SOAP",    "SOAP web services with XML payload")
        ));
    }

    @Override
    public Set<String> getActivationTags() {
        return Set.of("@api", "@rest", "@http", "@service");
    }

    @Override
    public int getOrder() {
        return PLUGIN_ORDER;
    }

    @Override
    public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
        LOG.debug("[ApiPlugin] Registrando servicios HTTP...");

        // HttpClient — inicialización lazy. Resuelve el motor (PLAYWRIGHT/APACHE) desde
        // ExecutionConfig.getHttpEngine() vía HttpClientFactory.create(config) (TASK-E04).
        // Antes de E04 esto creaba siempre BaseHttpClient (Unirest deprecated).
        registry.registerLazy(HttpClient.class, () -> HttpClientFactory.create(config));

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

        LOG.info("[ApiPlugin] Servicios registrados: HttpClient, AuthenticationService, ApiHelper");
    }

    @Override
    public void onScenarioStart(ExecutionContext context) {
        LOG.debug("[ApiPlugin] onScenarioStart — reiniciando estado HTTP para escenario");

        BaseUrlTracker tracker = new BaseUrlTracker();
        TRACKER.set(tracker);

        context.registry().get(HttpClient.class).ifPresent(client -> {
            client.clearRequestData();
            applyHttpPolicyFromExecutionConfig(context, client);
            bootstrapBaseUrl(context, client, tracker);
        });
    }

    /**
     * Bootstrap: si el ExecutionConfig trae una base URL del ambiente y el cliente
     * aún no tiene host configurado, la aplica automáticamente.
     *
     * <p>Esto hace que un escenario sin steps de host use el ambiente seleccionado
     * en la plataforma como fuente de verdad, sin necesidad de steps explícitos.
     *
     * <p>Si el cliente ya tiene host (rehúso entre escenarios sin reset completo),
     * se respeta el valor existente sin sobreescribir.
     */
    private void bootstrapBaseUrl(ExecutionContext context, HttpClient client, BaseUrlTracker tracker) {
        // Solo hace bootstrap si el cliente no tiene host previo
        if (client.hasValidHost()) {
            LOG.debug("[BASE-URL] Cliente ya tiene host configurado antes del bootstrap; se respeta.");
            return;
        }

        ExecutionConfig cfg = context.config();
        Optional<String> envUrl = BASE_URL_KEYS.stream().
            map(key -> cfg.getProperty(key)).
            filter(Optional::isPresent).
            map(Optional::get).
            filter(v -> !v.isBlank()).
            findFirst();

        if (envUrl.isPresent()) {
            String normalized = normalizeBaseUrl(envUrl.get());
            client.setHost(normalized);
            tracker.record(normalized, BaseUrlSource.ENVIRONMENT, null);
        } else {
            LOG.warn("[BASE-URL] No hay base URL de ambiente en ExecutionConfig. " +
                     "El escenario debe proveer el host explícitamente.");
        }
    }

    /**
     * Aplica un override de base URL desde un step.
     * Llamado por {@link com.qa.httpcore.steps.config.UrlConfigSteps} en lugar de
     * invocar {@code client.setHost()} directamente.
     *
     * <p>Valida la política {@code base.url.override.allowed} (inyectada por el BE).
     * En modo estricto, los steps con URL literal lanzan {@link BaseUrlOverrideException}.
     *
     * @param context     contexto de ejecución activo
     * @param newUrl      URL ya normalizada a aplicar
     * @param source      origen del cambio
     * @param stepPattern pattern del step que solicita el cambio (para logs y warnings)
     */
    public static void applyBaseUrlOverride(
        ExecutionContext context,
        String          newUrl,
        BaseUrlSource   source,
        String          stepPattern
    ) {
        boolean strict = "false".equalsIgnoreCase(
            context.config().getProperty("base.url.override.allowed").orElse("true"));

        if (strict && source == BaseUrlSource.STEP_LITERAL) {
            throw new BaseUrlOverrideException(
                "El proyecto no permite sobrescribir la base URL del ambiente desde un step " +
                "con URL literal. Step: '" + stepPattern + "'. " +
                "Usá paths relativos o configurá el host en el ambiente de la plataforma."
            );
        }

        String normalized = normalizeBaseUrl(newUrl);

        context.registry().get(HttpClient.class).ifPresent(c -> c.setHost(normalized));

        Optional.ofNullable(TRACKER.get()).ifPresent(t -> t.record(normalized, source, stepPattern));

        if (source == BaseUrlSource.STEP_LITERAL) {
            LOG.warn("[BASE-URL] Step '{}' sobrescribió la base URL con literal: '{}'",
                     stepPattern, normalized);
        } else {
            LOG.info("[BASE-URL] Step '{}' aplicó base URL ({}): '{}'",
                     stepPattern, source, normalized);
        }
    }

    /** Obtiene el {@link ExecutionMeta} del escenario actual para enviarlo al BE. */
    public static Optional<ExecutionMeta> currentExecutionMeta(ExecutionContext context) {
        return Optional.ofNullable(TRACKER.get()).map(ExecutionMeta::from);
    }

    /** Normaliza una base URL: agrega {@code https://} si falta schema, quita trailing slash. */
    public static String normalizeBaseUrl(String url) {
        String u = url == null ? "" : url.trim();
        if (u.isEmpty()) { return u; }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            LOG.warn("[BASE-URL] Schema no especificado en '{}'; asumiendo https://", u);
            u = "https://" + u;
        }
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    /**
     * Aplica timeouts, reintentos y SSL declarados en {@link ExecutionConfig} (inyectados por el BE).
     * Claves: {@code http.connect.timeout.ms}, {@code http.read.timeout.ms}, {@code http.max.retries}.
     * SSL: {@link ExecutionConfig#getSslContext()} y {@link ExecutionConfig#isTrustAllSsl()}.
     */
    private static void applyHttpPolicyFromExecutionConfig(ExecutionContext context, HttpClient client) {
        ExecutionConfig cfg = context.config();
        parsePositiveInt(cfg.getProperty("http.connect.timeout.ms")).ifPresent(v -> client.setConnectionTimeout(v));
        parsePositiveInt(cfg.getProperty("http.read.timeout.ms")).ifPresent(v -> client.setReadTimeout(v));
        Optional<String> retries = cfg.getProperty("http.max.retries");
        if (retries.isPresent()) {
            try {
                int n = Integer.parseInt(retries.get().trim());
                if (n >= 0) {
                    int delay = client.getRetryDelay() > 0 ? client.getRetryDelay() : 500;
                    client.setRetryPolicy(n, delay);
                }
            } catch (NumberFormatException ignored) {
                // mantener política por defecto del cliente
            }
        }
        // SSL: aplicar solo si hay configuración no-default
        if (cfg.isTrustAllSsl() || cfg.getSslContext() != null) {
            client.configureSsl(cfg.getSslContext(), cfg.isTrustAllSsl());
        }
    }

    private static Optional<Integer> parsePositiveInt(Optional<String> raw) {
        if (raw.isEmpty() || raw.get().isBlank()) {
            return Optional.empty();
        }
        try {
            int v = Integer.parseInt(raw.get().trim());
            if (v > 0) {
                return Optional.of(v);
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    @Override
    public void onScenarioEnd(ExecutionContext context) {
        LOG.debug("[ApiPlugin] onScenarioEnd — limpiando cliente HTTP");
        context.registry().get(HttpClient.class).ifPresent(HttpClient::reset);

        // Log del resumen de URL efectiva (para debugging en CI)
        Optional.ofNullable(TRACKER.get()).ifPresent(tracker -> {
            if (tracker.hasBaseUrl()) {
                LOG.info("[BASE-URL] URL efectiva al finalizar escenario: '{}' (fuente: {})",
                         tracker.getEffectiveBaseUrl(), tracker.getSource());
                if (tracker.wasOverriddenFromEnvironment()) {
                    LOG.warn("[BASE-URL] La URL del ambiente fue sobrescrita por un step literal. " +
                             "Revisar si el escenario es portable.");
                }
            }
        });
        TRACKER.remove();
    }

    /**
     * Declara los 12 componentes de steps API (Fase 2).
     *
     * <p>Cada componente apunta a su clase específica de steps
     * según {@code DISENO-STEPS-POR-COMPONENTES.md §3}.
     *
     * <p><em>Nota de deprecación:</em> {@code ApiRequestBodyComponent} (stepId {@code "api.body"})
     * se incluye durante el ciclo de transición hacia {@code "api.request.body"} (v2.2.0).
     * Remover en v2.3.0 una vez que todos los escenarios persistidos hayan sido migrados.
     */
    @SuppressWarnings("deprecation")
    @Override
    public List<StepComponent> getComponents() {
        return List.of(
                // GIVEN — Configuración
                new ApiUrlComponent(),
                new ApiAuthComponent(),
                new ApiHeaderComponent(),
                new ApiCookieComponent(),
                new ApiParameterComponent(),
                // TODO: remover ApiRequestBodyComponent en v2.3.0
                // @deprecated — reemplazar por "api.request.body" en v2.2.0
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

