package com.qa.httpcore.factories;

import com.qa.httpcore.implementations.BaseAuthenticationManager;
import com.qa.httpcore.interfaces.AuthenticationService;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.common.logging.TestLogger;

/**
 * Factory para crear instancias de AuthenticationService del QA Automation Framework.
 *
 * <p>Esta factory proporciona métodos estáticos para crear instancias de AuthenticationService
 * preconfiguradas y listas para usar. Facilita la creación de servicios de autenticación con
 * diferentes configuraciones según las necesidades del framework consumidor.
 *
 * <p><b>Características principales:</b>
 *
 * <ul>
 *   <li>Creación estandarizada de servicios de autenticación
 *   <li>Preconfiguración para diferentes tipos de autenticación
 *   <li>Integración automática con HttpClient
 *   <li>Soporte para múltiples proveedores de autenticación
 *   <li>Logging integrado para troubleshooting
 * </ul>
 *
 * <p><b>Uso típico:</b>
 *
 * <pre>
 * // Con HttpClient existente
 * HttpClient httpClient = HttpClientFactory.getInstance();
 * AuthenticationService authService = AuthenticationServiceFactory.getInstance(httpClient);
 *
 * // Con HttpClient automático
 * AuthenticationService authService = AuthenticationServiceFactory.getInstance();
 *
 * // Preconfigurado para OAuth2
 * AuthenticationService oauth2Service = AuthenticationServiceFactory.getOAuth2Instance(
 *     "client-id", "client-secret", "/oauth/token"
 * );
 * </pre>
 *
 * <p><b>Configuraciones disponibles:</b>
 *
 * <ul>
 *   <li><b>OAuth2 Client Credentials</b> - Para autenticación service-to-service
 *   <li><b>Bearer Token</b> - Para autenticación con tokens específicos
 *   <li><b>Basic Auth</b> - Para autenticación usuario/contraseña
 *   <li><b>API Key</b> - Para autenticación con claves de API
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2.0.0
 * @see AuthenticationService
 * @see BaseAuthenticationManager
 * @see HttpClientFactory
 */
public final class AuthenticationServiceFactory {

  private static final TestLogger.LoggerWrapper LOG =
      TestLogger.getLogger(AuthenticationServiceFactory.class);

  // Constructor privado para factory
  private AuthenticationServiceFactory() {
    throw new UnsupportedOperationException("AuthenticationServiceFactory es una clase factory");
  }

  /**
   * Crea una nueva instancia de AuthenticationService con HttpClient automático. Crea internamente
   * un HttpClient usando HttpClientFactory.
   *
   * @return nueva instancia de AuthenticationService
   */
  public static AuthenticationService getInstance() {
    LOG.debug("Creando AuthenticationService con HttpClient automático");
    HttpClient httpClient = HttpClientFactory.getInstance();
    return new BaseAuthenticationManager(httpClient);
  }

  /**
   * Crea una nueva instancia de AuthenticationService con HttpClient específico. Recomendado cuando
   * ya tienes un HttpClient configurado que quieres reutilizar.
   *
   * @param httpClient el cliente HTTP a utilizar
   * @return nueva instancia de AuthenticationService
   * @throws IllegalArgumentException si httpClient es null
   */
  public static AuthenticationService getInstance(HttpClient httpClient) {
    if (httpClient == null) {
      throw new IllegalArgumentException("HttpClient no puede ser null");
    }

    LOG.debug("Creando AuthenticationService con HttpClient proporcionado");
    return new BaseAuthenticationManager(httpClient);
  }

