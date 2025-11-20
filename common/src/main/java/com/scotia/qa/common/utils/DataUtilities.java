package com.scotia.qa.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Scotia QA Framework Team
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

        // SEGURIDAD: Prevenir inyección mediante getters maliciosos (CVE-2019-12384)
        mapper.configure(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS, false);

        // PERFORMANCE: Configuración de serialización
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        TestLogger.logInfo("DATA_UTILITIES_SECURITY",
            "ObjectMapper configurado con protecciones de seguridad", null);

        return mapper;
    }

    // Store de variables thread-safe para todos los frameworks
    private static final Map<String, String> variableStore = new ConcurrentHashMap<>();

    // Store de objetos complejos thread-safe (nuevo - para deserialización)
    private static final Map<String, Object> objectStore = new ConcurrentHashMap<>();

    // Store de variables con namespace thread-safe (para parallel execution)
    // Estructura: Map<namespace, Map<key, value>>
    private static final Map<String, Map<String, String>> namespacedVariableStore = new ConcurrentHashMap<>();

    // Store de objetos con namespace thread-safe
    // Estructura: Map<namespace, Map<key, object>>
    private static final Map<String, Map<String, Object>> namespacedObjectStore = new ConcurrentHashMap<>();

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
    // UTILIDADES DE ARCHIVOS Y SISTEMA
    // =================================================================================

    /**
     * Crea un directorio si no existe.
     *
     * @param directoryPath ruta del directorio a crear
     * @throws RuntimeException si no se puede crear el directorio
     */
    public static void createDirectoryIfNotExists(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del directorio no puede estar vacía");
        }

        java.io.File directory = new java.io.File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new RuntimeException("No se pudo crear el directorio: " + directoryPath);
            }
            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Directorio creado: %s", directoryPath),
                null);
        }
    }

    /**
     * Escribe un String a un archivo.
     *
     * @param content contenido a escribir
     * @param filepath ruta del archivo destino
     * @throws RuntimeException si hay error escribiendo el archivo
     */
    public static void writeStringToFile(String content, String filepath) {
        if (content == null || filepath == null) {
            throw new IllegalArgumentException("El contenido y filepath no pueden ser nulos");
        }

        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filepath);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            TestLogger.logDebug("DATA_UTILITIES",
                String.format("Archivo escrito: %s (%d bytes)", filepath, content.length()),
                null);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error escribiendo archivo: " + filepath, e);
        }
    }

    /**
     * Convierte un JSON String a un Map.
     *
     * @param jsonString JSON como String
     * @return Map representando el JSON
     * @throws FrameworkBusinessException si el JSON es inválido
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String jsonString) throws FrameworkBusinessException {
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return new HashMap<>();
            }

            return objectMapper.readValue(jsonString, Map.class);
        } catch (JsonProcessingException e) {
            throw new FrameworkBusinessException("jsonToMap",
                "Error convirtiendo JSON a Map: " + e.getMessage());
        }
    }

    // =================================================================================
    // GENERACIÓN DE DATOS AVANZADA (NUEVO)
    // =================================================================================

    /**
     * Genera un boolean aleatorio.
     * @since 1.2.0
     */
    public static boolean generateRandomBoolean() {
        return SecurityUtilities.getSecureRandomInstance().nextBoolean();
    }

    /**
     * Genera un número dentro de un rango (alias semántico para generateRandomNumber).
     * @since 1.2.0
     */
    public static int generateNumberInRange(int min, int max) {
        return generateRandomNumber(min, max);
    }

    /**
     * Genera un texto pseudo-largo (simple) de longitud aproximada.
     * @since 1.2.0
     */
    public static String generateLoremIpsum(int approxLength) {
        if (approxLength <= 0) return "";
        String base = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < approxLength) {
            sb.append(base);
        }
        return sb.substring(0, Math.min(approxLength, sb.length()));
    }

    /**
     * Genera una edad aleatoria dentro de un rango (útil para test data rápido).
     * @since 1.2.0
     */
    public static int generateAgeInRange(int minAge, int maxAge) {
        return generateRandomNumber(minAge, maxAge);
    }

    /**
     * Genera un valor monetario aleatorio dentro de un rango.
     * @since 1.2.0
     */
    public static double generateRandomAmount(double min, double max, int scale) {
        if (min > max) throw new IllegalArgumentException("min no puede ser > max");
        double raw = min + (SecurityUtilities.getSecureRandomInstance().nextDouble() * (max - min));
        return Math.round(raw * Math.pow(10, scale)) / Math.pow(10, scale);
    }

    /**
     * Genera un string aleatorio que parece un código (prefijo + alfanumérico).
     * @since 1.2.0
     */
    public static String generateCode(String prefix, int randomLength) {
        return (prefix != null ? prefix : "") + generateRandomAlphanumeric(randomLength).toUpperCase();
    }

    // =================================================================================
    // JSONPATH AVANZADO (NUEVO) - WRAPPER
    // =================================================================================

    /**
     * Obtiene un valor usando una expresión JSONPath avanzada. Soporta:
     *  - Indices: $.data.users[0].name
     *  - Wildcards: $.data.users[*].id
     *  - Filtros simples: $.data.users[?(@.active == true)].email
     * Si la expresión no comienza por '$', se añade automáticamente.
     *
     * @param jsonBody cuerpo JSON
     * @param jsonPath expresión JSONPath (ej: data.users[0].name)
     * @return Object resultado (List, Map, String, Number, Boolean o null)
     * @since 1.2.0
     */
    public static Object getByJsonPath(String jsonBody, String jsonPath) {
        if (!isValidString(jsonBody) || !isValidString(jsonPath)) {
            return null;
        }
        try {
            String path = jsonPath.startsWith("$") ? jsonPath : "$." + jsonPath;
            com.jayway.jsonpath.Configuration conf = com.jayway.jsonpath.Configuration.defaultConfiguration();
            return com.jayway.jsonpath.JsonPath.using(conf).parse(jsonBody).read(path);
        } catch (Exception e) {
            TestLogger.logDebug("DATA_UTILITIES", "JSONPath error: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Verifica si un JSONPath retorna algún valor no nulo.
     * @since 1.2.0
     */
    public static boolean hasJsonPath(String jsonBody, String jsonPath) {
        return getByJsonPath(jsonBody, jsonPath) != null;
    }

    /**
     * Obtiene una lista tipada desde JSONPath (convierte si es necesario).
     * @since 1.2.0
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getListByJsonPath(String jsonBody, String jsonPath, Class<T> elementType) {
        Object result = getByJsonPath(jsonBody, jsonPath);
        if (result == null) return Collections.emptyList();
        if (result instanceof List) {
            List<?> raw = (List<?>) result;
            List<T> converted = new ArrayList<>();
            for (Object o : raw) {
                try {
                    if (elementType.isInstance(o)) {
                        converted.add((T) o);
                    } else {
                        converted.add(objectMapper.convertValue(o, elementType));
                    }
                } catch (Exception ex) {
                    TestLogger.logDebug("DATA_UTILITIES", "Conversión fallida de elemento JSONPath: " + ex.getMessage(), null);
                }
            }
            return converted;
        }
        // Si es un único elemento intentando envolverlo
        try {
            T single = elementType.isInstance(result) ? (T) result : objectMapper.convertValue(result, elementType);
            return Collections.singletonList(single);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // =================================================================================
    // COMPARACIÓN DE JSON (NUEVO)
    // =================================================================================

    /**
     * Compara dos JSON y retorna true si son equivalentes ignorando el orden de arrays y espacios.
     * @since 1.2.0
     */
    public static boolean areJsonEqual(String json1, String json2) {
        if (!isValidString(json1) || !isValidString(json2)) return false;
        try {
            org.skyscreamer.jsonassert.JSONAssert.assertEquals(json1, json2, false);
            return true;
        } catch (AssertionError | Exception e) {
            return false;
        }
    }

    /**
     * Compara dos JSON y devuelve un diff textual simple (campos que difieren).
     * No es exhaustivo pero ayuda en debugging.
     * @since 1.2.0
     */
    public static String diffJson(String expectedJson, String actualJson) {
        if (!isValidString(expectedJson) || !isValidString(actualJson)) {
            return "<EMPTY_OR_INVALID_INPUT>";
        }
        try {
            Map<String, Object> expected = jsonToMap(expectedJson);
            Map<String, Object> actual = jsonToMap(actualJson);
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(expected.keySet());
            allKeys.addAll(actual.keySet());
            StringBuilder diff = new StringBuilder();
            for (String key : allKeys) {
                Object ev = expected.get(key);
                Object av = actual.get(key);
                if (!Objects.equals(ev, av)) {
                    diff.append(key).append(" => expected: ").append(ev).append(", actual: ").append(av).append("\n");
                }
            }
            return diff.length() == 0 ? "<NO_DIFF>" : diff.toString();
        } catch (Exception e) {
            return "<DIFF_ERROR: " + e.getMessage() + ">";
        }
    }

    /**
     * Verifica que el JSON actual contenga todos los campos del esperado (shallow level).
     * @since 1.2.0
     */
    public static boolean jsonContainsAllFields(String expectedJson, String actualJson) {
        try {
            Map<String, Object> expected = jsonToMap(expectedJson);
            Map<String, Object> actual = jsonToMap(actualJson);
            for (String key : expected.keySet()) {
                if (!actual.containsKey(key)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
