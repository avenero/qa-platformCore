package com.qa.common.utils;

import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fachada de compatibilidad para utilidades de datos del framework QA.
 *
 * <p><b>Arquitectura:</b> Esta clase es una fachada delgada. La lógica real reside en:
 * <ul>
 *   <li>{@link JsonUtilities}   — Serialización JSON, extracción, JSONPath, comparación</li>
 *   <li>{@link TextUtilities}   — Normalización, sanitización y comparación de texto</li>
 *   <li>{@link DataGenerator}   — Generación de datos de prueba y fechas</li>
 *   <li>{@link FileUtilities}   — Lectura/escritura de archivos</li>
 * </ul>
 *
 * <p><b>Lógica propia:</b> La resolución de placeholders ({@code ${var}} / {@code {{var}}})
 * y {@code saveToContext} son exclusivos de esta clase. Todos operan
 * <b>exclusivamente</b> sobre el {@link com.qa.common.runtime.VariableStore}
 * del {@link ExecutionContext} activo — sin stores estáticos.
 *
 * <p><b>⚠️ Uso recomendado para código nuevo:</b>
 * <ul>
 *   <li>JSON   → {@link JsonUtilities}</li>
 *   <li>Texto  → {@link TextUtilities}</li>
 *   <li>Datos  → {@link DataGenerator}</li>
 *   <li>Store  → {@code ExecutionContext.requireCurrent().variables().set(key, value)}</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 * @version 4.0.0 — stores estáticos eliminados (6-Abr-2026)
 */
public class DataUtilities {

    private DataUtilities() {
        // utility class — no instances
    }

    // =========================================================================
    // VARIABLE STORE — storeValue / getValue / clearVariables
    // =========================================================================

    /**
     * Almacena una variable en el {@link ExecutionContext} activo.
     *
     * <p>Para código nuevo usar directamente:
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
     * Obtiene una variable del {@link ExecutionContext} activo.
     *
     * @param key clave
     * @return valor como String, o {@code null} si no existe o no hay contexto
     */
    public static String getValue(String key) {
        if (key == null) {
            return null;
        }
        return ExecutionContext.current().flatMap(ctx -> ctx.variables().get(key, Object.class)).map(Object::toString).
            orElse(null);
    }

    /**
     * No-op: las variables se limpian automáticamente al cierre del
     * {@link ExecutionContext} (gestionado por {@code ScenarioExecutionHooks}).
     *
     * @deprecated Usar el ciclo de vida del ExecutionContext
     */
    @Deprecated
    public static void clearVariables() {
        TestLogger.logDebug("DATA_UTILITIES",
            "clearVariables() no-op: el ExecutionContext gestiona el ciclo de vida", null);
    }

    // =========================================================================
    // OBJECT STORE — storeObject / getObject / hasObject / clearObjects
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
     * Si el tipo no coincide exactamente, intenta conversión inteligente via Jackson.
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
        Object obj = ExecutionContext.current().flatMap(ctx -> ctx.variables().get(key, Object.class)).orElse(null);
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
     * Recupera un objeto del {@link ExecutionContext} activo.
     *
     * @param key clave
     * @return objeto almacenado, o {@code null} si no existe
     */
    public static Object getObject(String key) {
        if (key == null) {
            return null;
        }
        return ExecutionContext.current().flatMap(ctx -> ctx.variables().get(key, Object.class)).orElse(null);
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
        return ExecutionContext.current().flatMap(ctx -> ctx.variables().get(key, Object.class)).isPresent();
    }

    /**
     * Retorna la clase ({@link Class}) del objeto en el {@link ExecutionContext} activo.
     *
     * @param key clave
     * @return Class del objeto, o {@code null} si no existe
     */
    public static Class<?> getObjectType(String key) {
        Object obj = getObject(key);
        return obj != null ? obj.getClass() : null;
    }

    /**
     * No-op: los objetos se limpian automáticamente al cierre del {@link ExecutionContext}.
     *
     * @deprecated Usar el ciclo de vida del ExecutionContext
     */
    @Deprecated
    public static void clearObjects() {
        TestLogger.logDebug("DATA_UTILITIES",
            "clearObjects() no-op: el ExecutionContext gestiona el ciclo de vida", null);
    }