  /**
   * Crea una instancia preconfigurada para OAuth2 Client Credentials. Configura automáticamente las
   * credenciales del cliente y el endpoint de token.
   *
   * @param clientId identificador del cliente OAuth2
   * @param clientSecret secreto del cliente OAuth2
   * @param tokenEndpoint endpoint para obtener tokens (ej: "/oauth/token")
   * @return AuthenticationService configurado para OAuth2
   * @throws IllegalArgumentException si algún parámetro es null o vacío
   */
  public static AuthenticationService getOAuth2Instance(
      String clientId, String clientSecret, String tokenEndpoint) {
    validateParameter(clientId, "clientId");
    validateParameter(clientSecret, "clientSecret");
    validateParameter(tokenEndpoint, "tokenEndpoint");

    AuthenticationService authService = getInstance();
    authService.setDefaultClientCredentials(clientId, clientSecret);
    authService.setDefaultTokenEndpoint(tokenEndpoint);

    LOG.debug(
        "AuthenticationService configurado para OAuth2 - clientId: {}, endpoint: {}",
        clientId,
        tokenEndpoint);
    return authService;
  }

  /**
   * Crea una instancia preconfigurada para OAuth2 con scopes específicos. Incluye configuración de
   * scopes para control de permisos.
   *
   * @param clientId identificador del cliente OAuth2
   * @param clientSecret secreto del cliente OAuth2
   * @param tokenEndpoint endpoint para obtener tokens
   * @param scopes array de scopes OAuth2 (ej: ["read", "write"])
   * @return AuthenticationService configurado para OAuth2 con scopes
   * @throws IllegalArgumentException si algún parámetro es null o vacío
   */
  public static AuthenticationService getOAuth2InstanceWithScopes(
      String clientId, String clientSecret, String tokenEndpoint, String[] scopes) {
    AuthenticationService authService = getOAuth2Instance(clientId, clientSecret, tokenEndpoint);

    if (scopes != null && scopes.length > 0) {
      authService.setDefaultScopes(scopes);
      LOG.debug("Scopes configurados: {}", (Object) scopes);
    }

    return authService;
  }

  /**
   * Crea una instancia preconfigurada para Bearer Token. Configura el endpoint de refresh token
   * para bearer tokens.
   *
   * @param refreshTokenEndpoint endpoint para refresh tokens
   * @return AuthenticationService configurado para Bearer Token
   * @throws IllegalArgumentException si refreshTokenEndpoint es null o vacío
   */
  public static AuthenticationService getBearerTokenInstance(String refreshTokenEndpoint) {
    validateParameter(refreshTokenEndpoint, "refreshTokenEndpoint");

    AuthenticationService authService = getInstance();
    authService.setDefaultRefreshTokenEndpoint(refreshTokenEndpoint);

    LOG.debug(
        "AuthenticationService configurado para Bearer Token - refresh endpoint: {}",
        refreshTokenEndpoint);
    return authService;
  }

  /**
   * Crea una instancia preconfigurada para Basic Authentication. Nota: La interface actual no tiene
   * método específico para basic auth endpoint. Este método crea una instancia lista para usar con
   * getBasicAuthToken().
   *
   * @return AuthenticationService configurado para Basic Auth
   */
  public static AuthenticationService getBasicAuthInstance() {
    AuthenticationService authService = getInstance();

    LOG.debug("AuthenticationService configurado para Basic Auth");
    return authService;
  }

  /**
   * Crea una instancia preconfigurada para API Key authentication. Configura el header donde se
   * enviará la API Key.
   *
   * @param apiKeyHeader nombre del header para la API Key (ej: "X-API-Key")
   * @return AuthenticationService configurado para API Key
   * @throws IllegalArgumentException si apiKeyHeader es null o vacío
   */
  public static AuthenticationService getApiKeyInstance(String apiKeyHeader) {
    validateParameter(apiKeyHeader, "apiKeyHeader");

    AuthenticationService authService = getInstance();
    authService.setApiKeyHeader(apiKeyHeader);

    LOG.debug("AuthenticationService configurado para API Key - header: {}", apiKeyHeader);
    return authService;
  }

  /**
   * Crea una instancia completamente configurada para un entorno específico. Incluye todas las
   * configuraciones típicas para un ambiente de trabajo.
   *
   * @param config configuración del entorno de autenticación
   * @return AuthenticationService completamente configurado
   * @throws IllegalArgumentException si config es null o inválida
   */
  public static AuthenticationService getConfiguredInstance(AuthConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("AuthConfig no puede ser null");
    }

