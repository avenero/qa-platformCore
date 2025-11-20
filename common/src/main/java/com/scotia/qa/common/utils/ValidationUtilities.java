package com.scotia.qa.common.utils;

import com.scotia.qa.common.http.HttpResponse;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilidades de validación para el framework Scotia QA.
 *
 * <p>Esta clase proporciona funcionalidades técnicas puras para validaciones comunes
 * que pueden ser utilizadas por cualquier framework consumidor. Incluye validaciones
 * para respuestas HTTP, esquemas JSON, tipos de datos y reglas básicas.
 *
 * <p><b>Tipos de validación soportados:</b>
 * <ul>
 *   <li><b>HTTP Status Codes</b> - Validación exacta y por rangos</li>
 *   <li><b>HTTP Headers</b> - Presencia, valores y formatos</li>
 *   <li><b>JSON Path</b> - Validación usando JSONPath</li>
 *   <li><b>Data Types</b> - String, números, booleanos, arrays</li>
 *   <li><b>Response Body</b> - Contenido y formatos</li>
 *   <li><b>Regular Expressions</b> - Patrones y formatos</li>
 * </ul>
 *
 * <p><b>Características:</b>
 * <ul>
 *   <li>Métodos estáticos thread-safe</li>
 *   <li>Mensajes de error descriptivos</li>
 *   <li>Validaciones tipo-safe</li>
 *   <li>Logging detallado para debugging</li>
 *   <li>Sin estado - solo funciones puras</li>
 * </ul>
 *
 * <p><b>Uso típico:</b>
 * <pre>
 * // Validaciones HTTP básicas
 * ValidationUtilities.validateStatusCode(response, 200);
 * ValidationUtilities.validateHeaderExists(response, "Content-Type");
 *
 * // Validaciones JSON
 * ValidationUtilities.validateJsonPath(response, "$.user.name", "John");
 * ValidationUtilities.validateJsonType(response, "$.user.age", "integer");
 *
 * // Validaciones de datos
 * ValidationUtilities.validatePattern("email@test.com", ValidationUtilities.EMAIL_PATTERN);
 * ValidationUtilities.validateRange(25, 18, 65);
 * </pre>
 *
 * <p><b>Uso en frameworks específicos:</b>
 * <pre>
 * // En api-core
 * public class ApiValidator {
 *     public static void validateRestResponse(HttpResponse response) {
 *         ValidationUtilities.validateStatusCodeRange(response, 200, 299);
 *         ValidationUtilities.validateHeaderExists(response, "Content-Type");
 *         ValidationUtilities.validateHeaderValue(response, "Content-Type", "application/json");
 *     }
 * }
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 */
public class ValidationUtilities {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(ValidationUtilities.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =================================================================================
    // PATRONES DE VALIDACIÓN COMUNES (CONSOLIDADOS)
    // =================================================================================

    // Patrones básicos
    public static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    public static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*$");

    public static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    // Patrones específicos de Chile (movidos desde DataUtilities)
    public static final Pattern RUT_PATTERN = Pattern.compile(
        "^[0-9]{1,2}\\.[0-9]{3}\\.[0-9]{3}-[0-9kK]$");

    public static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+56[0-9]{8,9}$");

    public static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]+$");

    // Constructor privado para clase de utilidad
    private ValidationUtilities() {
        throw new UnsupportedOperationException("ValidationUtilities es una clase de utilidad");
    }

    // =================================================================================
    // VALIDACIONES HTTP STATUS CODES
    // =================================================================================

    /**
     * Valida que el status code sea exactamente el esperado.
     *
     * @param response la respuesta HTTP a validar
     * @param expectedStatus el status code esperado
     * @throws FrameworkBusinessException si el status no coincide
     */
    public static void validateStatusCode(HttpResponse response, int expectedStatus) throws FrameworkBusinessException {
        if (response == null) {
            throw new FrameworkBusinessException("validateStatusCode", "Response is null");
        }

        int actualStatus = response.getStatusCode();
        if (actualStatus != expectedStatus) {
            throw new FrameworkBusinessException("validateStatusCode",
                String.format("Expected status %d but was %d. Response: %s",
                    expectedStatus, actualStatus, truncateContent(response.getBody(), 200)));
        }

        log.debug("Status code validado correctamente: {}", expectedStatus);
    }

