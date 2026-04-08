package com.qa.common.utils;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;

import java.util.Set;

/**
 * Utilidades para manipulación, normalización y comparación de texto.
 *
 * <p>Consolida operaciones sobre strings que antes estaban dispersas en {@link DataUtilities}:</p>
 * <ul>
 *   <li>Normalización de texto (espacios, HTML entities, tabs/newlines)</li>
 *   <li>Comparación flexible (exact, ignore-case, trim, contains)</li>
 *   <li>Extracción de números desde texto</li>
 *   <li>Sanitización para logs (GDPR/PCI-DSS compliance)</li>
 *   <li>Comparación cross-layer API ↔ Web ↔ Mobile usando {@link ExecutionContext}</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.1.0
 */
public final class TextUtilities {

    // =========================================================================
    // CONFIGURACIÓN DE SEGURIDAD / LOGGING
    // =========================================================================

    /** Longitud máxima para valores en logs – previene logs gigantes. */
    private static final int MAX_LOG_VALUE_LENGTH = 200;

    /** Palabras clave que identifican datos sensibles (GDPR/PCI-DSS). */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "token", "secret", "apikey", "api_key", "authorization",
        "creditcard", "credit_card", "ssn", "pin", "otp", "privatekey",
        "private_key", "bearer", "refresh_token", "access_token", "session"
    );

    private TextUtilities() {
        throw new UnsupportedOperationException("TextUtilities es una clase de utilidad");
    }

    // =========================================================================
    // SANITIZACIÓN PARA LOGS
    // =========================================================================

    /**
     * Sanitiza un valor para incluirlo en logs de forma segura.
     *
     * <p>Retorna {@code "***HIDDEN***"} si la clave contiene palabras sensibles.
     * Trunca valores largos para evitar logs gigantes.
     *
     * @param key   nombre de la variable/campo
     * @param value valor a sanitizar
     * @return valor sanitizado seguro para logs
     */
    public static String sanitizeForLogging(String key, Object value) {
        if (value == null) return "null";
        String lowerKey = key != null ? key.toLowerCase() : "";
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) return "***HIDDEN***";
        }
        String valueStr = value.toString();
        return valueStr.length() > MAX_LOG_VALUE_LENGTH
            ? valueStr.substring(0, MAX_LOG_VALUE_LENGTH) + "... [TRUNCATED]"
            : valueStr;
    }

    /**
     * Sanitiza claves sensibles dentro de un JSON/texto para logs.
     *
     * <p>Reemplaza valores de campos como {@code password}, {@code token}, etc.
     *
     * @param data texto o JSON a sanitizar
     * @return texto con valores sensibles reemplazados por {@code "***HIDDEN***"}
     */
    public static String sanitizeForLog(String data) {
        if (data == null) return null;
        return data.replaceAll(
            "(password|token|secret|key)\"\\s*:\\s*\"[^\"]*\"",
            "$1\":\"***HIDDEN***\""
        );
    }

    /**
     * Sanitiza un valor usando la clave como contexto.
     *
     * @param key   clave/nombre del campo
     * @param value valor a sanitizar
     * @return valor sanitizado o {@code "***HIDDEN***"} si la clave es sensible
     */
    public static String sanitizeValue(String key, String value) {
        if (value == null) return null;
        if (key == null) return value;
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("password") || lowerKey.contains("token") ||
            lowerKey.contains("secret") || lowerKey.contains("key") ||
            lowerKey.contains("authorization")) {
            return "***HIDDEN***";
        }
        return value;
    }

    /**
     * Sanitiza el body de una petición/respuesta HTTP para logs.
     *
     * @param body body a sanitizar
     * @return body con campos sensibles enmascarados
     */
    public static String sanitizeBody(String body) {
        return sanitizeForLog(body);
    }

    // =========================================================================
    // TRUNCADO Y VALIDACIÓN
    // =========================================================================

    /**
     * Trunca contenido largo para incluirlo en logs o mensajes.
     *
     * @param content   contenido a truncar
     * @param maxLength longitud máxima permitida
     * @return contenido truncado con {@code "... [TRUNCATED]"} al final si aplica
     */
    public static String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "... [TRUNCATED]";
    }

    /**
     * Verifica si un string es válido (no nulo y no vacío).
     *
     * @param str string a verificar
     * @return {@code true} si el string es no nulo y no está en blanco
     */
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Capitaliza la primera letra de un string.
     *
     * <pre>
     * capitalize("hello")  → "Hello"
     * capitalize("HELLO")  → "HELLO"
     * capitalize(null)     → null
     * capitalize("")       → ""
     * </pre>
     *
     * @param str String a capitalizar
     * @return String con primera letra en mayúscula
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // =========================================================================
    // NORMALIZACIÓN DE TEXTO
    // =========================================================================

    /**
     * Normaliza un texto para comparaciones.
     *
     * <p>Operaciones aplicadas:
     * <ul>
     *   <li>Trim</li>
     *   <li>Reemplaza {@code &nbsp;}, {@code &amp;}, {@code &lt;}, {@code &gt;}, {@code &quot;}</li>
     *   <li>Convierte tabs y saltos de línea a espacios</li>
     *   <li>Colapsa espacios múltiples a uno</li>
     * </ul>
     *
     * @param text texto a normalizar
     * @return texto normalizado, o string vacío si el input es null
     */
    public static String normalizeText(String text) {
        if (text == null) return "";
        String normalized = text
            .replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("\t", " ").replace("\n", " ").replace("\r", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    // =========================================================================
    // COMPARACIÓN DE TEXTO
    // =========================================================================

    /**
     * Compara dos textos de forma flexible (normalización previa).
     *
     * @param text1      primer texto
     * @param text2      segundo texto
     * @param ignoreCase {@code true} para ignorar mayúsculas/minúsculas
     * @return {@code true} si los textos coinciden tras normalización
     */
    public static boolean compareTextFlexible(String text1, String text2, boolean ignoreCase) {
        if (text1 == null && text2 == null) return true;
        if (text1 == null || text2 == null) return false;
        String n1 = ignoreCase ? normalizeText(text1).toLowerCase() : normalizeText(text1);
        String n2 = ignoreCase ? normalizeText(text2).toLowerCase() : normalizeText(text2);
        boolean match = n1.equals(n2);
        TestLogger.logDebug("TEXT_UTILITIES",
            String.format("compareText: '%s' vs '%s' = %s (ignoreCase: %s)",
                truncateContent(n1, 50), truncateContent(n2, 50), match, ignoreCase), null);
        return match;
    }

    /**
     * Compara dos textos de forma exacta (case-sensitive, con normalización).
     *
     * @param text1 primer texto
     * @param text2 segundo texto
     * @return {@code true} si coinciden exactamente tras normalización
     */
    public static boolean compareText(String text1, String text2) {
        return compareTextFlexible(text1, text2, false);
    }

    /**
     * Compara dos textos ignorando mayúsculas/minúsculas (con normalización).
     *
     * @param text1 primer texto
     * @param text2 segundo texto
     * @return {@code true} si coinciden ignorando case
     */
    public static boolean compareTextIgnoreCase(String text1, String text2) {
        return compareTextFlexible(text1, text2, true);
    }

    /**
     * Verifica si un texto contiene otro (case-insensitive, normalizado).
     *
     * @param text      texto donde buscar
     * @param substring texto a buscar
     * @return {@code true} si {@code text} contiene {@code substring}
     */
    public static boolean containsText(String text, String substring) {
        if (text == null || substring == null) return false;
        return normalizeText(text).toLowerCase().contains(normalizeText(substring).toLowerCase());
    }

    // =========================================================================
    // EXTRACCIÓN DE NÚMEROS
    // =========================================================================

    /**
     * Extrae números de un texto.
     *
     * <pre>
     * extractNumber("$1,234.56") → "1234.56"
     * extractNumber("100 items") → "100"
     * </pre>
     *
     * @param text texto que contiene números
     * @return string con solo números y punto decimal, o {@code null}
     */
    public static String extractNumber(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String numbers = text.replaceAll("[^0-9.,]", "");
        if (numbers.contains(",") && !numbers.contains(".")) {
            numbers = numbers.replace(",", ".");
        } else if (numbers.contains(",") && numbers.contains(".")) {
            numbers = numbers.replace(",", "");
        }
        return numbers.isEmpty() ? null : numbers;
    }

    /**
     * Extrae y convierte un número de un texto a {@code Double}.
     *
     * @param text texto que contiene un número
     * @return número como Double, o {@code null}
     */
    public static Double extractNumberAsDouble(String text) {
        String numberStr = extractNumber(text);
        if (numberStr == null) return null;
        try {
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            TestLogger.logWarning("TEXT_UTILITIES",
                "No se pudo parsear número: '" + numberStr + "'", null);
            return null;
        }
    }

    /**
     * Extrae y convierte un número de un texto a {@code Integer}.
     *
     * @param text texto que contiene un entero
     * @return número como Integer, o {@code null}
     */
    public static Integer extractNumberAsInteger(String text) {
        Double d = extractNumberAsDouble(text);
        return d != null ? d.intValue() : null;
    }

    // =========================================================================
    // COMPARACIÓN CON MODO (ComparisonMode)
    // =========================================================================

    /**
     * Modos de comparación disponibles para {@link #compareValues(Object, Object, ComparisonMode)}.
     */
    public enum ComparisonMode {
        /** Comparación exacta (case-sensitive, sin modificaciones). */
        EXACT,
        /** Ignora mayúsculas/minúsculas. */
        IGNORE_CASE,
        /** Elimina espacios al inicio y final. */
        TRIM,
        /** Elimina espacios y normaliza múltiples a uno. */
        TRIM_AND_NORMALIZE,
        /** Ignora mayúsculas/minúsculas y elimina espacios. */
        IGNORE_CASE_AND_TRIM,
        /** Verifica si un valor contiene al otro. */
        CONTAINS
    }

    /**
     * Compara dos valores usando el modo de comparación especificado.
     *
     * @param value1 primer valor
     * @param value2 segundo valor
     * @param mode   modo de comparación
     * @return {@code true} si los valores son equivalentes según el modo
     */
    public static boolean compareValues(Object value1, Object value2, ComparisonMode mode) {
        if (value1 == null && value2 == null) return true;
        if (value1 == null || value2 == null) return false;
        String s1 = String.valueOf(value1);
        String s2 = String.valueOf(value2);
        switch (mode) {
            case EXACT:                 return s1.equals(s2);
            case IGNORE_CASE:           return s1.equalsIgnoreCase(s2);
            case TRIM:                  return s1.trim().equals(s2.trim());
            case TRIM_AND_NORMALIZE:    return normalizeText(s1).equals(normalizeText(s2));
            case IGNORE_CASE_AND_TRIM:  return s1.trim().equalsIgnoreCase(s2.trim());
            case CONTAINS:              return s1.contains(s2) || s2.contains(s1);
            default:                    return s1.equals(s2);
        }
    }

    /**
     * Compara dos valores usando comparación exacta.
     *
     * @param value1 primer valor
     * @param value2 segundo valor
     * @return {@code true} si son exactamente iguales
     */
    public static boolean compareValues(Object value1, Object value2) {
        return compareValues(value1, value2, ComparisonMode.EXACT);
    }

    // =========================================================================
    // COMPARACIÓN CROSS-LAYER (usa ExecutionContext)
    // =========================================================================

    /**
     * Compara un valor del ExecutionContext con otro del mismo contexto.
     *
     * <p>Ambas claves se resuelven desde {@link com.qa.common.runtime.VariableStore}.
     *
     * @param apiKey      clave del valor API
     * @param expectedKey clave del valor esperado
     * @param mode        modo de comparación
     * @return {@code true} si los valores coinciden
     * @throws IllegalStateException si alguna variable no existe en el contexto
     */
    public static boolean compareApiWithContext(String apiKey, String expectedKey, ComparisonMode mode) {
        Object apiValue = ExecutionContext.current()
            .flatMap(ctx -> ctx.variables().get(apiKey, Object.class))
            .orElseThrow(() -> new IllegalStateException("Variable API no encontrada: '" + apiKey + "'"));
        Object expectedValue = ExecutionContext.current()
            .flatMap(ctx -> ctx.variables().get(expectedKey, Object.class))
            .orElseThrow(() -> new IllegalStateException("Variable esperada no encontrada: '" + expectedKey + "'"));
        boolean result = compareValues(apiValue, expectedValue, mode);
        TestLogger.logDebug("TEXT_UTILITIES",
            String.format("compareApiWithContext: [%s]=%s vs [%s]=%s → %s",
                apiKey, sanitizeForLogging(apiKey, apiValue),
                expectedKey, sanitizeForLogging(expectedKey, expectedValue), result), null);
        return result;
    }

    /**
     * Aserta que dos valores del contexto coinciden, lanzando excepción si no es así.
     *
     * @param apiKey      clave del valor API
     * @param expectedKey clave del valor esperado
     * @param mode        modo de comparación
     * @throws AssertionError si los valores no coinciden
     */
    public static void assertApiWithContext(String apiKey, String expectedKey, ComparisonMode mode) {
        if (!compareApiWithContext(apiKey, expectedKey, mode)) {
            Object apiValue = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get(apiKey, Object.class)).orElse("N/A");
            Object expectedValue = ExecutionContext.current()
                .flatMap(ctx -> ctx.variables().get(expectedKey, Object.class)).orElse("N/A");
            throw new AssertionError(String.format(
                "Valores no coinciden — '%s' = '%s' vs '%s' = '%s' (modo: %s)",
                apiKey, sanitizeForLogging(apiKey, apiValue),
                expectedKey, sanitizeForLogging(expectedKey, expectedValue), mode));
        }
    }

    /**
     * Alias de {@link #assertApiWithContext(String, String, ComparisonMode)} con
     * comparación exacta. Mantiene compatibilidad con código que usaba
     * {@code DataUtilities.assertApiMatchesExpected}.
     *
     * @param apiKey      clave del valor API
     * @param expectedKey clave del valor esperado
     * @throws AssertionError si los valores no coinciden
     */
    public static void assertApiMatchesExpected(String apiKey, String expectedKey) {
        assertApiWithContext(apiKey, expectedKey, ComparisonMode.EXACT);
    }

    /**
     * Alias de {@link #assertApiWithContext(String, String, ComparisonMode)}.
     * Mantiene compatibilidad con código que usaba
     * {@code DataUtilities.assertApiMatchesExpected}.
     *
     * @param apiKey      clave del valor API
     * @param expectedKey clave del valor esperado
     * @param mode        modo de comparación
     * @throws AssertionError si los valores no coinciden
     */
    public static void assertApiMatchesExpected(String apiKey, String expectedKey, ComparisonMode mode) {
        assertApiWithContext(apiKey, expectedKey, mode);
    }
}