    AuthenticationService authService = getInstance();

    // Configurar según el tipo de autenticación
    switch (config.getAuthType().toLowerCase()) {
      case "oauth2":
        authService.setDefaultClientCredentials(config.getClientId(), config.getClientSecret());
        authService.setDefaultTokenEndpoint(config.getTokenEndpoint());
        if (config.getScopes() != null) {
          authService.setDefaultScopes(config.getScopes());
        }
        break;
      case "bearer":
        if (config.getRefreshTokenEndpoint() != null) {
          authService.setDefaultRefreshTokenEndpoint(config.getRefreshTokenEndpoint());
        }
        break;
      case "basic":
        // Basic auth no requiere configuración de endpoint específico
        LOG.debug("Basic auth configurado - usar getBasicAuthToken() con endpoint específico");
        break;
      case "apikey":
        authService.setApiKeyHeader(config.getApiKeyHeader());
        if (config.getApiKey() != null) {
          authService.setDefaultApiKey(config.getApiKey());
        }
        break;
      default:
        throw new IllegalArgumentException(
            "Tipo de autenticación no soportado: " + config.getAuthType());
    }

    // Configurar cache si está especificado
    if (config.isCacheEnabled()) {
      authService.setCacheEnabled(true);
    }

    LOG.debug(
        "AuthenticationService configurado completamente para tipo: {}", config.getAuthType());
    return authService;
  }

  /**
   * Obtiene información de debug sobre las capacidades del factory. Útil para troubleshooting y
   * verificación de configuración.
   *
   * @return string con información del factory
   */
  public static String getFactoryInfo() {
    return String.format(
        "AuthenticationServiceFactory v1.0.0 - Implementación: %s"
            + " - Tipos soportados: oauth2, bearer, basic, apikey",
        BaseAuthenticationManager.class.getSimpleName());
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // =================================================================================

  /** Valida que un parámetro no sea null o vacío. */
  private static void validateParameter(String parameter, String parameterName) {
    if (parameter == null || parameter.trim().isEmpty()) {
      throw new IllegalArgumentException(parameterName + " no puede ser null o vacío");
    }
  }

  // =================================================================================
  // CLASE INTERNA PARA CONFIGURACIÓN
  // =================================================================================

  /**
   * Clase de configuración para crear AuthenticationService completamente configurados. Patrón
   * Builder para facilitar la configuración de múltiples parámetros.
   */
  public static class AuthConfig {
    private String authType;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String refreshTokenEndpoint;
    private String apiKeyHeader;
    private String apiKey;
    private String[] scopes;
    private boolean cacheEnabled = true;

    public AuthConfig(String authType) {
      this.authType = authType;
    }

    // Builder methods
    public AuthConfig withOAuth2(String clientId, String clientSecret, String tokenEndpoint) {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
      this.tokenEndpoint = tokenEndpoint;
      return this;
    }

    public AuthConfig withScopes(String... scopes) {
      this.scopes = scopes;
      return this;
    }

    public AuthConfig withRefreshTokenEndpoint(String endpoint) {
      this.refreshTokenEndpoint = endpoint;
      return this;
    }

    public AuthConfig withApiKey(String apiKey, String header) {
      this.apiKey = apiKey;
      this.apiKeyHeader = header;
      return this;
    }

    public AuthConfig withCacheEnabled(boolean enabled) {
      this.cacheEnabled = enabled;
      return this;
    }

    // Getters
    public String getAuthType() {
      return authType;
    }

    public String getClientId() {
      return clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public String getTokenEndpoint() {
      return tokenEndpoint;
    }

    public String getRefreshTokenEndpoint() {
      return refreshTokenEndpoint;
    }

    public String getApiKeyHeader() {
      return apiKeyHeader;
    }

    public String getApiKey() {
      return apiKey;
    }

    public String[] getScopes() {
      return scopes;
    }

    public boolean isCacheEnabled() {
      return cacheEnabled;
    }
  }
}
