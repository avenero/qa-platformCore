package com.scotia.qa.apicore.utils;

import com.scotia.qa.apicore.interfaces.HttpClient;
import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.http.exceptions.FrameworkTechnicalException;
import com.scotia.qa.common.http.model.HttpResponse;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.DataUtilities;
import java.util.Base64;
import java.util.Map;

/**
 * Helper para operaciones de API en steps de Cucumber.
 * Encapsula la lógica de validación, manejo de errores y logging.
 * Similar a WebHelper pero para API testing.
 *
 * <p>Responsabilidades:
 * - Wrapper de ValidationUtilities con contexto de httpClient
 * - Manejo centralizado de try-catch
 * - Logging automático de validaciones
 * - Simplificación de steps de Cucumber
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2026
 */
public class ApiHelper {

    private final HttpClient httpClient;

    /**
     * Constructor que recibe el cliente HTTP.
     *
     * @param httpClient Cliente HTTP para ejecutar peticiones
     */
    public ApiHelper(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // =========================================================================
    // CONFIGURACIÓN DE ENDPOINTS
    // =========================================================================

    /**
     * Configura el endpoint usando una propiedad del archivo config-{env}.properties.
     * Lee de ConfigManager y soporta reemplazo de variables.
     *
     * @param propertyKey clave de la propiedad
     * @throws RuntimeException si la propiedad no existe
     */
    public void configureEndpoint(String propertyKey) {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            String endpointValue = configManager.get(propertyKey);

            if (endpointValue == null || endpointValue.trim().isEmpty()) {
                throw new RuntimeException(String.format(
                    "Propiedad '%s' no encontrada o está vacía en config-{env}.properties. " +
                    "Verifica que la propiedad exista en config-qa.properties", propertyKey));
            }

            String processedUrl = DataUtilities.replaceVariables(endpointValue);
            httpClient.setHost(processedUrl);

            TestLogger.logInfo("API_HELPER_CONFIG",
                String.format("✅ Endpoint configurado: %s = %s", propertyKey, processedUrl), null);

        } catch (Exception e) {
            throw new RuntimeException(String.format(
                "Error configurando endpoint desde propiedad '%s': %s", propertyKey, e.getMessage()), e);
        }
    }

    /**
     * Establece el host base procesando variables.
     *
     * @param host host a establecer (soporta variables ${...})
     */
    public void setBaseHost(String host) {
        String processedHost = DataUtilities.replaceVariables(host);
        httpClient.setHost(processedHost);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Host base establecido: %s", processedHost), null);
    }

    /**
     * Agrega un header HTTP procesando variables.
     */
    public void addHeader(String header, String value) {
        String processedValue = DataUtilities.replaceVariables(value);
        httpClient.addHeader(header, processedValue);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Header agregado: %s", header), null);
    }

    /**
     * Agrega un query parameter procesando variables.
     */
    public void addQueryParam(String param, String value) {
        String processedValue = DataUtilities.replaceVariables(value);
        httpClient.addQueryParam(param, processedValue);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Query param agregado: %s", param), null);
    }

    /**
     * Establece el cuerpo de la petición procesando variables.
     */
    public void setRequestBody(String body) {
        String processedBody = DataUtilities.replaceVariables(body);
        httpClient.setBody(processedBody);
        TestLogger.logInfo("API_HELPER_REQUEST", "✅ Request body agregado", null);
    }

    /**
     * Agrega un field procesando variables.
     */
    public void addField(String key, String value) {
        String processedKey = DataUtilities.replaceVariables(key);
        String processedValue = DataUtilities.replaceVariables(value);
        httpClient.addField(processedKey, processedValue);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Field agregado: %s", processedKey), null);
    }

    /**
     * Establece el cuerpo JSON desde string procesando variables.
     */
    public void setJsonBodyFromString(String jsonBody) {
        String processedBody = DataUtilities.replaceVariables(jsonBody);
        httpClient.addHeader("Content-Type", "application/json");
        httpClient.setBody(processedBody);
        TestLogger.logInfo("API_HELPER_REQUEST", "✅ Request JSON agregado", null);
    }

    // =========================================================================
    // VALIDACIONES DE RESPUESTA
    // =========================================================================

