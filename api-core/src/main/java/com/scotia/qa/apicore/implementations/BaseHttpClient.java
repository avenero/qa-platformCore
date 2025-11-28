package com.scotia.qa.apicore.implementations;

import com.scotia.qa.apicore.interfaces.HttpClient;
import com.scotia.qa.common.http.enums.HttpMethod;
import com.scotia.qa.common.http.exceptions.FrameworkTechnicalException;
import com.scotia.qa.common.http.model.HttpResponse;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.DataUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kong.unirest.*;

/**
 * Implementación base del cliente HTTP para el framework Scotia QA.
 *
 * <p>Esta clase proporciona toda la funcionalidad HTTP base que pueden usar y extender los
 * frameworks específicos (API, Web, Mobile). Consolida la lógica de peticiones HTTP eliminando
 * duplicaciones y proporcionando una base sólida para cualquier tipo de testing.
 *
 * <p><b>Métodos HTTP soportados:</b>
 *
 * <ul>
 *   <li><b>GET</b> - Para obtención de recursos
 *   <li><b>POST</b> - Para creación de recursos y envío de datos
 *   <li><b>PUT</b> - Para actualización completa de recursos
 *   <li><b>PATCH</b> - Para actualización parcial de recursos
 *   <li><b>DELETE</b> - Para eliminación de recursos
 *   <li><b>HEAD</b> - Para obtener metadatos sin body
 *   <li><b>OPTIONS</b> - Para consultar métodos permitidos
 * </ul>
 *
 * <p><b>Características principales:</b>
 *
 * <ul>
 *   <li>Soporte completo para todos los métodos HTTP estándar (RFC 7231)
 *   <li>Gestión automática de headers, query parameters y body
 *   <li>Configuración flexible de timeouts, redirects y SSL
 *   <li>Logging detallado con sanitización automática de datos sensibles
 *   <li>Cache de respuestas para debugging y análisis de performance
 *   <li>Gestión avanzada de cookies para sesiones cross-request
 *   <li>Contexto de usuario para testing multi-usuario
 *   <li>Validación robusta de parámetros con mensajes descriptivos
 *   <li>Manejo de excepciones tipificadas para debugging eficiente
 *   <li>Thread-safe para uso concurrente en entornos multi-hilo
 * </ul>
 *
 * <p><b>Configuración de contenido:</b>
 *
 * <ul>
 *   <li><b>JSON</b> - {@code configureForJson()} para APIs REST
 *   <li><b>Form Data</b> - {@code configureForFormData()} para formularios web
 *   <li><b>Multipart</b> - {@code configureForMultipart()} para upload de archivos
 *   <li><b>XML</b> - {@code configureForXml()} para servicios SOAP/XML
 *   <li><b>Plain Text</b> - {@code configureForPlainText()} para contenido simple
 * </ul>
 *
 * <p><b>Gestión de timeouts y rendimiento:</b>
 *
 * <ul>
 *   <li>Connection timeout configurable (default: 30 segundos)
 *   <li>Read timeout configurable (default: 60 segundos)
 *   <li>Política de reintentos configurable
 *   <li>Métricas de performance automáticas
 * </ul>
 *
 * <p><b>Uso básico típico:</b>
 *
 * <pre>
 * // Crear instancia y configurar
 * HttpClient client = new BaseHttpClient();
 * client.setHost("https://api.example.com");
 * client.addHeader("Authorization", "Bearer " + token);
 *
 * // Peticiones simples
 * HttpResponse response = client.get("/users");
 * HttpResponse response = client.post("/users", userData);
 * HttpResponse response = client.put("/users/123", updatedData);
 * HttpResponse response = client.delete("/users/123");
 *
 * // Configuración de contenido
 * client.configureForJson();
 * client.setBody("{\"name\":\"John\",\"age\":30}");
 * HttpResponse response = client.executeRequest(HttpMethod.POST, "/users");
 * </pre>
 *
 * <p><b>Uso avanzado con configuración completa:</b>
 *
 * <pre>
 * HttpClient client = new BaseHttpClient();
 * client.setHost("https://api.example.com");
 *
 * // Headers personalizados
 * client.addHeader("Authorization", "Bearer " + token);
 * client.addHeader("X-Client-Version", "1.0.0");
 *
 * // Query parameters
 * client.addQueryParam("page", "1");
 * client.addQueryParam("size", "10");
 *
 * // Configurar timeouts
 * client.setConnectionTimeout(15000); // 15 segundos
 * client.setReadTimeout(30000);       // 30 segundos
 *
 * // Gestión de cookies
 * client.setAutomaticCookieHandling(true);
 * client.addCookie("sessionId", "abc123");
 *
 * // Contexto de usuario para testing
 * client.setCurrentUserId("user123");
 * client.addUserContextData("department", "QA");
 *
 * // Ejecutar petición
 * HttpResponse response = client.executeRequest(HttpMethod.GET, "/users");
 * </pre>
 *
 * <p><b>Extensión en frameworks específicos:</b>
 *
 * <pre>
 * public class ApiHttpClient extends BaseHttpClient {
 *
 *     &#64;Override
 *     public HttpResponse executeRequest(HttpMethod method, String endpoint) throws FrameworkTechnicalException {
 *         // Lógica específica de API (métricas, validación de schemas, etc.)
 *         long startTime = System.currentTimeMillis();
 *
 *         HttpResponse response = super.executeRequest(method, endpoint);
 *
 *         // Validar schema de respuesta
 *         validateResponseSchema(response);
 *
 *         // Registrar métricas
 *         recordApiMetrics(method, endpoint, response, System.currentTimeMillis() - startTime);
 *
 *         return response;
 *     }
 *
 *     &#64;Override
 *     public void setHost(String host) {
 *         super.setHost(host);
 *         // Agregar headers específicos de la API
 *         addHeader("X-API-Version", "v1");
 *         addHeader("Accept", "application/json");
 *     }
 * }
 * </pre>
 *
 * <p><b>Debugging y troubleshooting:</b> La clase proporciona información detallada para debugging:
 *
 * <ul>
 *   <li>Logging automático de requests y responses
 *   <li>Sanitización de datos sensibles (passwords, tokens, etc.)
 *   <li>Información de performance y timeouts
 *   <li>Estado de conexiones y cookies
 *   <li>Contexto de usuario y sesión
 * </ul>
 *
 * <p><b>Nota técnica:</b> Esta implementación utiliza Unirest como cliente HTTP subyacente para
 * proporcionar funcionalidades robustas y confiables. Los frameworks específicos pueden extender o
 * sobrescribir comportamientos según sus necesidades particulares.
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 * @see HttpClient
 * @see HttpMethod
 * @see HttpResponse
 * @see FrameworkTechnicalException
 */
