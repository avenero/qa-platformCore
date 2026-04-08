package com.qa.common.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contenedor ligero de inyeccion de dependencias (DI).
 *
 * <p>Permite registrar servicios por tipo (Class) con inicializacion lazy.
 * Los servicios se resuelven por tipo y se inicializan al primer acceso.
 *
 * <p>Thread-safe. Una instancia por ejecucion, gestionada por {@link ExecutionContext}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    private final ConcurrentHashMap<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Object> instances = new ConcurrentHashMap<>();

    /**
     * Registra un servicio con inicializacion lazy.
     * El supplier se invoca solo al primer {@link #get(Class)}.
     *
     * @param type    tipo/interfaz del servicio
     * @param factory supplier que crea la instancia
     * @param <T>     tipo del servicio
     * @throws NullPointerException si type o factory son null
     */
    public <T> void registerLazy(Class<T> type, Supplier<T> factory) {
        Objects.requireNonNull(type, "type no puede ser null");
        Objects.requireNonNull(factory, "factory no puede ser null");
        factories.put(type, factory);
        log.debug("Servicio registrado (lazy): {}", type.getSimpleName());
    }

    /**
     * Registra una instancia ya creada.
     *
     * @param type     tipo/interfaz del servicio
     * @param instance instancia del servicio
     * @param <T>      tipo del servicio
     * @throws NullPointerException si type o instance son null
     */
    public <T> void registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type no puede ser null");
        Objects.requireNonNull(instance, "instance no puede ser null");
        instances.put(type, instance);
        log.debug("Servicio registrado (instancia): {}", type.getSimpleName());
    }

    /**
     * Obtiene un servicio por tipo.
     * Si fue registrado lazy, lo inicializa en el primer acceso.
     *
     * @param type tipo del servicio
     * @param <T>  tipo del servicio
     * @return Optional con la instancia, o vacio si no esta registrado
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type) {
        Objects.requireNonNull(type, "type no puede ser null");

        // Primero buscar instancia ya creada
        Object instance = instances.get(type);
        if (instance != null) {
            return Optional.of(type.cast(instance));
        }

        // Buscar factory lazy
        Supplier<?> factory = factories.get(type);
        if (factory != null) {
            // computeIfAbsent es thread-safe
            Object created = instances.computeIfAbsent(type, k -> {
                log.debug("Inicializando servicio lazy: {}", type.getSimpleName());
                return factory.get();
            });
            return Optional.of(type.cast(created));
        }

        return Optional.empty();
    }

    /**
     * Obtiene un servicio o lanza excepcion si no esta registrado.
     *
     * @param type tipo del servicio
     * @param <T>  tipo del servicio
     * @return instancia del servicio
     * @throws IllegalStateException si el servicio no esta registrado
     */
    public <T> T require(Class<T> type) {
        return get(type).orElseThrow(() ->
                new IllegalStateException("Servicio no registrado: " + type.getSimpleName()));
    }

    /**
     * Verifica si un servicio esta registrado (factory o instancia).
     * @param type tipo del servicio
     * @return true si esta registrado
     */
    public boolean isRegistered(Class<?> type) {
        return type != null && (instances.containsKey(type) || factories.containsKey(type));
    }

    /**
     * Cantidad de servicios registrados (factories + instancias directas).
     * @return cantidad total de registros
     */
    public int size() {
        // Contar factories que no tienen instancia aun + instancias
        long factoryOnly = factories.keySet().stream()
                .filter(k -> !instances.containsKey(k))
                .count();
        return (int) factoryOnly + instances.size();
    }

    /**
     * Destruye todas las instancias activas llamando {@link AutoCloseable#close()} si aplica,
     * luego limpia factories e instancias.
     *
     * <p>Debe llamarse al finalizar la ejecucion para liberar recursos costosos
     * (conexiones DB, WebDrivers, sockets HTTP).</p>
     */
    public void destroyAll() {
        instances.values().forEach(instance -> {
            if (instance instanceof AutoCloseable ac) {
                try {
                    ac.close();
                } catch (Exception e) {
                    log.warn("Error al cerrar servicio {}: {}", instance.getClass().getSimpleName(), e.getMessage());
                }
            }
        });
        instances.clear();
        factories.clear();
        log.debug("ServiceRegistry destruido (recursos liberados)");
    }

    /**
     * Limpia todos los registros SIN cerrar recursos. Usar solo en tests unitarios.
     * Para produccion, preferir {@link #destroyAll()}.
     */
    public void clear() {
        factories.clear();
        instances.clear();
        log.debug("ServiceRegistry limpiado (sin cerrar recursos)");
    }

    @Override
    public String toString() {
        return "ServiceRegistry{factories=" + factories.size()
                + ", instances=" + instances.size() + "}";
    }
}
