package com.qa.httpcore.utils;

import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.common.internal.config.ConfigManager;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.httpcore.model.HttpResponse;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.utils.json.JsonUtilities;
import com.qa.common.utils.text.TextUtilities;
import java.nio.charset.StandardCharsets;
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

    /** Max characters to display for field values in log messages. */
    private static final int LOG_VALUE_MAX_LENGTH = 50;

    /**
     * Base URL property keys in priority order — kept in sync with
     * {@code ApiPlugin.BASE_URL_KEYS} for safety-net resolution when the plugin
     * lifecycle didn't run (e.g. scenario with no {@code @api} tag).
     */
    private static final java.util.List<String> BASE_URL_KEYS = java.util.List.of(
        "base.url", "api.base.url", "api.baseurl", "api.baseurl.qa");

    /** Milliseconds per second, used for converting seconds to milliseconds in polling waits. */
    private static final long MILLIS_PER_SECOND = 1000L;

    private final HttpClient httpClient;

    /**
     * Constructor que recibe el cliente HTTP.
     * @param httpClient Cliente HTTP para ejecutar peticiones
     */
    public ApiHelper(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Constructor por defecto — crea un BaseHttpClient internamente.
     * Útil en step classes que no reciben inyección de dependencias.
     * @since 2.0.0
     */
    public ApiHelper() {
        this(new ApacheHttpClientImpl());
    }

    /**
     * Método de fábrica para obtener la instancia registrada en el {@link ExecutionContext} activo.
     *
     * <p>Comportamiento por modo de ejecución:
     * <ul>
     *   <li><b>Engine mode</b>: {@link com.qa.httpcore.plugin.ApiPlugin} registró {@code ApiHelper}
     *       antes de correr los escenarios → se reutiliza la instancia del registry.</li>
     *   <li><b>Standalone mode</b> (JUnit directo): {@link com.qa.httpcore.steps.ApiScenarioHooks}
     *       ejecutó {@code @Before(order=10)} y registró {@code ApiHelper} compartido →
     *       se reutiliza la misma instancia para todos los steps del escenario.</li>
     *   <li><b>Fallback sin registry</b>: si por algún motivo {@code ApiHelper} no está en el
     *       registry (p.ej. contexto recién creado sin hooks), se registra la instancia creada
     *       para que los pasos subsiguientes compartan estado.</li>
     *   <li><b>Sin contexto activo</b>: retorna una instancia aislada (backward compatibility).</li>
     * </ul>
     *
     * @return instancia de {@code ApiHelper} compartida o aislada (nunca null)
     * @since 2.0.0
     */
    public static ApiHelper forCurrentContext() {
        return ExecutionContext.current().map(ctx -> ctx.registry().get(ApiHelper.class).orElseGet(() -> {
                            // Fallback de seguridad: si el hook no corrió aún, bootstrapeamos
                            // aquí y registramos AMBOS servicios para que los steps posteriores
                            // (HeaderSteps, HttpExecutionSteps, etc.) encuentren el mismo HttpClient.
                            HttpClient httpClient = ctx.registry().get(HttpClient.class).orElseGet(() -> {
                                        HttpClient newClient = new ApacheHttpClientImpl();
                                        ctx.registry().registerInstance(HttpClient.class, newClient);
                                        return newClient;
                                    });
                            ApiHelper helper = new ApiHelper(httpClient);
                            ctx.registry().registerInstance(ApiHelper.class, helper);
                            TestLogger.logDebug("API_HELPER",
                                "HttpClient + ApiHelper auto-registrados (fallback standalone)", null);
                            return helper;
                        })).orElseGet(ApiHelper::new);
    }

    // =========================================================================
    // RESOLUCIÓN DE VARIABLES (interno)
    // =========================================================================

    /**
     * Resuelve variables {@code ${var}} en un texto.
     * Usa {@code ExecutionContext.current().variables().resolve()} si hay contexto activo,
     * o retorna el texto tal como está (backward compatibility).
     *
     * @param text texto con posibles referencias a variables
     * @return texto con variables resueltas
     */
    private String resolve(String text) {
        return ExecutionContext.current().map(ctx -> ctx.variables().resolve(text)).orElse(text);
    }

    // =========================================================================
    // HELPERS DE ALMACENAMIENTO EN CONTEXTO (Task 104 — Fase 3)
    // =========================================================================

    /**
     * Obtiene un objeto del {@link com.qa.common.api.runtime.VariableStore} del contexto activo.
     */
    private Object getContextVariable(String key) {
        return ExecutionContext.current().flatMap(ctx -> ctx.variables().get(key, Object.class)).orElse(null);
    }

    /**
     * Almacena un objeto en el {@link com.qa.common.api.runtime.VariableStore} del contexto activo.
     * Si {@code value} es {@code null}, elimina la variable del store.
     */
    private void setContextVariable(String key, Object value) {
        ExecutionContext.current().ifPresent(ctx -> {
            if (value == null) {
                ctx.variables().remove(key);
            } else {
                ctx.variables().set(key, value);
            }
        });
    }

    /**
     * Almacena un string en el {@link com.qa.common.api.runtime.VariableStore} del contexto activo.
     */
    private void setContextString(String key, Object value) {
        if (value == null) {
            return;
        }
        ExecutionContext.current().ifPresent(ctx -> ctx.variables().set(key, value.toString()));
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
            String endpointValue;
            // If the argument is already a URL, use it directly without property lookup
            if (propertyKey.startsWith("http://") || propertyKey.startsWith("https://")) {
                endpointValue = propertyKey;
            } else {
                // Preferir ExecutionContext.config() — es por ejecución e inmutable (safe en paralelo)
                // Fallback a ConfigManager para compatibilidad con ejecución sin Engine
                endpointValue = ExecutionContext.current().flatMap(ctx -> ctx.config().getProperty(propertyKey)).
                        filter(v -> !v.trim().isEmpty()).orElseGet(() -> ConfigManager.getInstance().get(propertyKey));

                if (endpointValue == null || endpointValue.trim().isEmpty()) {
                    endpointValue = resolveHostPathFallback(propertyKey);
                }
            }

            String processedUrl = resolve(endpointValue);
            httpClient.setHost(processedUrl);

            TestLogger.logInfo("API_HELPER_CONFIG",
                String.format("✅ Endpoint configurado: %s = %s", propertyKey, processedUrl), null);

        } catch (Exception e) {
            throw new RuntimeException(String.format(
                "Error configurando endpoint desde propiedad '%s': %s", propertyKey, e.getMessage()), e);
        }
    }

    private String resolveHostPathFallback(String propertyKey) {
        if (propertyKey.contains("/") && !propertyKey.contains("://")) {
            String existingHost = httpClient.getHost();
            if (existingHost == null || existingHost.isBlank()) {
                existingHost = ExecutionContext.current()
                    .flatMap(ctx -> BASE_URL_KEYS.stream()
                        .map(k -> ctx.config().getProperty(k))
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .filter(v -> !v.isBlank())
                        .findFirst())
                    .orElse(null);
                if (existingHost != null && !existingHost.isBlank()) {
                    httpClient.setHost(existingHost);
                    TestLogger.logInfo("API_HELPER_CONFIG",
                        "[BASE-URL] Host tomado de ExecutionConfig (bootstrap no ejecutado): "
                            + existingHost, null);
                }
            }
            if (existingHost != null && !existingHost.isBlank()) {
                String base = existingHost.endsWith("/")
                    ? existingHost.substring(0, existingHost.length() - 1)
                    : existingHost;
                String path = propertyKey.startsWith("/") ? propertyKey : "/" + propertyKey;
                String resolved = base + path;
                TestLogger.logInfo("API_HELPER_CONFIG",
                    String.format("ℹ️ Propiedad '%s' no encontrada — usando como path sobre host base: %s",
                        propertyKey, resolved), null);
                return resolved;
            }
            throw new RuntimeException(String.format(
                "Propiedad '%s' no encontrada y no hay host base configurado. " +
                "Verifica que el ambiente tenga una 'Base URL' configurada, o usa " +
                "el step 'uso la URL base del ambiente seleccionado' antes de este step.",
                propertyKey));
        }
        throw new RuntimeException(String.format(
            "Propiedad '%s' no encontrada o está vacía en config-{env}.properties. " +
            "Verifica que la propiedad exista en config-qa.properties", propertyKey));
    }

    /**
     * Establece el host base procesando variables.
     *
     * @param host host a establecer (soporta variables ${...})
     */
    public void setBaseHost(String host) {
        String processedHost = resolve(host);
        httpClient.setHost(processedHost);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Host base establecido: %s", processedHost), null);
    }

    /**
     * Agrega un header HTTP procesando variables.
     */
    public void addHeader(String header, String value) {
        String processedValue = resolve(value);
        httpClient.addHeader(header, processedValue);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Header agregado: %s", header), null);
    }

    /**
     * Agrega un query parameter procesando variables.
     */
    public void addQueryParam(String param, String value) {
        String processedValue = resolve(value);
        httpClient.addQueryParam(param, processedValue);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Query param agregado: %s", param), null);
    }

    /**
     * Establece el cuerpo de la petición procesando variables.
     */
    public void setRequestBody(String body) {
        String processedBody = resolve(body);
        httpClient.setBody(processedBody);
        TestLogger.logInfo("API_HELPER_REQUEST", "✅ Request body agregado", null);
    }

    /**
     * Agrega un field procesando variables.
     */
    public void addField(String key, String value) {
        String processedKey = resolve(key);
        String processedValue = resolve(value);
        httpClient.addField(processedKey, processedValue);
        TestLogger.logInfo("API_HELPER_CONFIG",
            String.format("✅ Field agregado: %s", processedKey), null);
    }

    /**
     * Establece el cuerpo JSON desde string procesando variables.
     */
    public void setJsonBodyFromString(String jsonBody) {
        String processedBody = resolve(jsonBody);
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
            // Preferir ExecutionContext.config() — es por ejecución e inmutable (safe en paralelo)
            // Fallback a ConfigManager para compatibilidad con ejecución sin Engine
            String baseUrl = ExecutionContext.current().flatMap(ctx -> ctx.config().getProperty(baseUrlKey)).
                    filter(v -> !v.trim().isEmpty()).orElseGet(() -> ConfigManager.getInstance().get(baseUrlKey));

            String endpointPath = ExecutionContext.current().flatMap(ctx -> ctx.config().getProperty(pathKey)).
                    filter(v -> !v.trim().isEmpty()).orElseGet(() -> ConfigManager.getInstance().get(pathKey));

            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new RuntimeException(
                    String.format("Base URL '%s' no encontrada en config-{env}.properties", baseUrlKey));
            }

            if (endpointPath == null || endpointPath.trim().isEmpty()) {
                throw new RuntimeException(
                    String.format("Endpoint path '%s' no encontrado en config-{env}.properties", pathKey));
            }

            String fullUrl = baseUrl.endsWith("/") ? baseUrl + endpointPath : baseUrl + "/" + endpointPath;
            String processedUrl = resolve(fullUrl);

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
            String processedUsername = resolve(username);
            String processedPassword = resolve(password);

            String credentials = Base64.getEncoder().
                encodeToString((processedUsername + ":" + processedPassword).getBytes(StandardCharsets.UTF_8));

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
        String processedToken = resolve(token);
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
     * Busca primero en el {@link com.qa.common.api.runtime.VariableStore} del contexto activo,
     * luego en la última respuesta deserializada.
     *
     * @param objectName nombre del objeto a resolver
     * @return el objeto encontrado o null si no existe
     */
    public Object resolveSimpleObject(String objectName) {
        Object stored = getContextVariable(objectName);

        if (stored != null) {
            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("✅ Objeto '%s' encontrado en store de contexto", objectName), null);
            return stored;
        }

        Object lastDeserialized = getContextVariable("__lastDeserialized");

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

        String getterName = "get" + TextUtilities.capitalize(fieldName);
        String booleanGetterName = "is" + TextUtilities.capitalize(fieldName);

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
            Object deserializedObject = JsonUtilities.deserializeJson(body, clazz);
            setContextVariable("__lastDeserialized", deserializedObject);

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
            Object lastDeserialized = getContextVariable("__lastDeserialized");

            if (lastDeserialized == null) {
                throw new FrameworkBusinessException("saveDeserializedObject",
                    "No hay objeto deserializado disponible. " +
                    "Usa el step 'serializo la respuesta en la clase...' primero.");
            }

            setContextVariable(variableName, lastDeserialized);
            setContextVariable("__lastDeserialized", null);  // Limpiar temporal

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
            Object value = JsonUtilities.getJsonParameter(responseBody, jsonPath);
            String processedVarName = resolve(variableName);
            setContextString(processedVarName, value);

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
        String processedEndpoint = resolve(endpoint);

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
            data.forEach((k, v) -> processedData.put(k, resolve(v)));

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
            Object value = JsonUtilities.getJsonParameter(responseBody, jsonPath);
            setContextString(jsonPath, value);

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

            // Look up the named object first, then fall back to __lastDeserialized
            Object lastResponse = getContextVariable(objectPath);
            if (lastResponse == null) {
                lastResponse = getContextVariable("__lastDeserialized");
            }

            if (lastResponse == null) {
                throw new FrameworkBusinessException("extractFieldFromObject",
                    "No hay respuesta deserializada disponible. " +
                    "Asegúrate de ejecutar primero una petición HTTP.");
            }

            Object fieldValue = null;

            // ESTRATEGIA 1: Buscar el campo directamente en toda la respuesta
            TestLogger.logDebug("API_HELPER_SERIALIZATION",
                String.format("🔍 Buscando campo '%s' en toda la respuesta", fieldName), null);

            fieldValue = JsonUtilities.findValue(lastResponse, fieldName);

            // ESTRATEGIA 2: Si no se encuentra y objectPath es diferente, buscar contenedor
            if (fieldValue == null && !objectPath.equals(fieldName)) {
                TestLogger.logDebug("API_HELPER_SERIALIZATION",
                    String.format("🔍 Campo no encontrado, buscando objeto contenedor '%s'",
                        objectPath), null);

                Object targetObject = JsonUtilities.findValue(lastResponse, objectPath);

                if (targetObject != null) {
                    TestLogger.logDebug("API_HELPER_SERIALIZATION",
                        String.format("✅ Objeto contenedor '%s' encontrado, buscando campo '%s' dentro",
                            objectPath, fieldName), null);

                    fieldValue = JsonUtilities.findValue(targetObject, fieldName);
                }
            }

            if (fieldValue == null) {
                throw new FrameworkBusinessException("extractFieldFromObject",
                    String.format("No se encontró el campo '%s' en el response. " +
                        "Verifica que el campo exista.", fieldName));
            }

            // Guardar el valor en el VariableStore del contexto (con prefijo de capa y sin prefijo)
            String valueToStore = fieldValue.toString();
            ExecutionContext.current().ifPresent(ctx -> ctx.variables().set("api." + variableName, valueToStore));
            setContextString(variableName, valueToStore);

            TestLogger.logInfo("API_HELPER_SERIALIZATION",
                String.format("✅ Campo extraído: campo='%s', valor=%s, guardado como='api.%s'",
                    fieldName,
                    fieldValue.toString().length() > LOG_VALUE_MAX_LENGTH
                        ? fieldValue.toString().substring(0, LOG_VALUE_MAX_LENGTH) + "..."
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

    // =========================================================================
    // MÉTODOS DE SOPORTE FASE 2 — Nuevas APIs para step classes especializadas
    // =========================================================================

    /**
     * Retorna la última respuesta HTTP del cliente.
     * Delegado de {@code httpClient.getLastResponse()} para uso desde step classes.
     * @since 2.0.0
     */
    public HttpResponse getLastResponse() {
        return httpClient.getLastResponse();
    }

    /**
     * Retorna el tiempo de la última petición en milisegundos.
     * @since 2.0.0
     */
    public long getLastResponseTimeMs() {
        return httpClient.getLastRequestDuration();
    }

    /**
     * Retorna la URL de la última petición.
     * @since 2.0.0
     */
    public String getLastRequestUrl() {
        return httpClient.getLastRequestUrl();
    }

    /**
     * Configura el protocolo base (http/https).
     * @param protocol "http" o "https"
     * @since 2.0.0
     */
    public void setProtocol(String protocol) {
        String current = httpClient.getHost();
        if (current != null && !current.isEmpty()) {
            if (current.startsWith("http://") || current.startsWith("https://")) {
                current = current.replaceFirst("https?://", protocol + "://");
            } else {
                current = protocol + "://" + current;
            }
            httpClient.setHost(current);
        }
        TestLogger.logInfo("API_HELPER", "Protocolo configurado: " + protocol, null);
    }

    /**
     * Establece cuerpo XML de la petición.
     * @param xml contenido XML
     * @since 2.0.0
     */
    public void setXmlBody(String xml) {
        httpClient.setBody(resolve(xml));
        httpClient.setContentType("application/xml");
    }

    /**
     * Establece cuerpo como form-data con los campos del mapa.
     * @param fields mapa de campos
     * @since 2.0.0
     */
    public void setFormDataBody(Map<String, String> fields) {
        httpClient.configureForFormData();
        fields.forEach((k, v) -> httpClient.addField(k, resolve(v)));
    }

    /**
     * Lee el body desde un archivo de recursos.
     * @param filePath ruta relativa al archivo
     * @since 2.0.0
     */
    public void setBodyFromFile(String filePath) {
        try {
            java.net.URL url = getClass().getClassLoader().getResource(filePath);
            if (url == null) {
                throw new IllegalArgumentException("Archivo de body no encontrado: " + filePath);
            }
            byte[] contentBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(url.toURI()));
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            httpClient.setBody(content);
            TestLogger.logInfo("API_HELPER", "Body cargado desde archivo: " + filePath, null);
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo body desde archivo: " + filePath, e);
        }
    }

    /**
     * Lee un template del classpath y reemplaza los placeholders con los datos del mapa.
     * @param template ruta al template
     * @param data mapa de variables a reemplazar
     * @since 2.0.0
     */
    public void setBodyFromTemplate(String template, Map<String, String> data) {
        // If template contains {{ or JSON chars, treat it as an inline template rather than a file path
        String body;
        if (template.contains("{{") || template.startsWith("{") || template.startsWith("[")) {
            body = template;
        } else {
            setBodyFromFile(template);
            body = httpClient.getBody() != null ? httpClient.getBody() : "";
        }
        // Replace both {{key}} and ${key} placeholder syntaxes
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String resolvedValue = resolve(entry.getValue());
            body = body.replace("{{" + entry.getKey() + "}}", resolvedValue);
            body = body.replace("${" + entry.getKey() + "}", resolvedValue);
        }
        httpClient.setBody(body);
    }

    /**
     * Valida que el campo JSON (jsonPath) tenga el valor esperado.
     * @since 2.0.0
     */
    public void validateJsonPathValue(String jsonPath, String expectedValue)
            throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonPath(response, jsonPath, resolve(expectedValue));
    }

    /**
     * Valida que el campo JSON (jsonPath) NO sea null.
     * @since 2.0.0
     */
    public void validateJsonPathNotNull(String jsonPath) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonPathExists(response, jsonPath);
    }

    /**
     * Valida el tipo de dato de un campo JSON.
     * @since 2.0.0
     */
    public void validateJsonPathType(String jsonPath, String type) throws FrameworkBusinessException {
        validateJsonFieldType(jsonPath, type);
    }

    /**
     * Valida el tamaño de un array JSON.
     * @param jsonPath path al array
     * @param expectedSize tamaño esperado
     * @since 2.0.0
     */
    public void validateJsonArraySize(String jsonPath, int expectedSize) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonArraySize(response, jsonPath, expectedSize);
    }

    /**
     * Valida que el campo JSON cumpla un patrón regex.
     * @since 2.0.0
     */
    public void validateJsonPathMatchesPattern(String jsonPath, String regex) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonPathMatchesPattern(response, jsonPath, regex);
    }

    // =========================================================================
    // NUEVAS VALIDACIONES — Fase 2 ampliada
    // =========================================================================

    /**
     * Valida que el array JSON en la ruta dada NO esté vacío.
     *
     * @param jsonPath ruta JSON al array (ej: "$.items")
     * @throws FrameworkBusinessException si el array está vacío o no existe
     * @since 2.0.0
     */
    public void validateJsonArrayNotEmpty(String jsonPath) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonArrayNotEmpty(response, jsonPath);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Lista '%s' validada como NO vacía", jsonPath), null);
    }

    /**
     * Valida que el campo JSON en la ruta dada sea un UUID v4 válido.
     *
     * @param jsonPath ruta JSON al campo (ej: "$.id")
     * @throws FrameworkBusinessException si el valor no es un UUID válido
     * @since 2.0.0
     */
    public void validateJsonPathIsUUID(String jsonPath) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonPathIsUUID(response, jsonPath);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Campo '%s' validado como UUID válido", jsonPath), null);
    }

    /**
     * Valida que el campo numérico JSON sea estrictamente mayor que {@code threshold}.
     *
     * @param jsonPath  ruta JSON al campo numérico
     * @param threshold umbral (el campo debe ser mayor)
     * @throws FrameworkBusinessException si el valor no es numérico o no supera el umbral
     * @since 2.0.0
     */
    public void validateJsonFieldGreaterThan(String jsonPath, int threshold) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonFieldGreaterThan(response, jsonPath, threshold);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Campo '%s' validado como > %d", jsonPath, threshold), null);
    }

    /**
     * Valida que el campo numérico JSON sea estrictamente menor que {@code threshold}.
     *
     * @param jsonPath  ruta JSON al campo numérico
     * @param threshold umbral (el campo debe ser menor)
     * @throws FrameworkBusinessException si el valor no es numérico o supera/iguala el umbral
     * @since 2.0.0
     */
    public void validateJsonFieldLessThan(String jsonPath, int threshold) throws FrameworkBusinessException {
        HttpResponse response = httpClient.getLastResponse();
        ValidationUtilities.validateJsonFieldLessThan(response, jsonPath, threshold);
        TestLogger.logInfo("API_HELPER_VALIDATION",
            String.format("Campo '%s' validado como < %d", jsonPath, threshold), null);
    }

    /**
     * Agrega un campo al body JSON de la petición resolviendo variables en clave y valor.
     * Alias semánticamente explícito de {@link #addField(String, String)}.
     *
     * @param key   nombre del campo (soporta variables {@code ${...}})
     * @param value valor del campo  (soporta variables {@code ${...}})
     * @since 2.0.0
     */
    public void addBodyField(String key, String value) {
        addField(key, value);
    }

    /**
     * Ejecuta con polling una petición HTTP hasta que el código de respuesta coincida.
     * Útil para endpoints asíncronos que devuelven eventualmente el estado esperado.
     *
     * @param method          método HTTP ("GET", "POST", etc.)
     * @param endpoint        endpoint a llamar
     * @param expectedCode    código HTTP esperado
     * @param maxAttempts     número máximo de intentos
     * @param waitSeconds     segundos de espera entre intentos
     * @throws FrameworkTechnicalException si se agotan los intentos sin éxito
     * @since 2.0.0
     */
    public void pollUntilStatusCode(String method, String endpoint,
                                    int expectedCode, int maxAttempts, int waitSeconds)
            throws FrameworkTechnicalException {
        String processedEndpoint = resolve(endpoint);
        Map<String, String> savedHeaders = new java.util.LinkedHashMap<>(httpClient.getHeaders());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            httpClient.setHost(processedEndpoint);
            if (!savedHeaders.isEmpty()) {
                httpClient.addHeaders(savedHeaders);
            }
            httpClient.get("");
            HttpResponse response = httpClient.getLastResponse();
            if (response != null && response.getStatusCode() == expectedCode) {
                TestLogger.logInfo("API_HELPER_POLL",
                    String.format("✅ Polling exitoso en intento %d/%d — código %d",
                        attempt, maxAttempts, expectedCode), null);
                return;
            }
            TestLogger.logInfo("API_HELPER_POLL",
                String.format("⏳ Intento %d/%d — código actual: %d, esperado: %d. Esperando %ds...",
                    attempt, maxAttempts,
                    response != null ? response.getStatusCode() : -1,
                    expectedCode, waitSeconds), null);
            if (attempt < maxAttempts) {
                try { Thread.sleep(waitSeconds * MILLIS_PER_SECOND); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new FrameworkTechnicalException("pollUntilStatusCode",
            String.format("Se agotaron %d intentos esperando código %d en %s %s",
                maxAttempts, expectedCode, method, processedEndpoint));
    }

    /**
     * Ejecuta con polling una petición HTTP hasta que un campo JSON tenga el valor esperado.
     * Útil para endpoints asíncronos cuyos datos cambian eventualmente.
     *
     * @param method         método HTTP ("GET", "POST", etc.)
     * @param endpoint       endpoint a llamar
     * @param jsonPath       ruta JSON al campo a vigilar
     * @param expectedValue  valor esperado en el campo
     * @param maxAttempts    número máximo de intentos
     * @param waitSeconds    segundos de espera entre intentos
     * @throws FrameworkTechnicalException si se agotan los intentos sin éxito
     * @since 2.0.0
     */
    public void pollUntilJsonFieldEquals(String method, String endpoint,
                                         String jsonPath, String expectedValue,
                                         int maxAttempts, int waitSeconds)
            throws FrameworkTechnicalException {
        String processedEndpoint = resolve(endpoint);
        String processedExpected  = resolve(expectedValue);
        Map<String, String> savedHeaders = new java.util.LinkedHashMap<>(httpClient.getHeaders());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            httpClient.setHost(processedEndpoint);
            if (!savedHeaders.isEmpty()) {
                httpClient.addHeaders(savedHeaders);
            }
            httpClient.get("");
            try {
                HttpResponse response = httpClient.getLastResponse();
                if (response != null && response.getBody() != null) {
                    Object actual = com.qa.common.utils.json.JsonUtilities.getJsonParameter(response.getBody(), jsonPath);
                    if (actual != null && processedExpected.equals(actual.toString())) {
                        TestLogger.logInfo("API_HELPER_POLL",
                            String.format("✅ Polling exitoso en intento %d/%d — '%s' = '%s'",
                                attempt, maxAttempts, jsonPath, processedExpected), null);
                        return;
                    }
                    TestLogger.logInfo("API_HELPER_POLL",
                        String.format("⏳ Intento %d/%d — '%s' actual: '%s', esperado: '%s'. Esperando %ds...",
                            attempt, maxAttempts, jsonPath, actual, processedExpected, waitSeconds), null);
                }
            } catch (Exception e) {
                TestLogger.logInfo("API_HELPER_POLL",
                    String.format("⚠️ Intento %d/%d — error leyendo campo '%s': %s",
                        attempt, maxAttempts, jsonPath, e.getMessage()), null);
            }
            if (attempt < maxAttempts) {
                try { Thread.sleep(waitSeconds * MILLIS_PER_SECOND); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new FrameworkTechnicalException("pollUntilJsonFieldEquals",
            String.format("Se agotaron %d intentos esperando '%s' = '%s' en %s %s",
                maxAttempts, jsonPath, processedExpected, method, processedEndpoint));
    }

    // =========================================================================
    // NUEVOS MÉTODOS — v2.2.1 (requeridos por steps genéricos)
    // =========================================================================

    /**
     * Valida que la respuesta cumpla un JSON Schema cargado desde un archivo.
     */
    public void validateResponseSchemaFromFile(String filePath) throws FrameworkBusinessException {
        try {
            byte[] schemaBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(resolve(filePath)));
            String schema = new String(schemaBytes, StandardCharsets.UTF_8);
            validateResponseSchema(schema);
        } catch (java.io.IOException e) {
            throw new FrameworkBusinessException("validateResponseSchemaFromFile",
                "No se pudo leer el archivo de esquema: " + filePath + " — " + e.getMessage());
        }
    }

    /**
     * Valida que un campo JSON tenga formato de fecha válido.
     */
    public void validateJsonPathDateFormat(String jsonPath, String dateFormat) throws FrameworkBusinessException {
        try {
            HttpResponse response = getLastResponse();
            Object value = JsonUtilities.getJsonParameter(response.getBody(), jsonPath);
            if (value == null) {
                throw new FrameworkBusinessException("validateJsonPathDateFormat",
                    "El campo '" + jsonPath + "' es null");
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(dateFormat);
            sdf.setLenient(false);
            sdf.parse(value.toString());
            TestLogger.logInfo("API_HELPER", "Fecha validada en '" + jsonPath + "': " + value, null);
        } catch (java.text.ParseException e) {
            throw new FrameworkBusinessException("validateJsonPathDateFormat",
                "El campo '" + jsonPath + "' no tiene formato de fecha válido '" + dateFormat + "'");
        }
    }

    /**
     * Extrae todos los valores de un array JSON y los guarda como variable (lista serializada).
     */
    public void extractJsonArrayValues(String jsonPath, String variable) throws FrameworkBusinessException {
        HttpResponse response = getLastResponse();
        Object value = JsonUtilities.getJsonParameter(response.getBody(), jsonPath);
        if (value == null) {
            throw new FrameworkBusinessException("extractJsonArrayValues",
                "El campo '" + jsonPath + "' es null o no existe");
        }
        ExecutionContext.requireCurrent().variables().set(variable, value.toString());
        TestLogger.logInfo("API_HELPER",
            "Array '" + jsonPath + "' guardado como '" + variable + "'", null);
    }

    /**
     * Valida que un campo JSON sea null.
     */
    public void validateJsonPathIsNull(String jsonPath) throws FrameworkBusinessException {
        HttpResponse response = getLastResponse();
        Object value = JsonUtilities.getJsonParameter(response.getBody(), jsonPath);
        if (value != null && !"null".equals(value.toString())) {
            throw new FrameworkBusinessException("validateJsonPathIsNull",
                "Se esperaba null en '" + jsonPath + "' pero se encontró: " + value);
        }
        TestLogger.logInfo("API_HELPER", "Campo '" + jsonPath + "' es null (OK)", null);
    }

    /**
     * Configura el timeout para la siguiente petición HTTP.
     */
    public void setTimeout(int millis) {
        httpClient.setTimeout(millis);
        TestLogger.logInfo("API_HELPER", "Timeout configurado: " + millis + "ms", null);
    }

    /**
     * Configura la codificación de la petición HTTP.
     */
    public void setEncoding(String encoding) {
        httpClient.addHeader("Accept-Charset", encoding);
        TestLogger.logInfo("API_HELPER", "Encoding configurado: " + encoding, null);
    }
}