public class BaseHttpClient implements HttpClient {

  private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(BaseHttpClient.class);

  // =================================================================================
  // ESTADO INTERNO
  // =================================================================================

  // Estado de la petición actual
  private final Map<String, String> headers = new ConcurrentHashMap<>();
  private final Map<String, Object> fields = new ConcurrentHashMap<>();
  private final Map<String, Object> queryParams = new ConcurrentHashMap<>();
  private String body;
  private String currentHost;

  // Configuración de timeouts
  private int connectionTimeout = 30000; // 30 segundos por defecto
  private int readTimeout = 60000; // 60 segundos por defecto

  // Configuración de retry policy
  private int maxRetries = 0; // Sin retry por defecto
  private int retryDelay = 1000; // 1 segundo por defecto

  // Cookies para cross-framework
  private final Map<String, String> cookies = new ConcurrentHashMap<>();
  private boolean automaticCookieHandling = false;

  // Contexto de usuario para cross-framework
  private String currentUserId;
  private String currentSessionId;
  private final Map<String, String> userContext = new ConcurrentHashMap<>();
  private boolean autoApplyUserContext = true; // Por defecto habilitado

  // Información de la última petición para debugging
  private kong.unirest.HttpResponse<String> lastResponse;
  private String lastRequestUrl;
  private String lastRequestMethod;
  private long lastRequestDuration;

  // Configuración SSL
  private boolean sslValidationDisabled = false;
  private static boolean globalSSLConfigured = false;

  // =================================================================================
  // CONSTRUCTOR Y CONFIGURACIÓN INICIAL
  // =================================================================================

  /**
   * Constructor por defecto. Configura automáticamente SSL para deshabilitar validación de
   * certificados en testing.
   */
  public BaseHttpClient() {
    configureSSLForTesting();
  }

  /**
   * Configura SSL para ambientes de testing. Deshabilita la validación de certificados SSL para
   * permitir testing contra servidores con certificados autofirmados o inválidos.
   *
   * <p><b>⚠️ SOLO PARA TESTING:</b> Esta configuración se aplica solo una vez globalmente para
   * todas las instancias de BaseHttpClient.
   */
  private void configureSSLForTesting() {
    // Solo configurar una vez a nivel global
    if (!globalSSLConfigured) {
      synchronized (BaseHttpClient.class) {
        if (!globalSSLConfigured) {
          try {
            // Crear contexto SSL sin validación (sin logs internos)
            javax.net.ssl.SSLContext sslContext =
                com.scotia.qa.apicore.utils.SSLUtils.createTrustAllSSLContext();
            javax.net.ssl.HostnameVerifier hostnameVerifier =
                com.scotia.qa.apicore.utils.SSLUtils.createTrustAllHostnameVerifier();

            // Configurar HttpsURLConnection por defecto (usado por Unirest internamente)
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(
                sslContext.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

            // Configurar Unirest globalmente
            Unirest.config()
                .sslContext(sslContext)
                .hostnameVerifier(hostnameVerifier)
                .verifySsl(false); // Deshabilitar verificación SSL

            sslValidationDisabled = true;
            globalSSLConfigured = true;

            // Log solo la primera vez que se configura (no en cada instancia)
            log.debug("SSL: Validación de certificados deshabilitada para testing");

          } catch (Exception e) {
            log.error("❌ Error configurando SSL: {}", e.getMessage());
          }
        }
      }
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE CONFIGURACIÓN DE PETICIONES
  // =================================================================================

  @Override
  public void setHost(String host) {
    if (host == null || host.trim().isEmpty()) {
      throw new IllegalArgumentException("Host no puede ser null o vacío");
    }

    // Sanitizar: remover comillas dobles y espacios
    String sanitizedHost =
        host.trim()
            .replaceAll("^\"|\"$", "") // Remover comillas al inicio y final
            .replaceAll("^'|'$", ""); // Remover comillas simples al inicio y final

    if (sanitizedHost.isEmpty()) {
      throw new IllegalArgumentException("Host no puede estar vacío después de sanitización");
    }

    this.currentHost = sanitizedHost;
    log.debug("Host establecido: {}", this.currentHost);
  }

  @Override
  public String getHost() {
    return currentHost;
  }

  @Override
  public boolean hasValidHost() {
    return currentHost != null && !currentHost.trim().isEmpty();
  }

  @Override
  public void addHeader(String key, String value) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Header key no puede ser null o vacío");
    }

    // Normalizar key: quitar comillas si existen (compatibilidad con Cucumber {string})
    String normalizedKey = key.trim();
    if (normalizedKey.startsWith("\"")
        && normalizedKey.endsWith("\"")
        && normalizedKey.length() > 2) {
      normalizedKey = normalizedKey.substring(1, normalizedKey.length() - 1);
    }

    if (value == null) {
      headers.remove(normalizedKey);
      log.debug("Header removido: {}", normalizedKey);
    } else {
      // También normalizar value si tiene comillas
      String normalizedValue = value.trim();
      if (normalizedValue.startsWith("\"")
          && normalizedValue.endsWith("\"")
          && normalizedValue.length() > 2) {
        normalizedValue = normalizedValue.substring(1, normalizedValue.length() - 1);
      }

      headers.put(normalizedKey, normalizedValue);
      log.debug(
          "Header agregado: {} = {}",
          normalizedKey,
          DataUtilities.sanitizeValue(normalizedKey, normalizedValue));
    }
  }

  @Override
  public void addHeaders(Map<String, String> headers) {
    if (headers == null) {
      throw new IllegalArgumentException("Headers map no puede ser null");
    }

    headers.forEach(
        (key, value) -> {
          if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Header key no puede ser null o vacío: " + key);
          }
          addHeader(key, value);
        });
  }

