package com.qa.common.utils;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integración del {@link ExecutionContext} para gestión de variables entre steps.
 *
 * <p><b>Responsabilidad única:</b> Esta clase provee acceso tipado al
 * {@link com.qa.common.runtime.VariableStore} del contexto activo, resolución de
 * placeholders en texto y almacenamiento con prefijo de capa.
 *
 * <p><b>Para código nuevo usar directamente:</b>
 * <ul>
 *   <li>JSON   → {@link JsonUtilities}</li>
 *   <li>Texto  → {@link TextUtilities}</li>
 *   <li>Datos  → {@link DataGenerator}</li>
 *   <li>Store  → {@code ExecutionContext.requireCurrent().variables().set(key, value)}</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 * @version 5.0.0 — delegaciones eliminadas; solo lógica propia de store y placeholders
 */
public class DataUtilities {

    private DataUtilities() {
    }

    // =========================================================================
    // VARIABLE STORE — storeValue / getValue
    // =========================================================================

    /**
     * Almacena una variable en el {@link ExecutionContext} activo.
     *
     * <p>Para código nuevo preferir:
     * {@code ExecutionContext.requireCurrent().variables().set(key, value)}.
     *
     * @param key   clave (no null)
     * @param value valor; {@code null} elimina la variable
     */
    public static void storeValue(String key, Object value) {
        if (key == null) {
            return;
        }
        boolean stored = ExecutionContext.current().map(ctx -> {
            if (value != null) {
                ctx.variables().set(key, value);
            } else {
                ctx.variables().remove(key);
            }
            return true;
        }).orElse(false);
        if (!stored) {
            TestLogger.logWarning("DATA_UTILITIES",
                "storeValue('" + key + "') ignorado: no hay ExecutionContext activo", null);
        } else {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Variable almacenada: %s = %s",
                    key, TextUtilities.sanitizeForLogging(key, value)), null);
        }
    }

    /**
     * Obtiene una variable del {@link ExecutionContext} activo como String.
     *
     * @param key clave
     * @return valor como String, o {@code null} si no existe o no hay contexto
     */
    public static String getValue(String key) {
        if (key == null) {
            return null;
        }
        return ExecutionContext.current()
            .flatMap(ctx -> ctx.variables().get(key, Object.class))
            .map(Object::toString)
            .orElse(null);
    }

    // =========================================================================
    // OBJECT STORE — storeObject / getObject / hasObject / getObjectType
    // =========================================================================

    /**
     * Almacena un objeto en el {@link ExecutionContext} activo.
     *
     * @param key    clave (no null ni vacía)
     * @param object objeto a almacenar; {@code null} elimina la entrada
     */
    public static void storeObject(String key, Object object) {
        if (key == null || key.trim().isEmpty()) {
            TestLogger.logWarning("DATA_UTILITIES", "Key nula o vacía al guardar objeto - ignorado", null);
            return;
        }
        boolean stored = ExecutionContext.current().map(ctx -> {
            if (object != null) {
                ctx.variables().set(key, object);
            } else {
                ctx.variables().remove(key);
            }
            return true;
        }).orElse(false);
        if (!stored) {
            TestLogger.logWarning("DATA_UTILITIES",
                "storeObject('" + key + "') ignorado: no hay ExecutionContext activo", null);
        } else {
            TestLogger.logInfo("DATA_UTILITIES",
                String.format("Objeto almacenado: %s = %s (tipo: %s)",
                    key,
                    TextUtilities.sanitizeForLogging(key, object),
                    object != null ? object.getClass().getSimpleName() : "null"), null);
        }
    }

    /**
     * Recupera un objeto tipado del {@link ExecutionContext} activo.
     * Si el tipo no coincide exactamente, intenta conversión via Jackson.
     *
     * @param <T>   tipo esperado
     * @param key   clave
     * @param clazz clase del tipo esperado
     * @return objeto del tipo dado, o {@code null} si no existe
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObject(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return null;
        }
        Object obj = ExecutionContext.current()
            .flatMap(ctx -> ctx.variables().get(key, Object.class))
            .orElse(null);
        if (obj == null) {
            return null;
        }
        if (clazz.isInstance(obj)) {
            return (T) obj;
        }
        try {
            return JsonUtilities.getObjectMapper().convertValue(obj, clazz);
        } catch (Exception e) {
            String msg = String.format("No se pudo convertir '%s' (%s) a %s: %s",
                key, obj.getClass().getSimpleName(), clazz.getSimpleName(), e.getMessage());
            TestLogger.logError("DATA_UTILITIES", msg, null);
            throw new ClassCastException(msg);
        }
    }

    /**
     * Recupera un objeto sin tipo del {@link ExecutionContext} activo.
     *
     * @param key clave
     * @return objeto almacenado, o {@code null} si no existe
     */
    public static Object getObject(String key) {
        if (key == null) {
            return null;
        }
        return ExecutionContext.current()
            .flatMap(ctx -> ctx.variables().get(key, Object.class))
            .orElse(null);
    }

    /**
     * Verifica si existe un objeto con la clave dada en el {@link ExecutionContext} activo.
     *
     * @param key clave
     * @return {@code true} si existe
     */
    public static boolean hasObject(String key) {
        if (key == null) {
            return false;
        }
        return ExecutionContext.current()
            .flatMap(ctx -> ctx.variables().get(key, Object.class))
            .isPresent();
    }

    /**
     * Retorna el tipo ({@link Class}) del objeto almacenado bajo la clave dada.
     *
     * @param key clave
     * @return {@code Class} del objeto, o {@code null} si no existe
     */
    public static Class<?> getObjectType(String key) {
        Object obj = getObject(key);
        return obj != null ? obj.getClass() : null;
    }

    // =========================================================================
    // RESOLUCIÓN DE VARIABLES EN TEXTO
    // =========================================================================

    /**
     * Resuelve placeholders {@code {{var}}} y {@code ${var}} en un texto.
     *
     * <ul>
     *   <li>{@code {{var}}} → busca solo en el {@link ExecutionContext} activo</li>
     *   <li>{@code ${var}}  → busca en ExecutionContext →
     *       {@link System#getProperty} → {@link System#getenv}</li>
     * </ul>
     *
     * @param text texto con posibles placeholders; puede ser {@code null}
     * @return texto con los placeholders resueltos (conserva los no encontrados)
     */
    public static String replaceVariables(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        String result = text;

        Pattern curly = Pattern.compile("\\{\\{([^}]+)}}");
        Matcher m1 = curly.matcher(result);
        while (m1.find()) {
            String name = m1.group(1);
            String repl = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get(name, Object.class))
                .map(Object::toString)
                .orElse(null);
            if (repl == null) {
                TestLogger.logWarning("DATA_UTILITIES",
                    "Variable {{" + name + "}} no encontrada - conservando placeholder", null);
                repl = "{{" + name + "}}";
            }
            result = result.replace("{{" + name + "}}", repl);
        }

        Pattern dollar = Pattern.compile("\\$\\{([^}]+)}");
        Matcher m2 = dollar.matcher(result);
        while (m2.find()) {
            String name = m2.group(1);
            String repl = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get(name, Object.class))
                .map(Object::toString)
                .orElse(null);
            if (repl == null) {
                repl = System.getProperty(name);
            }
            if (repl == null) {
                repl = System.getenv(name);
            }
            if (repl == null) {
                TestLogger.logWarning("DATA_UTILITIES",
                    "Variable ${" + name + "} no encontrada - conservando placeholder", null);
                repl = "${" + name + "}";
            }
            result = result.replace("${" + name + "}", repl);
        }
        return result;
    }

    // =========================================================================
    // GUARDAR EN CONTEXTO CON PREFIJO DE CAPA
    // =========================================================================

    /**
     * Guarda un valor en el {@link ExecutionContext} activo prefijando la capa.
     *
     * <p>Ejemplo: {@code saveToContext("api", "token", "abc123")} almacena
     * la clave {@code "api.token"} con valor {@code "abc123"}.
     *
     * @param layer capa del framework (ej: "api", "web", "mobile"); puede ser null
     * @param key   clave del valor (no null ni vacía)
     * @param value valor a almacenar
     */
    public static void saveToContext(String layer, String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave no puede ser null o vacía");
        }
        String fullKey = (layer != null && !layer.trim().isEmpty()) ? layer + "." + key : key;
        ExecutionContext.current().ifPresent(ctx -> ctx.variables().set(fullKey, value));
        TestLogger.logDebug("DATA_UTILITIES",
            String.format("Guardado en contexto [%s]: %s", fullKey,
                TextUtilities.sanitizeForLogging(key, value)), null);
    }

    // =========================================================================
    // MAPA DE VARIABLES (acceso bulk)
    // =========================================================================

    /**
     * Retorna todas las variables almacenadas en el {@link ExecutionContext} activo.
     *
     * @return mapa inmutable con todas las variables, o mapa vacío si no hay contexto
     */
    public static Map<String, Object> getAllVariables() {
        return ExecutionContext.current()
            .map(ctx -> ctx.variables().getAll())
            .orElse(Map.of());
    }
}
