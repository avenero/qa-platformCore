package com.scotia.qa.common.cucumber;

import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contexto centralizado para compartir datos entre diferentes capas (API, Web, Mobile)
 * durante la ejecución de un escenario Cucumber.
 * <p>
 * Permite almacenar y recuperar valores de manera thread-safe para:
 * - Variables extraídas de respuestas API
 * - Textos capturados de elementos Web/Mobile
 * - Cualquier dato que necesite compartirse entre steps
 * </p>
 * <p>
 * Soporta organización por capas usando prefijos:
 * - api.* : Variables de API
 * - web.* : Variables de Web
 * - mobile.* : Variables de Mobile
 * - shared.* : Variables compartidas entre capas
 * </p>
 *
 * @author QA Automation Team
 * @version 2.0
 */
public class ScenarioContext {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioContext.class);
    private static final ThreadLocal<Map<String, Object>> context = ThreadLocal.withInitial(ConcurrentHashMap::new);

    // Prefijos para organizar variables por origen
    public static final String API_PREFIX = "api.";
    public static final String WEB_PREFIX = "web.";
    public static final String MOBILE_PREFIX = "mobile.";
    public static final String SHARED_PREFIX = "shared.";

    /**
     * Almacena un valor en el contexto con prefijo de capa.
     *
     * @param layer Capa de origen (api, web, mobile, shared)
     * @param key   Clave para identificar el valor
     * @param value Valor a almacenar
     */
    public static void setByLayer(String layer, String key, Object value) throws FrameworkBusinessException {
        String fullKey = layer.toLowerCase() + "." + key;
        set(fullKey, value);
        TestLogger.logDebug("SCENARIO_CONTEXT",
                String.format("Valor almacenado en capa '%s' - Key: '%s', Value: '%s'", layer, key, value), null);
    }

    /**
     * Recupera un valor buscando en todas las capas si no se especifica prefijo.
     * Orden de búsqueda: shared -> api -> web -> mobile -> sin prefijo
     *
     * @param key Clave del valor a recuperar
     * @return El valor encontrado, o null si no existe
     */
    public static Object getFromAnyLayer(String key) {
        // Si ya tiene prefijo, buscar directamente
        if (key.contains(".")) {
            return context.get().get(key);
        }

        // Buscar en orden: shared -> api -> web -> mobile -> sin prefijo
        Object value = context.get().get(SHARED_PREFIX + key);
        if (value != null) return value;

        value = context.get().get(API_PREFIX + key);
        if (value != null) return value;

        value = context.get().get(WEB_PREFIX + key);
        if (value != null) return value;

        value = context.get().get(MOBILE_PREFIX + key);
        if (value != null) return value;

        return context.get().get(key);
    }

    /**
     * Recupera un valor de una capa específica.
     *
     * @param layer Capa de origen (api, web, mobile, shared)
     * @param key   Clave del valor
     * @return El valor almacenado, o null si no existe
     */
    public static Object getFromLayer(String layer, String key) {
        String fullKey = layer.toLowerCase() + "." + key;
        return context.get().get(fullKey);
    }

    /**
     * Obtiene todas las variables de una capa específica.
     *
     * @param layerPrefix Prefijo de la capa (API_PREFIX, WEB_PREFIX, etc.)
     * @return Mapa con las variables de esa capa (sin el prefijo en las claves)
     */
    public static Map<String, Object> getByLayer(String layerPrefix) {
        Map<String, Object> layerContext = new ConcurrentHashMap<>();
        context.get().forEach((key, value) -> {
            if (key.startsWith(layerPrefix)) {
                layerContext.put(key.substring(layerPrefix.length()), value);
            }
        });
        return layerContext;
    }

    /**
     * Compara un valor del contexto (de cualquier capa) con un valor esperado.
     *
     * @param key           Clave del valor en contexto
     * @param expectedValue Valor esperado para comparar
     * @return true si coinciden, false si no
     */
    public static boolean compareWithValue(String key, Object expectedValue) {
        Object actualValue = getFromAnyLayer(key);
        if (actualValue == null) {
            TestLogger.logWarning("SCENARIO_CONTEXT",
                    String.format("Variable '%s' no encontrada para comparación", key), null);
            return false;
        }

        boolean matches = actualValue.equals(expectedValue) ||
                String.valueOf(actualValue).equals(String.valueOf(expectedValue));

        if (matches) {
            TestLogger.logInfo("SCENARIO_CONTEXT",
                    String.format("✅ Comparación exitosa - Key: '%s', Esperado: '%s', Actual: '%s'",
                            key, expectedValue, actualValue), null);
        } else {
            TestLogger.logWarning("SCENARIO_CONTEXT",
                    String.format("❌ Comparación falló - Key: '%s', Esperado: '%s', Actual: '%s'",
                            key, expectedValue, actualValue), null);
        }

        return matches;
    }

    /**
     * Imprime el estado del contexto agrupado por capas (útil para debugging).
     */
    public static void printContextByLayers() {
        Map<String, Object> ctx = context.get();
        TestLogger.logInfo("SCENARIO_CONTEXT",
                String.format("=== 📋 Estado del Contexto (%d variables) ===", ctx.size()), null);

        printLayerVariables("🌐 API", API_PREFIX);
        printLayerVariables("🖥️  WEB", WEB_PREFIX);
        printLayerVariables("📱 MOBILE", MOBILE_PREFIX);
        printLayerVariables("🔗 SHARED", SHARED_PREFIX);

        // Variables sin prefijo
        long noPrefixCount = ctx.keySet().stream().filter(k -> !k.contains(".")).count();
        if (noPrefixCount > 0) {
            TestLogger.logInfo("SCENARIO_CONTEXT", String.format("  🔹 SIN PREFIJO (%d):", noPrefixCount), null);
            ctx.forEach((key, value) -> {
                if (!key.contains(".")) {
                    TestLogger.logInfo("SCENARIO_CONTEXT",
                            String.format("    • %s = %s", key, truncateValue(value)), null);
                }
            });
        }

        TestLogger.logInfo("SCENARIO_CONTEXT", "=".repeat(50), null);
    }

    private static void printLayerVariables(String layerName, String prefix) {
        Map<String, Object> layerVars = getByLayer(prefix);
        if (!layerVars.isEmpty()) {
            TestLogger.logInfo("SCENARIO_CONTEXT",
                    String.format("  %s (%d):", layerName, layerVars.size()), null);
            layerVars.forEach((key, value) ->
                    TestLogger.logInfo("SCENARIO_CONTEXT",
                            String.format("    • %s = %s", key, truncateValue(value)), null)
            );
        }
    }

    private static String truncateValue(Object value) {
        if (value == null) return "null";
        String str = String.valueOf(value);
        return str.length() > 100 ? str.substring(0, 100) + "..." : str;
    }

    /**
     * Almacena un valor en el contexto del escenario actual.
     *
     * @param key   Clave única para identificar el valor
     * @param value Valor a almacenar (puede ser String, Integer, Map, List, etc.)
     */
    public static void set(String key, Object value) throws FrameworkBusinessException {
        if (key == null || key.trim().isEmpty()) {
            TestLogger.logError("SCENARIO_CONTEXT", "No se puede guardar un valor con clave vacía o nula", null);
            throw new FrameworkBusinessException("set: La clave no puede ser nula o vacía");
        }
        context.get().put(key, value);
        TestLogger.logDebug("SCENARIO_CONTEXT",
                String.format("Valor almacenado en contexto - Key: '%s', Value: '%s'", key, value), null);
    }

    /**
     * Recupera un valor del contexto como Object.
     *
     * @param key Clave del valor a recuperar
     * @return El valor almacenado, o null si no existe
     */
    public static Object get(String key) throws FrameworkBusinessException {
        if (key == null || key.trim().isEmpty()) {
            TestLogger.logError("SCENARIO_CONTEXT", "No se puede recuperar un valor con clave vacía o nula", null);
            throw new FrameworkBusinessException("get: La clave no puede ser nula o vacía");
        }
        Object value = context.get().get(key);
        TestLogger.logDebug("SCENARIO_CONTEXT",
                String.format("Valor recuperado del contexto - Key: '%s', Value: '%s'", key, value), null);
        return value;
    }

    /**
     * Recupera un valor del contexto como String.
     * Busca automáticamente en todas las capas si no se especifica prefijo.
     *
     * @param key Clave del valor a recuperar
     * @return El valor como String, o null si no existe
     */
    public static String getString(String key) throws FrameworkBusinessException {
        Object value = key.contains(".") ? get(key) : getFromAnyLayer(key);
        if (value == null) {
            TestLogger.logWarning("SCENARIO_CONTEXT",
                    String.format("No se encontró valor para la clave: '%s'", key), null);
            return null;
        }
        return value.toString();
    }

    /**
     * Recupera un valor del contexto con un valor por defecto si no existe.
     *
     * @param key          Clave del valor a recuperar
     * @param defaultValue Valor por defecto a retornar si la clave no existe
     * @return El valor almacenado o el valor por defecto
     */
    public static String getString(String key, String defaultValue) throws FrameworkBusinessException {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Verifica si existe una clave en el contexto.
     *
     * @param key Clave a verificar
     * @return true si la clave existe, false en caso contrario
     */
    public static boolean containsKey(String key) {
        boolean exists = context.get().containsKey(key);
        TestLogger.logDebug("SCENARIO_CONTEXT",
                String.format("Verificación de clave '%s' en contexto: %s", key, exists), null);
        return exists;
    }

    /**
     * Elimina un valor del contexto.
     *
     * @param key Clave del valor a eliminar
     * @return El valor eliminado, o null si no existía
     */
    public static Object remove(String key) {
        Object removed = context.get().remove(key);
        TestLogger.logDebug("SCENARIO_CONTEXT",
                String.format("Valor eliminado del contexto - Key: '%s'", key), null);
        return removed;
    }

    /**
     * Limpia completamente el contexto del escenario actual.
     * Este método debe ser llamado automáticamente por los hooks de Cucumber.
     */
    public static void clear() {
        int size = context.get().size();
        context.get().clear();
        TestLogger.logDebug("SCENARIO_CONTEXT",
                String.format("Contexto limpiado. Se eliminaron %d elementos", size), null);
    }

    /**
     * Obtiene el tamaño actual del contexto.
     *
     * @return Número de elementos almacenados en el contexto
     */
    public static int size() {
        return context.get().size();
    }

    /**
     * Obtiene una copia del mapa completo del contexto.
     * Útil para debugging o logging.
     *
     * @return Mapa con todos los valores del contexto
     */
    public static Map<String, Object> getAll() {
        return new ConcurrentHashMap<>(context.get());
    }

    /**
     * Reemplaza variables en un texto usando el formato ${variable}.
     * Busca las variables en el contexto (en todas las capas) y las reemplaza por sus valores.
     *
     * @param text Texto que puede contener variables en formato ${variable}
     * @return Texto con variables reemplazadas por sus valores del contexto
     */
    public static String replaceVariables(String text) {
        if (text == null || !text.contains("${")) {
            return text;
        }

        String result = text;

        // Buscar y reemplazar todas las variables ${...}
        while (result.contains("${")) {
            int start = result.indexOf("${");
            int end = result.indexOf("}", start);

            if (end == -1) break;

            String varName = result.substring(start + 2, end);
            Object value = getFromAnyLayer(varName);

            if (value != null) {
                String replacement = String.valueOf(value);
                result = result.substring(0, start) + replacement + result.substring(end + 1);
                TestLogger.logDebug("SCENARIO_CONTEXT",
                        String.format("🔄 Variable reemplazada: ${%s} -> %s", varName, replacement), null);
            } else {
                TestLogger.logWarning("SCENARIO_CONTEXT",
                        String.format("⚠️  Variable no encontrada en contexto: ${%s}", varName), null);
                break; // Evitar loop infinito
            }
        }

        return result;
    }

    /**
     * Compara dos valores del contexto.
     *
     * @param key1 Primera clave
     * @param key2 Segunda clave
     * @return true si los valores son iguales, false en caso contrario
     */
    public static boolean compareValues(String key1, String key2) throws FrameworkBusinessException {
        Object value1 = get(key1);
        Object value2 = get(key2);

        if (value1 == null || value2 == null) {
            TestLogger.logWarning("SCENARIO_CONTEXT",
                    String.format("No se pueden comparar valores: key1='%s' (%s), key2='%s' (%s)",
                            key1, value1, key2, value2), null);
            return false;
        }

        boolean equals = value1.toString().equals(value2.toString());
        TestLogger.logInfo("SCENARIO_CONTEXT",
                String.format("Comparación de valores - key1='%s' (%s) vs key2='%s' (%s): %s",
                        key1, value1, key2, value2, equals ? "IGUALES" : "DIFERENTES"), null);
        return equals;
    }

    /**
     * Verifica si un valor del contexto contiene un texto específico.
     *
     * @param key          Clave del valor a verificar
     * @param expectedText Texto que debe contener
     * @return true si el valor contiene el texto, false en caso contrario
     */
    public static boolean valueContains(String key, String expectedText) throws FrameworkBusinessException {
        String value = getString(key);
        if (value == null) {
            TestLogger.logWarning("SCENARIO_CONTEXT",
                    String.format("No se puede verificar contenido: la clave '%s' no existe", key), null);
            return false;
        }

        boolean contains = value.contains(expectedText);
        TestLogger.logInfo("SCENARIO_CONTEXT",
                String.format("Verificación de contenido - key='%s', valor='%s', contiene '%s': %s",
                        key, value, expectedText, contains), null);
        return contains;
    }

    /**
     * Obtiene todas las claves almacenadas en el contexto.
     *
     * @return Set con todas las claves del contexto
     */
    public static Set<String> getKeys() {
        return new HashSet<>(context.get().keySet());
    }
}

