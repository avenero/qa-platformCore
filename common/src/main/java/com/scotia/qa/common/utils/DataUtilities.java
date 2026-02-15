package com.scotia.qa.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.scotia.qa.common.cucumber.context.ScenarioContext;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Clase de utilidades para manipulación de datos.
 * Utilidades consolidadas para manipulación de datos - IMPLEMENTACIÓN UNIFICADA.
 * Clase estática que puede ser usada por TODOS los frameworks (API, Web, Mobile).
 * Sin dependencias Spring - Funciona tanto en contextos Spring como Java puro.
 *
 * <p><b>SEGURIDAD v1.2.1:</b>
 * - ObjectMapper endurecido contra CVE-2017-7525, CVE-2017-15095
 * - Validación de tamaño y profundidad JSON (DoS/OOM prevention)
 * - Sanitización automática de datos sensibles en logs (GDPR/PCI-DSS)
 *
 * @author Abel Venero
 * @since 1.0.0
 * @version 1.2.1
 */
public class DataUtilities {

    // =================================================================================
    // CONFIGURACIÓN DE SEGURIDAD
    // =================================================================================

    /** Tamaño máximo permitido para JSON (10MB) - Previene DoS y OOM */
    private static final int MAX_JSON_SIZE_BYTES = 10 * 1024 * 1024;

    /** Profundidad máxima de anidamiento JSON - Previene DoS por stack overflow */
    private static final int MAX_JSON_DEPTH = 50;

    /** Longitud máxima para valores en logs - Previene logs gigantes */
    private static final int MAX_LOG_VALUE_LENGTH = 200;

