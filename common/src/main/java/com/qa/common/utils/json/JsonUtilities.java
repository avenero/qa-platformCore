package com.qa.common.utils.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.logging.TestLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilidades JSON para el framework de QA.
 *
 * <p>Consolida todas las operaciones JSON en un único lugar con seguridad endurecida:</p>
 * <ul>
 *   <li>Serialización / deserialización con Jackson seguro (OWASP)</li>
 *   <li>Navegación y extracción de campos con notación punto y JSONPath</li>
 *   <li>Búsqueda recursiva en respuestas API (Map, List, JSON String)</li>
 *   <li>Comparación estructural de JSON (ignora orden de campos)</li>
 *   <li>Protecciones DoS: límite de tamaño (10 MB) y profundidad (50 niveles)</li>
 * </ul>
 *
 * <p><b>Seguridad aplicada:</b>
 * <ul>
 *   <li>CVE-2017-7525: Deserialización polimórfica deshabilitada</li>
 *   <li>CVE-2017-15095: Auto-typing deshabilitado</li>
 *   <li>CVE-2019-12384: Visibilidad restringida por configuración explícita</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.1.0
 */
public final class JsonUtilities {

    // =========================================================================
    // CONFIGURACIÓN DE SEGURIDAD
    // =========================================================================

    /** Tamaño máximo permitido para JSON (10 MB) – previene DoS / OOM. */
    private static final int MAX_JSON_SIZE_BYTES = 10 * 1024 * 1024;

    /** Profundidad máxima de anidamiento – previene DoS por stack overflow. */
    private static final int MAX_JSON_DEPTH = 50;

    /** Bytes per kilobyte, used for size-to-MB conversion in error messages. */
    private static final double BYTES_PER_KB = 1024.0;


    /**
     * ObjectMapper seguro configurado según OWASP best practices.
     * Instancia única compartida – ObjectMapper es thread-safe.
     */
    private static final ObjectMapper OBJECT_MAPPER = createSecureObjectMapper();

    private JsonUtilities() {
        throw new UnsupportedOperationException("JsonUtilities es una clase de utilidad");
    }