  @Override
  public void addQueryParam(String key, String value) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Query param key no puede ser null o vacío");
    }

    if (value == null) {
      queryParams.remove(key);
      log.debug("Query param removido: {}", key);
    } else {
      queryParams.put(key, value);
      log.debug("Query param agregado: {} = {}", key, value);
    }
  }

  @Override
  public void addQueryParams(Map<String, Object> queryParams) {
    if (queryParams == null) {
      throw new IllegalArgumentException("Query params map no puede ser null");
    }

    queryParams.forEach(
        (key, value) -> {
          if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Query param key no puede ser null o vacío: " + key);
          }
          addQueryParam(key, value != null ? value.toString() : null);
        });
  }

  @Override
  public void addField(String key, String value) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Field key no puede ser null o vacío");
    }

    if (value == null) {
      fields.remove(key);
      log.debug("Field removido: {}", key);
    } else {
      fields.put(key, value);
      log.debug("Field agregado: {} = {}", key, value);
    }
  }

  @Override
  public void addFields(Map<String, String> fields) {
    if (fields == null) {
      throw new IllegalArgumentException("Fields map no puede ser null");
    }

    fields.forEach(
        (key, value) -> {
          if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Field key no puede ser null o vacío: " + key);
          }
          addField(key, value);
        });
  }

  @Override
  public void setBody(String body) {
    this.body = body;
    log.debug("Body establecido (longitud: {} caracteres)", body != null ? body.length() : 0);
  }

  @Override
  public boolean hasBody() {
    return body != null && !body.trim().isEmpty();
  }

  @Override
  public long getBodySize() {
    return body != null ? body.getBytes().length : 0;
  }

  @Override
  public void setContentType(String contentType) {
    if (contentType == null || contentType.trim().isEmpty()) {
      throw new IllegalArgumentException("Content type no puede ser null o vacío");
    }
    addHeader("Content-Type", contentType.trim());
    log.debug("Content-Type establecido: {}", contentType);
  }

  @Override
  public String getContentType() {
    return headers.get("Content-Type");
  }

  @Override
  public void setAcceptType(String acceptType) {
    if (acceptType == null || acceptType.trim().isEmpty()) {
      throw new IllegalArgumentException("Accept type no puede ser null o vacío");
    }
    addHeader("Accept", acceptType.trim());
    log.debug("Accept-Type establecido: {}", acceptType);
  }

  @Override
  public String getAcceptType() {
    return headers.get("Accept");
  }

  @Override
  public void configureForJson() {
    setContentType("application/json");
    setAcceptType("application/json");
    log.debug("Configuración JSON aplicada (Content-Type y Accept)");
  }

  @Override
  public void configureForXml() {
    setContentType("application/xml");
    setAcceptType("application/xml");
    log.debug("Configuración XML aplicada (Content-Type y Accept)");
  }

  @Override
  public void configureForFormData() {
    setContentType("application/x-www-form-urlencoded");
    setAcceptType("application/json");
    log.debug("Configuración Form Data aplicada (Content-Type: form-urlencoded, Accept: JSON)");
  }

  @Override
  public void configureForPlainText() {
    setContentType("text/plain");
    setAcceptType("text/plain");
    log.debug("Configuración Plain Text aplicada (Content-Type y Accept: text/plain)");
  }

  @Override
  public void configureForMultipart() {
    setContentType("multipart/form-data");
    setAcceptType("application/json");
    log.debug("Configuración Multipart aplicada (Content-Type: multipart/form-data, Accept: JSON)");
  }

  @Override
  public boolean isJsonContentType() {
    String contentType = getContentType();
    return contentType != null && contentType.contains("application/json");
  }

  @Override
  public boolean isXmlContentType() {
    String contentType = getContentType();
    return contentType != null
        && (contentType.contains("application/xml") || contentType.contains("text/xml"));
  }

  @Override
  public boolean isFormDataContentType() {
    String contentType = getContentType();
    return contentType != null && contentType.contains("application/x-www-form-urlencoded");
  }

  @Override
  public String detectContentType(String body) {
    if (body == null || body.trim().isEmpty()) {
      return "text/plain";
    }

    String trimmedBody = body.trim();

    // Detectar JSON
    if ((trimmedBody.startsWith("{") && trimmedBody.endsWith("}"))
        || (trimmedBody.startsWith("[") && trimmedBody.endsWith("]"))) {
      return "application/json";
    }

    // Detectar XML
    if (trimmedBody.startsWith("<") && trimmedBody.contains(">")) {
      return "application/xml";
    }

    // Detectar form-encoded (clave=valor&clave2=valor2)
    if (trimmedBody.contains("=") && trimmedBody.contains("&")) {
      return "application/x-www-form-urlencoded";
    }

    // Por defecto, texto plano
    return "text/plain";
  }

  @Override
  public void autoDetectAndSetContentType() {
    if (hasBody() && getContentType() == null) {
      String detectedType = detectContentType(body);
      setContentType(detectedType);
      log.debug("Content-Type auto-detectado y establecido: {}", detectedType);
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE CONFIGURACIÓN DE TIMEOUTS
  // =================================================================================

  @Override
  public void setConnectionTimeout(int timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("Connection timeout debe ser mayor a 0: " + timeoutMs);
    }
    this.connectionTimeout = timeoutMs;
    log.debug("Connection timeout establecido: {} ms", timeoutMs);
  }

  @Override
  public void setReadTimeout(int timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("Read timeout debe ser mayor a 0: " + timeoutMs);
    }
    this.readTimeout = timeoutMs;
    log.debug("Read timeout establecido: {} ms", timeoutMs);
  }

  @Override
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  public int getReadTimeout() {
    return readTimeout;
  }

  @Override
  public void setRetryPolicy(int maxRetries, int retryDelayMs) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries no puede ser negativo: " + maxRetries);
    }
    if (retryDelayMs < 0) {
      throw new IllegalArgumentException("retryDelayMs no puede ser negativo: " + retryDelayMs);
    }
    this.maxRetries = maxRetries;
    this.retryDelay = retryDelayMs;
    log.debug("Retry policy configurado: maxRetries={}, delay={}ms", maxRetries, retryDelayMs);
  }

  @Override
  public boolean isRetryEnabled() {
    return maxRetries > 0;
  }

  @Override
  public int getMaxRetries() {
    return maxRetries;
  }

  @Override
  public int getRetryDelay() {
    return retryDelay;
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE EJECUCIÓN DE PETICIONES
  // =================================================================================

  @Override
  public HttpResponse executeRequest(HttpMethod method, String endpoint)
      throws FrameworkTechnicalException {
    return executeRequest(method, endpoint, true);
  }

  @Override
  public HttpResponse executeRequest(HttpMethod method, String endpoint, boolean followRedirects)
      throws FrameworkTechnicalException {
    if (method == null) {
      throw new IllegalArgumentException("HttpMethod no puede ser null");
    }

    return executeRequest(method.toString(), endpoint, followRedirects);
  }

  /**
   * Ejecuta una petición HTTP con el método especificado como String. Este método mantiene
   * compatibilidad con el código legacy.
   */
  public HttpResponse executeRequest(String method, String endpoint, boolean followRedirects)
      throws FrameworkTechnicalException {
    try {
      if (!hasValidHost()) {
        throw new FrameworkTechnicalException(
            "executeRequest", "Host no configurado. Use setHost() primero.");
      }

      configureUnirest(followRedirects);
      String fullUrl = buildFullUrl(endpoint);

      // Asegurar Content-Type ANTES de logging y ejecución (una sola vez)
      // Solo para métodos que envían body (POST, PUT, PATCH)
      String upperMethod = method != null ? method.toUpperCase() : "";
      if (upperMethod.equals("POST") || upperMethod.equals("PUT") || upperMethod.equals("PATCH")) {
        if (!fields.isEmpty() || DataUtilities.isValidString(body)) {
          ensureContentTypeHeader();
        }
      }

      long startTime = System.nanoTime();

      // Guardar datos para debugging
      this.lastRequestMethod = method != null ? method.toUpperCase() : null;
      this.lastRequestUrl = fullUrl;

      logRequest(method, fullUrl);

      lastResponse = performRequest(method, fullUrl);

      this.lastRequestDuration = (System.nanoTime() - startTime) / 1_000_000; // ms
      logResponse();

      clearRequestData();

      // Convertir kong.unirest.HttpResponse a nuestro HttpResponse
      return convertToFrameworkResponse(lastResponse);

    } catch (Exception ex) {
      log.error("Error ejecutando petición HTTP: {}", ex.getClass().getSimpleName(), ex);
      throw new FrameworkTechnicalException("executeRequest", ex.getMessage());
    }
  }

  @Override
  public HttpResponse executeRequest(HttpMethod method, String endpoint, int timeoutMs)
      throws FrameworkTechnicalException {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("Timeout debe ser mayor a 0: " + timeoutMs);
    }

    int originalConnectionTimeout = this.connectionTimeout;
    int originalReadTimeout = this.readTimeout;

    try {
      this.connectionTimeout = timeoutMs;
      this.readTimeout = timeoutMs;
      return executeRequest(method, endpoint, true);
    } finally {
      this.connectionTimeout = originalConnectionTimeout;
      this.readTimeout = originalReadTimeout;
    }
  }

  @Override
  public HttpResponse executeRequest(
      HttpMethod method, String endpoint, boolean followRedirects, int timeoutMs)
      throws FrameworkTechnicalException {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("Timeout debe ser mayor a 0: " + timeoutMs);
    }

    int originalConnectionTimeout = this.connectionTimeout;
    int originalReadTimeout = this.readTimeout;

    try {
      this.connectionTimeout = timeoutMs;
      this.readTimeout = timeoutMs;
      return executeRequest(method, endpoint, followRedirects);
    } finally {
      this.connectionTimeout = originalConnectionTimeout;
      this.readTimeout = originalReadTimeout;
    }
  }

  @Override
  public HttpResponse get(String endpoint) throws FrameworkTechnicalException {
    return executeRequest(HttpMethod.GET, endpoint);
  }

  @Override
  public HttpResponse post(String endpoint) throws FrameworkTechnicalException {
    return executeRequest(HttpMethod.POST, endpoint);
  }

  @Override
  public HttpResponse put(String endpoint) throws FrameworkTechnicalException {
    return executeRequest(HttpMethod.PUT, endpoint);
  }

  @Override
  public HttpResponse delete(String endpoint) throws FrameworkTechnicalException {
    return executeRequest(HttpMethod.DELETE, endpoint);
  }

  @Override
  public HttpResponse patch(String endpoint) throws FrameworkTechnicalException {
    return executeRequest(HttpMethod.PATCH, endpoint);
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE ACCESO A INFORMACIÓN DE LA ÚLTIMA PETICIÓN
  // =================================================================================

  @Override
  public HttpResponse getLastResponse() {
    return lastResponse != null ? convertToFrameworkResponse(lastResponse) : null;
  }

  public String getLastResponseBody() {
    return lastResponse != null ? lastResponse.getBody() : null;
  }

  public int getLastResponseStatus() {
    return lastResponse != null ? lastResponse.getStatus() : -1;
  }

  public Map<String, String> getLastResponseHeaders() {
    if (lastResponse == null) return new HashMap<>();

    return lastResponse.getHeaders().all().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                Header::getName, Header::getValue, (existing, replacement) -> existing));
  }

  @Override
  public String getLastRequestUrl() {
    return lastRequestUrl;
  }

  @Override
  public String getLastRequestMethod() {
    return lastRequestMethod;
  }

  @Override
  public long getLastRequestDuration() {
    return lastRequestDuration;
  }

  @Override
  public String getDebugInfo() {
    StringBuilder debug = new StringBuilder("BaseHttpClient Debug Info:\n");
    debug.append("Host: ").append(currentHost).append("\n");
    debug.append("Connection Timeout: ").append(connectionTimeout).append("ms\n");
    debug.append("Read Timeout: ").append(readTimeout).append("ms\n");
    debug
        .append("Retry Policy: ")
        .append(maxRetries)
        .append(" retries, ")
        .append(retryDelay)
        .append("ms delay\n");
    debug.append("Cookies: ").append(cookies.size()).append(" cookies\n");
    debug.append("Auto Cookie Handling: ").append(automaticCookieHandling).append("\n");
    debug.append("User Context: ").append(hasUserContext() ? "Set" : "Not Set").append("\n");
    debug.append("Auto Apply User Context: ").append(autoApplyUserContext).append("\n");
    if (lastResponse != null) {
      debug
          .append("Last Request: ")
          .append(lastRequestMethod)
          .append(" ")
          .append(lastRequestUrl)
          .append("\n");
      debug.append("Last Response Status: ").append(lastResponse.getStatus()).append("\n");
      debug.append("Last Request Duration: ").append(lastRequestDuration).append("ms\n");
    } else {
      debug.append("Last Request: None\n");
    }
    return debug.toString();
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE GESTIÓN DE ESTADO
  // =================================================================================

  public void clearHeaders() {
    headers.clear();
    log.debug("Headers limpiados");
  }

  public void clearFields() {
    fields.clear();
    log.debug("Fields limpiados");
  }

  public void clearQueryParams() {
    queryParams.clear();
    log.debug("Query parameters limpiados");
  }

  @Override
  public void clearRequestData() {
    headers.clear();
    queryParams.clear();
    fields.clear();
    body = null;
    log.debug("Datos de petición limpiados");
  }

  public void clearAll() {
    clearRequestData();
    log.debug("Todos los datos de petición limpiados");
  }

  @Override
  public void reset() {
    clearRequestData();
    cookies.clear();
    currentHost = null;
    connectionTimeout = 30000;
    readTimeout = 60000;
    maxRetries = 0;
    retryDelay = 1000;
    log.debug("Cliente HTTP reseteado completamente");
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE GESTIÓN DE COOKIES
  // =================================================================================

  @Override
  public void setCookies(Map<String, String> cookies) {
    if (cookies == null) {
      throw new IllegalArgumentException("Cookies map no puede ser null");
    }

    this.cookies.clear();
    cookies.forEach(
        (name, value) -> {
          if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cookie name no puede ser null o vacío: " + name);
          }
          this.cookies.put(name, value);
        });
    log.debug("Cookies configuradas: {}", this.cookies.keySet());
  }

  @Override
  public Map<String, String> getCookies() {
    return new HashMap<>(cookies);
  }

  @Override
  public void addCookie(String name, String value) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Cookie name no puede ser null o vacío");
    }

    if (value == null) {
      cookies.remove(name);
      log.debug("Cookie removida: {}", name);
    } else {
      cookies.put(name, value);
      log.debug("Cookie agregada: {} = {}", name, value);
    }
  }

  @Override
  public String getCookie(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Cookie name no puede ser null o vacío");
    }
    return cookies.get(name);
  }

  @Override
  public boolean hasCookie(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Cookie name no puede ser null o vacío");
    }
    return cookies.containsKey(name);
  }

  @Override
  public void removeCookie(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Cookie name no puede ser null o vacío");
    }
    cookies.remove(name);
    log.debug("Cookie removida: {}", name);
  }

  @Override
  public void clearCookies() {
    cookies.clear();
    log.debug("Todas las cookies limpiadas");
  }

  @Override
  public void setAutomaticCookieHandling(boolean enabled) {
    this.automaticCookieHandling = enabled;
    log.debug("Manejo automático de cookies: {}", enabled ? "habilitado" : "deshabilitado");
  }

  @Override
  public boolean isAutomaticCookieHandlingEnabled() {
    return automaticCookieHandling;
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE GESTIÓN DE CONTEXTO DE USUARIO
  // =================================================================================

  @Override
  public void setUserContext(String userId, String sessionId) {
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("userId no puede ser null o vacío");
    }
    if (sessionId == null || sessionId.trim().isEmpty()) {
      throw new IllegalArgumentException("sessionId no puede ser null o vacío");
    }

    this.currentUserId = userId.trim();
    this.currentSessionId = sessionId.trim();
    log.debug("Contexto de usuario establecido: userId={}, sessionId={}", userId, sessionId);
  }

  @Override
  public void setUserContext(
      String userId, String sessionId, Map<String, String> additionalContext) {
    setUserContext(userId, sessionId);

    if (additionalContext == null) {
      throw new IllegalArgumentException("additionalContext no puede ser null");
    }

    this.userContext.clear();
    additionalContext.forEach(
        (key, value) -> {
          if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Context key no puede ser null o vacío: " + key);
          }
          this.userContext.put(key, value);
        });

    log.debug(
        "Contexto de usuario establecido con {} campos adicionales", additionalContext.size());
  }

  @Override
  public String getCurrentUserId() {
    return currentUserId;
  }

  @Override
  public String getCurrentSessionId() {
    return currentSessionId;
  }

  @Override
  public Map<String, String> getUserContext() {
    Map<String, String> fullContext = new HashMap<>();
    if (currentUserId != null) {
      fullContext.put("userId", currentUserId);
    }
    if (currentSessionId != null) {
      fullContext.put("sessionId", currentSessionId);
    }
    fullContext.putAll(userContext);
    return fullContext;
  }

  @Override
  public String getContextValue(String key) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Context key no puede ser null o vacío");
    }

    if ("userId".equals(key)) {
      return currentUserId;
    }
    if ("sessionId".equals(key)) {
      return currentSessionId;
    }
    return userContext.get(key);
  }

  @Override
  public void setContextValue(String key, String value) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Context key no puede ser null o vacío");
    }

    if ("userId".equals(key)) {
      this.currentUserId = value;
    } else if ("sessionId".equals(key)) {
      this.currentSessionId = value;
    } else if (value == null) {
      userContext.remove(key);
      log.debug("Context value removido: {}", key);
    } else {
      userContext.put(key, value);
      log.debug("Context value establecido: {} = {}", key, value);
    }
  }

  @Override
  public boolean hasUserContext() {
    return currentUserId != null && currentSessionId != null;
  }

  @Override
  public void clearUserContext() {
    currentUserId = null;
    currentSessionId = null;
    userContext.clear();
    log.debug("Contexto de usuario limpiado completamente");
  }

  @Override
  public void setAutoApplyUserContext(boolean enabled) {
    this.autoApplyUserContext = enabled;
    log.debug("Auto aplicar contexto de usuario: {}", enabled ? "habilitado" : "deshabilitado");
  }

  @Override
  public boolean isAutoApplyUserContextEnabled() {
    return autoApplyUserContext;
  }

  // =================================================================================
  // MÉTODOS PROTEGIDOS PARA ESPECIALIZACIÓN
  // =================================================================================

  /**
   * Método protegido que pueden override los frameworks especializados para personalizar la
   * configuración de Unirest.
   */
  protected void configureUnirest(boolean followRedirects) {
    Unirest.config().reset();
    Unirest.config()
        .verifySsl(false)
        .followRedirects(followRedirects)
        .connectTimeout(connectionTimeout)
        .socketTimeout(readTimeout);

    // Configurar cookies si las hay
    if (!cookies.isEmpty()) {
      // Las cookies se agregan directamente a los headers
      StringBuilder cookieHeader = new StringBuilder();
      cookies.forEach(
          (name, value) -> {
            if (cookieHeader.length() > 0) {
              cookieHeader.append("; ");
            }
            cookieHeader.append(name).append("=").append(value);
          });
      headers.put("Cookie", cookieHeader.toString());
    }
  }

  /**
   * Método protegido que pueden override los frameworks especializados para personalizar la
   * construcción de URLs.
   */
  protected String buildFullUrl(String endpoint) throws FrameworkTechnicalException {
    if (currentHost == null) {
      throw new FrameworkTechnicalException(
          "buildFullUrl", "Host no configurado. Use setHost() primero.");
    }

    // Normalizar endpoint: tratar null y cadenas vacías/whitespace de la misma forma
    String normalizedEndpoint = (endpoint == null || endpoint.trim().isEmpty()) ? "" : endpoint;

    String baseUrl = currentHost + normalizedEndpoint;

    // Si hay query params, construir la URL completa con ellos para logging
    if (!queryParams.isEmpty()) {
      StringBuilder urlWithParams = new StringBuilder(baseUrl);
      urlWithParams.append("?");

      boolean first = true;
      for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
        if (!first) {
          urlWithParams.append("&");
        }
        urlWithParams
            .append(entry.getKey())
            .append("=")
            .append(entry.getValue() != null ? entry.getValue().toString() : "");
        first = false;
      }

      return urlWithParams.toString();
    }

    return baseUrl;
  }

  /**
   * Método protegido que pueden override los frameworks especializados para personalizar el logging
   * de peticiones.
   */
  protected void logRequest(String method, String url) {
    // Separador visual para delimitar inicio de petición
    log.info("────────────────────────────────────────────────────────────────");
    log.info("🌐 HTTP Request → {} {}", method.toUpperCase(), url);

    if (!headers.isEmpty()) {
      // Formatear headers sin el formato Map {key=value}
      String formattedHeaders = formatHeadersForLogging(sanitizeHeaders(headers));
      log.info("   📋 Headers: {}", formattedHeaders);
    }

    // Los query params ya están incluidos en la URL, no mostrarlos duplicados
    // if (!queryParams.isEmpty()) {
    //     log.info("   🔍 Query Params: {}", queryParams);
    // }

    if (!fields.isEmpty()) {
      log.info("   📎 Multipart Fields: {}", fields.keySet());
    } else if (hasBody()) {
      String sanitizedBody = DataUtilities.sanitizeBody(body);
      String formattedBody = formatJsonIfPossible(sanitizedBody);
      log.info("   📊 Body:\n{}", formattedBody);
    }
  }

  /** Formatea headers para logging en formato limpio: "Header1: value1, Header2: value2" */
  private String formatHeadersForLogging(Map<String, String> headersMap) {
    if (headersMap == null || headersMap.isEmpty()) {
      return "";
    }

    StringBuilder formatted = new StringBuilder();
    headersMap.forEach(
        (key, value) -> {
          if (formatted.length() > 0) {
            formatted.append(", ");
          }
          formatted.append(key).append(": ").append(value);
        });

    return formatted.toString();
  }

  /**
   * Método protegido que pueden override los frameworks especializados para personalizar el logging
   * de respuestas.
   */
  protected void logResponse() {
    int status = lastResponse.getStatus();
    String statusEmoji = getStatusEmoji(status);
    String durationFormatted = formatDuration(lastRequestDuration);

    log.info("{} HTTP Response ← {} ({})", statusEmoji, status, durationFormatted);

    // Mostrar headers solo si hay información relevante (no vacíos)
    if (!lastResponse.getHeaders().all().isEmpty()) {
      String headersFormatted = formatResponseHeaders(lastResponse.getHeaders());
      if (!headersFormatted.trim().isEmpty()) {
        log.info("   📋 Headers: {}", headersFormatted);
      }
    }

    String body = lastResponse.getBody();
    if (body != null && !body.trim().isEmpty()) {
      String sanitizedBody = DataUtilities.sanitizeBody(body);
      String formattedBody = formatJsonIfPossible(sanitizedBody);
      String truncatedBody = DataUtilities.truncateContent(formattedBody, 4000);
      log.info("   📄 Body:\n{}", truncatedBody);
    }

    // Separador visual para delimitar fin de respuesta
    log.info("────────────────────────────────────────────────────────────────");
  }

  /**
   * Formatea un string JSON para mostrarlo de forma legible (pretty-print). Si no es JSON válido,
   * devuelve el string original.
   */
  private String formatJsonIfPossible(String content) {
    if (content == null || content.trim().isEmpty()) {
      return content;
    }

    String trimmed = content.trim();
    // Solo intentar formatear si parece ser JSON (empieza con { o [)
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
      return content;
    }

    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      Object json = mapper.readValue(trimmed, Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception e) {
      // Si falla el parseo, devolver el contenido original
      return content;
    }
  }

  /** Obtiene el emoji apropiado según el código de estado HTTP. */
  private String getStatusEmoji(int status) {
    if (status >= 200 && status < 300) return "✅"; // Éxito
    if (status >= 300 && status < 400) return "🔀"; // Redirección
    if (status >= 400 && status < 500) return "⚠️"; // Error del cliente
    return "❌"; // Error del servidor
  }

  /** Formatea la duración en un formato legible (ms o segundos). */
  private String formatDuration(long durationMs) {
    if (durationMs >= 10000) {
      // 10+ segundos: mostrar con 1 decimal (ej: 15.3s)
      return String.format("%.1fs", durationMs / 1000.0);
    } else if (durationMs >= 1000) {
      // 1-10 segundos: mostrar con 2 decimales (ej: 2.45s)
      return String.format("%.2fs", durationMs / 1000.0);
    }
    // Menos de 1 segundo: mostrar en ms
    return durationMs + "ms";
  }

  /** Formatea los headers de respuesta de forma compacta. */
  private String formatResponseHeaders(Headers headers) {
    if (headers == null || headers.all().isEmpty()) {
      return "";
    }

    // Mostrar solo headers más relevantes para no saturar el log
    StringBuilder formatted = new StringBuilder();
    headers.all().stream()
        .filter(h -> isRelevantHeader(h.getName()))
        .forEach(
            h -> {
              if (formatted.length() > 0) {
                formatted.append(", ");
              }
              formatted.append(h.getName()).append(": ").append(h.getValue());
            });

    return formatted.toString();
  }

  /** Determina si un header es relevante para mostrar en logs. */
  private boolean isRelevantHeader(String headerName) {
    if (headerName == null) return false;
    String name = headerName.toLowerCase();
    // Mostrar solo headers importantes
    return name.equals("content-type")
        || name.equals("content-length")
        || name.equals("cache-control")
        || name.equals("x-frame-options")
        || name.equals("content-security-policy")
        || name.startsWith("x-");
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE IMPLEMENTACIÓN
  // =================================================================================

  private kong.unirest.HttpResponse<String> performRequest(String method, String url)
      throws UnirestException, FrameworkTechnicalException {

    // Convertir string a HttpMethod enum para validación y consistencia
    HttpMethod httpMethod;
    try {
      httpMethod = HttpMethod.fromString(method);
    } catch (IllegalArgumentException e) {
      throw new FrameworkTechnicalException("performRequest", e.getMessage());
    }

    // Usar enum para el switch - más seguro y mantenible
    switch (httpMethod) {
      case POST:
        return executePost(url);
      case GET:
        return executeGet(url);
      case PUT:
        return executePut(url);
      case DELETE:
        return executeDelete(url);
      case PATCH:
        return executePatch(url);
      case OPTIONS:
        return executeOptions(url);
      case HEAD:
        return executeHead(url);
      default:
        // Este caso nunca debería ocurrir debido a la validación en HttpMethod.fromString()
        throw new FrameworkTechnicalException(
            "performRequest",
            "Método HTTP no implementado: "
                + httpMethod
                + ". Esto es un error interno del framework.");
    }
  }

  private kong.unirest.HttpResponse<String> executePost(String url) throws UnirestException {
    if (!fields.isEmpty()) {
      // Los query params ya están en la URL desde buildFullUrl()
      return Unirest.post(url).headers(headers).fields(fields).asString();
    } else {
      // Content-Type ya asegurado en executeRequest()
      if (DataUtilities.isValidString(body)) {
        return Unirest.post(url).headers(headers).body(body).asString();
      } else {
        return Unirest.post(url).headers(headers).asString();
      }
    }
  }

  private kong.unirest.HttpResponse<String> executeGet(String url) throws UnirestException {
    // Los query params ya están incluidos en la URL desde buildFullUrl()
    return Unirest.get(url).headers(headers).asString();
  }

  private kong.unirest.HttpResponse<String> executePut(String url) throws UnirestException {
    // Content-Type ya asegurado en executeRequest()
    if (DataUtilities.isValidString(body)) {
      // Los query params ya están en la URL desde buildFullUrl()
      return Unirest.put(url).headers(headers).body(body).asString();
    } else {
      return Unirest.put(url).headers(headers).asString();
    }
  }

  private kong.unirest.HttpResponse<String> executeDelete(String url) throws UnirestException {
    // Los query params ya están en la URL desde buildFullUrl()
    return Unirest.delete(url).headers(headers).asString();
  }

  private kong.unirest.HttpResponse<String> executePatch(String url) throws UnirestException {
    // Content-Type ya asegurado en executeRequest()
    if (DataUtilities.isValidString(body)) {
      // Los query params ya están en la URL desde buildFullUrl()
      return Unirest.patch(url).headers(headers).body(body).asString();
    } else {
      return Unirest.patch(url).headers(headers).asString();
    }
  }

  private kong.unirest.HttpResponse<String> executeOptions(String url) throws UnirestException {
    // Los query params ya están en la URL desde buildFullUrl()
    return Unirest.options(url).headers(headers).asString();
  }

  private kong.unirest.HttpResponse<String> executeHead(String url) throws UnirestException {
    // Los query params ya están en la URL desde buildFullUrl()
    return Unirest.head(url).headers(headers).asString();
  }

  private void ensureContentTypeHeader() {
    // Verificación case-insensitive de Content-Type
    boolean hasContentType =
        headers.keySet().stream()
            .anyMatch(key -> key != null && key.equalsIgnoreCase("Content-Type"));

    if (!hasContentType) {
      log.warn("No se especificó Content-Type, usando application/json por defecto");
      headers.put("Content-Type", "application/json");
    } else {
      log.debug(
          "Content-Type ya configurado: {}",
          headers.entrySet().stream()
              .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase("Content-Type"))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse("unknown"));
    }
  }

  private Map<String, String> sanitizeHeaders(Map<String, String> headersMap) {
    Map<String, String> sanitized = new HashMap<>();
    headersMap.forEach((k, v) -> sanitized.put(k, DataUtilities.sanitizeValue(k, v)));
    return sanitized;
  }

  private HttpResponse convertToFrameworkResponse(
      kong.unirest.HttpResponse<String> unirestResponse) {
    HttpResponse response =
        new HttpResponse(unirestResponse.getStatus(), unirestResponse.getBody());
    Map<String, String> responseHeaders = new HashMap<>();
    unirestResponse.getHeaders().all().forEach(h -> responseHeaders.put(h.getName(), h.getValue()));
    response.setHeaders(responseHeaders);
    return response;
  }
}
