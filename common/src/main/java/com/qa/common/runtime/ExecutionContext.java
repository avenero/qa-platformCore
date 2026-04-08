package com.qa.common.runtime;

import com.qa.common.runtime.events.EventBus;
import com.qa.common.runtime.events.ExecutionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Contexto central de una ejecucion BDD.
 *
 * <p>Actua como fachada unificada que agrupa:
 * <ul>
 *   <li>{@link ExecutionConfig} - configuracion inmutable</li>
 *   <li>{@link ServiceRegistry} - contenedor DI</li>
 *   <li>{@link VariableStore} - variables compartidas entre steps</li>
 *   <li>{@link EventBus} - bus de eventos pub/sub</li>
 * </ul>
 *
 * <p>Se accede via {@code ExecutionContext.current()} usando ThreadLocal
 * para soporte de ejecucion paralela.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ExecutionContext {

    private static final Logger log = LoggerFactory.getLogger(ExecutionContext.class);
    private static final ThreadLocal<ExecutionContext> CURRENT = new ThreadLocal<>();

    private final ExecutionConfig config;
    private final ServiceRegistry registry;
    private final VariableStore variables;
    private final EventBus eventBus;

    /**
     * Constructor con todos los componentes.
     *
     * <p><b>Visibilidad package-private intencional.</b>
     * El punto de entrada público es {@link #builder()}.
     * Código del mismo paquete ({@code DefaultLifecycleManager}, tests unitarios)
     * puede usar este constructor directamente.
     */
    ExecutionContext(ExecutionConfig config, ServiceRegistry registry,
                    VariableStore variables, EventBus eventBus) {
        this.config = Objects.requireNonNull(config, "config no puede ser null");
        this.registry = Objects.requireNonNull(registry, "registry no puede ser null");
        this.variables = Objects.requireNonNull(variables, "variables no puede ser null");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus no puede ser null");
    }

    /**
     * Obtiene el contexto actual del hilo.
     * @return Optional con el contexto, o vacio si no hay uno activo
     */
    public static Optional<ExecutionContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * Obtiene el contexto actual o lanza excepcion.
     * @return contexto activo
     * @throws IllegalStateException si no hay contexto activo en el hilo
     */
    public static ExecutionContext requireCurrent() {
        ExecutionContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException(
                    "No hay ExecutionContext activo en el hilo actual: " + Thread.currentThread().getName());
        }
        return ctx;
    }

    /**
     * Establece este contexto como el activo del hilo.
     */
    public void activate() {
        CURRENT.set(this);
        log.debug("ExecutionContext activado en hilo: {}", Thread.currentThread().getName());
    }

    /**
     * Desactiva el contexto del hilo actual.
     */
    public static void deactivate() {
        CURRENT.remove();
        log.debug("ExecutionContext desactivado en hilo: {}", Thread.currentThread().getName());
    }

    // --- Accessors ---

    public ExecutionConfig config() { return config; }
    public ServiceRegistry registry() { return registry; }
    public VariableStore variables() { return variables; }
    public EventBus eventBus() { return eventBus; }

    /**
     * Atajo: obtiene un servicio del registry.
     * @param type tipo del servicio
     * @param <T>  tipo
     * @return instancia del servicio
     * @throws IllegalStateException si no esta registrado
     */
    public <T> T service(Class<T> type) {
        return registry.require(type);
    }

    /**
     * Atajo: publica un evento en el bus.
     * @param type tipo de evento
     * @param name nombre descriptivo
     */
    public void publishEvent(ExecutionEvent.Type type, String name) {
        eventBus.publish(ExecutionEvent.of(type, name));
    }

    /**
     * Limpia todos los recursos internos. Se invoca al final de la ejecucion.
     *
     * <p>Llama {@link ServiceRegistry#destroyAll()} para cerrar correctamente
     * conexiones DB, WebDrivers y otros recursos {@link AutoCloseable}.</p>
     */
    public void cleanup() {
        variables.clear();
        registry.destroyAll();   // Cierra recursos AutoCloseable antes de limpiar
        eventBus.clear();
        deactivate();
        log.debug("ExecutionContext limpiado completamente");
    }

    /**
     * Crea un builder de conveniencia para tests y uso simplificado.
     *
     * <p>Permite construir un {@link ExecutionContext} completo con valores por defecto,
     * solo sobreescribiendo lo necesario:
     * <pre>{@code
     * ExecutionContext ctx = ExecutionContext.builder()
     *     .scenarioId("my-scenario-123")
     *     .build();
     * }</pre>
     *
     * @return nuevo builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Builder de conveniencia
    // -------------------------------------------------------------------------

    /**
     * Builder de conveniencia para {@link ExecutionContext}.
     *
     * <p>Todos los campos son opcionales: si no se especifican, se crean con valores
     * por defecto listos para usar (especialmente útil en tests unitarios).
     */
    public static final class Builder {

        private String scenarioId;
        private ExecutionConfig config;
        private ServiceRegistry registry;
        private VariableStore variables;
        private EventBus eventBus;

        private Builder() {}

        /**
         * Identificador del escenario (se almacena en {@link ExecutionConfig} como
         * propiedad {@code "scenarioId"}).
         *
         * @param scenarioId identificador único del escenario
         * @return este builder
         */
        public Builder scenarioId(String scenarioId) {
            this.scenarioId = scenarioId;
            return this;
        }

        /** Configuración inmutable. Si no se provee, se crea con valores por defecto. */
        public Builder config(ExecutionConfig config) {
            this.config = config;
            return this;
        }

        /** Registro de servicios. Si no se provee, se crea uno vacío. */
        public Builder registry(ServiceRegistry registry) {
            this.registry = registry;
            return this;
        }

        /** Almacen de variables. Si no se provee, se crea uno vacío. */
        public Builder variables(VariableStore variables) {
            this.variables = variables;
            return this;
        }

        /** Bus de eventos. Si no se provee, se crea uno nuevo. */
        public Builder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        /**
         * Construye el {@link ExecutionContext} con los valores configurados.
         * Los componentes no especificados se crean con valores por defecto.
         *
         * @return nuevo ExecutionContext listo para usar
         */
        public ExecutionContext build() {
            if (config == null) {
                ExecutionConfig.Builder cfgBuilder = new ExecutionConfig.Builder()
                        .environment("test");
                String sid = (scenarioId != null) ? scenarioId : UUID.randomUUID().toString();
                cfgBuilder.property("scenarioId", sid);
                config = cfgBuilder.build();
            }
            if (registry == null)  registry  = new ServiceRegistry();
            if (variables == null) variables  = new VariableStore();
            if (eventBus  == null) eventBus   = new EventBus();

            return new ExecutionContext(config, registry, variables, eventBus);
        }
    }

    @Override
    public String toString() {
        return "ExecutionContext{config=" + config + ", registry=" + registry
                + ", variables=" + variables + "}";
    }
}