    /**
     * Valida que el código de respuesta sea el esperado.
     * Encapsula try-catch y logging.
     */
    public void validateResponseStatusCode(int expectedCode) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateStatusCode(lastResponse, expectedCode);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Status code validado: %d", expectedCode), null);
    }

    /**
     * Valida que la respuesta contenga un texto específico.
     */
    public void validateResponseContainsText(String expectedText) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        String responseBody = lastResponse.getBody();

        if (responseBody == null || !responseBody.contains(expectedText)) {
            throw new FrameworkBusinessException("validateResponseContainsText",
                String.format("Texto '%s' no encontrado en la respuesta", expectedText));
        }

        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Texto validado en respuesta: %s", expectedText), null);
    }

    /**
     * Valida que la respuesta cumpla con un esquema JSON.
     */
    public void validateResponseSchema(String schemaOrPath) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateJsonSchema(lastResponse, schemaOrPath);
        TestLogger.logInfo("API_HELPER_VALIDATION", "Esquema JSON validado exitosamente", null);
    }

    /**
     * Valida un campo JSON usando JSONPath.
     */
    public void validateJsonField(String jsonPath, Object expectedValue) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateJsonPath(lastResponse, jsonPath, expectedValue);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Campo JSON validado: %s = %s", jsonPath, expectedValue), null);
    }

    /**
     * Valida que un campo JSON exista.
     */
    public void validateJsonFieldExists(String jsonPath) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateJsonPathExists(lastResponse, jsonPath);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Campo JSON existe: %s", jsonPath), null);
    }

    /**
     * Valida el tipo de un campo JSON.
     */
    public void validateJsonFieldType(String jsonPath, String expectedType) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateJsonType(lastResponse, jsonPath, expectedType);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Tipo de campo JSON validado: %s es %s", jsonPath, expectedType), null);
    }

    // =========================================================================
    // VALIDACIONES DE HEADERS
    // =========================================================================

    /**
     * Valida que un header exista en la respuesta.
     */
    public void validateHeaderExists(String headerName) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateHeaderExists(lastResponse, headerName);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Header existe: %s", headerName), null);
    }

    /**
     * Valida el valor de un header en la respuesta.
     */
    public void validateHeaderValue(String headerName, String expectedValue) throws FrameworkBusinessException {
        HttpResponse lastResponse = httpClient.getLastResponse();
        ValidationUtilities.validateHeaderValue(lastResponse, headerName, expectedValue);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Header validado: %s = %s", headerName, expectedValue), null);
    }

    // =========================================================================
    // CONFIGURACIÓN DE ENDPOINT
    // =========================================================================

    /**
     * Configura endpoint desde ConfigManager con base URL y path.
     * Encapsula toda la lógica de construcción de URL.
     */
    public void configureEndpointFromConfig(String baseUrlKey, String pathKey) {
        try {
            ConfigManager configManager = ConfigManager.getInstance();

            String baseUrl = configManager.get(baseUrlKey);
            String endpointPath = configManager.get(pathKey);

            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new RuntimeException(
                    String.format("Base URL '%s' no encontrada en config-{env}.properties", baseUrlKey));
            }

            if (endpointPath == null || endpointPath.trim().isEmpty()) {
                throw new RuntimeException(
                    String.format("Endpoint path '%s' no encontrado en config-{env}.properties", pathKey));
            }

            String fullUrl = baseUrl.endsWith("/") ? baseUrl + endpointPath : baseUrl + "/" + endpointPath;
            String processedUrl = DataUtilities.replaceVariables(fullUrl);

            httpClient.setHost(processedUrl);

            TestLogger.logInfo("API_HELPER_CONFIG",
                String.format("✅ Endpoint configurado: %s + %s = %s", baseUrlKey, pathKey, processedUrl), null);

        } catch (Exception e) {
            TestLogger.logError("API_HELPER_CONFIG",
                "❌ Error configurando endpoint: " + e.getMessage(), null);
            throw new RuntimeException(
                String.format("Error configurando endpoint con base '%s' y path '%s': %s",
                    baseUrlKey, pathKey, e.getMessage()), e);
        }
    }

    // =========================================================================
    // AUTENTICACIÓN
    // =========================================================================

    /**
     * Agrega autenticación básica con usuario y contraseña.
     * Encapsula encoding Base64 y configuración de header.
     */
    public void addBasicAuthentication(String username, String password) {
        try {
            String processedUsername = DataUtilities.replaceVariables(username);
            String processedPassword = DataUtilities.replaceVariables(password);

            String credentials = Base64.getEncoder()
                .encodeToString((processedUsername + ":" + processedPassword).getBytes());

            httpClient.addHeader("Authorization", "Basic " + credentials);

            TestLogger.logInfo("API_HELPER_AUTH",
                String.format("✅ Autenticación básica configurada para: %s", processedUsername), null);
        } catch (Exception e) {
            TestLogger.logError("API_HELPER_AUTH",
                "❌ Error configurando autenticación básica: " + e.getMessage(), null);
            throw new RuntimeException("Error configurando autenticación básica", e);
        }
    }

    /**
     * Agrega token Bearer personalizado.
     */
    public void addBearerToken(String token) {
        String processedToken = DataUtilities.replaceVariables(token);
        httpClient.addHeader("Authorization", "Bearer " + processedToken);
        TestLogger.logInfo("API_HELPER_AUTH",
            "✅ Token Bearer configurado", null);
    }

    // =========================================================================
    // MÉTODOS DE NAVEGACIÓN Y RESOLUCIÓN DE OBJETOS
    // =========================================================================

    /**
     * Resuelve un objeto usando notación de punto (ej: "response.data.user").
     * Soporta navegación por múltiples niveles.
     *
     * @param objectPath path del objeto a resolver
     * @return el objeto encontrado o null si no existe
     */
    public Object resolveObjectPath(String objectPath) throws Exception {
        if (objectPath == null || objectPath.trim().isEmpty()) {
            return null;
        }

        if (objectPath.contains(".")) {
            String[] pathParts = objectPath.split("\\.");

            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("🔍 Navegando por path con %d niveles: %s", pathParts.length, objectPath), null);

            Object current = resolveSimpleObject(pathParts[0]);

            if (current == null) {
                return null;
            }

            for (int i = 1; i < pathParts.length; i++) {
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("➡️ Navegando a nivel '%s'", pathParts[i]), null);

                current = extractFieldValue(current, pathParts[i]);

                if (current == null) {
                    TestLogger.logDebug("API_HELPER_SERIALIZATION",
                        String.format("❌ Nivel '%s' no encontrado o es null", pathParts[i]), null);
                    return null;
                }
            }

            return current;
        } else {
            return resolveSimpleObject(objectPath);
        }
    }

    /**
     * Resuelve un nombre de objeto simple (sin notación de punto).
     * Busca primero en DataUtilities, luego en la última respuesta deserializada.
     *
     * @param objectName nombre del objeto a resolver
     * @return el objeto encontrado o null si no existe
     */
    public Object resolveSimpleObject(String objectName) {
        Object stored = DataUtilities.getObject(objectName);

        if (stored != null) {
            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("✅ Objeto '%s' encontrado en DataUtilities", objectName), null);
            return stored;
        }

        Object lastDeserialized = DataUtilities.getObject("__lastDeserialized");

        if (lastDeserialized != null) {
            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("🔍 Buscando '%s' en última respuesta deserializada (tipo: %s)",
                    objectName, lastDeserialized.getClass().getSimpleName()), null);

            Object found = findObjectInStructure(lastDeserialized, objectName);

            if (found != null) {
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("✅ Objeto '%s' encontrado en estructura", objectName), null);
                return found;
            }

            if (lastDeserialized instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) lastDeserialized;
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("📋 Keys disponibles en raíz: %s", map.keySet()), null);
            }
        }

        TestLogger.logDebug("API_HELPER_SERIALIZATION",
            String.format("❌ Objeto '%s' no encontrado", objectName), null);

        return null;
    }

    /**
     * Carga una clase de forma flexible.
     * Primero intenta FQCN, luego busca en packages comunes.
     *
     * @param className nombre de la clase a cargar
     * @return la clase cargada
     * @throws ClassNotFoundException si la clase no se encuentra
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            String[] commonPackages = {
                "com.module.models.",
                "com.module.dto.",
                "com.module.responses.",
                "com.test.models.",
                "models.",
                "dto."
            };

            for (String pkg : commonPackages) {
                try {
                    return Class.forName(pkg + className);
                } catch (ClassNotFoundException ignored) {
                    // Continuar buscando
                }
            }

            throw new ClassNotFoundException(
                String.format("Clase '%s' no encontrada. Usa FQCN: com.module.models.%s",
                    className, className));
        }
    }

    /**
     * Extrae el valor de un campo de un objeto usando getter o acceso directo.
     *
     * @param object objeto del cual extraer el campo
     * @param fieldName nombre del campo a extraer
     * @return el valor del campo
     * @throws Exception si el campo no existe
     */
    public Object extractFieldValue(Object object, String fieldName) throws Exception {
        if (object instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) object;
            if (map.containsKey(fieldName)) {
                return map.get(fieldName);
            }
            throw new NoSuchFieldException(
                String.format("Campo '%s' no encontrado en Map. Keys disponibles: %s",
                    fieldName, map.keySet()));
        }

        Class<?> clazz = object.getClass();

        String getterName = "get" + DataUtilities.capitalize(fieldName);
        String booleanGetterName = "is" + DataUtilities.capitalize(fieldName);

        try {
            try {
                java.lang.reflect.Method getter = clazz.getMethod(getterName);
                return getter.invoke(object);
            } catch (NoSuchMethodException e) {
                java.lang.reflect.Method booleanGetter = clazz.getMethod(booleanGetterName);
                return booleanGetter.invoke(object);
            }
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ex) {
                throw new NoSuchFieldException(
                    String.format("Campo '%s' no encontrado en clase %s",
                        fieldName, clazz.getSimpleName()));
            }
        }
    }

    /**
     * Busca un objeto por nombre en la estructura de un objeto complejo.
     * Navega recursivamente por todos los campos.
     *
     * @param root objeto raíz donde buscar
     * @param objectName nombre del objeto a buscar
     * @return el objeto encontrado o null si no se encuentra
     */
    public Object findObjectInStructure(Object root, String objectName) {
        if (root == null) {
            return null;
        }

        TestLogger.logDebug("API_HELPER_SERIALIZATION",
            String.format("🔍 Buscando '%s' en tipo: %s",
                objectName, root.getClass().getSimpleName()), null);

        if (root instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) root;

            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("📋 Map con keys: %s", map.keySet()), null);

            if (map.containsKey(objectName)) {
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("✅ Key '%s' encontrada en Map", objectName), null);
                return map.get(objectName);
            }

            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value != null && !ValidationUtilities.isPrimitiveOrWrapper(value.getClass())) {
                    TestLogger.logDebug("API_HELPER_SERIALIZATION",
                        String.format("🔎 Explorando valor de key '%s' (tipo: %s)",
                            entry.getKey(), value.getClass().getSimpleName()), null);
                    Object found = findObjectInStructure(value, objectName);
                    if (found != null) {
                        return found;
                    }
                }
            }
        } else {
            try {
                Object result = extractFieldValue(root, objectName);
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("✅ Campo '%s' encontrado directamente", objectName), null);
                return result;
            } catch (Exception e) {
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("➡️ Campo '%s' no está en raíz, buscando recursivamente...",
                        objectName), null);

                try {
                    java.lang.reflect.Field[] fields = root.getClass().getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        field.setAccessible(true);
                        Object fieldValue = field.get(root);

                        if (fieldValue != null) {
                            if (field.getName().equals(objectName)) {
                                return fieldValue;
                            }

                            if (!ValidationUtilities.isPrimitiveOrWrapper(fieldValue.getClass())) {
                                Object found = findObjectInStructure(fieldValue, objectName);
                                if (found != null) {
                                    return found;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        return null;
    }

    // =========================================================================
    // DESERIALIZACIÓN Y SERIALIZACIÓN
    // =========================================================================

    /**
     * Deserializa la respuesta HTTP en un objeto Java tipado.
     * Almacena el objeto deserializado para uso posterior.
     *
     * @param className nombre de la clase destino (FQCN o nombre simple)
     * @throws FrameworkBusinessException si falla la deserialización
     */
    public void deserializeResponse(String className) throws FrameworkBusinessException {
        try {
            HttpResponse response = httpClient.getLastResponse();

            if (response == null || response.getBody() == null) {
                throw new FrameworkBusinessException("deserializeResponse",
                    "No hay respuesta disponible para deserializar. Ejecuta primero una petición HTTP.");
            }

            String body = response.getBody();
            Class<?> clazz = loadClass(className);
            Object deserializedObject = DataUtilities.deserializeJson(body, clazz);
            DataUtilities.storeObject("__lastDeserialized", deserializedObject);

            TestLogger.logInfo("API_HELPER_SERIALIZATION",
                String.format("✅ Respuesta deserializada exitosamente a tipo: %s", clazz.getSimpleName()), null);

        } catch (FrameworkBusinessException e) {
            TestLogger.logError("API_HELPER_SERIALIZATION",
                "❌ Error deserializando respuesta: " + e.getMessage(), null);
            throw e;
        } catch (ClassNotFoundException e) {
            String errorMsg = String.format("Clase no encontrada: %s", className);
            TestLogger.logError("API_HELPER_SERIALIZATION", errorMsg, null);
            throw new FrameworkBusinessException("deserializeResponse", errorMsg);
        } catch (Exception e) {
            String errorMsg = String.format("Error inesperado deserializando respuesta: %s", e.getMessage());
            TestLogger.logError("API_HELPER_SERIALIZATION", errorMsg, null);
            throw new FrameworkBusinessException("deserializeResponse", errorMsg);
        }
    }

    /**
     * Guarda el último objeto deserializado con un nombre específico.
     *
     * @param variableName nombre con el que guardar el objeto
     * @throws FrameworkBusinessException si no hay objeto deserializado
     */
    public void saveDeserializedObject(String variableName) throws FrameworkBusinessException {
        try {
            Object lastDeserialized = DataUtilities.getObject("__lastDeserialized");

            if (lastDeserialized == null) {
                throw new FrameworkBusinessException("saveDeserializedObject",
                    "No hay objeto deserializado disponible. " +
                    "Usa el step 'serializo la respuesta en la clase...' primero.");
            }

            DataUtilities.storeObject(variableName, lastDeserialized);
            DataUtilities.storeObject("__lastDeserialized", null);  // Limpiar temporal

            TestLogger.logInfo("API_HELPER_SERIALIZATION",
                String.format("✅ Objeto guardado como '%s' (tipo: %s)",
                    variableName, lastDeserialized.getClass().getSimpleName()), null);

        } catch (FrameworkBusinessException e) {
            TestLogger.logError("API_HELPER_SERIALIZATION",
                "❌ Error guardando objeto: " + e.getMessage(), null);
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error inesperado guardando objeto: %s", e.getMessage());
            TestLogger.logError("API_HELPER_SERIALIZATION", errorMsg, null);
            throw new FrameworkBusinessException("saveDeserializedObject", errorMsg);
        }
    }

    /**
     * Extrae un valor usando JSONPath desde la respuesta y lo guarda como variable.
     * Wrapper limpio para steps de extracción de valores JSON.
     *
     * @param jsonPath path JSON para extraer el valor
     * @param variableName nombre de la variable donde guardar
     * @throws FrameworkBusinessException si falla la extracción
     */
    public void extractAndStoreJsonValue(String jsonPath, String variableName)
        throws FrameworkBusinessException {
        try {
            HttpResponse lastResponse = httpClient.getLastResponse();

            if (lastResponse == null || lastResponse.getBody() == null) {
                throw new FrameworkBusinessException("extractAndStoreJsonValue",
                    "No hay respuesta disponible para extraer datos");
            }

            String responseBody = lastResponse.getBody();
            Object value = DataUtilities.getJsonParameter(responseBody, jsonPath);
            String processedVarName = DataUtilities.replaceVariables(variableName);
            DataUtilities.storeValue(processedVarName, value);

            TestLogger.logInfo("API_HELPER_DATA",
                String.format("✅ Valor extraído y almacenado: %s -> %s = %s",
                    jsonPath, processedVarName, value), null);

        } catch (FrameworkBusinessException e) {
            TestLogger.logError("API_HELPER_DATA",
                "❌ Error extrayendo valor JSON: " + e.getMessage(), null);
            throw new FrameworkBusinessException("extractAndStoreJsonValue",
                "Error extrayendo valor: " + e.getMessage());
        } catch (Exception e) {
            throw new FrameworkBusinessException("extractAndStoreJsonValue",
                "Error inesperado: " + e.getMessage());
        }
    }

    // =========================================================================
    // EJECUCIÓN DE PETICIONES HTTP
    // =========================================================================

    /**
     * Ejecuta una petición HTTP con el método especificado.
     * Maneja automáticamente el switch de métodos y logging.
     *
     * @param method método HTTP (GET, POST, PUT, DELETE, PATCH)
     * @param endpoint endpoint a llamar (soporta reemplazo de variables)
     * @throws FrameworkTechnicalException si falla la ejecución
     */
    public void executeRequest(String method, String endpoint) throws FrameworkTechnicalException {
        String processedEndpoint = DataUtilities.replaceVariables(endpoint);

        try {
            switch (method.toUpperCase()) {
                case "GET":
                    httpClient.get(processedEndpoint);
                    break;
                case "POST":
                    httpClient.post(processedEndpoint);
                    break;
                case "PUT":
                    httpClient.put(processedEndpoint);
                    break;
                case "DELETE":
                    httpClient.delete(processedEndpoint);
                    break;
                case "PATCH":
                    httpClient.patch(processedEndpoint);
                    break;
                default:
                    throw new FrameworkTechnicalException("executeRequest",
                        "Método HTTP no soportado: " + method);
            }

            TestLogger.logInfo("API_HELPER_EXECUTION",
                String.format("✅ Petición %s ejecutada al endpoint: %s", method, processedEndpoint), null);

        } catch (FrameworkTechnicalException e) {
            throw e;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("executeRequest",
                String.format("Error ejecutando petición %s: %s", method, e.getMessage()));
        }
    }

    /**
     * Establece el cuerpo de la petición como JSON desde un Map.
     * Procesa variables y convierte automáticamente a JSON.
     *
     * @param data Map con los datos a enviar
     * @throws RuntimeException si falla la serialización
     */
    public void setJsonBody(Map<String, String> data) {
        try {
            // Procesar variables en los valores
            Map<String, String> processedData = new java.util.HashMap<>();
            data.forEach((k, v) -> processedData.put(k, DataUtilities.replaceVariables(v)));

            // Convertir a JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonBody = mapper.writeValueAsString(processedData);

            httpClient.addHeader("Content-Type", "application/json");
            httpClient.setBody(jsonBody);

            TestLogger.logInfo("API_HELPER_REQUEST", "✅ Request JSON creado y agregado", null);

        } catch (Exception e) {
            throw new RuntimeException("Error creando cuerpo JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Extrae un valor usando JSONPath y lo almacena con el mismo nombre que el path.
     * Versión simplificada cuando el nombre de variable coincide con el jsonPath.
     *
     * @param jsonPath path JSON para extraer el valor (también será el nombre de la variable)
     * @throws FrameworkBusinessException si falla la extracción
     */
    public void extractAndStoreJsonValueSimple(String jsonPath) throws FrameworkBusinessException {
        try {
            HttpResponse lastResponse = httpClient.getLastResponse();

            if (lastResponse == null || lastResponse.getBody() == null) {
                throw new FrameworkBusinessException("extractAndStoreJsonValueSimple",
                    "No hay respuesta disponible para extraer datos");
            }

            String responseBody = lastResponse.getBody();
            Object value = DataUtilities.getJsonParameter(responseBody, jsonPath);
            DataUtilities.storeValue(jsonPath, value);

            TestLogger.logInfo("API_HELPER_DATA",
                String.format("✅ Valor almacenado desde JSON: %s = %s", jsonPath, value), null);

        } catch (FrameworkBusinessException e) {
            throw new RuntimeException("Error extrayendo valor JSON: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // UTILIDADES DE DEBUGGING
    // =========================================================================

    /**
     * Muestra información detallada de la última petición HTTP ejecutada.
     * Útil para debugging de tests.
     */
    public void showLastRequestInfo() {
        try {
            TestLogger.logInfo("API_HELPER_DEBUG", "=== INFORMACIÓN DE LA ÚLTIMA PETICIÓN ===", null);

            HttpResponse lastResponse = httpClient.getLastResponse();
            if (lastResponse != null) {
                TestLogger.logInfo("API_HELPER_DEBUG",
                    "Status Code: " + lastResponse.getStatusCode(), null);
                TestLogger.logInfo("API_HELPER_DEBUG",
                    "Headers: " + lastResponse.getHeaders(), null);
                TestLogger.logInfo("API_HELPER_DEBUG",
                    "Response Body Length: " +
                    (lastResponse.getBody() != null ? lastResponse.getBody().length() : 0), null);
            } else {
                TestLogger.logInfo("API_HELPER_DEBUG", "No hay respuesta disponible", null);
            }

            TestLogger.logInfo("API_HELPER_DEBUG", "===========================================", null);
        } catch (Exception e) {
            TestLogger.logWarning("API_HELPER_DEBUG",
                "Error mostrando información de petición: " + e.getMessage(), null);
        }
    }

    /**
     * Extrae un campo específico de un objeto deserializado y lo guarda como variable.
     * Implementa búsqueda recursiva con fallback a búsqueda por contenedor.
     *
     * @param fieldName nombre del campo a extraer
     * @param objectPath nombre del objeto contenedor
     * @param variableName nombre de la variable donde guardar
     * @throws FrameworkBusinessException si el campo no se encuentra
     */
    public void extractFieldFromObject(String fieldName, String objectPath, String variableName)
        throws FrameworkBusinessException {
        try {
            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("🔍 Iniciando extracción de campo '%s' desde objeto '%s'",
                    fieldName, objectPath), null);

            // Obtener la última respuesta deserializada
            Object lastResponse = DataUtilities.getObject("__lastDeserialized");

            if (lastResponse == null) {
                throw new FrameworkBusinessException("extractFieldFromObject",
                    "No hay respuesta deserializada disponible. " +
                    "Asegúrate de ejecutar primero una petición HTTP.");
            }

            Object fieldValue = null;

            // ESTRATEGIA 1: Buscar el campo directamente en toda la respuesta
            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("🔍 Buscando campo '%s' en toda la respuesta", fieldName), null);

            fieldValue = DataUtilities.findValue(lastResponse, fieldName);

            // ESTRATEGIA 2: Si no se encuentra y objectPath es diferente, buscar contenedor
            if (fieldValue == null && !objectPath.equals(fieldName)) {
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("🔍 Campo no encontrado, buscando objeto contenedor '%s'",
                        objectPath), null);

                Object targetObject = DataUtilities.findValue(lastResponse, objectPath);

                if (targetObject != null) {
                    TestLogger.logDebug("API_HELPER_SERIALIZATION",
                        String.format("✅ Objeto contenedor '%s' encontrado, buscando campo '%s' dentro",
                            objectPath, fieldName), null);

                    fieldValue = DataUtilities.findValue(targetObject, fieldName);
                }
            }

            if (fieldValue == null) {
                throw new FrameworkBusinessException("extractFieldFromObject",
                    String.format("No se encontró el campo '%s' en el response. " +
                        "Verifica que el campo exista.", fieldName));
            }

            // Guardar el valor en ambos contextos
            String valueToStore = fieldValue.toString();
            DataUtilities.saveToContext("api", variableName, valueToStore);
            DataUtilities.storeValue(variableName, valueToStore);

            TestLogger.logInfo("API_HELPER_SERIALIZATION",
                String.format("✅ Campo extraído: campo='%s', valor=%s, guardado como='api.%s'",
                    fieldName,
                    fieldValue.toString().length() > 50
                        ? fieldValue.toString().substring(0, 50) + "..."
                        : fieldValue.toString(),
                    variableName), null);

        } catch (FrameworkBusinessException e) {
            TestLogger.logError("API_HELPER_SERIALIZATION",
                "❌ Error extrayendo campo: " + e.getMessage(), null);
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error inesperado al obtener campo '%s' del objeto '%s': %s",
                fieldName, objectPath, e.getMessage());
            TestLogger.logError("API_HELPER_SERIALIZATION", "❌ " + errorMsg, null);
            throw new FrameworkBusinessException("extractFieldFromObject", errorMsg);
        }
    }
}

