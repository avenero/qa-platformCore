package com.qa.apicore.implementations;

import com.qa.apicore.interfaces.AuthenticationService;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.common.http.enums.HttpMethod;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.http.exceptions.FrameworkTechnicalException;
import com.qa.common.http.model.HttpResponse;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.utils.JsonUtilities;
import com.qa.common.utils.TextUtilities;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación base del gestor de autenticación para el QA Automation Framework.
 *
 * <p>Esta clase proporciona una implementación completa y robusta del gestor de autenticación que
 * puede ser utilizada directamente o extendida por los frameworks específicos (API, Web, Mobile).
 * Soporta estándares modernos de autenticación incluyendo OAuth2, JWT, API Keys y Multi-Factor
 * Authentication.
 *
 * <p>...existing documentation...
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 * @see AuthenticationService
 * @see HttpClient
 * @see BaseHttpClient
 */
public class BaseAuthenticationManager implements AuthenticationService {

  private static final TestLogger.LoggerWrapper LOG =
      TestLogger.getLogger(BaseAuthenticationManager.class);

  /** Máximo TTL para tokens MFA: 15 minutos en milisegundos. */
  private static final long MFA_MAX_TTL_MS = 15 * 60 * 1000;

  /** HTTP status code threshold: codes &gt;= this value indicate an error response. */
  private static final int HTTP_ERROR_THRESHOLD = 300;

  /** HTTP 204 No Content — successful revocation response per RFC 7009. */
  private static final int HTTP_NO_CONTENT = 204;

  // =================================================================================
  // CONFIGURACIÓN Y DEPENDENCIAS
  // =================================================================================

  private final HttpClient httpClient;

  // Cache de tokens thread-safe
  private final Map<String, TokenCacheEntry> tokenCache = new ConcurrentHashMap<>();

  // Configuración
  private long tokenCacheTTL = 30 * 60 * 1000; // 30 minutos por defecto
  private boolean cacheEnabled = true;

  // Credenciales por defecto (configurables)
  private String defaultClientId;
  private String defaultClientSecret;

  // Endpoints configurables (DEBEN ser establecidos por los frameworks específicos)
  private String defaultTokenEndpoint;
  private String defaultBearerTokenEndpoint;
  private String defaultBasicAuthEndpoint;
  private String defaultCustomTokenEndpoint;

  // Configuración de formatos (DEBEN ser establecidos por los frameworks específicos)
  private String tokenResponseKey;
  private String usernameFieldName;
  private String passwordFieldName;

  // =================================================================================
  // NUEVAS PROPIEDADES DE CONFIGURACIÓN - ALTA PRIORIDAD
  // =================================================================================

  // Refresh Token Support
  private String defaultRefreshTokenEndpoint;
  private String refreshTokenResponseKey;

  // API Key Management
  private String defaultApiKey;
  private String apiKeyHeader;
  private int apiKeyMinLength;
  private int apiKeyMaxLength;
  private String apiKeyAllowedPattern;

  // Scopes Management
  private String[] defaultScopes;

  // =================================================================================
  // NUEVAS PROPIEDADES DE CONFIGURACIÓN - MEDIA PRIORIDAD
  // =================================================================================

  // Token Introspection
  private String defaultIntrospectionEndpoint;
  private String introspectionClientId;
  private String introspectionClientSecret;

  // Token Revocation
  private String defaultRevocationEndpoint;

  // Multi-Factor Authentication
  private String mfaFieldName;

  // =================================================================================
  // CONSTRUCTORES
  // =================================================================================

  /**
   * Constructor principal que requiere un cliente HTTP.
   *
   * @param httpClient el cliente HTTP a utilizar para las peticiones
   * @throws IllegalArgumentException si httpClient es null
   */
  public BaseAuthenticationManager(HttpClient httpClient) {
    if (httpClient == null) {
      throw new IllegalArgumentException("HttpClient no puede ser null");
    }
    this.httpClient = httpClient;
    LOG.debug("BaseAuthenticationManager inicializado con cache TTL: {} ms", tokenCacheTTL);
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE AUTENTICACIÓN OAUTH2
  // =================================================================================

  @Override
  public String getClientCredentialsToken() throws FrameworkBusinessException {
    if (defaultTokenEndpoint == null || defaultTokenEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken",
          "Default token endpoint no configurado. Use setDefaultTokenEndpoint() primero.");
    }
    return getClientCredentialsToken(defaultTokenEndpoint);
  }