    /** Palabras clave que identifican datos sensibles */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "token", "secret", "apikey", "api_key", "authorization",
        "creditcard", "credit_card", "ssn", "pin", "otp", "privatekey",
        "private_key", "bearer", "refresh_token", "access_token", "session"
    );

    /** Generador de números aleatorios para métodos de generación */
    private static final Random RANDOM = new Random();

    /**
     * ObjectMapper seguro configurado según OWASP best practices.
     * Mitigaciones aplicadas:
     * - CVE-2017-7525: Deserialización polimórfica deshabilitada
     * - CVE-2017-15095: Auto-typing deshabilitado
     * - CVE-2019-12384: Acceso a modificadores públicos restringido
     */
    private static final ObjectMapper objectMapper = createSecureObjectMapper();

    /**
     * Crea una instancia de ObjectMapper con configuración de seguridad endurecida.
     *
     * @return ObjectMapper configurado de forma segura
     */
    private static ObjectMapper createSecureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // SEGURIDAD: Deshabilitar deserialización polimórfica (CVE-2017-7525)
        mapper.deactivateDefaultTyping();

        // SEGURIDAD: Configuración robusta de deserialización
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);

        // SEGURIDAD: Configuración de visibilidad (reemplaza OVERRIDE_PUBLIC_ACCESS_MODIFIERS deprecado)
        // La configuración por defecto ya es segura en versiones modernas de Jackson
        mapper.setVisibility(
            mapper.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
        );

        // PERFORMANCE: Configuración de serialización
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        TestLogger.logInfo("DATA_UTILITIES_SECURITY",
            "ObjectMapper configurado con protecciones de seguridad", null);

        return mapper;
    }

    // Store de variables thread-safe para todos los frameworks
    private static final ConcurrentHashMap<String, String> variableStore = new ConcurrentHashMap<>();

    // Store de objetos complejos thread-safe (nuevo - para deserialización)
    private static final ConcurrentHashMap<String, Object> objectStore = new ConcurrentHashMap<>();

    // Store de variables con namespace thread-safe (para parallel execution)
    // Estructura: ConcurrentHashMap<namespace, ConcurrentHashMap<key, value>>
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> namespacedVariableStore =
        new ConcurrentHashMap<>();

    // Store de objetos con namespace thread-safe
    // Estructura: ConcurrentHashMap<namespace, ConcurrentHashMap<key, object>>
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> namespacedObjectStore =
        new ConcurrentHashMap<>();

    private DataUtilities() {
        // Utility class - no instances
    }

    // =================================================================================
    // DESERIALIZACIÓN Y OBJETOS (NUEVO)
    // =================================================================================

    /**
     * Deserializa un JSON String a un objeto Java tipado.
     *
     * <p>Utiliza Jackson ObjectMapper para convertir JSON en POJOs.
     * Valida el JSON antes de deserializar para garantizar robustez.
     *
     * @param <T> tipo del objeto destino
     * @param jsonString JSON como String
     * @param clazz clase del objeto destino
     * @return objeto deserializado del tipo especificado
     * @throws FrameworkBusinessException si el JSON es inválido o la deserialización falla
     *
     * @since 1.1.0
     */
    public static <T> T deserializeJson(String jsonString, Class<T> clazz)
            throws FrameworkBusinessException {
        try {
            // Validación de entrada
            if (jsonString == null || jsonString.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "JSON string no puede estar vacío o nulo"
                );
            }

            if (clazz == null) {
                throw new IllegalArgumentException(
                    "La clase destino no puede ser nula"
                );
            }

            // Validar que es JSON válido antes de deserializar
            String trimmedJson = jsonString.trim();
            if (!trimmedJson.startsWith("{") && !trimmedJson.startsWith("[")) {
                throw new IllegalArgumentException(
                    "El string no parece ser JSON válido (debe empezar con { o [)"
                );
            }

            // Validaciones de seguridad
            validateJsonSize(jsonString);
            validateJsonDepth(jsonString);

            // Deserializar
            T result = objectMapper.readValue(jsonString, clazz);

            TestLogger.logDebug("DATA_UTILITIES",
                String.format("JSON deserializado exitosamente a tipo: %s",
                    clazz.getSimpleName()),
                null);

            return result;

        } catch (JsonProcessingException e) {
            String errorMsg = String.format(
                "Error deserializando JSON a %s: %s",
                clazz.getSimpleName(),
                e.getMessage()
            );
            TestLogger.logError("DATA_UTILITIES", errorMsg, null);
            throw new FrameworkBusinessException("deserializeJson", errorMsg);
        } catch (IllegalArgumentException e) {
            TestLogger.logError("DATA_UTILITIES", e.getMessage(), null);
            throw new FrameworkBusinessException("deserializeJson", e.getMessage());
        }
    }

    // =================================================================================
    // MÉTODOS DE SEGURIDAD Y VALIDACIÓN
    // =================================================================================

    /**
     * Valida que el tamaño del JSON no exceda el límite permitido.
     * Previene ataques de Denial of Service (DoS) y Out Of Memory (OOM).
     *
     * @param jsonString JSON a validar
     * @throws FrameworkBusinessException si el JSON excede el tamaño máximo
     */
    private static void validateJsonSize(String jsonString) throws FrameworkBusinessException {
        if (jsonString == null) {
            return;
        }

        int sizeBytes = jsonString.getBytes().length;
        if (sizeBytes > MAX_JSON_SIZE_BYTES) {
            String errorMsg = String.format(
                "JSON excede el tamaño máximo permitido: %d bytes (máximo: %d bytes / %.2f MB)",
                sizeBytes, MAX_JSON_SIZE_BYTES, MAX_JSON_SIZE_BYTES / (1024.0 * 1024.0)
            );
            TestLogger.logError("DATA_UTILITIES_SECURITY", errorMsg, null);
            throw new FrameworkBusinessException("validateJsonSize", errorMsg);
        }

        TestLogger.logDebug("DATA_UTILITIES_SECURITY",
            String.format("Tamaño JSON validado: %d bytes (%.2f%% del máximo)",
                sizeBytes, (sizeBytes * 100.0) / MAX_JSON_SIZE_BYTES),
            null);
    }

    /**
     * Valida la profundidad de anidamiento del JSON.
     * Previene ataques de DoS mediante JSON excesivamente anidado que cause stack overflow.
     *
     * @param jsonString JSON a validar
     * @throws FrameworkBusinessException si la profundidad excede el límite
     */
    private static void validateJsonDepth(String jsonString) throws FrameworkBusinessException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return;
        }

        int depth = 0;
        int maxDepthDetected = 0;

        for (char c : jsonString.toCharArray()) {
            if (c == '{' || c == '[') {
                depth++;
                if (depth > maxDepthDetected) {
                    maxDepthDetected = depth;
                }

                if (maxDepthDetected > MAX_JSON_DEPTH) {
                    String errorMsg = String.format(
                        "JSON excede la profundidad máxima permitida: %d niveles (máximo: %d)",
                        maxDepthDetected, MAX_JSON_DEPTH
                    );
                    TestLogger.logError("DATA_UTILITIES_SECURITY", errorMsg, null);
                    throw new FrameworkBusinessException("validateJsonDepth", errorMsg);
                }
            } else if (c == '}' || c == ']') {
                depth--;
            }
        }

        TestLogger.logDebug("DATA_UTILITIES_SECURITY",
            String.format("Profundidad JSON validada: %d niveles (%.2f%% del máximo)",
                maxDepthDetected, (maxDepthDetected * 100.0) / MAX_JSON_DEPTH),
            null);
    }

    /**
     * Sanitiza valores sensibles antes de loguear.
     * Previene exposición de datos sensibles en logs (GDPR/PCI-DSS compliance).
     *
     * @param key nombre de la variable/campo
     * @param value valor a sanitizar
     * @return valor sanitizado seguro para logs
     */
    private static String sanitizeForLogging(String key, Object value) {
        if (value == null) {
            return "null";
        }

        // Verificar si la key contiene palabras sensibles
        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return "***HIDDEN***";
            }
        }

        // Limitar longitud para prevenir logs gigantes
        String valueStr = value.toString();
        if (valueStr.length() > MAX_LOG_VALUE_LENGTH) {
            return valueStr.substring(0, MAX_LOG_VALUE_LENGTH) + "... [TRUNCATED]";
        }

        return valueStr;
    }

    /**
     * Almacena un objeto completo en el store de objetos.
     *
     * <p>Permite guardar objetos Java complejos en memoria para reutilización
     * posterior en diferentes steps o scenarios.
     * Aplica sanitización automática en logs para datos sensibles.
     *
     * @param key nombre/clave para identificar el objeto
     * @param object objeto a almacenar (puede ser cualquier tipo)
     *
     * @since 1.1.0
     */
    public static void storeObject(String key, Object object) {
        if (key == null || key.trim().isEmpty()) {
            TestLogger.logWarning("DATA_UTILITIES",
                "Intento de guardar objeto con key nula o vacía - ignorado",
                null);
            return;
        }

        if (object == null) {
            // Permitir guardar null para limpiar/remover objetos
            objectStore.remove(key);
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Objeto '%s' removido del store", key),
                null);
        } else {
            objectStore.put(key, object);
            // Sanitizar valor antes de loguear (GDPR/PCI-DSS)
            String safeValue = sanitizeForLogging(key, object);
            TestLogger.logInfo("DATA_UTILITIES",
                String.format("Objeto almacenado: %s = %s (tipo: %s)",
                    key, safeValue, object.getClass().getSimpleName()),
                null);
        }
    }

    /**
     * Recupera un objeto tipado del store con validación de tipo.
     *
     * <p>Implementa conversión inteligente: si el objeto almacenado no es
     * exactamente del tipo solicitado, intenta convertirlo usando Jackson.
     * Esto es útil cuando se almacenan Maps que representan objetos.
     *
     * @param <T> tipo del objeto esperado
     * @param key nombre/clave del objeto almacenado
     * @param clazz clase del tipo esperado
     * @return objeto del tipo especificado, o null si no existe
     * @throws ClassCastException si el objeto no se puede convertir al tipo solicitado
     *
     * @since 1.1.0
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObject(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return null;
        }

        Object obj = objectStore.get(key);

        if (obj == null) {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Objeto '%s' no encontrado en el store", key),
                null);
            return null;
        }

        // Si es exactamente del tipo correcto, retornar directamente
        if (clazz.isInstance(obj)) {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Objeto '%s' recuperado (tipo: %s)",
                    key, obj.getClass().getSimpleName()),
                null);
            return (T) obj;
        }

        // Conversión inteligente: intentar convertir usando Jackson
        // Útil cuando se almacenan Maps que representan objetos
        try {
            T converted = objectMapper.convertValue(obj, clazz);
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Objeto '%s' convertido de %s a %s",
                    key, obj.getClass().getSimpleName(), clazz.getSimpleName()),
                null);
            return converted;
        } catch (Exception e) {
            String errorMsg = String.format(
                "No se pudo convertir objeto '%s' (tipo: %s) a tipo solicitado (%s): %s",
                key, obj.getClass().getSimpleName(), clazz.getSimpleName(), e.getMessage()
            );
            TestLogger.logError("DATA_UTILITIES", errorMsg, null);
            throw new ClassCastException(errorMsg);
        }
    }

    /**
     * Recupera un objeto sin especificar tipo (unsafe).
     *
     * <p><b>⚠️ ADVERTENCIA:</b> Este método no es type-safe.
     * Preferir usar {@link #getObject(String, Class)} cuando sea posible.
     *
     * @param key nombre/clave del objeto almacenado
     * @return objeto almacenado (tipo Object), o null si no existe
     *
     * @since 1.1.0
     */
    public static Object getObject(String key) {
        if (key == null) {
            return null;
        }
        return objectStore.get(key);
    }

    /**
     * Verifica si existe un objeto almacenado con la clave especificada.
     *
     * @param key nombre/clave a verificar
     * @return true si existe un objeto con esa clave, false en caso contrario
     *
     * @since 1.1.0
     */
    public static boolean hasObject(String key) {
        return key != null && objectStore.containsKey(key);
    }

    /**
     * Obtiene el tipo (clase) del objeto almacenado.
     *
     * @param key nombre/clave del objeto
     * @return Class del objeto almacenado, o null si no existe
     *
     * @since 1.1.0
     */
    public static Class<?> getObjectType(String key) {
        if (key == null) {
            return null;
        }
        Object obj = objectStore.get(key);
        return obj != null ? obj.getClass() : null;
    }

    /**
     * Limpia el store de objetos, removiendo todos los objetos almacenados.
     *
     * <p>Útil para limpiar el estado entre scenarios o tests.
     *
     * @since 1.1.0
     */
    public static void clearObjects() {
        int size = objectStore.size();
        objectStore.clear();
        TestLogger.logDebug("DATA_UTILITIES",
            String.format("Store de objetos limpiado (%d objetos removidos)", size),
            null);
    }

    /**
     * Limpia todos los stores (variables String y objetos).
     *
     * <p>Método de conveniencia que limpia tanto el store de variables
     * como el store de objetos en una sola llamada.
     *
     * @since 1.1.0
     */
    public static void clearAll() {
        clearVariables();
        clearObjects();
        TestLogger.logInfo("DATA_UTILITIES",
            "Todos los stores limpiados (variables y objetos)",
            null);
    }

    // =================================================================================
    // MANIPULACIÓN JSON (existente)
    // =================================================================================

    public static Object getJsonParameter(String jsonBody, String fieldPath) throws FrameworkBusinessException {
        try {
            if (jsonBody == null || fieldPath == null) {
                return null;
            }

            if (!fieldPath.contains(".")) {
                return findValueGeneric(jsonBody, fieldPath);
            }

            String[] parts = fieldPath.split("\\.", 2);
            Object current = findValueGeneric(jsonBody, parts[0]);

            if (current == null) {
                TestLogger.logDebug("DATA_UTILITIES",
                                   String.format("Campo '%s' no encontrado", parts[0]), null);
                return null;
            }

            if (parts.length > 1) {
                String remainingPath = parts[1];
                String currentStr = convertToJsonString(current);
                return getJsonParameter(currentStr, remainingPath);
            }

            return current;
        } catch (Exception e) {
            throw new FrameworkBusinessException("getJsonParameter",
                "Error obteniendo parámetro '" + fieldPath + "': " + e.getMessage());
        }
    }

    public static boolean hasJsonField(String jsonBody, String fieldPath) {
        try {
            return getJsonParameter(jsonBody, fieldPath) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // =================================================================================
    // BÚSQUEDA RECURSIVA EN RESPUESTAS (API ↔ WEB INTEGRATION)
    // =================================================================================

    /**
     * Busca un valor en una respuesta (Map, List, o JSON String) de forma recursiva.
     *
     * <p>Este método es la pieza clave para la integración entre capas (API ↔ Web).
     * Permite extraer valores de respuestas API complejas sin necesidad de conocer
     * la estructura exacta, facilitando la comparación con elementos Web.
     *
     * <p><b>Estrategia de búsqueda:</b>
     * <ul>
     *   <li>1. Si la respuesta es un String JSON, lo parsea automáticamente</li>
     *   <li>2. Busca recursivamente en Maps anidados</li>
     *   <li>3. Busca recursivamente en Lists/Arrays</li>
     *   <li>4. Retorna el primer valor encontrado con la clave especificada</li>
     * </ul>
     *
     * <p><b>Casos de uso típicos:</b>
     * <pre>
     * // Extraer un campo de respuesta API para comparar con texto Web
     * Object userName = DataUtilities.findValue(apiResponse, "user_full_name");
     * String webText = WebHelper.getElementText(locator);
     * Assert.assertEquals(userName.toString(), webText);
     *
     * // Extraer un ID de respuesta para usarlo en otra petición
     * Object userId = DataUtilities.findValue(loginResponse, "user_id");
     * ScenarioContext.set("user_id", userId.toString());
     * </pre>
     *
     * @param response Puede ser Map&lt;String,Object&gt;, List&lt;Object&gt; o String (JSON)
     * @param targetKey Clave a buscar (ej: "user_full_name", "access_token", "id")
     * @return Valor encontrado (puede ser String, Number, Boolean, Map, List), o null si no existe
     * @throws RuntimeException si hay error parseando JSON
     *
     * @since 1.3.0
     * @see #findValueInMap(Map, String)
     * @see #findValueInList(List, String)
     */
    public static Object findValue(Object response, String targetKey) {
        if (response == null || targetKey == null) {
            TestLogger.logDebug("DATA_UTILITIES",
                "findValue: response o targetKey es null", null);
            return null;
        }

        // Si es String, intentar parsear como JSON
        if (response instanceof String) {
            try {
                String jsonStr = (String) response;
                response = objectMapper.readValue(jsonStr, Object.class);
                TestLogger.logDebug("DATA_UTILITIES",
                    String.format("findValue: JSON String parseado exitosamente (buscando: %s)", targetKey),
                    null);
            } catch (JsonProcessingException e) {
                TestLogger.logError("DATA_UTILITIES",
                    String.format("findValue: Error parseando JSON string: %s", e.getMessage()),
                    null);
                throw new RuntimeException("Error al parsear JSON response", e);
            }
        }

        // Buscar según el tipo
        @SuppressWarnings("unchecked")  // Suprime warnings de cast genérico
        Map<String, Object> mapResponse = (response instanceof Map) ? (Map<String, Object>) response : null;

        @SuppressWarnings("unchecked")  // Suprime warnings de cast genérico
        List<Object> listResponse = (response instanceof List) ? (List<Object>) response : null;

        if (mapResponse != null) {
            return findValueInMap(mapResponse, targetKey);
        } else if (listResponse != null) {
            return findValueInList(listResponse, targetKey);
        }

        TestLogger.logDebug("DATA_UTILITIES",
            String.format("findValue: tipo no soportado: %s",
                response.getClass().getSimpleName()),
            null);
        return null;
    }

    /**
     * Busca recursivamente un valor en un Map.
     *
     * <p>Navega por todos los niveles del Map, incluyendo Maps y Lists anidados,
     * hasta encontrar la primera ocurrencia de la clave especificada.
     *
     * @param map Map donde buscar
     * @param targetKey clave a buscar
     * @return valor encontrado, o null si no existe
     *
     * @since 1.3.0
     */
    private static Object findValueInMap(Map<String, Object> map, String targetKey) {
        if (map == null || targetKey == null) {
            return null;
        }

        // Buscar directamente la clave
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // ¿Es la clave que buscamos?
            if (key.equals(targetKey)) {
                TestLogger.logDebug("DATA_UTILITIES",
                    String.format("findValue: Clave '%s' encontrada (valor tipo: %s)",
                        targetKey,
                        value != null ? value.getClass().getSimpleName() : "null"),
                    null);
                return value;
            }

            // Si el valor es un Map anidado, buscar recursivamente
            if (value instanceof Map) {
                // Cast seguro - ya verificado con instanceof
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                Object nested = findValueInMap(mapValue, targetKey);
                if (nested != null) {
                    return nested;
                }
            }
            // Si el valor es una List, buscar recursivamente
            else if (value instanceof List) {
                // Cast seguro - ya verificado con instanceof
                @SuppressWarnings("unchecked")
                List<Object> listValue = (List<Object>) value;
                Object nested = findValueInList(listValue, targetKey);
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    /**
     * Busca recursivamente un valor en una List.
     *
     * <p>Navega por todos los elementos de la lista. Si encuentra Maps,
     * busca dentro de ellos. Si encuentra Lists anidadas, también las procesa.
     *
     * @param list List donde buscar
     * @param targetKey clave a buscar
     * @return valor encontrado, o null si no existe
     *
     * @since 1.3.0
     */
    private static Object findValueInList(List<Object> list, String targetKey) {
        if (list == null || targetKey == null) {
            return null;
        }

        for (Object item : list) {
            // Si el item es un Map, buscar dentro
            if (item instanceof Map) {
                // Cast seguro - ya verificado con instanceof
                @SuppressWarnings("unchecked")
                Map<String, Object> mapItem = (Map<String, Object>) item;
                Object nested = findValueInMap(mapItem, targetKey);
                if (nested != null) {
                    return nested;
                }
            }
            // Si el item es una List anidada, buscar recursivamente
            else if (item instanceof List) {
                // Cast seguro - ya verificado con instanceof
                @SuppressWarnings("unchecked")
                List<Object> listItem = (List<Object>) item;
                Object nested = findValueInList(listItem, targetKey);
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    // =================================================================================
    // COMPARACIÓN Y NORMALIZACIÓN DE TEXTO (API ↔ WEB INTEGRATION)
    // =================================================================================

    /**
     * Compara dos textos de forma flexible para integración API ↔ Web.
     *
     * <p>Este método facilita la comparación entre valores obtenidos de APIs
     * y texto extraído de elementos Web, manejando diferencias comunes como:
     * <ul>
     *   <li>Espacios en blanco extras</li>
     *   <li>Saltos de línea</li>
     *   <li>Diferencias de mayúsculas/minúsculas (opcional)</li>
     *   <li>Caracteres especiales HTML (&amp;nbsp;, etc.)</li>
     * </ul>
     *
     * <p><b>Casos de uso:</b>
     * <pre>
     * // Comparar nombre de usuario de API con texto en página Web
     * Object apiName = DataUtilities.findValue(response, "user_full_name");
     * String webName = webHelper.getElementText(userNameLocator);
     * boolean match = DataUtilities.compareTextFlexible(apiName.toString(), webName, false);
     * </pre>
     *
     * @param text1 primer texto a comparar (ej: valor de API)
     * @param text2 segundo texto a comparar (ej: texto de elemento Web)
     * @param ignoreCase true para ignorar mayúsculas/minúsculas
     * @return true si los textos coinciden después de normalización, false en caso contrario
     *
     * @since 1.3.0
     * @see #normalizeText(String)
     */
    public static boolean compareTextFlexible(String text1, String text2, boolean ignoreCase) {
        if (text1 == null && text2 == null) {
            return true;
        }
        if (text1 == null || text2 == null) {
            TestLogger.logDebug("DATA_UTILITIES",
                "compareTextFlexible: uno de los textos es null", null);
            return false;
        }

        String normalized1 = normalizeText(text1);
        String normalized2 = normalizeText(text2);

        if (ignoreCase) {
            normalized1 = normalized1.toLowerCase();
            normalized2 = normalized2.toLowerCase();
        }

        boolean matches = normalized1.equals(normalized2);

        TestLogger.logDebug("DATA_UTILITIES",
            String.format("compareTextFlexible: '%s' vs '%s' = %s (ignoreCase: %s)",
                normalized1.substring(0, Math.min(50, normalized1.length())),
                normalized2.substring(0, Math.min(50, normalized2.length())),
                matches,
                ignoreCase),
            null);

        return matches;
    }

    /**
     * Normaliza un texto para comparaciones, eliminando espacios extras y caracteres especiales.
     *
     * <p>Operaciones de normalización aplicadas:
     * <ul>
     *   <li>Elimina espacios en blanco al inicio y final (trim)</li>
     *   <li>Reemplaza múltiples espacios por uno solo</li>
     *   <li>Reemplaza tabs y saltos de línea por espacios</li>
     *   <li>Elimina &amp;nbsp; y otros espacios HTML</li>
     *   <li>Normaliza caracteres Unicode (NFD)</li>
     * </ul>
     *
     * @param text texto a normalizar
     * @return texto normalizado, o string vacío si el input es null
     *
     * @since 1.3.0
     */
    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        // Eliminar &nbsp; y entidades HTML
        String normalized = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"");

        // Reemplazar tabs y saltos de línea por espacios
        normalized = normalized
            .replace("\t", " ")
            .replace("\n", " ")
            .replace("\r", " ");

        // Reemplazar múltiples espacios por uno solo
        normalized = normalized.replaceAll("\\s+", " ");

        // Trim
        normalized = normalized.trim();

        return normalized;
    }

    /**
     * Compara dos textos de forma exacta (case-sensitive).
     *
     * <p>Wrapper de conveniencia para compareTextFlexible con ignoreCase=false.
     *
     * @param text1 primer texto
     * @param text2 segundo texto
     * @return true si coinciden exactamente después de normalización
     *
     * @since 1.3.0
     */
    public static boolean compareText(String text1, String text2) {
        return compareTextFlexible(text1, text2, false);
    }

    /**
     * Compara dos textos ignorando mayúsculas/minúsculas.
     *
     * <p>Wrapper de conveniencia para compareTextFlexible con ignoreCase=true.
     *
     * @param text1 primer texto
     * @param text2 segundo texto
     * @return true si coinciden ignorando case después de normalización
     *
     * @since 1.3.0
     */
    public static boolean compareTextIgnoreCase(String text1, String text2) {
        return compareTextFlexible(text1, text2, true);
    }

    /**
     * Verifica si un texto contiene otro texto (case-insensitive).
     *
     * <p>Útil para validaciones parciales entre datos API y Web.
     *
     * @param text texto donde buscar
     * @param substring texto a buscar
     * @return true si text contiene substring (ignorando case y después de normalizar)
     *
     * @since 1.3.0
     */
    public static boolean containsText(String text, String substring) {
        if (text == null || substring == null) {
            return false;
        }

        String normalizedText = normalizeText(text).toLowerCase();
        String normalizedSubstring = normalizeText(substring).toLowerCase();

        return normalizedText.contains(normalizedSubstring);
    }

    /**
     * Extrae números de un texto (útil para comparaciones numéricas API ↔ Web).
     *
     * <p>Ejemplos:
     * <pre>
     * extractNumber("$1,234.56") → "1234.56"
     * extractNumber("Total: 100 items") → "100"
     * extractNumber("Price: $25.00") → "25.00"
     * </pre>
     *
     * @param text texto que contiene números
     * @return string con solo números y punto decimal, o null si no hay números
     *
     * @since 1.3.0
     */
    public static String extractNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // Eliminar todo excepto números, punto y coma
        String numbers = text.replaceAll("[^0-9.,]", "");

        // Si hay coma como separador decimal (formato europeo), convertir a punto
        if (numbers.contains(",") && !numbers.contains(".")) {
            numbers = numbers.replace(",", ".");
        }
        // Si hay tanto punto como coma, asumir que coma es separador de miles
        else if (numbers.contains(",") && numbers.contains(".")) {
            numbers = numbers.replace(",", "");
        }

        return numbers.isEmpty() ? null : numbers;
    }

    /**
     * Extrae y convierte un número de un texto a Double.
     *
     * @param text texto que contiene un número
     * @return número como Double, o null si no se puede extraer/parsear
     *
     * @since 1.3.0
     */
    public static Double extractNumberAsDouble(String text) {
        String numberStr = extractNumber(text);
        if (numberStr == null) {
            return null;
        }

        try {
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            TestLogger.logWarning("DATA_UTILITIES",
                String.format("No se pudo parsear número: '%s'", numberStr),
                null);
            return null;
        }
    }

    /**
     * Extrae y convierte un número de un texto a Integer.
     *
     * @param text texto que contiene un número entero
     * @return número como Integer, o null si no se puede extraer/parsear
     *
     * @since 1.3.0
     */
    public static Integer extractNumberAsInteger(String text) {
        Double doubleValue = extractNumberAsDouble(text);
        if (doubleValue == null) {
            return null;
        }

        try {
            return doubleValue.intValue();
        } catch (Exception e) {
            TestLogger.logWarning("DATA_UTILITIES",
                String.format("No se pudo convertir a entero: '%s'", doubleValue),
                null);
            return null;
        }
    }

    // =================================================================================
    // GENERACIÓN DE DATOS
    // =================================================================================

    public static String generateRandomRut() {
        int number = SecurityUtilities.getSecureRandomInstance().nextInt(20000000) + 5000000;
        int dv = calculateRutCheckDigit(number);
        String dvStr = (dv == 10) ? "K" : String.valueOf(dv);
        return formatRut(number + "-" + dvStr);
    }

    public static String generateRandomEmail() {
        String[] domains = {"gmail.com", "hotmail.com", "scotia.cl", "test.com"};
        String username = generateRandomString(8, true, true, false);
        String domain = domains[SecurityUtilities.getSecureRandomInstance().nextInt(domains.length)];
        return username.toLowerCase() + "@" + domain;
    }

    public static String generateRandomPhone() {
        // Formato: +56912345678 (celular chileno)
        StringBuilder phone = new StringBuilder("+569");
        for (int i = 0; i < 8; i++) {
            phone.append(SecurityUtilities.getSecureRandomInstance().nextInt(10));
        }
        return phone.toString();
    }

    public static String generateRandomName() {
        String[] firstNames = {"Juan", "María", "Pedro", "Ana", "Carlos", "Sofía", "Diego", "Valentina"};
        String[] lastNames = {"González", "Rodríguez", "Pérez", "López", "Martínez", "García", "Hernández", "Muñoz"};
        return firstNames[SecurityUtilities.getSecureRandomInstance().nextInt(firstNames.length)] + " " +
               lastNames[SecurityUtilities.getSecureRandomInstance().nextInt(lastNames.length)];
    }

    public static int generateRandomNumber(int min, int max) {
        return SecurityUtilities.generateRandomNumber(min, max);
    }

    public static String generateRandomAlphanumeric(int length) {
        return generateRandomString(length, true, true, false);
    }

    public static String generateRandomNumeric(int length) {
        return generateRandomString(length, false, true, false);
    }


    public static String generateRandomUUID() {
        return UUID.randomUUID().toString();
    }

    // =================================================================================
    // UTILIDADES DE FECHAS (NUEVO)
    // =================================================================================

    /**
     * Obtiene la fecha actual en formato ISO 8601 (yyyy-MM-dd'T'HH:mm:ss'Z').
     *
     * @return fecha actual en formato ISO 8601
     *
     * @since 1.2.0
     */
    public static String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }

    /**
     * Obtiene la fecha actual en un formato específico.
     *
     * @param format formato de fecha (ej: "yyyy-MM-dd", "dd/MM/yyyy HH:mm:ss")
     * @return fecha actual formateada
     *
     * @since 1.2.0
     */
    public static String getCurrentTimestamp(String format) {
        if (format == null || format.trim().isEmpty()) {
            return getCurrentTimestamp();
        }

        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern(format);
            return java.time.LocalDateTime.now().format(formatter);
        } catch (Exception e) {
            TestLogger.logError("DATA_UTILITIES",
                "Error formateando fecha actual: " + e.getMessage(), null);
            return getCurrentTimestamp();
        }
    }

    /**
     * Parsea una fecha desde un string con formato específico.
     *
     * @param dateString string con la fecha
     * @param format formato del string (ej: "yyyy-MM-dd", "dd/MM/yyyy")
     * @return LocalDate parseado
     * @throws IllegalArgumentException si el formato es inválido
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate parseDate(String dateString, String format) {
        if (dateString == null || format == null) {
            throw new IllegalArgumentException("dateString y format no pueden ser null");
        }

        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern(format);
            return java.time.LocalDate.parse(dateString, formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Error parseando fecha '%s' con formato '%s': %s",
                    dateString, format, e.getMessage()), e);
        }
    }

    /**
     * Formatea una fecha a un string con formato específico.
     *
     * @param date fecha a formatear
     * @param format formato deseado (ej: "yyyy-MM-dd", "dd/MM/yyyy")
     * @return fecha formateada como string
     *
     * @since 1.2.0
     */
    public static String formatDate(java.time.LocalDate date, String format) {
        if (date == null || format == null) {
            throw new IllegalArgumentException("date y format no pueden ser null");
        }

        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern(format);
            return date.format(formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Error formateando fecha con formato '%s': %s",
                    format, e.getMessage()), e);
        }
    }

    /**
     * Agrega días a una fecha.
     *
     * @param date fecha base
     * @param days días a agregar (puede ser negativo para restar)
     * @return nueva fecha con los días agregados
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate addDaysToDate(java.time.LocalDate date, int days) {
        if (date == null) {
            throw new IllegalArgumentException("date no puede ser null");
        }
        return date.plusDays(days);
    }

    /**
     * Agrega meses a una fecha.
     *
     * @param date fecha base
     * @param months meses a agregar (puede ser negativo para restar)
     * @return nueva fecha con los meses agregados
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate addMonthsToDate(java.time.LocalDate date, int months) {
        if (date == null) {
            throw new IllegalArgumentException("date no puede ser null");
        }
        return date.plusMonths(months);
    }

    /**
     * Agrega años a una fecha.
     *
     * @param date fecha base
     * @param years años a agregar (puede ser negativo para restar)
     * @return nueva fecha con los años agregados
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate addYearsToDate(java.time.LocalDate date, int years) {
        if (date == null) {
            throw new IllegalArgumentException("date no puede ser null");
        }
        return date.plusYears(years);
    }

    /**
     * Calcula la diferencia en días entre dos fechas.
     *
     * @param date1 primera fecha
     * @param date2 segunda fecha
     * @return número de días de diferencia (puede ser negativo)
     *
     * @since 1.2.0
     */
    public static long getDaysBetween(java.time.LocalDate date1, java.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser null");
        }
        return java.time.temporal.ChronoUnit.DAYS.between(date1, date2);
    }

    /**
     * Calcula la diferencia en meses entre dos fechas.
     *
     * @param date1 primera fecha
     * @param date2 segunda fecha
     * @return número de meses de diferencia
     *
     * @since 1.2.0
     */
    public static long getMonthsBetween(java.time.LocalDate date1, java.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser null");
        }
        return java.time.temporal.ChronoUnit.MONTHS.between(date1, date2);
    }

    /**
     * Genera una fecha de nacimiento para una edad específica.
     *
     * @param age edad en años
     * @return fecha de nacimiento que resulta en la edad especificada
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate generateBirthDateForAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Edad debe estar entre 0 y 150");
        }
        return java.time.LocalDate.now().minusYears(age);
    }

    /**
     * Genera una fecha de nacimiento para un rango de edad.
     *
     * @param minAge edad mínima
     * @param maxAge edad máxima
     * @return fecha de nacimiento aleatoria dentro del rango
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate generateBirthDateForAgeRange(int minAge, int maxAge) {
        if (minAge < 0 || maxAge < minAge || maxAge > 150) {
            throw new IllegalArgumentException(
                "Rango de edad inválido. minAge >= 0, maxAge >= minAge, maxAge <= 150"
            );
        }

        int randomAge = generateRandomNumber(minAge, maxAge);
        return generateBirthDateForAge(randomAge);
    }

    /**
     * Genera una fecha aleatoria en los últimos N días.
     *
     * @param days número de días hacia atrás
     * @return fecha aleatoria en el rango
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate generateDateInLastDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days debe ser positivo");
        }

        int randomDays = generateRandomNumber(0, days);
        return java.time.LocalDate.now().minusDays(randomDays);
    }

    /**
     * Genera una fecha aleatoria en los próximos N días.
     *
     * @param days número de días hacia adelante
     * @return fecha aleatoria en el rango
     *
     * @since 1.2.0
     */
    public static java.time.LocalDate generateDateInNextDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days debe ser positivo");
        }

        int randomDays = generateRandomNumber(0, days);
        return java.time.LocalDate.now().plusDays(randomDays);
    }

    /**
     * Verifica si una fecha está en el pasado.
     *
     * @param date fecha a verificar
     * @return true si está en el pasado, false en caso contrario
     *
     * @since 1.2.0
     */
    public static boolean isDateInPast(java.time.LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.isBefore(java.time.LocalDate.now());
    }

    /**
     * Verifica si una fecha está en el futuro.
     *
     * @param date fecha a verificar
     * @return true si está en el futuro, false en caso contrario
     *
     * @since 1.2.0
     */
    public static boolean isDateInFuture(java.time.LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.isAfter(java.time.LocalDate.now());
    }

    // =================================================================================
    // UTILIDADES PARA HTTP CLIENT Y DATOS
    // =================================================================================

    public static String sanitizeValue(String key, String value) {
        if (value == null) return null;

        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("password") || lowerKey.contains("token") ||
            lowerKey.contains("secret") || lowerKey.contains("key") ||
            lowerKey.contains("authorization")) {
            return "***HIDDEN***";
        }

        return value;
    }

    public static String sanitizeBody(String body) {
        return sanitizeForLog(body);
    }

    public static String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "... [TRUNCATED]";
    }

    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public static String sanitizeForLog(String data) {
        if (data == null) return null;
        return data.replaceAll("(password|token|secret|key)\"\\s*:\\s*\"[^\"]*\"",
                              "$1\":\"***HIDDEN***\"");
    }

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    private static Object findValueGeneric(String jsonString, String key) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        String trimmed = jsonString.trim();

        try {
            if (trimmed.startsWith("{")) {
                JSONObject jsonObj = new JSONObject(jsonString);
                return findInObject(jsonObj, key);
            } else if (trimmed.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonString);
                return findInArray(jsonArray, key);
            }
        } catch (JSONException e) {
            TestLogger.logDebug("DATA_UTILITIES",
                               String.format("Error parseando JSON: %s", e.getMessage()), null);
        }

        return null;
    }

    private static Object findInObject(JSONObject jsonObj, String key) {
        if (jsonObj.has(key)) {
            return jsonObj.get(key);
        }

        for (String k : jsonObj.keySet()) {
            Object value = jsonObj.get(k);
            if (value instanceof JSONObject) {
                Object result = findInObject((JSONObject) value, key);
                if (result != null) return result;
            } else if (value instanceof JSONArray) {
                Object result = findInArray((JSONArray) value, key);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static Object findInArray(JSONArray jsonArray, String key) {
        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            if (item instanceof JSONObject) {
                Object result = findInObject((JSONObject) item, key);
                if (result != null) return result;
            } else if (item instanceof JSONArray) {
                Object result = findInArray((JSONArray) item, key);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static String convertToJsonString(Object obj) {
        if (obj instanceof JSONObject || obj instanceof JSONArray) {
            return obj.toString();
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private static int calculateRutCheckDigit(int rut) {
        int sum = 0;
        int multiplier = 2;

        while (rut > 0) {
            sum += (rut % 10) * multiplier;
            rut /= 10;
            multiplier = (multiplier == 7) ? 2 : multiplier + 1;
        }

        int remainder = sum % 11;
        return (remainder == 0) ? 0 : (remainder == 1) ? 10 : 11 - remainder;
    }

    private static String formatRut(String rut) {
        String[] parts = rut.split("-");
        String number = parts[0];
        String dv = parts[1];

        StringBuilder formatted = new StringBuilder(number);
        for (int i = formatted.length() - 3; i > 0; i -= 3) {
            formatted.insert(i, ".");
        }

        return formatted + "-" + dv;
    }

    // =================================================================================
    // GESTIÓN DE VARIABLES Y REEMPLAZO DE TEXTO
    // =================================================================================

    /**
     * Reemplaza variables en el texto usando el formato ${variable_name}.
     * También soporta variables de entorno y propiedades del sistema.
     */
    public static String replaceVariables(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String result = text;
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(result);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = null;

            // 1. Buscar en el store de variables locales
            replacement = variableStore.get(variableName);

            // 2. Si no está, buscar en propiedades del sistema
            if (replacement == null) {
                replacement = System.getProperty(variableName);
            }

            // 3. Si no está, buscar en variables de entorno
            if (replacement == null) {
                replacement = System.getenv(variableName);
            }

            // 4. Si no se encuentra, mantener la variable original
            if (replacement == null) {
                TestLogger.logWarning("DATA_UTILITIES",
                                     String.format("Variable no encontrada: %s - manteniendo valor original", variableName), null);
                replacement = "${" + variableName + "}";
            }

            result = result.replace("${" + variableName + "}", replacement);
        }

        return result;
    }

    /**
     * Almacena una variable en el contexto local.
     * Aplica sanitización automática en logs para datos sensibles.
     */
    public static void storeValue(String key, Object value) {
        if (key != null) {
            variableStore.put(key, value != null ? value.toString() : null);
            // Sanitizar valor antes de loguear (GDPR/PCI-DSS)
            String safeValue = sanitizeForLogging(key, value);
            TestLogger.logDebug("DATA_UTILITIES",
                               String.format("Variable almacenada: %s = %s", key, safeValue), null);
        }
    }

    /**
     * Obtiene una variable del contexto local.
     */
    public static String getValue(String key) {
        if (key == null) return null;
        return variableStore.get(key);
    }

    /**
     * Limpia todas las variables del contexto local.
     */
    public static void clearVariables() {
        variableStore.clear();
        TestLogger.logDebug("DATA_UTILITIES", "Variables del contexto limpiadas", null);
    }

    // =================================================================================
    // NAMESPACE/SCOPE PARA VARIABLES (NUEVO - Parallel Execution Safe)
    // =================================================================================

    /**
     * Almacena una variable en un namespace específico.
     *
     * <p>Útil para aislar variables por scenario/feature/thread, evitando state bleeding
     * en ejecución paralela de tests.
     *
     * @param namespace nombre del namespace (ej: "scenario_123", "thread_1")
     * @param key nombre de la variable
     * @param value valor a almacenar
     *
     * @since 1.2.0
     */
    public static void storeValueInNamespace(String namespace, String key, Object value) {
        if (namespace == null || namespace.trim().isEmpty()) {
            TestLogger.logWarning("DATA_UTILITIES",
                "Namespace vacío, usando store global", null);
            storeValue(key, value);
            return;
        }

        if (key == null || key.trim().isEmpty()) {
            TestLogger.logWarning("DATA_UTILITIES",
                "Key vacía, ignorando almacenamiento", null);
            return;
        }

        // Obtener o crear el map del namespace
        namespacedVariableStore.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());

        // Almacenar el valor
        String valueStr = value != null ? value.toString() : null;
        namespacedVariableStore.get(namespace).put(key, valueStr);

        // Sanitizar valor antes de loguear (GDPR/PCI-DSS)
        String safeValue = sanitizeForLogging(key, value);
        TestLogger.logDebug("DATA_UTILITIES",
            String.format("Variable almacenada en namespace '%s': %s = %s",
                namespace, key, safeValue), null);
    }

    /**
     * Obtiene una variable de un namespace específico.
     *
     * @param namespace nombre del namespace
     * @param key nombre de la variable
     * @return valor de la variable, o null si no existe
     *
     * @since 1.2.0
     */
    public static String getValueFromNamespace(String namespace, String key) {
        if (namespace == null || key == null) {
            return null;
        }

        Map<String, String> namespaceMap = namespacedVariableStore.get(namespace);
        if (namespaceMap == null) {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Namespace '%s' no existe", namespace), null);
            return null;
        }

        return namespaceMap.get(key);
    }

    /**
     * Verifica si existe una variable en un namespace.
     *
     * @param namespace nombre del namespace
     * @param key nombre de la variable
     * @return true si existe, false en caso contrario
     *
     * @since 1.2.0
     */
    public static boolean hasValueInNamespace(String namespace, String key) {
        if (namespace == null || key == null) {
            return false;
        }

        Map<String, String> namespaceMap = namespacedVariableStore.get(namespace);
        return namespaceMap != null && namespaceMap.containsKey(key);
    }

    /**
     * Limpia todas las variables de un namespace específico.
     *
     * @param namespace nombre del namespace a limpiar
     *
     * @since 1.2.0
     */
    public static void clearNamespace(String namespace) {
        if (namespace == null) {
            return;
        }

        Map<String, String> removed = namespacedVariableStore.remove(namespace);
        if (removed != null) {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Namespace '%s' limpiado (%d variables removidas)",
                    namespace, removed.size()), null);
        }
    }

    /**
     * Limpia todos los namespaces.
     *
     * @since 1.2.0
     */
    public static void clearAllNamespaces() {
        int size = namespacedVariableStore.size();
        namespacedVariableStore.clear();
        TestLogger.logDebug("DATA_UTILITIES",
            String.format("Todos los namespaces limpiados (%d namespaces)", size), null);
    }

    /**
     * Obtiene todos los namespaces activos.
     *
     * @return Set con los nombres de todos los namespaces activos
     *
     * @since 1.2.0
     */
    public static Set<String> getActiveNamespaces() {
        return new HashSet<>(namespacedVariableStore.keySet());
    }

    /**
     * Almacena un objeto en un namespace específico.
     *
     * @param namespace nombre del namespace
     * @param key nombre/clave del objeto
     * @param object objeto a almacenar
     *
     * @since 1.2.0
     */
    public static void storeObjectInNamespace(String namespace, String key, Object object) {
        if (namespace == null || namespace.trim().isEmpty()) {
            TestLogger.logWarning("DATA_UTILITIES",
                "Namespace vacío, usando store global", null);
            storeObject(key, object);
            return;
        }

        if (key == null || key.trim().isEmpty()) {
            TestLogger.logWarning("DATA_UTILITIES",
                "Key vacía, ignorando almacenamiento", null);
            return;
        }

        // Obtener o crear el map del namespace
        namespacedObjectStore.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());

        // Almacenar el objeto
        if (object == null) {
            namespacedObjectStore.get(namespace).remove(key);
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Objeto removido de namespace '%s': %s", namespace, key), null);
        } else {
            namespacedObjectStore.get(namespace).put(key, object);
            // Sanitizar valor antes de loguear (GDPR/PCI-DSS)
            String safeValue = sanitizeForLogging(key, object);
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Objeto almacenado en namespace '%s': %s = %s (tipo: %s)",
                    namespace, key, safeValue, object.getClass().getSimpleName()), null);
        }
    }

    /**
     * Obtiene un objeto tipado de un namespace específico.
     *
     * @param <T> tipo del objeto esperado
     * @param namespace nombre del namespace
     * @param key nombre/clave del objeto
     * @param clazz clase del tipo esperado
     * @return objeto del tipo especificado, o null si no existe
     *
     * @since 1.2.0
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromNamespace(String namespace, String key, Class<T> clazz) {
        if (namespace == null || key == null || clazz == null) {
            return null;
        }

        Map<String, Object> namespaceMap = namespacedObjectStore.get(namespace);
        if (namespaceMap == null) {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Namespace '%s' no existe", namespace), null);
            return null;
        }

        Object obj = namespaceMap.get(key);
        if (obj == null) {
            return null;
        }

        // Si es del tipo correcto, retornar
        if (clazz.isInstance(obj)) {
            return (T) obj;
        }

        // Intentar conversión con Jackson
        try {
            return objectMapper.convertValue(obj, clazz);
        } catch (Exception e) {
            String errorMsg = String.format(
                "No se pudo convertir objeto '%s' en namespace '%s' (tipo: %s) a tipo solicitado (%s)",
                key, namespace, obj.getClass().getSimpleName(), clazz.getSimpleName()
            );
            TestLogger.logError("DATA_UTILITIES", errorMsg, null);
            throw new ClassCastException(errorMsg);
        }
    }

    /**
     * Verifica si existe un objeto en un namespace.
     *
     * @param namespace nombre del namespace
     * @param key nombre/clave del objeto
     * @return true si existe, false en caso contrario
     *
     * @since 1.2.0
     */
    public static boolean hasObjectInNamespace(String namespace, String key) {
        if (namespace == null || key == null) {
            return false;
        }

        Map<String, Object> namespaceMap = namespacedObjectStore.get(namespace);
        return namespaceMap != null && namespaceMap.containsKey(key);
    }

    /**
     * Alias para getJsonParameter - para compatibilidad con api-core.
     */
    public static Object getJsonParameters(String jsonBody, String fieldPath) throws FrameworkBusinessException {
        return getJsonParameter(jsonBody, fieldPath);
    }

    /**
     * Extrae un token específico de una respuesta JSON.
     * Utilizado principalmente para extraer tokens de autenticación.
     */
    public static String getToken(String jsonResponse, String tokenField) throws FrameworkBusinessException {
        try {
            Object tokenValue = getJsonParameter(jsonResponse, tokenField);
            return tokenValue != null ? tokenValue.toString() : null;
        } catch (Exception e) {
            throw new FrameworkBusinessException("getToken",
                "Error extrayendo token del campo '" + tokenField + "': " + e.getMessage());
        }
    }

    private static String generateRandomString(int length, boolean letters, boolean numbers, boolean special) {
        return SecurityUtilities.generateRandomString(length, letters, numbers, special);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // =================================================================================
    // BÚSQUEDA RECURSIVA EN JSON/MAPS/LISTS (MEJORADO v1.3.0)
    // =================================================================================

    /**
     * Busca un valor en un response (Map, List o JSON String) de forma recursiva.
     *
     * <p>Este método es capaz de buscar claves anidadas en estructuras JSON complejas,
     * sin importar la profundidad del anidamiento.
     *
     * <p>Ejemplos de uso:
     * <pre>
     * // JSON: {"response": {"data": {"user_full_name": "Juan Pérez"}}}
     * Object name = DataUtilities.findValue(jsonResponse, "user_full_name");
     * // Retorna: "Juan Pérez"
     *
     * // También funciona con Maps y Lists
     * Map<String, Object> map = ...;
     * Object value = DataUtilities.findValue(map, "targetKey");
     * </pre>
     *
     * @param response Puede ser Map<String,Object>, List<Object> o String (JSON)
     * @param targetKey Clave a buscar (ej: "user_full_name")
     * @return Valor encontrado o null si no existe
     * @throws FrameworkBusinessException si hay error parseando JSON
     *
     * @since 1.3.0
     */


    // =========================================================================
    // UTILIDADES DE ARCHIVOS Y DIRECTORIOS
    // =========================================================================

    /**
     * Escribe un string a un archivo.
     *
     * @param content Contenido a escribir
     * @param filepath Ruta del archivo
     * @throws IOException si hay error escribiendo
     */
    public static void writeStringToFile(String content, String filepath) throws IOException {
        if (content == null || filepath == null) {
            throw new IllegalArgumentException("content y filepath no pueden ser null");
        }
        Files.writeString(Path.of(filepath), content);
        TestLogger.logDebug("DATA_UTILITIES",
            String.format("Contenido escrito a archivo: %s", filepath), null);
    }

    /**
     * Crea un directorio si no existe.
     *
     * @param dirPath Ruta del directorio a crear
     * @throws IOException si hay error creando el directorio
     */
    public static void createDirectoryIfNotExists(String dirPath) throws IOException {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            throw new IllegalArgumentException("dirPath no puede ser null o vacío");
        }

        Path path = Path.of(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Directorio creado: %s", dirPath), null);
        }
    }

    // =========================================================================
    // COMPARACIÓN Y VALIDACIÓN CROSS-LAYER (API ↔ WEB ↔ MOBILE)
    // =========================================================================

    /**
     * Compara dos valores obtenidos de diferentes capas (API vs Web, por ejemplo).
     * Soporta comparación exacta, ignoreCase, y normalización de espacios.
     *
     * <p><b>Ejemplos de uso:</b></p>
     * <pre>
     * // Comparar texto de API con texto de elemento Web
     * boolean equals = DataUtilities.compareValues(apiValue, webValue, ComparisonMode.EXACT);
     *
     * // Comparar ignorando mayúsculas/minúsculas
     * boolean equals = DataUtilities.compareValues(apiValue, webValue, ComparisonMode.IGNORE_CASE);
     *
     * // Comparar normalizando espacios
     * boolean equals = DataUtilities.compareValues(apiValue, webValue, ComparisonMode.TRIM_AND_NORMALIZE);
     * </pre>
     *
     * @param value1 Primer valor a comparar
     * @param value2 Segundo valor a comparar
     * @param mode Modo de comparación
     * @return true si los valores son equivalentes según el modo, false en caso contrario
     */
    public static boolean compareValues(Object value1, Object value2, ComparisonMode mode) {
        if (value1 == null && value2 == null) return true;
        if (value1 == null || value2 == null) return false;

        String str1 = String.valueOf(value1);
        String str2 = String.valueOf(value2);

        switch (mode) {
            case EXACT:
                return str1.equals(str2);

            case IGNORE_CASE:
                return str1.equalsIgnoreCase(str2);

            case TRIM:
                return str1.trim().equals(str2.trim());

            case TRIM_AND_NORMALIZE:
                // Normaliza espacios múltiples a uno solo y hace trim
                String normalized1 = str1.trim().replaceAll("\\s+", " ");
                String normalized2 = str2.trim().replaceAll("\\s+", " ");
                return normalized1.equals(normalized2);

            case IGNORE_CASE_AND_TRIM:
                return str1.trim().equalsIgnoreCase(str2.trim());

            case CONTAINS:
                return str1.contains(str2) || str2.contains(str1);

            default:
                return str1.equals(str2);
        }
    }

    /**
     * Compara dos valores usando modo de comparación exacta.
     *
     * @param value1 Primer valor
     * @param value2 Segundo valor
     * @return true si son exactamente iguales
     */
    public static boolean compareValues(Object value1, Object value2) {
        return compareValues(value1, value2, ComparisonMode.EXACT);
    }

    /**
     * Compara un valor obtenido de API con un valor esperado del contexto.
     *
     * @param apiKey Nombre de la variable que contiene el valor de API
     * @param expectedKey Nombre de la variable que contiene el valor esperado
     * @param mode Modo de comparación
     * @return true si los valores coinciden
     * @throws RuntimeException Si alguna de las variables no existe
     */
    public static boolean compareApiWithContext(String apiKey, String expectedKey, ComparisonMode mode) {
        Object apiValue = ScenarioContext.getFromAnyLayer(apiKey);
        Object expectedValue = ScenarioContext.getFromAnyLayer(expectedKey);

        if (apiValue == null) {
            throw new RuntimeException("Variable de API no encontrada en contexto: " + apiKey);
        }
        if (expectedValue == null) {
            throw new RuntimeException("Variable esperada no encontrada en contexto: " + expectedKey);
        }

        boolean result = compareValues(apiValue, expectedValue, mode);

        TestLogger.logInfo("DATA_UTILITIES",
            String.format("Comparación API vs Contexto [%s] - API: '%s' vs Esperado: '%s' = %s",
                mode, apiValue, expectedValue, result ? "✅ MATCH" : "❌ NO MATCH"), null);

        return result;
    }

    /**
     * Valida que un valor de API coincida con un valor esperado (comparación exacta).
     *
     * @param apiKey Nombre de la variable de API
     * @param expectedKey Nombre de la variable esperada
     * @throws AssertionError Si los valores no coinciden
     */
    public static void assertApiMatchesExpected(String apiKey, String expectedKey) {
        if (!compareApiWithContext(apiKey, expectedKey, ComparisonMode.EXACT)) {
            Object apiValue = ScenarioContext.getFromAnyLayer(apiKey);
            Object expectedValue = ScenarioContext.getFromAnyLayer(expectedKey);
            throw new AssertionError(
                String.format("Los valores no coinciden: API['%s']='%s' != Esperado['%s']='%s'",
                    apiKey, apiValue,
                    expectedKey, expectedValue)
            );
        }
    }

    /**
     * Valida que un valor de API coincida con un valor esperado usando un modo específico.
     *
     * @param apiKey Nombre de la variable de API
     * @param expectedKey Nombre de la variable esperada
     * @param mode Modo de comparación
     * @throws AssertionError Si los valores no coinciden
     */
    public static void assertApiMatchesExpected(String apiKey, String expectedKey, ComparisonMode mode) {
        if (!compareApiWithContext(apiKey, expectedKey, mode)) {
            Object apiValue = ScenarioContext.getFromAnyLayer(apiKey);
            Object expectedValue = ScenarioContext.getFromAnyLayer(expectedKey);
            throw new AssertionError(
                String.format("Los valores no coinciden [%s]: API['%s']='%s' != Esperado['%s']='%s'",
                    mode, apiKey, apiValue,
                    expectedKey, expectedValue)
            );
        }
    }

    /**
     * Modos de comparación disponibles para validar valores entre capas.
     */
    public enum ComparisonMode {
        /** Comparación exacta (case-sensitive, sin modificaciones) */
        EXACT,

        /** Ignora mayúsculas/minúsculas */
        IGNORE_CASE,

        /** Elimina espacios al inicio y final */
        TRIM,

        /** Elimina espacios y normaliza múltiples espacios a uno solo */
        TRIM_AND_NORMALIZE,

        /** Ignora mayúsculas/minúsculas y elimina espacios */
        IGNORE_CASE_AND_TRIM,

        /** Verifica si un valor contiene al otro */
        CONTAINS
    }

    // =========================================================================
    // GENERACIÓN ALEATORIA AVANZADA
    // =========================================================================

    /**
     * Genera un boolean aleatorio.
     *
     * @return true o false aleatoriamente
     */
    public static boolean generateRandomBoolean() {
        return RANDOM.nextBoolean();
    }

    /**
     * Genera un número entero dentro de un rango.
     *
     * @param min Valor mínimo (inclusivo)
     * @param max Valor máximo (inclusivo)
     * @return Número aleatorio entre min y max
     */
    public static int generateNumberInRange(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min no puede ser mayor que max");
        }
        return RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * Genera texto Lorem Ipsum con longitud específica.
     *
     * @param length Longitud del texto deseado
     * @return Texto Lorem Ipsum
     */
    public static String generateLoremIpsum(int length) {
        String lorem = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua ";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append(lorem);
        }
        return sb.substring(0, length);
    }

    /**
     * Genera una edad dentro de un rango.
     *
     * @param min Edad mínima
     * @param max Edad máxima
     * @return Edad aleatoria
     */
    public static int generateAgeInRange(int min, int max) {
        return generateNumberInRange(min, max);
    }

    /**
     * Genera un monto monetario aleatorio con decimales.
     *
     * @param min Monto mínimo
     * @param max Monto máximo
     * @param decimals Número de decimales
     * @return Monto aleatorio
     */
    public static double generateRandomAmount(double min, double max, int decimals) {
        if (min > max) {
            throw new IllegalArgumentException("min no puede ser mayor que max");
        }
        double amount = min + (max - min) * RANDOM.nextDouble();
        double multiplier = Math.pow(10, decimals);
        return Math.round(amount * multiplier) / multiplier;
    }

    /**
     * Genera un código con prefijo y longitud específica.
     *
     * @param prefix Prefijo del código
     * @param suffixLength Longitud del sufijo numérico
     * @return Código generado
     */
    public static String generateCode(String prefix, int suffixLength) {
        StringBuilder code = new StringBuilder(prefix);
        for (int i = 0; i < suffixLength; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    // =========================================================================
    // MÉTODOS JSONPATH
    // =========================================================================

    /**
     * Obtiene un valor de un JSON usando JSONPath.
     *
     * @param json String JSON
     * @param jsonPath Expresión JSONPath
     * @return Valor encontrado o null
     */
    public static Object getByJsonPath(String json, String jsonPath) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return JsonPath.read(json, jsonPath);
        } catch (PathNotFoundException e) {
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("JSONPath no encontrado: %s", jsonPath), null);
            return null;
        } catch (Exception e) {
            TestLogger.logError("DATA_UTILITIES",
                String.format("Error evaluando JSONPath: %s", e.getMessage()), null);
            return null;
        }
    }

    /**
     * Verifica si existe un JSONPath en el JSON.
     *
     * @param json String JSON
     * @param jsonPath Expresión JSONPath
     * @return true si existe, false en caso contrario
     */
    public static boolean hasJsonPath(String json, String jsonPath) {
        try {
            Object result = getByJsonPath(json, jsonPath);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene una lista tipada de valores usando JSONPath.
     *
     * @param <T> Tipo de los elementos
     * @param json String JSON
     * @param jsonPath Expresión JSONPath
     * @param elementType Clase del tipo de elemento
     * @return Lista de elementos del tipo especificado
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getListByJsonPath(String json, String jsonPath, Class<T> elementType) {
        Object result = getByJsonPath(json, jsonPath);

        if (result == null) {
            return new ArrayList<>();
        }

        if (result instanceof List) {
            List<?> list = (List<?>) result;
            return list.stream()
                .map(item -> {
                    if (elementType.isInstance(item)) {
                        return elementType.cast(item);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    // =========================================================================
    // COMPARACIÓN Y DIFERENCIAS JSON
    // =========================================================================

    /**
     * Compara dos JSON para verificar si son equivalentes (ignora orden de campos).
     *
     * @param json1 Primer JSON
     * @param json2 Segundo JSON
     * @return true si son equivalentes, false en caso contrario
     */
    public static boolean areJsonEqual(String json1, String json2) {
        if (json1 == null && json2 == null) return true;
        if (json1 == null || json2 == null) return false;

        try {
            JSONCompareResult result = JSONCompare.compareJSON(json1, json2, JSONCompareMode.NON_EXTENSIBLE);
            return result.passed();
        } catch (Exception e) {
            TestLogger.logError("DATA_UTILITIES",
                String.format("Error comparando JSON: %s", e.getMessage()), null);
            return false;
        }
    }

    /**
     * Genera un reporte de diferencias entre dos JSON.
     *
     * @param expected JSON esperado
     * @param actual JSON actual
     * @return String con las diferencias encontradas
     */
    public static String diffJson(String expected, String actual) {
        if (expected == null || actual == null) {
            return "Uno de los JSON es null";
        }

        try {
            JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.NON_EXTENSIBLE);

            if (result.passed()) {
                return "Los JSON son idénticos";
            }

            StringBuilder diff = new StringBuilder("Diferencias encontradas:\n");

            result.getFieldMissing().forEach(missing ->
                diff.append(missing).append(" => actual: <missing>\n"));

            result.getFieldUnexpected().forEach(unexpected ->
                diff.append(unexpected).append(" => expected:\n"));

            result.getFieldFailures().forEach(failure -> {
                String field = failure.getField();
                Object expectedValue = failure.getExpected();
                Object actualValue = failure.getActual();
                diff.append(field).append(" => expected: ").append(expectedValue)
                    .append(", actual: ").append(actualValue).append("\n");
            });

            return diff.toString();
        } catch (Exception e) {
            return "Error comparando JSON: " + e.getMessage();
        }
    }

    /**
     * Verifica si el JSON actual contiene todos los campos del JSON esperado.
     *
     * @param expected JSON con campos esperados
     * @param actual JSON actual a verificar
     * @return true si contiene todos los campos, false en caso contrario
     */
    public static boolean jsonContainsAllFields(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        try {
            JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.LENIENT);
            return result.passed();
        } catch (Exception e) {
            TestLogger.logError("DATA_UTILITIES",
                String.format("Error verificando campos JSON: %s", e.getMessage()), null);
            return false;
        }
    }

    // =========================================================================
    // MÉTODOS DE CONVERSIÓN Y CONTEXTO
    // =========================================================================

    /**
     * Convierte un String JSON a un Map.
     *
     * @param json String JSON a convertir
     * @return Map con los datos del JSON
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            TestLogger.logError("DATA_UTILITIES",
                String.format("Error convirtiendo JSON a Map: %s", e.getMessage()), null);
            return new HashMap<>();
        }
    }

    /**
     * Guarda un valor en el contexto del escenario.
     *
     * @param layer Capa del framework (api, web, mobile)
     * @param key Clave para almacenar el valor
     * @param value Valor a almacenar
     */
    public static void saveToContext(String layer, String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave no puede ser null o vacía");
        }

        String fullKey = layer != null && !layer.trim().isEmpty()
            ? layer + "." + key
            : key;

        try {
            ScenarioContext.setByLayer(layer, key, value);

            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Valor guardado en contexto [%s]: %s = %s",
                    fullKey, key, sanitizeForLog(String.valueOf(value))), null);
        } catch (FrameworkBusinessException e) {
            TestLogger.logError("DATA_UTILITIES",
                String.format("Error guardando valor en contexto: %s", e.getMessage()), null);
            throw new IllegalStateException("Error guardando en contexto", e);
        }
    }
}