    /**
     * Valida que el status code esté dentro del rango especificado.
     *
     * @param response la respuesta HTTP a validar
     * @param minStatus status code mínimo (inclusivo)
     * @param maxStatus status code máximo (inclusivo)
     * @throws FrameworkBusinessException si el status está fuera del rango
     */
    public static void validateStatusCodeRange(HttpResponse response, int minStatus, int maxStatus) throws FrameworkBusinessException {
        if (response == null) {
            throw new FrameworkBusinessException("validateStatusCodeRange", "Response is null");
        }

        int actualStatus = response.getStatusCode();
        if (actualStatus < minStatus || actualStatus > maxStatus) {
            throw new FrameworkBusinessException("validateStatusCodeRange",
                String.format("Expected status between %d and %d but was %d. Response: %s",
                    minStatus, maxStatus, actualStatus, truncateContent(response.getBody(), 200)));
        }

        log.debug("Status code en rango validado: {} ({}-{})", actualStatus, minStatus, maxStatus);
    }

    // =================================================================================
    // VALIDACIONES HTTP HEADERS
    // =================================================================================

    /**
     * Valida que existe un header específico.
     *
     * @param response la respuesta HTTP a validar
     * @param headerName nombre del header a verificar
     * @throws FrameworkBusinessException si el header no existe
     */
    public static void validateHeaderExists(HttpResponse response, String headerName) throws FrameworkBusinessException {
        if (response == null) {
            throw new FrameworkBusinessException("validateHeaderExists", "Response is null");
        }

        Map<String, String> headers = response.getHeaders();
        if (headers == null || !headers.containsKey(headerName)) {
            throw new FrameworkBusinessException("validateHeaderExists",
                String.format("Header '%s' not found in response. Available headers: %s",
                    headerName, headers != null ? headers.keySet() : "none"));
        }

        log.debug("Header existe: {}", headerName);
    }

    /**
     * Valida el valor específico de un header.
     *
     * @param response la respuesta HTTP a validar
     * @param headerName nombre del header
     * @param expectedValue valor esperado del header
     * @throws FrameworkBusinessException si el header no existe o el valor no coincide
     */
    public static void validateHeaderValue(HttpResponse response, String headerName, String expectedValue) throws FrameworkBusinessException {
        validateHeaderExists(response, headerName);

        String actualValue = response.getHeaders().get(headerName);
        if (!expectedValue.equals(actualValue)) {
            throw new FrameworkBusinessException("validateHeaderValue",
                String.format("Header '%s' expected '%s' but was '%s'",
                    headerName, expectedValue, actualValue));
        }

        log.debug("Header valor validado: {} = {}", headerName, expectedValue);
    }

    /**
     * Valida que un header contenga un valor específico (substring).
     *
     * @param response la respuesta HTTP a validar
     * @param headerName nombre del header
     * @param expectedSubstring substring que debe contener
     * @throws FrameworkBusinessException si el header no contiene el substring
     */
    public static void validateHeaderContains(HttpResponse response, String headerName, String expectedSubstring) throws FrameworkBusinessException {
        validateHeaderExists(response, headerName);

        String actualValue = response.getHeaders().get(headerName);
        if (!actualValue.contains(expectedSubstring)) {
            throw new FrameworkBusinessException("validateHeaderContains",
                String.format("Header '%s' does not contain '%s'. Actual value: '%s'",
                    headerName, expectedSubstring, actualValue));
        }

        log.debug("Header contiene substring validado: {} contiene '{}'", headerName, expectedSubstring);
    }

    // =================================================================================
    // VALIDACIONES JSON SCHEMA
    // =================================================================================