  @Override
  public String getClientCredentialsToken(String tokenEndpoint) throws FrameworkBusinessException {
    if (defaultClientId == null || defaultClientSecret == null) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken",
          "Credenciales por defecto no configuradas. Use setDefaultClientCredentials() primero.");
    }
    return getClientCredentialsToken(tokenEndpoint, defaultClientId, defaultClientSecret);
  }

  @Override
  public String getClientCredentialsToken(String clientId, String clientSecret)
      throws FrameworkBusinessException {
    if (defaultTokenEndpoint == null || defaultTokenEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken",
          "Default token endpoint no configurado. Use setDefaultTokenEndpoint() primero.");
    }
    return getClientCredentialsToken(defaultTokenEndpoint, clientId, clientSecret);
  }

  @Override
  public String getClientCredentialsToken(
      String tokenEndpoint, String clientId, String clientSecret)
      throws FrameworkBusinessException {

    validateParameters(tokenEndpoint, "tokenEndpoint");
    validateParameters(clientId, "clientId");
    validateParameters(clientSecret, "clientSecret");

    String cacheKey = "client_credentials:" + tokenEndpoint + ":" + clientId;

    // Verificar cache si está habilitado
    if (cacheEnabled) {
      TokenCacheEntry cached = tokenCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
        LOG.debug(
            "Token Client Credentials obtenido del cache para clientId: {}",
            TextUtilities.sanitizeValue("clientId", clientId));
        return cached.getToken();
      }
    }

    try {
      LOG.info(
          "Obteniendo token Client Credentials desde: {} para clientId: {}",
          tokenEndpoint,
          TextUtilities.sanitizeValue("clientId", clientId));

      // Limpiar estado anterior del cliente HTTP
      httpClient.clearRequestData();

      // Configurar autenticación básica
      String credentials =
          Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
      httpClient.addHeader("Authorization", "Basic " + credentials);
      httpClient.configureForFormData();
      httpClient.setBody("grant_type=client_credentials");

      HttpResponse response = httpClient.executeRequest(HttpMethod.POST, tokenEndpoint);

      if (response.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
        throw new FrameworkBusinessException(
            "getClientCredentialsToken",
            String.format(
                "Error obteniendo token Client Credentials. Status: %d, Response: %s",
                response.getStatusCode(), TextUtilities.truncateContent(response.getBody(), 500)));
      }

      if (tokenResponseKey == null || tokenResponseKey.trim().isEmpty()) {
        throw new FrameworkBusinessException(
            "getClientCredentialsToken",
            "Token response key no configurado. Use setTokenResponseKey() primero.");
      }

      String token = extractTokenFromResponse(response.getBody(), tokenResponseKey);

      // Cachear token si el cache está habilitado
      if (cacheEnabled) {
        tokenCache.put(cacheKey, new TokenCacheEntry(token, tokenCacheTTL));
      }

      LOG.info(
          "✓ Token Client Credentials obtenido exitosamente para clientId: {}",
          TextUtilities.sanitizeValue("clientId", clientId));
      return token;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken", "Error técnico obteniendo token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken",
          "Error obteniendo token Client Credentials: " + e.getMessage());
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE BEARER TOKENS
  // =================================================================================

  @Override
  public String getBearerTokenForIdentifier(String identifier) throws FrameworkBusinessException {
    if (defaultBearerTokenEndpoint == null || defaultBearerTokenEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "getBearerTokenForIdentifier",
          "Default bearer token endpoint no configurado. Use setDefaultBearerTokenEndpoint() primero.");
    }
    return getBearerTokenForIdentifier(defaultBearerTokenEndpoint, identifier);
  }

  @Override
  public String getBearerTokenForIdentifier(String endpoint, String identifier)
      throws FrameworkBusinessException {
    validateParameters(endpoint, "endpoint");
    validateParameters(identifier, "identifier");

    String processedIdentifier = ExecutionContext.current().map(ctx -> ctx.variables().resolve(identifier)).
        orElse(identifier);
    String cacheKey = "bearer_token:" + endpoint + ":" + processedIdentifier;

    // Verificar cache si está habilitado
    if (cacheEnabled) {
      TokenCacheEntry cached = tokenCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
        LOG.debug(
            "Token Bearer obtenido del cache para identifier: {}",
            TextUtilities.sanitizeValue("identifier", processedIdentifier));
        return cached.getToken();
      }
    }

    try {
      LOG.info(
          "Obteniendo token Bearer desde: {} para identifier: {}",
          endpoint,
          TextUtilities.sanitizeValue("identifier", processedIdentifier));

      httpClient.clearRequestData();
      String fullEndpoint = endpoint + "/" + processedIdentifier;
      HttpResponse response = httpClient.executeRequest(HttpMethod.GET, fullEndpoint);

      if (response.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
        throw new FrameworkBusinessException(
            "getBearerTokenForIdentifier",
            String.format(
                "Error obteniendo token Bearer. Status: %d, Response: %s",
                response.getStatusCode(), TextUtilities.truncateContent(response.getBody(), 500)));
      }

      if (tokenResponseKey == null || tokenResponseKey.trim().isEmpty()) {
        throw new FrameworkBusinessException(
            "getBearerTokenForIdentifier",
            "Token response key no configurado. Use setTokenResponseKey() primero.");
      }

      String token = extractTokenFromResponse(response.getBody(), tokenResponseKey);

      // Cachear token si el cache está habilitado
      if (cacheEnabled) {
        tokenCache.put(cacheKey, new TokenCacheEntry(token, tokenCacheTTL));
      }

      LOG.info(
          "✓ Token Bearer obtenido exitosamente para identifier: {}",
          TextUtilities.sanitizeValue("identifier", processedIdentifier));
      return token;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "getBearerTokenForIdentifier", "Error técnico obteniendo token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "getBearerTokenForIdentifier", "Error obteniendo token Bearer: " + e.getMessage());
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE AUTENTICACIÓN BÁSICA
  // =================================================================================

  @Override
  public String getBasicAuthToken(String username, String password)
      throws FrameworkBusinessException {
    if (defaultBasicAuthEndpoint == null || defaultBasicAuthEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "getBasicAuthToken",
          "Default basic auth endpoint no configurado. Use setDefaultBasicAuthEndpoint() primero.");
    }
    return getBasicAuthToken(defaultBasicAuthEndpoint, username, password);
  }

  @Override
  public String getBasicAuthToken(String authEndpoint, String username, String password)
      throws FrameworkBusinessException {
    validateParameters(authEndpoint, "authEndpoint");
    validateParameters(username, "username");
    validateParameters(password, "password");

    String cacheKey = "basic_auth:" + authEndpoint + ":" + username;

    if (cacheEnabled) {
      TokenCacheEntry cached = tokenCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
        LOG.debug("Token Basic Auth obtenido del cache para usuario: {}",
            TextUtilities.sanitizeValue("username", username));
        return cached.getToken();
      }
    }

    try {
      LOG.info("Obteniendo token Basic Auth desde: {} para usuario: {}",
          authEndpoint, TextUtilities.sanitizeValue("username", username));

      httpClient.clearRequestData();
      httpClient.configureForJson();
      prepareBasicAuthBody(username, password);

      HttpResponse response = httpClient.executeRequest(HttpMethod.POST, authEndpoint);
      validateTokenResponse(response, "getBasicAuthToken",
          "Error obteniendo token Basic Auth");
      String token = extractTokenFromResponse(response.getBody(), tokenResponseKey);

      if (cacheEnabled) {
        tokenCache.put(cacheKey, new TokenCacheEntry(token, tokenCacheTTL));
      }
      LOG.info("✓ Token Basic Auth obtenido exitosamente para usuario: {}",
          TextUtilities.sanitizeValue("username", username));
      return token;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "getBasicAuthToken", "Error técnico obteniendo token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "getBasicAuthToken", "Error obteniendo token Basic Auth: " + e.getMessage());
    }
  }

  /**
   * Validates that username/password field names are configured and sets the JSON body.
   *
   * @param username plain-text username
   * @param password plain-text password (sanitized before embedding)
   * @throws FrameworkBusinessException if field name configuration is missing
   */
  private void prepareBasicAuthBody(String username, String password)
      throws FrameworkBusinessException {
    if (usernameFieldName == null || usernameFieldName.trim().isEmpty()) {
      throw new FrameworkBusinessException("getBasicAuthToken",
          "Username field name no configurado. Use setUsernameFieldName() primero.");
    }
    if (passwordFieldName == null || passwordFieldName.trim().isEmpty()) {
      throw new FrameworkBusinessException("getBasicAuthToken",
          "Password field name no configurado. Use setPasswordFieldName() primero.");
    }
    String body = String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}",
        usernameFieldName, username,
        passwordFieldName, TextUtilities.sanitizeValue("password", password));
    httpClient.setBody(body);
  }

  /**
   * Validates an HTTP token-endpoint response: checks status code and token response key.
   *
   * @param response     HTTP response from the token endpoint
   * @param operation    name of the calling operation (for error messages)
   * @param errorMessage human-readable prefix for the status-code error
   * @throws FrameworkBusinessException if the status indicates an error or the key is missing
   */
  private void validateTokenResponse(HttpResponse response, String operation,
      String errorMessage) throws FrameworkBusinessException {
    if (response.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
      throw new FrameworkBusinessException(operation,
          String.format("%s. Status: %d, Response: %s", errorMessage,
              response.getStatusCode(),
              TextUtilities.truncateContent(response.getBody(), 500)));
    }
    if (tokenResponseKey == null || tokenResponseKey.trim().isEmpty()) {
      throw new FrameworkBusinessException(operation,
          "Token response key no configurado. Use setTokenResponseKey() primero.");
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE TOKENS PERSONALIZADOS
  // =================================================================================

  @Override
  public String getCustomToken(String tokenType, Map<String, String> parameters)
      throws FrameworkBusinessException {
    validateParameters(tokenType, "tokenType");
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters no puede ser null");
    }

    String cacheKey = "custom_token:" + tokenType + ":" + parameters.hashCode();

    // Verificar cache si está habilitado
    if (cacheEnabled) {
      TokenCacheEntry cached = tokenCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
        LOG.debug("Token personalizado obtenido del cache para tipo: {}", tokenType);
        return cached.getToken();
      }
    }

    try {
      LOG.info("Obteniendo token personalizado de tipo: {}", tokenType);

      httpClient.clearRequestData();
      httpClient.configureForJson();

      if (defaultCustomTokenEndpoint == null || defaultCustomTokenEndpoint.trim().isEmpty()) {
        throw new FrameworkBusinessException(
            "getCustomToken",
            "Default custom token endpoint no configurado. Use setDefaultCustomTokenEndpoint() primero.");
      }

      // Construir body con los parámetros
      StringBuilder bodyBuilder =
          new StringBuilder("{\"tokenType\":\"").append(tokenType).append("\"");
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        bodyBuilder.append(",\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
      }
      bodyBuilder.append("}");

      httpClient.setBody(bodyBuilder.toString());
      HttpResponse response =
          httpClient.executeRequest(HttpMethod.POST, defaultCustomTokenEndpoint);

      if (response.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
        throw new FrameworkBusinessException(
            "getCustomToken",
            String.format(
                "Error obteniendo token personalizado. Status: %d, Response: %s",
                response.getStatusCode(), TextUtilities.truncateContent(response.getBody(), 500)));
      }

      if (tokenResponseKey == null || tokenResponseKey.trim().isEmpty()) {
        throw new FrameworkBusinessException(
            "getCustomToken",
            "Token response key no configurado. Use setTokenResponseKey() primero.");
      }

      String token = extractTokenFromResponse(response.getBody(), tokenResponseKey);

      // Cachear token si el cache está habilitado
      if (cacheEnabled) {
        tokenCache.put(cacheKey, new TokenCacheEntry(token, tokenCacheTTL));
      }

      LOG.info("✓ Token personalizado obtenido exitosamente para tipo: {}", tokenType);
      return token;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "getCustomToken", "Error técnico obteniendo token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "getCustomToken", "Error obteniendo token personalizado: " + e.getMessage());
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE GESTIÓN DE CACHE
  // =================================================================================

  @Override
  public void setTokenCacheTTL(long ttlMilliseconds) {
    if (ttlMilliseconds <= 0) {
      throw new IllegalArgumentException("TTL debe ser mayor a 0: " + ttlMilliseconds);
    }
    this.tokenCacheTTL = ttlMilliseconds;
    LOG.debug("Token cache TTL establecido: {} ms", ttlMilliseconds);
  }

  @Override
  public long getTokenCacheTTL() {
    return tokenCacheTTL;
  }

  @Override
  public void clearTokenCache() {
    int size = tokenCache.size();
    tokenCache.clear();
    LOG.debug("Cache de tokens limpiado. {} tokens eliminados", size);
  }

  @Override
  public void clearTokenFromCache(String tokenKey) {
    if (tokenKey == null || tokenKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Token key no puede ser null o vacío");
    }
    TokenCacheEntry removed = tokenCache.remove(tokenKey);
    if (removed != null) {
      LOG.debug("Token eliminado del cache: {}", tokenKey);
    }
  }

  @Override
  public boolean isTokenCached(String tokenKey) {
    if (tokenKey == null || tokenKey.trim().isEmpty()) {
      return false;
    }
    TokenCacheEntry cached = tokenCache.get(tokenKey);
    return cached != null && !cached.isExpired();
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE CONFIGURACIÓN
  // =================================================================================

  @Override
  public void setDefaultClientCredentials(String clientId, String clientSecret) {
    validateParameters(clientId, "clientId");
    validateParameters(clientSecret, "clientSecret");
    this.defaultClientId = clientId;
    this.defaultClientSecret = clientSecret;
    LOG.debug(
        "Credenciales por defecto configuradas para clientId: {}",
        TextUtilities.sanitizeValue("clientId", clientId));
  }

  @Override
  public void setDefaultTokenEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultTokenEndpoint = endpoint;
    LOG.debug("Endpoint por defecto configurado: {}", endpoint);
  }

  @Override
  public void setCacheEnabled(boolean enabled) {
    this.cacheEnabled = enabled;
    LOG.debug("Cache de tokens {}", enabled ? "habilitado" : "deshabilitado");
    if (!enabled) {
      clearTokenCache();
    }
  }

  @Override
  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  // =================================================================================
  // MÉTODOS DE CONFIGURACIÓN ADICIONAL (Para frameworks específicos)
  // =================================================================================

  /**
   * Configura el endpoint por defecto para obtener Bearer tokens.
   *
   * @param endpoint la URL del endpoint para Bearer tokens
   */
  public void setDefaultBearerTokenEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultBearerTokenEndpoint = endpoint;
    LOG.debug("Endpoint por defecto para Bearer tokens configurado: {}", endpoint);
  }

  /**
   * Configura el endpoint por defecto para autenticación básica.
   *
   * @param endpoint la URL del endpoint para autenticación básica
   */
  public void setDefaultBasicAuthEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultBasicAuthEndpoint = endpoint;
    LOG.debug("Endpoint por defecto para autenticación básica configurado: {}", endpoint);
  }

  /**
   * Configura el endpoint por defecto para tokens personalizados.
   *
   * @param endpoint la URL del endpoint para tokens personalizados
   */
  public void setDefaultCustomTokenEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultCustomTokenEndpoint = endpoint;
    LOG.debug("Endpoint por defecto para tokens personalizados configurado: {}", endpoint);
  }

  /**
   * Configura la clave que se busca en las respuestas JSON para extraer el token. Por defecto es
   * "access_token", pero puede ser "token", "authToken", etc.
   *
   * @param tokenKey la clave del token en la respuesta JSON
   */
  public void setTokenResponseKey(String tokenKey) {
    validateParameters(tokenKey, "tokenKey");
    this.tokenResponseKey = tokenKey;
    LOG.debug("Clave de respuesta de token configurada: {}", tokenKey);
  }

  /**
   * Configura el nombre del campo de usuario en las peticiones de autenticación básica. Por defecto
   * es "username", pero puede ser "email", "user", etc.
   *
   * @param fieldName el nombre del campo de usuario
   */
  public void setUsernameFieldName(String fieldName) {
    validateParameters(fieldName, "fieldName");
    this.usernameFieldName = fieldName;
    LOG.debug("Nombre de campo de usuario configurado: {}", fieldName);
  }

  /**
   * Configura el nombre del campo de contraseña en las peticiones de autenticación básica. Por
   * defecto es "password", pero puede ser "pwd", "pass", etc.
   *
   * @param fieldName el nombre del campo de contraseña
   */
  public void setPasswordFieldName(String fieldName) {
    validateParameters(fieldName, "fieldName");
    this.passwordFieldName = fieldName;
    LOG.debug("Nombre de campo de contraseña configurado: {}", fieldName);
  }

  /**
   * Obtiene el endpoint por defecto para Bearer tokens.
   *
   * @return el endpoint configurado
   */
  public String getDefaultBearerTokenEndpoint() {
    return defaultBearerTokenEndpoint;
  }

  /**
   * Obtiene el endpoint por defecto para autenticación básica.
   *
   * @return el endpoint configurado
   */
  public String getDefaultBasicAuthEndpoint() {
    return defaultBasicAuthEndpoint;
  }

  /**
   * Obtiene el endpoint por defecto para tokens personalizados.
   *
   * @return el endpoint configurado
   */
  public String getDefaultCustomTokenEndpoint() {
    return defaultCustomTokenEndpoint;
  }

  /**
   * Obtiene la clave de respuesta de token configurada.
   *
   * @return la clave de token en respuestas JSON
   */
  public String getTokenResponseKey() {
    return tokenResponseKey;
  }

  /**
   * Obtiene el nombre de campo de usuario configurado.
   *
   * @return el nombre del campo de usuario
   */
  public String getUsernameFieldName() {
    return usernameFieldName;
  }

  /**
   * Obtiene el nombre de campo de contraseña configurado.
   *
   * @return el nombre del campo de contraseña
   */
  public String getPasswordFieldName() {
    return passwordFieldName;
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE INFORMACIÓN Y DEBUG
  // =================================================================================

  @Override
  public String getDebugInfo() {
    StringBuilder debug = new StringBuilder("BaseAuthenticationService Debug Info:\n");
    debug.append("Cache Enabled: ").append(cacheEnabled).append("\n");
    debug.append("Cache TTL: ").append(tokenCacheTTL).append(" ms\n");
    debug.append("Cached Tokens: ").append(tokenCache.size()).append("\n");
    debug.append("Default Token Endpoint: ").append(defaultTokenEndpoint).append("\n");
    debug.append("Default Bearer Token Endpoint: ").append(defaultBearerTokenEndpoint).append("\n");
    debug.append("Default Basic Auth Endpoint: ").append(defaultBasicAuthEndpoint).append("\n");
    debug.append("Default Custom Token Endpoint: ").append(defaultCustomTokenEndpoint).append("\n");
    debug.append("Token Response Key: ").append(tokenResponseKey).append("\n");
    debug.append("Username Field Name: ").append(usernameFieldName).append("\n");
    debug.append("Password Field Name: ").append(passwordFieldName).append("\n");
    debug.append("Default Client ID: ").append(defaultClientId != null ? "Configured" : "Not Configured").append("\n");
    debug.append("HTTP Client: ").append(httpClient.getClass().getSimpleName()).append("\n");

    // Información de tokens en cache (sin exponer los tokens)
    int expiredTokens = 0;
    for (TokenCacheEntry entry : tokenCache.values()) {
      if (entry.isExpired()) {
        expiredTokens++;
      }
    }
    debug.append("Expired Tokens in Cache: ").append(expiredTokens).append("\n");

    return debug.toString();
  }

  @Override
  public int getCachedTokensCount() {
    return tokenCache.size();
  }

  @Override
  public boolean isValidToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return false;
    }

    // Validación básica de formato
    String trimmedToken = token.trim();
    return trimmedToken.length() >= 10 && !trimmedToken.contains(" ");
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // =================================================================================

  private void validateParameters(String value, String paramName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(paramName + " no puede ser null o vacío");
    }
  }

  private String extractTokenFromResponse(String responseBody, String tokenKey)
      throws FrameworkBusinessException {
    try {
      String token = JsonUtilities.getToken(responseBody, tokenKey);
      if (token == null || token.trim().isEmpty()) {
        throw new FrameworkBusinessException(
            "extractTokenFromResponse",
            "Token no encontrado en la respuesta con clave: " + tokenKey);
      }
      return token;
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "extractTokenFromResponse", "Error extrayendo token de la respuesta: " + e.getMessage());
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE REFRESH TOKENS (ALTA PRIORIDAD)
  // =================================================================================

  @Override
  public String refreshAccessToken(String refreshToken) throws FrameworkBusinessException {
    if (defaultRefreshTokenEndpoint == null || defaultRefreshTokenEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "refreshAccessToken",
          "Default refresh token endpoint no configurado. Use setDefaultRefreshTokenEndpoint() primero.");
    }
    return refreshAccessToken(refreshToken, defaultRefreshTokenEndpoint);
  }

  @Override
  public String refreshAccessToken(String refreshToken, String endpoint)
      throws FrameworkBusinessException {
    validateParameters(refreshToken, "refreshToken");
    validateParameters(endpoint, "endpoint");

    String cacheKey = "refresh_token:" + endpoint + ":" + refreshToken.hashCode();

    try {
      LOG.info("Renovando token de acceso usando refresh token en: {}", endpoint);

      httpClient.clearRequestData();
      httpClient.configureForFormData();

      // Configurar body para refresh token
      httpClient.setBody("grant_type=refresh_token&refresh_token=" + refreshToken);

      HttpResponse response = httpClient.executeRequest(HttpMethod.POST, endpoint);

      if (response.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
        throw new FrameworkBusinessException(
            "refreshAccessToken",
            String.format(
                "Error renovando token. Status: %d, Response: %s",
                response.getStatusCode(), TextUtilities.truncateContent(response.getBody(), 500)));
      }

      if (tokenResponseKey == null || tokenResponseKey.trim().isEmpty()) {
        throw new FrameworkBusinessException(
            "refreshAccessToken",
            "Token response key no configurado. Use setTokenResponseKey() primero.");
      }

      String newToken = extractTokenFromResponse(response.getBody(), tokenResponseKey);

      // Cachear nuevo token si el cache está habilitado
      if (cacheEnabled) {
        tokenCache.put(cacheKey, new TokenCacheEntry(newToken, tokenCacheTTL));
      }

      LOG.info("✓ Token renovado exitosamente");
      return newToken;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "refreshAccessToken", "Error técnico renovando token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "refreshAccessToken", "Error renovando token: " + e.getMessage());
    }
  }

  @Override
  public void setDefaultRefreshTokenEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultRefreshTokenEndpoint = endpoint;
    LOG.debug("Endpoint por defecto para refresh token configurado: {}", endpoint);
  }

  @Override
  public String getDefaultRefreshTokenEndpoint() {
    return defaultRefreshTokenEndpoint;
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE JWT TOKEN MANAGEMENT (ALTA PRIORIDAD)
  // =================================================================================

  @Override
  public boolean isJWTTokenExpired(String jwtToken) throws FrameworkBusinessException {
    try {
      Instant expiration = extractJWTExpiration(jwtToken);
      return expiration != null && Instant.now().isAfter(expiration);
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "isJWTTokenExpired", "Error verificando expiración del JWT: " + e.getMessage());
    }
  }

  @Override
  public Map<String, Object> decodeJWTClaims(String jwtToken) throws FrameworkBusinessException {
    try {
      validateParameters(jwtToken, "jwtToken");

      // Decodificar JWT básico (sin validación de firma)
      String[] parts = jwtToken.split("\\.");
      if (parts.length != 3) {
        throw new FrameworkBusinessException(
            "decodeJWTClaims", "Formato de JWT inválido. Debe tener 3 partes separadas por '.'");
      }

      // Decodificar payload (segunda parte)
      String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      return JsonUtilities.jsonToMap(payloadJson);

    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "decodeJWTClaims", "Error decodificando claims del JWT: " + e.getMessage());
    }
  }

  @Override
  public boolean validateJWTSignature(String jwtToken, String secret)
      throws FrameworkBusinessException {
    try {
      validateParameters(jwtToken, "jwtToken");
      validateParameters(secret, "secret");

      // Implementación básica de validación JWT
      // NOTA: En producción se recomienda usar una librería como JJWT o Auth0 JWT
      String[] parts = jwtToken.split("\\.");
      if (parts.length != 3) {
        return false;
      }

      // Por ahora, implementación básica que siempre retorna true
      // Los frameworks específicos pueden sobrescribir esto con librerías robustas
      LOG.warn(
          "validateJWTSignature: Implementación básica activa. "
              + "Se recomienda usar librería especializada en frameworks específicos.");
      return true;

    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "validateJWTSignature", "Error validando firma del JWT: " + e.getMessage());
    }
  }

  @Override
  public String extractJWTSubject(String jwtToken) throws FrameworkBusinessException {
    try {
      Map<String, Object> claims = decodeJWTClaims(jwtToken);
      return (String) claims.get("sub");
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "extractJWTSubject", "Error extrayendo subject del JWT: " + e.getMessage());
    }
  }

  @Override
  public Instant extractJWTExpiration(String jwtToken) throws FrameworkBusinessException {
    try {
      Map<String, Object> claims = decodeJWTClaims(jwtToken);
      Object exp = claims.get("exp");
      if (exp instanceof Number) {
        return Instant.ofEpochSecond(((Number) exp).longValue());
      }
      return null;
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "extractJWTExpiration", "Error extrayendo expiración del JWT: " + e.getMessage());
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE API KEY MANAGEMENT (ALTA PRIORIDAD)
  // =================================================================================

  @Override
  public void setDefaultApiKey(String apiKey) {
    this.defaultApiKey = apiKey;
    LOG.debug("API key por defecto configurada: {}", TextUtilities.sanitizeValue("apiKey", apiKey));
  }

  @Override
  public String getDefaultApiKey() {
    return defaultApiKey;
  }

  @Override
  public void setApiKeyHeader(String headerName) {
    validateParameters(headerName, "headerName");
    this.apiKeyHeader = headerName;
    LOG.debug("Header para API key configurado: {}", headerName);
  }

  @Override
  public String getApiKeyHeader() {
    return apiKeyHeader;
  }

  @Override
  public boolean validateApiKeyFormat(String apiKey) {
    if (apiKey == null) {
      return false;
    }

    // Validar longitud
    if (apiKeyMinLength > 0 && apiKey.length() < apiKeyMinLength) {
      return false;
    }
    if (apiKeyMaxLength > 0 && apiKey.length() > apiKeyMaxLength) {
      return false;
    }

    // Validar patrón si está configurado
    if (apiKeyAllowedPattern != null && !apiKeyAllowedPattern.trim().isEmpty()) {
      return apiKey.matches(apiKeyAllowedPattern);
    }

    return true;
  }

  @Override
  public void setApiKeyValidationRules(int minLength, int maxLength, String allowedPattern) {
    this.apiKeyMinLength = minLength;
    this.apiKeyMaxLength = maxLength;
    this.apiKeyAllowedPattern = allowedPattern;
    LOG.debug(
        "Reglas de validación API key configuradas: min={}, max={}, pattern={}",
        minLength,
        maxLength,
        allowedPattern);
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE SCOPES MANAGEMENT (ALTA PRIORIDAD)
  // =================================================================================

  @Override
  public String getClientCredentialsToken(String[] scopes) throws FrameworkBusinessException {
    if (defaultTokenEndpoint == null || defaultTokenEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken",
          "Default token endpoint no configurado. Use setDefaultTokenEndpoint() primero.");
    }
    if (defaultClientId == null || defaultClientSecret == null) {
      throw new FrameworkBusinessException(
          "getClientCredentialsToken",
          "Credenciales por defecto no configuradas. Use setDefaultClientCredentials() primero.");
    }
    return getClientCredentialsToken(
        defaultTokenEndpoint, defaultClientId, defaultClientSecret, scopes);
  }

  @Override
  public String getClientCredentialsToken(
      String endpoint, String clientId, String clientSecret, String[] scopes)
      throws FrameworkBusinessException {

    validateParameters(endpoint, "endpoint");
    validateParameters(clientId, "clientId");
    validateParameters(clientSecret, "clientSecret");

    String scopesKey = scopes != null && scopes.length > 0 ? String.join(" ", scopes) : "";
    String cacheKey =
        "client_credentials_scopes:" + endpoint + ":" + clientId + ":" + scopesKey.hashCode();

    if (cacheEnabled) {
      TokenCacheEntry cached = tokenCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
        LOG.debug("Token Client Credentials con scopes obtenido del cache para clientId: {}",
            TextUtilities.sanitizeValue("clientId", clientId));
        return cached.getToken();
      }
    }

    try {
      LOG.info("Obteniendo token Client Credentials con scopes desde: {} para clientId: {}",
          endpoint, TextUtilities.sanitizeValue("clientId", clientId));

      String token = executeClientCredentialsWithScopes(endpoint, clientId, clientSecret, scopes);

      if (cacheEnabled) {
        tokenCache.put(cacheKey, new TokenCacheEntry(token, tokenCacheTTL));
      }
      LOG.info("✓ Token Client Credentials con scopes obtenido exitosamente para clientId: {}",
          TextUtilities.sanitizeValue("clientId", clientId));
      return token;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException("getClientCredentialsToken",
          "Error técnico obteniendo token con scopes: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException("getClientCredentialsToken",
          "Error obteniendo token con scopes: " + e.getMessage());
    }
  }

  /**
   * Executes the OAuth2 client_credentials flow with optional scopes.
   *
   * @param endpoint     token endpoint URL
   * @param clientId     OAuth2 client ID
   * @param clientSecret OAuth2 client secret
   * @param scopes       optional scopes array (may be null or empty)
   * @return extracted token string
   * @throws FrameworkBusinessException  on HTTP error or misconfiguration
   * @throws FrameworkTechnicalException on underlying HTTP failure
   */
  private String executeClientCredentialsWithScopes(
      String endpoint, String clientId, String clientSecret, String[] scopes)
      throws FrameworkBusinessException, FrameworkTechnicalException {
    httpClient.clearRequestData();
    String credentials =
        Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    httpClient.addHeader("Authorization", "Basic " + credentials);
    httpClient.configureForFormData();

    StringBuilder bodyBuilder = new StringBuilder("grant_type=client_credentials");
    if (scopes != null && scopes.length > 0) {
      bodyBuilder.append("&scope=").append(String.join(" ", scopes));
    }
    httpClient.setBody(bodyBuilder.toString());

    HttpResponse response = httpClient.executeRequest(HttpMethod.POST, endpoint);
    validateTokenResponse(response, "getClientCredentialsToken",
        "Error obteniendo token con scopes");
    return extractTokenFromResponse(response.getBody(), tokenResponseKey);
  }

  @Override
  public void setDefaultScopes(String[] scopes) {
    this.defaultScopes = scopes != null ? scopes.clone() : null;
    LOG.debug(
        "Scopes por defecto configurados: {}", scopes != null ? String.join(", ", scopes) : "null");
  }

  @Override
  public String[] getDefaultScopes() {
    return defaultScopes != null ? defaultScopes.clone() : null;
  }

  @Override
  public boolean tokenHasScope(String token, String scope) throws FrameworkBusinessException {
    try {
      validateParameters(token, "token");
      validateParameters(scope, "scope");

      // Intentar decodificar como JWT primero
      try {
        Map<String, Object> claims = decodeJWTClaims(token);
        Object scopesObj = claims.get("scope");
        if (scopesObj instanceof String) {
          String[] tokenScopes = ((String) scopesObj).split("\\s+");
          for (String tokenScope : tokenScopes) {
            if (scope.equals(tokenScope)) {
              return true;
            }
          }
        }
        return false;
      } catch (Exception e) {
        // Si no es JWT, usar introspección si está disponible
        if (defaultIntrospectionEndpoint != null) {
          Map<String, Object> introspection = introspectToken(token);
          Object scopesObj = introspection.get("scope");
          if (scopesObj instanceof String) {
            String[] tokenScopes = ((String) scopesObj).split("\\s+");
            for (String tokenScope : tokenScopes) {
              if (scope.equals(tokenScope)) {
                return true;
              }
            }
          }
        }
        return false;
      }
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "tokenHasScope", "Error verificando scope en token: " + e.getMessage());
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE TOKEN INTROSPECTION (MEDIA PRIORIDAD)
  // =================================================================================

  @Override
  public Map<String, Object> introspectToken(String token) throws FrameworkBusinessException {
    if (defaultIntrospectionEndpoint == null || defaultIntrospectionEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "introspectToken",
          "Default introspection endpoint no configurado. Use setDefaultIntrospectionEndpoint() primero.");
    }

    try {
      validateParameters(token, "token");

      LOG.info("Realizando introspección de token en: {}", defaultIntrospectionEndpoint);

      httpClient.clearRequestData();
      httpClient.configureForFormData();

      // Configurar autenticación para introspección si está disponible
      if (introspectionClientId != null && introspectionClientSecret != null) {
        String credentials =
            Base64.getEncoder().encodeToString(
                    (introspectionClientId + ":" + introspectionClientSecret).getBytes(StandardCharsets.UTF_8));
        httpClient.addHeader("Authorization", "Basic " + credentials);
      }

      // Body para introspección OAuth2
      httpClient.setBody("token=" + token + "&token_type_hint=access_token");

      HttpResponse response =
          httpClient.executeRequest(HttpMethod.POST, defaultIntrospectionEndpoint);

      if (response.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
        throw new FrameworkBusinessException(
            "introspectToken",
            String.format(
                "Error en introspección de token. Status: %d, Response: %s",
                response.getStatusCode(), TextUtilities.truncateContent(response.getBody(), 500)));
      }

      Map<String, Object> introspectionResult = JsonUtilities.jsonToMap(response.getBody());
      LOG.debug("✓ Introspección de token completada exitosamente");
      return introspectionResult;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "introspectToken", "Error técnico en introspección de token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "introspectToken", "Error realizando introspección de token: " + e.getMessage());
    }
  }

  @Override
  public boolean isTokenActive(String token) throws FrameworkBusinessException {
    try {
      Map<String, Object> introspection = introspectToken(token);
      Object active = introspection.get("active");
      return Boolean.TRUE.equals(active);
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "isTokenActive", "Error verificando si el token está activo: " + e.getMessage());
    }
  }

  @Override
  public void setDefaultIntrospectionEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultIntrospectionEndpoint = endpoint;
    LOG.debug("Endpoint por defecto para introspección configurado: {}", endpoint);
  }

  @Override
  public String getDefaultIntrospectionEndpoint() {
    return defaultIntrospectionEndpoint;
  }

  /**
   * Configura las credenciales para el cliente de introspección. Estas credenciales se usan para
   * autenticarse en el endpoint de introspección.
   *
   * @param clientId el ID del cliente para introspección
   * @param clientSecret el secret del cliente para introspección
   */
  public void setIntrospectionClientCredentials(String clientId, String clientSecret) {
    validateParameters(clientId, "clientId");
    validateParameters(clientSecret, "clientSecret");
    this.introspectionClientId = clientId;
    this.introspectionClientSecret = clientSecret;
    LOG.debug(
        "Credenciales de introspección configuradas para clientId: {}",
        TextUtilities.sanitizeValue("clientId", clientId));
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE TOKEN REVOCATION (MEDIA PRIORIDAD)
  // =================================================================================

  @Override
  public boolean revokeToken(String token) throws FrameworkBusinessException {
    if (defaultRevocationEndpoint == null || defaultRevocationEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "revokeToken",
          "Default revocation endpoint no configurado. Use setDefaultRevocationEndpoint() primero.");
    }

    try {
      validateParameters(token, "token");

      LOG.info("Revocando token de acceso en: {}", defaultRevocationEndpoint);

      httpClient.clearRequestData();
      httpClient.configureForFormData();

      // Configurar autenticación para revocación si está disponible
      if (introspectionClientId != null && introspectionClientSecret != null) {
        String credentials =
            Base64.getEncoder().encodeToString(
                    (introspectionClientId + ":" + introspectionClientSecret).getBytes(StandardCharsets.UTF_8));
        httpClient.addHeader("Authorization", "Basic " + credentials);
      }

      // Body para revocación OAuth2
      httpClient.setBody("token=" + token + "&token_type_hint=access_token");

      HttpResponse response = httpClient.executeRequest(HttpMethod.POST, defaultRevocationEndpoint);

      // RFC 7009: El servidor debe responder con 200 para revocación exitosa
      boolean success = response.getStatusCode() == 200 || response.getStatusCode() == HTTP_NO_CONTENT;

      if (success) {
        // Remover token del cache si existe
        String[] cacheKeys = tokenCache.keySet().toArray(new String[0]);
        for (String cacheKey : cacheKeys) {
          TokenCacheEntry entry = tokenCache.get(cacheKey);
          if (entry != null && token.equals(entry.getToken())) {
            tokenCache.remove(cacheKey);
            LOG.debug("Token revocado removido del cache: {}", cacheKey);
            break;
          }
        }
        LOG.info("✓ Token revocado exitosamente");
      } else {
        LOG.warn(
            "Revocación de token falló. Status: {}, Response: {}",
            response.getStatusCode(),
            TextUtilities.truncateContent(response.getBody(), 200));
      }

      return success;

    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "revokeToken", "Error técnico revocando token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "revokeToken", "Error revocando token: " + e.getMessage());
    }
  }

  @Override
  public boolean revokeRefreshToken(String refreshToken) throws FrameworkBusinessException {
    if (defaultRevocationEndpoint == null || defaultRevocationEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "revokeRefreshToken",
          "Default revocation endpoint no configurado. Use setDefaultRevocationEndpoint() primero.");
    }

    try {
      validateParameters(refreshToken, "refreshToken");

      LOG.info("Revocando refresh token en: {}", defaultRevocationEndpoint);

      httpClient.clearRequestData();
      httpClient.configureForFormData();

      // Configurar autenticación para revocación si está disponible
      if (introspectionClientId != null && introspectionClientSecret != null) {
        String credentials =
            Base64.getEncoder().encodeToString(
                    (introspectionClientId + ":" + introspectionClientSecret).getBytes(StandardCharsets.UTF_8));
        httpClient.addHeader("Authorization", "Basic " + credentials);
      }

      // Body para revocación de refresh token
      httpClient.setBody("token=" + refreshToken + "&token_type_hint=refresh_token");

      HttpResponse response = httpClient.executeRequest(HttpMethod.POST, defaultRevocationEndpoint);

      boolean success = response.getStatusCode() == 200 || response.getStatusCode() == HTTP_NO_CONTENT;

      if (success) {
        LOG.info("✓ Refresh token revocado exitosamente");
      } else {
        LOG.warn(
            "Revocación de refresh token falló. Status: {}, Response: {}",
            response.getStatusCode(),
            TextUtilities.truncateContent(response.getBody(), 200));
      }

      return success;

    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "revokeRefreshToken", "Error técnico revocando refresh token: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "revokeRefreshToken", "Error revocando refresh token: " + e.getMessage());
    }
  }

  @Override
  public void setDefaultRevocationEndpoint(String endpoint) {
    validateParameters(endpoint, "endpoint");
    this.defaultRevocationEndpoint = endpoint;
    LOG.debug("Endpoint por defecto para revocación configurado: {}", endpoint);
  }

  @Override
  public String getDefaultRevocationEndpoint() {
    return defaultRevocationEndpoint;
  }

  // =================================================================================
  // IMPLEMENTACIÓN DE MULTI-FACTOR AUTHENTICATION (MEDIA PRIORIDAD)
  // =================================================================================

  @Override
  public String getMFAToken(String username, String password, String mfaCode)
      throws FrameworkBusinessException {
    if (defaultBasicAuthEndpoint == null || defaultBasicAuthEndpoint.trim().isEmpty()) {
      throw new FrameworkBusinessException(
          "getMFAToken",
          "Default basic auth endpoint no configurado. Use setDefaultBasicAuthEndpoint() primero.");
    }
    return getMFAToken(defaultBasicAuthEndpoint, username, password, mfaCode);
  }

  @Override
  public String getMFAToken(String endpoint, String username, String password, String mfaCode)
      throws FrameworkBusinessException {
    validateParameters(endpoint, "endpoint");
    validateParameters(username, "username");
    validateParameters(password, "password");
    validateParameters(mfaCode, "mfaCode");

    String cacheKey = "mfa_auth:" + endpoint + ":" + username + ":" + mfaCode.hashCode();

    try {
      LOG.info("Obteniendo token MFA desde: {} para usuario: {}",
          endpoint, TextUtilities.sanitizeValue("username", username));

      httpClient.clearRequestData();
      httpClient.configureForJson();
      prepareMFABody(username, password, mfaCode);

      HttpResponse response = httpClient.executeRequest(HttpMethod.POST, endpoint);
      validateTokenResponse(response, "getMFAToken", "Error obteniendo token MFA");
      String token = extractTokenFromResponse(response.getBody(), tokenResponseKey);

      if (cacheEnabled) {
        long mfaTtl = Math.min(tokenCacheTTL, MFA_MAX_TTL_MS);
        tokenCache.put(cacheKey, new TokenCacheEntry(token, mfaTtl));
      }
      LOG.info("✓ Token MFA obtenido exitosamente para usuario: {}",
          TextUtilities.sanitizeValue("username", username));
      return token;

    } catch (FrameworkBusinessException e) {
      throw e;
    } catch (FrameworkTechnicalException e) {
      throw new FrameworkBusinessException(
          "getMFAToken", "Error técnico obteniendo token MFA: " + e.getMessage());
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "getMFAToken", "Error obteniendo token MFA: " + e.getMessage());
    }
  }

  /**
   * Validates MFA field name configuration and sets the JSON body for MFA authentication.
   *
   * @param username plain-text username
   * @param password plain-text password (sanitized before embedding)
   * @param mfaCode  one-time MFA code
   * @throws FrameworkBusinessException if any required field name is not configured
   */
  private void prepareMFABody(String username, String password, String mfaCode)
      throws FrameworkBusinessException {
    if (usernameFieldName == null || usernameFieldName.trim().isEmpty()) {
      throw new FrameworkBusinessException("getMFAToken",
          "Username field name no configurado. Use setUsernameFieldName() primero.");
    }
    if (passwordFieldName == null || passwordFieldName.trim().isEmpty()) {
      throw new FrameworkBusinessException("getMFAToken",
          "Password field name no configurado. Use setPasswordFieldName() primero.");
    }
    if (mfaFieldName == null || mfaFieldName.trim().isEmpty()) {
      throw new FrameworkBusinessException("getMFAToken",
          "MFA field name no configurado. Use setMFAFieldName() primero.");
    }
    String body = String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}",
        usernameFieldName, username,
        passwordFieldName, TextUtilities.sanitizeValue("password", password),
        mfaFieldName, mfaCode);
    httpClient.setBody(body);
  }

  @Override
  public void setMFAFieldName(String fieldName) {
    validateParameters(fieldName, "fieldName");
    this.mfaFieldName = fieldName;
    LOG.debug("Nombre de campo MFA configurado: {}", fieldName);
  }

  @Override
  public String getMFAFieldName() {
    return mfaFieldName;
  }

  // =================================================================================
  // CLASE INTERNA PARA CACHE DE TOKENS
  // =================================================================================

  /** Entrada del cache de tokens con tiempo de vida. */
  private static class TokenCacheEntry {
    private final String token;
    private final long expirationTime;

    TokenCacheEntry(String token, long ttlMilliseconds) {
      this.token = token;
      this.expirationTime = System.currentTimeMillis() + ttlMilliseconds;
    }

    public String getToken() {
      return token;
    }

    public boolean isExpired() {
      return System.currentTimeMillis() > expirationTime;
    }
  }
}
