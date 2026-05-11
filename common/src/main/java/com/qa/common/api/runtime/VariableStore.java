package com.qa.common.api.runtime;
import com.qa.common.utils.security.SecurityUtilities;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Almacen unificado de variables para compartir datos entre steps.
 *
 * <p>Reemplaza el uso disperso de HashMaps y ThreadLocals. Es thread-safe
 * gracias al uso de {@link ConcurrentHashMap}.
 *
 * <p>Alcance: una instancia por escenario, gestionada por {@link ExecutionContext}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class VariableStore {

    private final ConcurrentHashMap<String, Object> variables = new ConcurrentHashMap<>();

    /**
     * Almacena un valor con la clave dada.
     * @param key   clave, no null
     * @param value valor, no null
     * @throws NullPointerException si key o value son null
     */
    public void set(String key, Object value) {
        Objects.requireNonNull(key, "key no puede ser null");
        Objects.requireNonNull(value, "value no puede ser null");
        variables.put(key, value);
    }

    /**
     * Obtiene un valor por clave con casting seguro.
     * @param key  clave
     * @param type clase esperada
     * @param <T>  tipo esperado
     * @return Optional con el valor casteado, o vacio si no existe o tipo incorrecto
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key no puede ser null");
        Objects.requireNonNull(type, "type no puede ser null");
        Object value = variables.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Obtiene un valor o lanza excepcion si no existe.
     * @param key  clave
     * @param type clase esperada
     * @param <T>  tipo esperado
     * @return valor casteado
     * @throws IllegalStateException si no existe o tipo incorrecto
     */
    public <T> T require(String key, Class<T> type) {
        return get(key, type).orElseThrow(() ->
                new IllegalStateException("Variable requerida no encontrada: '" + key
                        + "' de tipo " + type.getSimpleName()));
    }

    /**
     * Verifica si existe una variable con la clave dada.
     * @param key clave
     * @return true si existe
     */
    public boolean has(String key) {
        return key != null && variables.containsKey(key);
    }

    /**
     * Elimina una variable.
     * @param key clave
     * @return true si existia y fue removida
     */
    public boolean remove(String key) {
        return key != null && variables.remove(key) != null;
    }

    /**
     * Retorna una vista inmutable de todas las variables.
     * @return mapa inmutable
     */
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * Cantidad de variables almacenadas.
     * @return cantidad
     */
    public int size() {
        return variables.size();
    }

    /**
     * Resuelve variables {@code ${nombre}} en un texto usando este store.
     *
     * <p>Orden de busqueda para cada placeholder:
     * <ol>
     *   <li>Este VariableStore</li>
     *   <li>{@link System#getProperty(String)}</li>
     *   <li>{@link System#getenv(String)}</li>
     *   <li>Si no se encuentra: mantiene el placeholder original</li>
     * </ol>
     *
     * @param text texto que puede contener {@code ${var}} placeholders; puede ser null
     * @return texto con variables resueltas, o el mismo texto si no hay placeholders
     */
    public String resolve(String text) {
        if (text == null || !text.contains("${")) {
            return text;
        }
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = null;
            // 1. Buscar en este store
            Object stored = variables.get(varName);
            if (stored != null) {
                replacement = stored.toString();
            }
            // 2. System properties
            if (replacement == null) {
                replacement = System.getProperty(varName);
            }
            // 3. Environment variables
            if (replacement == null) {
                replacement = System.getenv(varName);
            }
            // 4. Mantener placeholder si no se encontró
            if (replacement == null) {
                replacement = "${" + varName + "}";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Limpia todas las variables. Se invoca al final de cada escenario.
     */
    public void clear() {
        variables.clear();
    }

    @Override
    public String toString() {
        return "VariableStore{size=" + variables.size() + ", keys=" + variables.keySet() + "}";
    }
}
