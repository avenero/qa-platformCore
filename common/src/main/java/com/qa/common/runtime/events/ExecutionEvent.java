package com.qa.common.runtime.events;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Evento inmutable del ciclo de ejecucion BDD.
 *
 * <p>Tipos de eventos: SCENARIO_START, SCENARIO_END, STEP_START, STEP_END,
 * EXECUTION_START, EXECUTION_END, CUSTOM.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ExecutionEvent {

    /**
     * Tipos de eventos del ciclo de vida.
     */
    public enum Type {
        EXECUTION_START,
        EXECUTION_END,
        SCENARIO_START,
        SCENARIO_END,
        STEP_START,
        STEP_END,
        CUSTOM
    }

    private final Type type;
    private final String name;
    private final Instant timestamp;
    private final Map<String, Object> data;

    private ExecutionEvent(Type type, String name, Map<String, Object> data) {
        this.type = Objects.requireNonNull(type, "type no puede ser null");
        this.name = Objects.requireNonNull(name, "name no puede ser null");
        this.timestamp = Instant.now();
        this.data = Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * Crea un evento sin datos adicionales.
     */
    public static ExecutionEvent of(Type type, String name) {
        return new ExecutionEvent(type, name, Map.of());
    }

    /**
     * Crea un evento con datos adicionales.
     */
    public static ExecutionEvent of(Type type, String name, Map<String, Object> data) {
        Objects.requireNonNull(data, "data no puede ser null");
        return new ExecutionEvent(type, name, data);
    }

    public Type getType() { return type; }
    public String getName() { return name; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }

    /**
     * Obtiene un dato con casting seguro.
     * @param key  clave
     * @param type tipo esperado
     * @param <T>  tipo
     * @return valor o null si no existe
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public String toString() {
        return "ExecutionEvent{type=" + type + ", name='" + name
                + "', timestamp=" + timestamp + ", data=" + data.size() + "}";
    }
}