    /**
     * Crea el ObjectMapper con configuración de seguridad endurecida.
     */
    private static ObjectMapper createSecureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // SEGURIDAD: Deshabilitar deserialización polimórfica (CVE-2017-7525)
        mapper.deactivateDefaultTyping();
        // SEGURIDAD: Configuración de deserialización robusta
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
        // SEGURIDAD: Visibilidad explícita (CVE-2019-12384)
        mapper.setVisibility(
            mapper.getSerializationConfig().getDefaultVisibilityChecker().
                withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE).
                withSetterVisibility(JsonAutoDetect.Visibility.NONE).
                withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
        );
        // PERFORMANCE: Serialización
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    /**
     * Expone el ObjectMapper seguro para uso en otras clases del framework.
     *
     * @return ObjectMapper singleton thread-safe configurado de forma segura
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    // =========================================================================
    // VALIDACIONES DE SEGURIDAD (PROTECCIÓN DoS)
    // =========================================================================

    /**
     * Valida que el tamaño del JSON no exceda el límite permitido.
     *
     * @param jsonString JSON a validar
     * @throws FrameworkBusinessException si excede {@value #MAX_JSON_SIZE_BYTES} bytes
     */
    public static void validateJsonSize(String jsonString) throws FrameworkBusinessException {
        if (jsonString == null) {
            return;
        }
        int sizeBytes = jsonString.getBytes(StandardCharsets.UTF_8).length;
        if (sizeBytes > MAX_JSON_SIZE_BYTES) {
            String msg = String.format(
                "JSON excede el tamaño máximo: %d bytes (máximo: %d bytes / %.2f MB)",
                sizeBytes, MAX_JSON_SIZE_BYTES, MAX_JSON_SIZE_BYTES / (BYTES_PER_KB * BYTES_PER_KB));
            TestLogger.logError("JSON_UTILITIES_SECURITY", msg, null);
            throw new FrameworkBusinessException("validateJsonSize", msg);
        }
    }

    /**
     * Valida la profundidad de anidamiento del JSON.
     *
     * @param jsonString JSON a validar
     * @throws FrameworkBusinessException si supera {@value #MAX_JSON_DEPTH} niveles
     */
    public static void validateJsonDepth(String jsonString) throws FrameworkBusinessException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return;
        }
        int depth = 0;
        int maxDepth = 0;
        for (char c : jsonString.toCharArray()) {
            if (c == '{' || c == '[') {
                if (++depth > maxDepth) {
                    maxDepth = depth;
                }
                if (maxDepth > MAX_JSON_DEPTH) {
                    String msg = String.format(
                        "JSON excede profundidad máxima: %d niveles (máximo: %d)",
                        maxDepth, MAX_JSON_DEPTH);
                    TestLogger.logError("JSON_UTILITIES_SECURITY", msg, null);
                    throw new FrameworkBusinessException("validateJsonDepth", msg);
                }
            } else if (c == '}' || c == ']') {
                depth--;
            }
        }
    }

    // =========================================================================
    // SERIALIZACIÓN / DESERIALIZACIÓN
    // =========================================================================

    /**
     * Deserializa un JSON String a un objeto Java tipado.
     *
     * @param <T>        tipo del objeto destino
     * @param jsonString JSON como String
     * @param clazz      clase del objeto destino
     * @return objeto deserializado del tipo especificado
     * @throws FrameworkBusinessException si el JSON es inválido o la deserialización falla
     */
    public static <T> T deserializeJson(String jsonString, Class<T> clazz)
            throws FrameworkBusinessException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new FrameworkBusinessException("deserializeJson", "JSON string no puede ser vacío o nulo");
        }
        if (clazz == null) {
            throw new FrameworkBusinessException("deserializeJson", "La clase destino no puede ser nula");
        }
        String trimmed = jsonString.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw new FrameworkBusinessException("deserializeJson",
                "El string no parece ser JSON válido (debe empezar con { o [)");
        }
        try {
            validateJsonSize(jsonString);
            validateJsonDepth(jsonString);
            T result = OBJECT_MAPPER.readValue(jsonString, clazz);
            TestLogger.logDebug("JSON_UTILITIES",
                String.format("JSON deserializado exitosamente a tipo: %s", clazz.getSimpleName()), null);
            return result;
        } catch (JsonProcessingException e) {
            String msg = String.format("Error deserializando JSON a %s: %s", clazz.getSimpleName(), e.getMessage());
            TestLogger.logError("JSON_UTILITIES", msg, null);
            throw new FrameworkBusinessException("deserializeJson", msg);
        }
    }

    /**
     * Convierte un JSON String a un {@code Map<String, Object>}.
     *
     * @param json String JSON a convertir
     * @return Map con los datos del JSON, o mapa vacío si el JSON es nulo/vacío/inválido
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            TestLogger.logError("JSON_UTILITIES",
                "Error convirtiendo JSON a Map: " + e.getMessage(), null);
            return new HashMap<>();
        }
    }

    /**
     * Convierte un objeto a su representación JSON String.
     *
     * @param obj objeto a convertir
     * @return representación JSON del objeto
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    // =========================================================================
    // EXTRACCIÓN DE CAMPOS JSON (notación punto)
    // =========================================================================

    /**
     * Extrae un campo de un JSON usando notación punto.
     *
     * <p>Soporta tanto campos planos como rutas anidadas ({@code "user.address.city"}).
     * Busca de forma recursiva en objetos y arrays.
     *
     * @param jsonBody  JSON como String
     * @param fieldPath ruta al campo (ej: {@code "id"}, {@code "user.name"})
     * @return valor encontrado o {@code null} si no existe
     * @throws FrameworkBusinessException si hay error procesando el JSON
     */
    public static Object getJsonParameter(String jsonBody, String fieldPath)
            throws FrameworkBusinessException {
        try {
            if (jsonBody == null || fieldPath == null) {
                return null;
            }
            // JSONPath syntax ($.field, $[0], etc.) → delegate to JsonPath library
            if (fieldPath.startsWith("$")) {
                return getByJsonPath(jsonBody, fieldPath);
            }
            if (!fieldPath.contains(".")) {
                return findValueGeneric(jsonBody, fieldPath);
            }
            String[] parts = fieldPath.split("\\.", 2);
            Object current = findValueGeneric(jsonBody, parts[0]);
            if (current == null) {
                return null;
            }
            if (parts.length > 1) {
                return getJsonParameter(toJsonString(current), parts[1]);
            }
            return current;
        } catch (Exception e) {
            throw new FrameworkBusinessException("getJsonParameter",
                "Error obteniendo parámetro '" + fieldPath + "': " + e.getMessage());
        }
    }

    /**
     * Verifica si existe un campo en el JSON.
     *
     * @param jsonBody  JSON como String
     * @param fieldPath ruta al campo
     * @return {@code true} si el campo existe
     */
    public static boolean hasJsonField(String jsonBody, String fieldPath) {
        try {
            return getJsonParameter(jsonBody, fieldPath) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Alias de {@link #getJsonParameter(String, String)} para compatibilidad.
     *
     * @param jsonBody  JSON como String
     * @param fieldPath ruta al campo
     * @return valor encontrado o {@code null}
     * @throws FrameworkBusinessException si hay error
     */
    public static Object getJsonParameters(String jsonBody, String fieldPath)
            throws FrameworkBusinessException {
        return getJsonParameter(jsonBody, fieldPath);
    }

    /**
     * Extrae un token específico de una respuesta JSON.
     *
     * @param jsonResponse respuesta JSON
     * @param tokenField   nombre del campo que contiene el token
     * @return valor del token como String, o {@code null}
     * @throws FrameworkBusinessException si hay error
     */
    public static String getToken(String jsonResponse, String tokenField)
            throws FrameworkBusinessException {
        try {
            Object tokenValue = getJsonParameter(jsonResponse, tokenField);
            return tokenValue != null ? tokenValue.toString() : null;
        } catch (Exception e) {
            throw new FrameworkBusinessException("getToken",
                "Error extrayendo token del campo '" + tokenField + "': " + e.getMessage());
        }
    }

    // =========================================================================
    // BÚSQUEDA RECURSIVA EN RESPUESTAS (Map / List / JSON String)
    // =========================================================================

    /**
     * Busca un valor recursivamente en una respuesta (Map, List o JSON String).
     *
     * <p>Estrategia de búsqueda:
     * <ol>
     *   <li>Si la respuesta es String JSON, la parsea automáticamente</li>
     *   <li>Navega Maps recursivamente</li>
     *   <li>Navega Lists recursivamente</li>
     *   <li>Retorna el <em>primer</em> valor encontrado con la clave dada</li>
     * </ol>
     *
     * @param response  puede ser {@code Map<String,Object>}, {@code List<Object>} o {@code String} (JSON)
     * @param targetKey clave a buscar (ej: {@code "access_token"}, {@code "user_id"})
     * @return valor encontrado, o {@code null} si no existe
     */
    public static Object findValue(Object response, String targetKey) {
        if (response == null || targetKey == null) {
            return null;
        }
        // Parsear String JSON
        if (response instanceof String) {
            try {
                response = OBJECT_MAPPER.readValue((String) response, Object.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parseando JSON response en findValue", e);
            }
        }
        if (response instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapResp = (Map<String, Object>) response;
            return findValueInMap(mapResp, targetKey);
        } else if (response instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listResp = (List<Object>) response;
            return findValueInList(listResp, targetKey);
        }
        return null;
    }

    // =========================================================================
    // JSONPATH
    // =========================================================================

    /**
     * Extrae un valor de un JSON usando una expresión JSONPath.
     *
     * @param json     String JSON
     * @param jsonPath expresión JSONPath (ej: {@code "$.user.name"}, {@code "$.items[0].id"})
     * @return valor encontrado o {@code null}
     */
    public static Object getByJsonPath(String json, String jsonPath) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JsonPath.read(json, jsonPath);
        } catch (PathNotFoundException e) {
            TestLogger.logDebug("JSON_UTILITIES",
                "JSONPath no encontrado: " + jsonPath, null);
            return null;
        } catch (Exception e) {
            TestLogger.logError("JSON_UTILITIES",
                "Error evaluando JSONPath: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Verifica si existe un JSONPath en el JSON.
     *
     * @param json     String JSON
     * @param jsonPath expresión JSONPath
     * @return {@code true} si existe
     */
    public static boolean hasJsonPath(String json, String jsonPath) {
        return getByJsonPath(json, jsonPath) != null;
    }

    /**
     * Obtiene una lista tipada de valores usando JSONPath.
     *
     * @param <T>         tipo de los elementos
     * @param json        String JSON
     * @param jsonPath    expresión JSONPath
     * @param elementType clase del tipo de elemento
     * @return Lista de elementos del tipo especificado, nunca {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getListByJsonPath(String json, String jsonPath, Class<T> elementType) {
        Object result = getByJsonPath(json, jsonPath);
        if (result == null) {
            return new ArrayList<>();
        }
        if (result instanceof List) {
            List<?> list = (List<?>) result;
            return list.stream().filter(item -> elementType.isInstance(item)).map(item -> elementType.cast(item)).
                collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    // =========================================================================
    // COMPARACIÓN Y DIFERENCIAS JSON
    // =========================================================================

    /**
     * Compara dos JSON para verificar si son equivalentes (ignora orden de campos).
     *
     * @param json1 primer JSON
     * @param json2 segundo JSON
     * @return {@code true} si son estructuralmente equivalentes
     */
    public static boolean areJsonEqual(String json1, String json2) {
        if (json1 == null && json2 == null) {
            return true;
        }
        if (json1 == null || json2 == null) {
            return false;
        }
        try {
            JSONCompareResult result = JSONCompare.compareJSON(json1, json2, JSONCompareMode.NON_EXTENSIBLE);
            return result.passed();
        } catch (Exception e) {
            TestLogger.logError("JSON_UTILITIES",
                "Error comparando JSON: " + e.getMessage(), null);
            return false;
        }
    }

    /**
     * Genera un reporte legible de diferencias entre dos JSON.
     *
     * @param expected JSON esperado
     * @param actual   JSON actual
     * @return String con las diferencias encontradas
     */
    public static String diffJson(String expected, String actual) {
        if (expected == null || actual == null) {
            return "Uno de los JSON es null";
        }
        try {
            JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
            return result.passed() ? "JSON son equivalentes" : result.getMessage();
        } catch (Exception e) {
            return "Error al comparar JSON: " + e.getMessage();
        }
    }

    /**
     * Verifica si una cadena es JSON válido (objeto o array).
     *
     * @param json cadena a evaluar; puede ser null
     * @return {@code true} si es JSON válido
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        String trimmed = json.trim();
        try {
            if (trimmed.startsWith("{")) {
                new org.json.JSONObject(json);
                return true;
            } else if (trimmed.startsWith("[")) {
                new org.json.JSONArray(json);
                return true;
            }
        } catch (org.json.JSONException ignored) { /* invalid */ }
        return false;
    }

    /**
     * Verifica que el JSON {@code actual} contenga todos los campos del JSON {@code expected}
     * (comparación LENIENT: el actual puede tener campos extra).
     *
     * @param expected JSON con los campos esperados
     * @param actual   JSON a verificar
     * @return {@code true} si actual contiene todos los campos de expected
     */
    public static boolean jsonContainsAllFields(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        try {
            JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.LENIENT);
            return result.passed();
        } catch (Exception e) {
            TestLogger.logError("JSON_UTILITIES",
                "Error verificando campos JSON: " + e.getMessage(), null);
            return false;
        }
    }

    // =========================================================================
    // MÉTODOS PRIVADOS (helpers de navegación)
    // =========================================================================

    /**
     * Busca un valor en un JSON String plano (sin notación punto).
     * Maneja tanto objetos JSON ({}) como arrays ([]).
     */
    static Object findValueGeneric(String jsonString, String key) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        String trimmed = jsonString.trim();
        try {
            if (trimmed.startsWith("{")) {
                return findInObject(new JSONObject(jsonString), key);
            } else if (trimmed.startsWith("[")) {
                return findInArray(new JSONArray(jsonString), key);
            }
        } catch (JSONException e) {
            TestLogger.logDebug("JSON_UTILITIES",
                "Error parseando JSON: " + e.getMessage(), null);
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
                if (result != null) {
                    return result;
                }
            } else if (value instanceof JSONArray) {
                Object result = findInArray((JSONArray) value, key);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static Object findInArray(JSONArray jsonArray, String key) {
        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            if (item instanceof JSONObject) {
                Object result = findInObject((JSONObject) item, key);
                if (result != null) {
                    return result;
                }
            } else if (item instanceof JSONArray) {
                Object result = findInArray((JSONArray) item, key);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Busca recursivamente un valor en un Map.
     */
    static Object findValueInMap(Map<String, Object> map, String targetKey) {
        if (map == null || targetKey == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equals(targetKey)) {
                return entry.getValue();
            }
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Object nested = findValueInMap((Map<String, Object>) value, targetKey);
                if (nested != null) {
                    return nested;
                }
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                Object nested = findValueInList((List<Object>) value, targetKey);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * Busca recursivamente un valor en una List.
     */
    static Object findValueInList(List<Object> list, String targetKey) {
        if (list == null || targetKey == null) {
            return null;
        }
        for (Object item : list) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Object nested = findValueInMap((Map<String, Object>) item, targetKey);
                if (nested != null) {
                    return nested;
                }
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                Object nested = findValueInList((List<Object>) item, targetKey);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}