    /**
     * Valida que el cuerpo de la respuesta HTTP cumpla con un esquema JSON.
     *
     * <p>Este método puede recibir el esquema de dos formas:
     * <ul>
     *   <li><b>Schema inline</b>: El esquema JSON completo como string (ejemplo desde DocString de Cucumber)</li>
     *   <li><b>Schema file</b>: Ruta a un archivo .json con el esquema (ejemplo: "schemas/user-schema.json")</li>
     * </ul>
     *
     * <p><b>Ejemplo de uso en Cucumber - Schema inline:</b>
     * <pre>
     * Then valido que el cuerpo de la respuesta tenga el siguiente esquema
     *   """
     *   {
     *     "type": "object",
     *     "properties": {
     *       "id": {"type": "integer"},
     *       "name": {"type": "string"}
     *     },
     *     "required": ["id", "name"]
     *   }
     *   """
     * </pre>
     *
     * <p><b>Ejemplo de uso en Cucumber - Schema file:</b>
     * <pre>
     * Then valido que el cuerpo de la respuesta tenga el siguiente esquema
     *   """
     *   schemas/user-response-schema.json
     *   """
     * </pre>
     *
     * @param response la respuesta HTTP cuyo body se va a validar
     * @param schemaOrPath esquema JSON como string o ruta al archivo de esquema
     * @throws FrameworkBusinessException si la validación falla o hay errores de esquema
     */
    public static void validateJsonSchema(HttpResponse response, String schemaOrPath) throws FrameworkBusinessException {
        if (response == null || response.getBody() == null) {
            throw new FrameworkBusinessException("validateJsonSchema", "Response or body is null");
        }

        try {
            // Determinar si es un archivo o un esquema inline
            String schemaContent = resolveSchemaContent(schemaOrPath);

            // Crear factory de JSON Schema (usando Draft 7 por defecto)
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

            // Parsear el esquema
            JsonNode schemaNode = MAPPER.readTree(schemaContent);
            JsonSchema schema = factory.getSchema(schemaNode);

            // Parsear el JSON response
            JsonNode responseNode = MAPPER.readTree(response.getBody());

            // Validar
            Set<ValidationMessage> errors = schema.validate(responseNode);

            if (!errors.isEmpty()) {
                String errorMessages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("\n  - "));

                throw new FrameworkBusinessException("validateJsonSchema",
                    String.format("JSON Schema validation failed with %d error(s):\n  - %s",
                        errors.size(), errorMessages));
            }

            log.info("✅ JSON Schema validado exitosamente");

        } catch (IOException e) {
            throw new FrameworkBusinessException("validateJsonSchema",
                String.format("Error parsing JSON or schema: %s", e.getMessage()));
        } catch (Exception e) {
            throw new FrameworkBusinessException("validateJsonSchema",
                String.format("Error validating JSON schema: %s", e.getMessage()));
        }
    }

    /**
     * Valida un JSON string contra un esquema JSON.
     *
     * @param jsonContent contenido JSON a validar
     * @param schemaOrPath esquema JSON como string o ruta al archivo de esquema
     * @throws FrameworkBusinessException si la validación falla
     */
    public static void validateJsonSchema(String jsonContent, String schemaOrPath) throws FrameworkBusinessException {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            throw new FrameworkBusinessException("validateJsonSchema", "JSON content is null or empty");
        }

        try {
            // Determinar si es un archivo o un esquema inline
            String schemaContent = resolveSchemaContent(schemaOrPath);

            // Crear factory de JSON Schema
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

            // Parsear el esquema
            JsonNode schemaNode = MAPPER.readTree(schemaContent);
            JsonSchema schema = factory.getSchema(schemaNode);

            // Parsear el JSON content
            JsonNode contentNode = MAPPER.readTree(jsonContent);

            // Validar
            Set<ValidationMessage> errors = schema.validate(contentNode);

            if (!errors.isEmpty()) {
                String errorMessages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("\n  - "));

                throw new FrameworkBusinessException("validateJsonSchema",
                    String.format("JSON Schema validation failed with %d error(s):\n  - %s",
                        errors.size(), errorMessages));
            }

            log.info("✅ JSON Schema validado exitosamente");

        } catch (IOException e) {
            throw new FrameworkBusinessException("validateJsonSchema",
                String.format("Error parsing JSON or schema: %s", e.getMessage()));
        } catch (Exception e) {
            throw new FrameworkBusinessException("validateJsonSchema",
                String.format("Error validating JSON schema: %s", e.getMessage()));
        }
    }

    /**
     * Resuelve el contenido del esquema desde un archivo o string inline.
     *
     * @param schemaOrPath esquema JSON inline o ruta a archivo
     * @return contenido del esquema como string
     * @throws IOException si hay error leyendo el archivo
     */
    private static String resolveSchemaContent(String schemaOrPath) throws IOException {
        if (schemaOrPath == null || schemaOrPath.trim().isEmpty()) {
            throw new IOException("Schema or path is null or empty");
        }

        String trimmed = schemaOrPath.trim();

        // Si empieza con { o [, es un JSON inline
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            log.debug("📄 Usando esquema JSON inline");
            return trimmed;
        }

        // Si no, intentar leer como archivo
        // Buscar en múltiples ubicaciones posibles
        Path schemaPath = resolveSchemaFilePath(trimmed);

        if (schemaPath == null || !Files.exists(schemaPath)) {
            throw new IOException(String.format(
                "Schema file not found: '%s'. Tried locations: " +
                "current directory, src/test/resources/schemas/, test/resources/schemas/, resources/schemas/",
                trimmed));
        }

        log.debug("📁 Leyendo esquema desde archivo: {}", schemaPath.toAbsolutePath());
        return Files.readString(schemaPath);
    }

    /**
     * Resuelve la ruta del archivo de esquema buscando en múltiples ubicaciones.
     *
     * @param filename nombre del archivo de esquema
     * @return Path al archivo encontrado, o null si no existe
     */
    private static Path resolveSchemaFilePath(String filename) {
        // 1. Intentar ruta directa (relativa o absoluta)
        Path directPath = Paths.get(filename);
        if (Files.exists(directPath)) {
            return directPath;
        }

        // 2. Intentar en src/test/resources/schemas/
        Path testResourcesPath = Paths.get("src/test/resources/schemas", filename);
        if (Files.exists(testResourcesPath)) {
            return testResourcesPath;
        }

        // 3. Intentar en test/resources/schemas/
        Path testResourcesAltPath = Paths.get("test/resources/schemas", filename);
        if (Files.exists(testResourcesAltPath)) {
            return testResourcesAltPath;
        }

        // 4. Intentar en resources/schemas/
        Path resourcesPath = Paths.get("resources/schemas", filename);
        if (Files.exists(resourcesPath)) {
            return resourcesPath;
        }

        // 5. Intentar en src/main/resources/schemas/
        Path mainResourcesPath = Paths.get("src/main/resources/schemas", filename);
        if (Files.exists(mainResourcesPath)) {
            return mainResourcesPath;
        }

        // 6. Intentar como recurso del classpath
        try {
            InputStream resourceStream = ValidationUtilities.class.getClassLoader()
                .getResourceAsStream("schemas/" + filename);
            if (resourceStream != null) {
                // Crear archivo temporal para leer el recurso
                Path tempFile = Files.createTempFile("schema-", ".json");
                Files.copy(resourceStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                return tempFile;
            }
        } catch (IOException e) {
            log.debug("No se pudo leer recurso desde classpath: {}", filename);
        }

        return null;
    }

    // =================================================================================
    // VALIDACIONES JSON PATH
    // =================================================================================

    /**
     * Valida el valor en una ruta JSON específica.
     *
     * @param response la respuesta HTTP con contenido JSON
     * @param jsonPath ruta JSON (ej: "$.user.name")
     * @param expectedValue valor esperado
     * @throws FrameworkBusinessException si la ruta no existe o el valor no coincide
     */
    public static void validateJsonPath(HttpResponse response, String jsonPath, Object expectedValue) throws FrameworkBusinessException {
        if (response == null || response.getBody() == null) {
            throw new FrameworkBusinessException("validateJsonPath", "Response or body is null");
        }

        try {
            Object actualValue = DataUtilities.getJsonParameter(response.getBody(), jsonPath);
            if (!expectedValue.equals(actualValue)) {
                throw new FrameworkBusinessException("validateJsonPath",
                    String.format("JSON path '%s' expected '%s' but was '%s'",
                        jsonPath, expectedValue, actualValue));
            }

            log.debug("JSON path validado: {} = {}", jsonPath, expectedValue);

        } catch (Exception e) {
            throw new FrameworkBusinessException("validateJsonPath",
                String.format("Error validating JSON path '%s': %s", jsonPath, e.getMessage()));
        }
    }

    /**
     * Valida que existe una ruta JSON específica.
     *
     * @param response la respuesta HTTP con contenido JSON
     * @param jsonPath ruta JSON a verificar
     * @throws FrameworkBusinessException si la ruta no existe
     */
    public static void validateJsonPathExists(HttpResponse response, String jsonPath) throws FrameworkBusinessException {
        if (response == null || response.getBody() == null) {
            throw new FrameworkBusinessException("validateJsonPathExists", "Response or body is null");
        }

        boolean exists = DataUtilities.hasJsonField(response.getBody(), jsonPath);
        if (!exists) {
            throw new FrameworkBusinessException("validateJsonPathExists",
                String.format("JSON path '%s' does not exist in response", jsonPath));
        }

        log.debug("JSON path existe: {}", jsonPath);
    }

    /**
     * Valida el tipo de dato en una ruta JSON específica.
     *
     * @param response la respuesta HTTP con contenido JSON
     * @param jsonPath ruta JSON
     * @param expectedType tipo esperado ("string", "integer", "boolean", "array", "object")
     * @throws FrameworkBusinessException si el tipo no coincide
     */
    public static void validateJsonType(HttpResponse response, String jsonPath, String expectedType) throws FrameworkBusinessException {
        if (response == null || response.getBody() == null) {
            throw new FrameworkBusinessException("validateJsonType", "Response or body is null");
        }

        try {
            Object value = DataUtilities.getJsonParameter(response.getBody(), jsonPath);
            if (value == null) {
                throw new FrameworkBusinessException("validateJsonType",
                    String.format("JSON path '%s' is null", jsonPath));
            }

            String actualType = getJsonValueType(value);
            if (!expectedType.equalsIgnoreCase(actualType)) {
                throw new FrameworkBusinessException("validateJsonType",
                    String.format("JSON path '%s' expected type '%s' but was '%s'",
                        jsonPath, expectedType, actualType));
            }

            log.debug("JSON type validado: {} es {}", jsonPath, expectedType);

        } catch (Exception e) {
            throw new FrameworkBusinessException("validateJsonType",
                String.format("Error validating JSON type '%s': %s", jsonPath, e.getMessage()));
        }
    }

    // =================================================================================
    // VALIDACIONES DE PATRONES Y FORMATOS
    // =================================================================================

    /**
     * Valida que un string coincida con un patrón regex.
     *
     * @param value valor a validar
     * @param pattern patrón regex
     * @throws FrameworkBusinessException si no coincide con el patrón
     */
    public static void validatePattern(String value, Pattern pattern) throws FrameworkBusinessException {
        if (value == null) {
            throw new FrameworkBusinessException("validatePattern", "Value is null");
        }

        if (!pattern.matcher(value).matches()) {
            throw new FrameworkBusinessException("validatePattern",
                String.format("Value '%s' does not match pattern '%s'", value, pattern.pattern()));
        }

        log.debug("Patrón validado: {} coincide con {}", value, pattern.pattern());
    }

    /**
     * Valida que un string sea un email válido.
     *
     * @param email email a validar
     * @throws FrameworkBusinessException si no es un email válido
     */
    public static void validateEmail(String email) throws FrameworkBusinessException {
        validatePattern(email, EMAIL_PATTERN);
    }

    /**
     * Valida que un string sea una URL válida.
     *
     * @param url URL a validar
     * @throws FrameworkBusinessException si no es una URL válida
     */
    public static void validateUrl(String url) throws FrameworkBusinessException {
        validatePattern(url, URL_PATTERN);
    }

    /**
     * Valida que un string sea un UUID válido.
     *
     * @param uuid UUID a validar
     * @throws FrameworkBusinessException si no es un UUID válido
     */
    public static void validateUUID(String uuid) throws FrameworkBusinessException {
        validatePattern(uuid, UUID_PATTERN);
    }

    /**
     * Valida formato de email.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Valida formato de RUT chileno.
     */
    public static boolean isValidRut(String rut) {
        if (rut == null || rut.trim().isEmpty()) {
            return false;
        }
        return RUT_PATTERN.matcher(rut.trim()).matches();
    }

    /**
     * Valida formato de teléfono chileno.
     */
    public static boolean isValidPhoneChile(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Valida que una cadena sea alpanumérica.
     */
    public static boolean isAlphanumeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(value).matches();
    }

    // =================================================================================
    // VALIDACIONES NUMÉRICAS
    // =================================================================================

    /**
     * Valida que un número esté dentro de un rango.
     *
     * @param value valor a validar
     * @param min valor mínimo (inclusivo)
     * @param max valor máximo (inclusivo)
     * @throws FrameworkBusinessException si está fuera del rango
     */
    public static void validateRange(Number value, Number min, Number max) throws FrameworkBusinessException {
        if (value == null) {
            throw new FrameworkBusinessException("validateRange", "Value is null");
        }

        double doubleValue = value.doubleValue();
        double doubleMin = min.doubleValue();
        double doubleMax = max.doubleValue();

        if (doubleValue < doubleMin || doubleValue > doubleMax) {
            throw new FrameworkBusinessException("validateRange",
                String.format("Value %s is not within range [%s, %s]", value, min, max));
        }

        log.debug("Rango validado: {} está en [{}, {}]", value, min, max);
    }

    /**
     * Valida que un número sea positivo.
     *
     * @param value número a validar
     * @throws FrameworkBusinessException si es negativo o cero
     */
    public static void validatePositive(Number value) throws FrameworkBusinessException {
        if (value == null || value.doubleValue() <= 0) {
            throw new FrameworkBusinessException("validatePositive",
                String.format("Value %s is not positive", value));
        }

        log.debug("Número positivo validado: {}", value);
    }

    // =================================================================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =================================================================================

    /**
     * Determina el tipo de un valor JSON.
     */
    private static String getJsonValueType(Object value) {
        if (value instanceof String) return "string";
        if (value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof Float) return "integer";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List) return "array";
        if (value instanceof Map) return "object";
        return "unknown";
    }

    /**
     * Trunca contenido para mensajes de error.
     */
    private static String truncateContent(String content, int maxLength) {
        if (content == null) return "null";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
}

