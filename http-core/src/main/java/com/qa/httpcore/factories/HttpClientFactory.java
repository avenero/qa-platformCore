package com.qa.httpcore.factories;

import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.implementations.BaseHttpClient;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.HttpEngine;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Factory para crear instancias de {@link HttpClient}.
 *
 * <h2>Resolución de la implementación</h2>
 *
 * <p>Desde TASK-E04 se selecciona la implementación a partir del {@link HttpEngine}
 * configurado en el {@link ExecutionConfig} de la ejecución. La API recomendada es:
 *
 * <pre>{@code
 * HttpClient client = HttpClientFactory.create(execContext.config());
 * }</pre>
 *
 * <h2>Registry de motores</h2>
 *
 * <p>Cada motor se asocia a un {@link Supplier} en un registro estático mutable
 * (idempotente, thread-safe):
 * <ul>
 *   <li>{@link HttpEngine#APACHE} viene pre-registrado con {@link ApacheHttpClientImpl}.</li>
 *   <li>{@link HttpEngine#PLAYWRIGHT} <b>NO</b> se registra desde {@code http-core} — eso
 *       requeriría depender de {@code web-core} y violaría la regla inviolable
 *       "ningún módulo especializado conoce a otro" (Parte 1 §2 de la propuesta).
 *       La capa que sí conoce {@code PlaywrightManager} (BE / tests / wiring) debe
 *       registrar el motor antes de la primera llamada:
 *
 *       <pre>{@code
 *       HttpClientFactory.register(HttpEngine.PLAYWRIGHT,
 *           () -> new PlaywrightHttpEngine(PlaywrightManager::getApiContext));
 *       }</pre>
 *   </li>
 * </ul>
 *
 * <p>Si se solicita un motor no registrado, se emite warning y se retorna
 * {@link ApacheHttpClientImpl} como fallback seguro (nunca falla la ejecución por
 * un missing-supplier).
 *
 * <h2>Compatibilidad hacia atrás — {@code getInstance()}</h2>
 *
 * <p>{@link #getInstance()} preserva su firma. Resuelve en este orden:
 * <ol>
 *   <li>Si hay {@link ExecutionContext} activo y tiene {@link ExecutionConfig#getHttpEngine()},
 *       delega a {@link #create(ExecutionConfig)}.</li>
 *   <li>Si no, usa la property legacy {@value #CLIENT_IMPL_KEY} (valores
 *       {@value #IMPL_APACHE} / {@value #IMPL_UNIREST}) — modo histórico para
 *       callers fuera del runtime BDD (tests aislados, scripts CLI).</li>
 * </ol>
 *
 * @author Abel Venero
 * @version 2.3.0
 * @since 2.0.0
 * @see HttpClient
 * @see HttpEngine
 * @see ExecutionConfig
 */
public final class HttpClientFactory {

    /** Property legacy ({@code apache} | {@code unirest}). Usar {@link HttpEngine} en su lugar. */
    public static final String CLIENT_IMPL_KEY     = "http.client";
    public static final String IMPL_APACHE         = "apache";
    /** @deprecated Unirest está deprecado; usa {@link HttpEngine#APACHE}. */
    @Deprecated(since = "2.2.0", forRemoval = true)
    public static final String IMPL_UNIREST        = "unirest";

    private static final TestLogger.LoggerWrapper LOG = TestLogger.getLogger(HttpClientFactory.class);

    /**
     * Registro de motor → constructor. {@link ConcurrentHashMap} para registro thread-safe;
     * {@link EnumMap} no es concurrente y no aporta beneficio aquí (lookup O(1) igual).
     */
    private static final Map<HttpEngine, Supplier<HttpClient>> REGISTRY = new ConcurrentHashMap<>();

    static {
        // APACHE viene siempre disponible desde http-core (sin dependencias externas).
        REGISTRY.put(HttpEngine.APACHE, ApacheHttpClientImpl::new);
        // PLAYWRIGHT no se registra aquí — la capa wiring (BE / tests) debe hacerlo.
    }

    private HttpClientFactory() {
        throw new UnsupportedOperationException("HttpClientFactory es una clase factory");
    }

    // =========================================================================
    // API moderna (TASK-E04)
    // =========================================================================

    /**
     * Crea un nuevo {@link HttpClient} según {@code config.getHttpEngine()}.
     *
     * @param config configuración de la ejecución; no puede ser null.
     * @return instancia del motor correspondiente, o {@link ApacheHttpClientImpl} como
     *         fallback si el motor solicitado no fue registrado (con warning logueado).
     * @throws NullPointerException si {@code config} es null.
     */
    public static HttpClient create(ExecutionConfig config) {
        Objects.requireNonNull(config, "config no puede ser null");
        HttpEngine engine = config.getHttpEngine();
        if (engine == null) {
            // ExecutionConfig.getHttpEngine() nunca debería retornar null (lo resuelve el Builder),
            // pero somos defensivos para configs construidas por reflexión / tests legacy.
            engine = HttpEngine.resolveDefault();
            LOG.warn("ExecutionConfig.httpEngine es null — fallback a {}", engine);
        }
        Supplier<HttpClient> supplier = REGISTRY.get(engine);
        if (supplier == null) {
            LOG.warn("Motor HTTP {} no está registrado en HttpClientFactory — usando APACHE como fallback. " +
                    "Para habilitarlo, llama HttpClientFactory.register({}, ...) durante el bootstrap.",
                    engine, engine);
            supplier = REGISTRY.get(HttpEngine.APACHE);
        }
        HttpClient client = supplier.get();
        LOG.debug("HttpClient creado: motor={}, instancia={}", engine, client.getClass().getSimpleName());
        return client;
    }

    /**
     * Registra (o reemplaza) el supplier asociado a un motor HTTP.
     * Es idempotente: registrar dos veces el mismo motor es válido y simplemente
     * sobreescribe el supplier anterior.
     *
     * <p>Uso típico desde el wiring del BE (cuando se conoce {@code PlaywrightManager}):
     * <pre>{@code
     * HttpClientFactory.register(HttpEngine.PLAYWRIGHT,
     *     () -> new PlaywrightHttpEngine(PlaywrightManager::getApiContext));
     * }</pre>
     *
     * @param engine    motor HTTP, no null.
     * @param supplier  fábrica de la implementación, no null.
     * @throws NullPointerException si cualquier parámetro es null.
     */
    public static void register(HttpEngine engine, Supplier<HttpClient> supplier) {
        Objects.requireNonNull(engine,   "engine no puede ser null");
        Objects.requireNonNull(supplier, "supplier no puede ser null");
        REGISTRY.put(engine, supplier);
        LOG.info("HttpClientFactory: motor {} registrado.", engine);
    }

    /**
     * Quita un motor del registro. Util para limpieza en tests.
     * No quita {@link HttpEngine#APACHE} (lo reinstala con su default).
     */
    public static void unregister(HttpEngine engine) {
        if (engine == null) return;
        if (engine == HttpEngine.APACHE) {
            REGISTRY.put(HttpEngine.APACHE, ApacheHttpClientImpl::new);
            return;
        }
        REGISTRY.remove(engine);
    }

    /** @return {@code true} si {@code engine} tiene un supplier registrado. */
    public static boolean isRegistered(HttpEngine engine) {
        return engine != null && REGISTRY.containsKey(engine);
    }

    // =========================================================================
    // API legacy — preservada para no romper consumidores
    // =========================================================================

    /**
     * Crea un {@link HttpClient} resolviendo el motor desde el contexto activo.
     * Si no hay {@link ExecutionContext} activo, cae al property legacy
     * {@value #CLIENT_IMPL_KEY}.
     *
     * @return nueva instancia.
     */
    public static HttpClient getInstance() {
        Optional<ExecutionContext> ctx = ExecutionContext.current();
        if (ctx.isPresent() && ctx.get().config() != null) {
            return create(ctx.get().config());
        }
        // Fallback histórico para callers fuera del runtime BDD.
        String impl = resolveLegacyImpl();
        return createForLegacyImpl(impl);
    }

    /**
     * Crea una instancia de {@link HttpClient} específica para un framework.
     * Conservada por backward-compat; ignora el argumento (todos los frameworks
     * comparten implementación).
     *
     * @deprecated Usa {@link #create(ExecutionConfig)}.
     */
    @Deprecated(since = "2.3.0")
    public static HttpClient getInstance(String frameworkType) {
        if (frameworkType == null || frameworkType.isBlank()) {
            LOG.warn("frameworkType es null o vacío, usando implementación por defecto");
        }
        return getInstance();
    }

    /** Crea una instancia preconfigurada con host. */
    public static HttpClient getInstanceWithHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host no puede ser null o vacío");
        }
        HttpClient client = getInstance();
        client.setHost(host.trim());
        LOG.debug("HttpClient configurado con host: {}", host);
        return client;
    }

    /** Crea una instancia preconfigurada para JSON. */
    public static HttpClient getJsonInstance() {
        HttpClient client = getInstance();
        client.configureForJson();
        LOG.debug("HttpClient configurado para JSON");
        return client;
    }

    /** Crea una instancia preconfigurada para JSON con host. */
    public static HttpClient getJsonInstanceWithHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host no puede ser null o vacío");
        }
        HttpClient client = getInstance();
        client.setHost(host.trim());
        client.configureForJson();
        LOG.debug("HttpClient configurado para JSON con host: {}", host);
        return client;
    }

    /** @return información de debug del factory (motores registrados + property legacy). */
    public static String getFactoryInfo() {
        return String.format(
                "HttpClientFactory v2.3.0 — engines registrados: %s. Property legacy: '%s' (apache | unirest@deprecated).",
                REGISTRY.keySet(), CLIENT_IMPL_KEY);
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private static String resolveLegacyImpl() {
        String impl = System.getProperty(CLIENT_IMPL_KEY);
        if (impl == null || impl.isBlank()) {
            try {
                impl = com.qa.common.config.ConfigManager.getInstance().get(CLIENT_IMPL_KEY, IMPL_APACHE);
            } catch (Exception e) {
                impl = IMPL_APACHE;
            }
        }
        return impl.trim().toLowerCase();
    }

    @SuppressWarnings("removal")
    private static HttpClient createForLegacyImpl(String impl) {
        return switch (impl) {
            case IMPL_UNIREST -> {
                LOG.warn("Usando BaseHttpClient (Unirest) que está @Deprecated(forRemoval=true). " +
                        "Migra a HttpEngine.APACHE: -D{}={}", CLIENT_IMPL_KEY, IMPL_APACHE);
                yield new BaseHttpClient();
            }
            default -> {
                if (!IMPL_APACHE.equals(impl)) {
                    LOG.warn("Implementación '{}' no reconocida, usando Apache HttpClient 5 por defecto", impl);
                }
                yield new ApacheHttpClientImpl();
            }
        };
    }
}