    /**
     * No-op: todos los stores se limpian automáticamente al cierre del {@link ExecutionContext}.
     *
     * @deprecated Usar el ciclo de vida del ExecutionContext
     */
    @Deprecated
    public static void clearAll() {
        TestLogger.logDebug("DATA_UTILITIES",
            "clearAll() no-op: el ExecutionContext gestiona el ciclo de vida", null);
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

        // --- Formato {{var}} → solo ExecutionContext ---
        Pattern curly = Pattern.compile("\\{\\{([^}]+)}}");
        Matcher m1 = curly.matcher(result);
        while (m1.find()) {
            String name = m1.group(1);
            String repl = ExecutionContext.current().flatMap(ctx -> ctx.variables().get(name, Object.class)).
                map(Object::toString).orElse(null);
            if (repl == null) {
                TestLogger.logWarning("DATA_UTILITIES",
                    "Variable {{" + name + "}} no encontrada - conservando placeholder", null);
                repl = "{{" + name + "}}";
            }
            result = result.replace("{{" + name + "}}", repl);
        }

        // --- Formato ${var} → ExecutionContext → System ---
        Pattern dollar = Pattern.compile("\\$\\{([^}]+)}");
        Matcher m2 = dollar.matcher(result);
        while (m2.find()) {
            String name = m2.group(1);
            String repl = ExecutionContext.current().flatMap(ctx -> ctx.variables().get(name, Object.class)).
                map(Object::toString).orElse(null);
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
    // DELEGACIONES → JsonUtilities  (backward compatibility)
    // =========================================================================

    /**
     * Deserializa un JSON a un objeto tipado.
     *
     * @param <T>   tipo del objeto resultado
     * @param json  cadena JSON a deserializar
     * @param clazz clase de destino
     * @return objeto del tipo indicado
     * @throws FrameworkBusinessException si el JSON es inválido
     * @see JsonUtilities#deserializeJson(String, Class)
     */
    public static <T> T deserializeJson(String json, Class<T> clazz) throws FrameworkBusinessException {
        return JsonUtilities.deserializeJson(json, clazz);
    }

    /**
     * Obtiene un parámetro del JSON usando JSONPath.
     *
     * @param jsonBody  cuerpo JSON como String
     * @param fieldPath ruta al campo (JSONPath o notación de punto)
     * @return valor del campo encontrado
     * @throws FrameworkBusinessException si el JSON es inválido
     * @see JsonUtilities#getJsonParameter(String, String)
     */
    public static Object getJsonParameter(String jsonBody, String fieldPath) throws FrameworkBusinessException {
        return JsonUtilities.getJsonParameter(jsonBody, fieldPath);
    }

    /**
     * Obtiene un parámetro del JSON usando JSONPath (alias de getJsonParameter).
     *
     * @param jsonBody  cuerpo JSON como String
     * @param fieldPath ruta al campo (JSONPath o notación de punto)
     * @return valor del campo encontrado
     * @throws FrameworkBusinessException si el JSON es inválido
     * @see JsonUtilities#getJsonParameter(String, String)
     */
    public static Object getJsonParameters(String jsonBody, String fieldPath) throws FrameworkBusinessException {
        return JsonUtilities.getJsonParameter(jsonBody, fieldPath);
    }

    /**
     * Verifica si un campo existe en el JSON dado.
     *
     * @param jsonBody  cuerpo JSON como String
     * @param fieldPath ruta al campo a verificar
     * @return {@code true} si el campo existe
     * @see JsonUtilities#hasJsonField(String, String)
     */
    public static boolean hasJsonField(String jsonBody, String fieldPath) {
        return JsonUtilities.hasJsonField(jsonBody, fieldPath);
    }

    /**
     * Busca un valor en un objeto o mapa por clave recursivamente.
     *
     * @param response  objeto o mapa donde buscar
     * @param targetKey clave a buscar
     * @return valor encontrado o {@code null}
     * @see JsonUtilities#findValue(Object, String)
     */
    public static Object findValue(Object response, String targetKey) {
        return JsonUtilities.findValue(response, targetKey);
    }

    /**
     * Extrae un token de un JSON de respuesta.
     *
     * @param jsonResponse respuesta JSON como String
     * @param tokenField   nombre del campo que contiene el token
     * @return valor del token encontrado
     * @throws FrameworkBusinessException si el JSON es inválido o el campo no existe
     * @see JsonUtilities#getToken(String, String)
     */
    public static String getToken(String jsonResponse, String tokenField) throws FrameworkBusinessException {
        return JsonUtilities.getToken(jsonResponse, tokenField);
    }

    /**
     * Convierte un JSON a un mapa de clave-valor.
     *
     * @param json cadena JSON a convertir
     * @return mapa con los campos del JSON
     * @see JsonUtilities#jsonToMap(String)
     */
    public static Map<String, Object> jsonToMap(String json) {
        return JsonUtilities.jsonToMap(json);
    }

    // =========================================================================
    // DELEGACIONES → TextUtilities  (backward compatibility)
    // =========================================================================

    /**
     * Capitaliza la primera letra de un String.
     *
     * @param str String a capitalizar
     * @return String con la primera letra en mayúscula
     * @see TextUtilities#capitalize(String)
     */
    public static String capitalize(String str) {
        return TextUtilities.capitalize(str);
    }

    /**
     * Verifica si un String es válido (no nulo y no vacío).
     *
     * @param str String a verificar
     * @return {@code true} si es no nulo y no está en blanco
     * @see TextUtilities#isValidString(String)
     */
    public static boolean isValidString(String str) {
        return TextUtilities.isValidString(str);
    }

    /**
     * Sanitiza un valor usando la clave como contexto para detectar datos sensibles.
     *
     * @param key   clave/nombre del campo
     * @param value valor a sanitizar
     * @return valor sanitizado o {@code "***HIDDEN***"} si es sensible
     * @see TextUtilities#sanitizeValue(String, String)
     */
    public static String sanitizeValue(String key, String value) {
        return TextUtilities.sanitizeValue(key, value);
    }

    /**
     * Sanitiza el body de una petición/respuesta HTTP para logs.
     *
     * @param body body a sanitizar
     * @return body con campos sensibles enmascarados
     * @see TextUtilities#sanitizeBody(String)
     */
    public static String sanitizeBody(String body) {
        return TextUtilities.sanitizeBody(body);
    }

    /**
     * Trunca contenido largo para incluirlo en logs o mensajes.
     *
     * @param content   contenido a truncar
     * @param maxLength longitud máxima permitida
     * @return contenido truncado con {@code "... [TRUNCATED]"} si aplica
     * @see TextUtilities#truncateContent(String, int)
     */
    public static String truncateContent(String content, int maxLength) {
        return TextUtilities.truncateContent(content, maxLength);
    }

    /**
     * Sanitiza claves sensibles dentro de un JSON/texto para logs.
     *
     * @param data texto o JSON a sanitizar
     * @return texto con valores sensibles reemplazados
     * @see TextUtilities#sanitizeForLog(String)
     */
    public static String sanitizeForLog(String data) {
        return TextUtilities.sanitizeForLog(data);
    }
}
