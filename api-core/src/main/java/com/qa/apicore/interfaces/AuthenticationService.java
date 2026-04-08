package com.qa.apicore.interfaces;

import com.qa.common.http.exceptions.FrameworkBusinessException;

/**
 * Interface para servicios de autenticación del QA Automation Framework.
 *
 * <p>Define el contrato para manejo unificado de autenticación y tokens. Soporta múltiples tipos de
 * autenticación con cache inteligente y gestión automática del ciclo de vida de tokens.
 *
 * <p>Tipos de autenticación soportados:
 *
 * <ul>
 *   <li>Client Credentials OAuth2 - Para autenticación service-to-service
 *   <li>Bearer Tokens - Para autenticación con tokens específicos
 *   <li>Basic Authentication - Para autenticación con usuario/contraseña
 *   <li>Custom Tokens - Para tokens personalizados del negocio
 * </ul>
 *
 * <p>Características principales:
 *
 * <ul>
 *   <li>Cache automático de tokens con TTL configurable
 *   <li>Renovación automática de tokens expirados
 *   <li>Soporte para múltiples proveedores de autenticación
 *   <li>Thread-safe para uso concurrente
 *   <li>Logging detallado para debugging
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public interface AuthenticationService {

  // =================================================================================
  // MÉTODOS DE AUTENTICACIÓN OAUTH2
  // =================================================================================

  /**
   * Obtiene un token OAuth2 usando el flujo Client Credentials. Utiliza el endpoint por defecto
   * /oauth/token.
   *
   * @return el token de acceso OAuth2
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getClientCredentialsToken() throws FrameworkBusinessException;

  /**
   * Obtiene un token OAuth2 usando el flujo Client Credentials con endpoint personalizado.
   *
   * @param tokenEndpoint la URL del endpoint para obtener el token
   * @return el token de acceso OAuth2
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getClientCredentialsToken(String tokenEndpoint) throws FrameworkBusinessException;

  /**
   * Obtiene un token OAuth2 usando credenciales específicas de cliente.
   *
   * @param clientId el ID del cliente OAuth2
   * @param clientSecret el secret del cliente OAuth2
   * @return el token de acceso OAuth2
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getClientCredentialsToken(String clientId, String clientSecret)
      throws FrameworkBusinessException;

  /**
   * Obtiene un token OAuth2 usando credenciales específicas y endpoint personalizado.
   *
   * @param tokenEndpoint la URL del endpoint para obtener el token
   * @param clientId el ID del cliente OAuth2
   * @param clientSecret el secret del cliente OAuth2
   * @return el token de acceso OAuth2
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getClientCredentialsToken(String tokenEndpoint, String clientId, String clientSecret)
      throws FrameworkBusinessException;

  // =================================================================================
  // MÉTODOS DE BEARER TOKENS
  // =================================================================================

  /**
   * Obtiene un token Bearer para un identificador específico (RUT, ID, etc.).
   *
   * @param identifier el identificador del usuario (ej: RUT)
   * @return el token Bearer
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getBearerTokenForIdentifier(String identifier) throws FrameworkBusinessException;

  /**
   * Obtiene un token Bearer usando endpoint personalizado.
   *
   * @param endpoint la URL del endpoint para obtener el token
   * @param identifier el identificador del usuario
   * @return el token Bearer
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getBearerTokenForIdentifier(String endpoint, String identifier)
      throws FrameworkBusinessException;

  // =================================================================================
  // MÉTODOS DE AUTENTICACIÓN BÁSICA
  // =================================================================================

  /**
   * Obtiene un token usando autenticación básica (usuario/contraseña).
   *
   * @param username el nombre de usuario
   * @param password la contraseña
   * @return el token de acceso
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getBasicAuthToken(String username, String password) throws FrameworkBusinessException;

  /**
   * Obtiene un token usando autenticación básica con endpoint personalizado.
   *
   * @param authEndpoint la URL del endpoint de autenticación
   * @param username el nombre de usuario
   * @param password la contraseña
   * @return el token de acceso
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getBasicAuthToken(String authEndpoint, String username, String password)
      throws FrameworkBusinessException;

  // =================================================================================
  // MÉTODOS DE TOKENS PERSONALIZADOS
  // =================================================================================

  /**
   * Obtiene un token personalizado usando parámetros específicos del negocio.
   *
   * @param tokenType el tipo de token personalizado
   * @param parameters los parámetros específicos para obtener el token
   * @return el token personalizado
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getCustomToken(String tokenType, java.util.Map<String, String> parameters)
      throws FrameworkBusinessException;

  // =================================================================================
  // MÉTODOS DE GESTIÓN DE CACHE
  // =================================================================================

  /**
   * Configura el tiempo de vida (TTL) para el cache de tokens.
   *
   * @param ttlMilliseconds el tiempo de vida en milisegundos
   */
  void setTokenCacheTTL(long ttlMilliseconds);

  /**
   * Obtiene el tiempo de vida actual del cache.
   *
   * @return el TTL en milisegundos
   */
  long getTokenCacheTTL();

  /** Limpia todos los tokens del cache. */
  void clearTokenCache();

  /**
   * Limpia un token específico del cache.
   *
   * @param tokenKey la clave del token en el cache
   */
  void clearTokenFromCache(String tokenKey);

  /**
   * Verifica si un token específico está en cache y no ha expirado.
   *
   * @param tokenKey la clave del token en el cache
   * @return true si el token está en cache y es válido
   */
  boolean isTokenCached(String tokenKey);

  // =================================================================================
  // MÉTODOS DE CONFIGURACIÓN
  // =================================================================================

  /**
   * Configura las credenciales por defecto para Client Credentials.
   *
   * @param clientId el ID del cliente por defecto
   * @param clientSecret el secret del cliente por defecto
   */
  void setDefaultClientCredentials(String clientId, String clientSecret);

  /**
   * Configura el endpoint por defecto para tokens OAuth2.
   *
   * @param endpoint la URL del endpoint por defecto
   */
  void setDefaultTokenEndpoint(String endpoint);

  /**
   * Habilita o deshabilita el cache de tokens.
   *
   * @param enabled true para habilitar el cache, false para deshabilitarlo
   */
  void setCacheEnabled(boolean enabled);

  /**
   * Verifica si el cache de tokens está habilitado.
   *
   * @return true si el cache está habilitado
   */
  boolean isCacheEnabled();

  // =================================================================================
  // MÉTODOS DE INFORMACIÓN Y DEBUG
  // =================================================================================

  /**
   * Obtiene información de debug sobre el estado del servicio de autenticación.
   *
   * @return información detallada para debugging
   */
  String getDebugInfo();

  /**
   * Obtiene el número de tokens actualmente en cache.
   *
   * @return el número de tokens en cache
   */
  int getCachedTokensCount();

  /**
   * Valida que un token sea válido (no null, no vacío, formato correcto).
   *
   * @param token el token a validar
   * @return true si el token es válido
   */
  boolean isValidToken(String token);

  // =================================================================================
  // MÉTODOS DE REFRESH TOKENS (ALTA PRIORIDAD)
  // =================================================================================

  /**
   * Renueva un token de acceso usando un refresh token. Utiliza el endpoint de refresh configurado
   * por defecto.
   *
   * @param refreshToken el refresh token para renovar el acceso
   * @return el nuevo token de acceso
   * @throws FrameworkBusinessException si hay problemas renovando el token
   */
  String refreshAccessToken(String refreshToken) throws FrameworkBusinessException;

  /**
   * Renueva un token de acceso usando un refresh token con endpoint específico.
   *
   * @param refreshToken el refresh token para renovar el acceso
   * @param endpoint la URL del endpoint para renovar el token
   * @return el nuevo token de acceso
   * @throws FrameworkBusinessException si hay problemas renovando el token
   */
  String refreshAccessToken(String refreshToken, String endpoint) throws FrameworkBusinessException;

  /**
   * Configura el endpoint por defecto para renovación de tokens.
   *
   * @param endpoint la URL del endpoint de refresh por defecto
   */
  void setDefaultRefreshTokenEndpoint(String endpoint);

  /**
   * Obtiene el endpoint por defecto para renovación de tokens.
   *
   * @return el endpoint configurado o null si no está configurado
   */
  String getDefaultRefreshTokenEndpoint();

  // =================================================================================
  // MÉTODOS DE JWT TOKEN MANAGEMENT (ALTA PRIORIDAD)
  // =================================================================================

  /**
   * Verifica si un JWT token ha expirado.
   *
   * @param jwtToken el JWT token a verificar
   * @return true si el token ha expirado
   * @throws FrameworkBusinessException si hay problemas validando el token
   */
  boolean isJWTTokenExpired(String jwtToken) throws FrameworkBusinessException;

  /**
   * Decodifica y obtiene los claims de un JWT token.
   *
   * @param jwtToken el JWT token a decodificar
   * @return un mapa con los claims del token
   * @throws FrameworkBusinessException si hay problemas decodificando el token
   */
  java.util.Map<String, Object> decodeJWTClaims(String jwtToken) throws FrameworkBusinessException;

  /**
   * Valida la firma de un JWT token usando un secreto.
   *
   * @param jwtToken el JWT token a validar
   * @param secret el secreto para validar la firma
   * @return true si la firma es válida
   * @throws FrameworkBusinessException si hay problemas validando la firma
   */
  boolean validateJWTSignature(String jwtToken, String secret) throws FrameworkBusinessException;

  /**
   * Extrae el claim 'subject' (sub) de un JWT token.
   *
   * @param jwtToken el JWT token del cual extraer el subject
   * @return el valor del subject o null si no existe
   * @throws FrameworkBusinessException si hay problemas extrayendo el claim
   */
  String extractJWTSubject(String jwtToken) throws FrameworkBusinessException;

  /**
   * Extrae la fecha de expiración de un JWT token.
   *
   * @param jwtToken el JWT token del cual extraer la expiración
   * @return la fecha de expiración o null si no existe
   * @throws FrameworkBusinessException si hay problemas extrayendo la fecha
   */
  java.time.Instant extractJWTExpiration(String jwtToken) throws FrameworkBusinessException;

  // =================================================================================
  // MÉTODOS DE API KEY MANAGEMENT (ALTA PRIORIDAD)
  // =================================================================================

  /**
   * Configura la API key por defecto.
   *
   * @param apiKey la API key a utilizar por defecto
   */
  void setDefaultApiKey(String apiKey);

  /**
   * Obtiene la API key por defecto configurada.
   *
   * @return la API key configurada o null si no está configurada
   */
  String getDefaultApiKey();

  /**
   * Configura el nombre del header donde se envía la API key.
   *
   * @param headerName el nombre del header (ej: "X-API-Key", "Authorization", etc.)
   */
  void setApiKeyHeader(String headerName);

  /**
   * Obtiene el nombre del header configurado para la API key.
   *
   * @return el nombre del header configurado
   */
  String getApiKeyHeader();

  /**
   * Valida si una API key tiene el formato correcto según reglas configuradas.
   *
   * @param apiKey la API key a validar
   * @return true si el formato es válido
   */
  boolean validateApiKeyFormat(String apiKey);

  /**
   * Configura las reglas de validación para el formato de API keys.
   *
   * @param minLength longitud mínima requerida
   * @param maxLength longitud máxima permitida
   * @param allowedPattern patrón regex permitido (opcional)
   */
  void setApiKeyValidationRules(int minLength, int maxLength, String allowedPattern);

  // =================================================================================
  // MÉTODOS DE SCOPES MANAGEMENT (ALTA PRIORIDAD)
  // =================================================================================

  /**
   * Obtiene un token OAuth2 Client Credentials con scopes específicos. Utiliza las credenciales y
   * endpoint por defecto.
   *
   * @param scopes array de scopes requeridos
   * @return el token de acceso OAuth2 con los scopes solicitados
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getClientCredentialsToken(String[] scopes) throws FrameworkBusinessException;

  /**
   * Obtiene un token OAuth2 Client Credentials con scopes específicos y parámetros personalizados.
   *
   * @param endpoint la URL del endpoint para obtener el token
   * @param clientId el ID del cliente OAuth2
   * @param clientSecret el secret del cliente OAuth2
   * @param scopes array de scopes requeridos
   * @return el token de acceso OAuth2 con los scopes solicitados
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getClientCredentialsToken(
      String endpoint, String clientId, String clientSecret, String[] scopes)
      throws FrameworkBusinessException;

  /**
   * Configura los scopes por defecto para tokens OAuth2.
   *
   * @param scopes array de scopes por defecto
   */
  void setDefaultScopes(String[] scopes);

  /**
   * Obtiene los scopes por defecto configurados.
   *
   * @return array de scopes configurados o null si no hay scopes por defecto
   */
  String[] getDefaultScopes();

  /**
   * Verifica si un token contiene un scope específico. Útil para tokens JWT que incluyen scopes en
   * los claims.
   *
   * @param token el token a verificar
   * @param scope el scope a buscar
   * @return true si el token contiene el scope
   * @throws FrameworkBusinessException si hay problemas verificando el token
   */
  boolean tokenHasScope(String token, String scope) throws FrameworkBusinessException;

  // =================================================================================
  // MÉTODOS DE TOKEN INTROSPECTION (MEDIA PRIORIDAD)
  // =================================================================================

  /**
   * Realiza introspección de un token OAuth2 para obtener su metadatos.
   *
   * @param token el token a inspeccionar
   * @return mapa con los metadatos del token (active, scope, client_id, etc.)
   * @throws FrameworkBusinessException si hay problemas con la introspección
   */
  java.util.Map<String, Object> introspectToken(String token) throws FrameworkBusinessException;

  /**
   * Verifica si un token está activo usando introspección OAuth2.
   *
   * @param token el token a verificar
   * @return true si el token está activo
   * @throws FrameworkBusinessException si hay problemas verificando el token
   */
  boolean isTokenActive(String token) throws FrameworkBusinessException;

  /**
   * Configura el endpoint por defecto para introspección de tokens.
   *
   * @param endpoint la URL del endpoint de introspección
   */
  void setDefaultIntrospectionEndpoint(String endpoint);

  /**
   * Obtiene el endpoint por defecto para introspección de tokens.
   *
   * @return el endpoint configurado o null si no está configurado
   */
  String getDefaultIntrospectionEndpoint();

  // =================================================================================
  // MÉTODOS DE TOKEN REVOCATION (MEDIA PRIORIDAD)
  // =================================================================================

  /**
   * Revoca un token de acceso.
   *
   * @param token el token a revocar
   * @return true si la revocación fue exitosa
   * @throws FrameworkBusinessException si hay problemas revocando el token
   */
  boolean revokeToken(String token) throws FrameworkBusinessException;

  /**
   * Revoca un refresh token específico.
   *
   * @param refreshToken el refresh token a revocar
   * @return true si la revocación fue exitosa
   * @throws FrameworkBusinessException si hay problemas revocando el refresh token
   */
  boolean revokeRefreshToken(String refreshToken) throws FrameworkBusinessException;

  /**
   * Configura el endpoint por defecto para revocación de tokens.
   *
   * @param endpoint la URL del endpoint de revocación
   */
  void setDefaultRevocationEndpoint(String endpoint);

  /**
   * Obtiene el endpoint por defecto para revocación de tokens.
   *
   * @return el endpoint configurado o null si no está configurado
   */
  String getDefaultRevocationEndpoint();

  // =================================================================================
  // MÉTODOS DE MULTI-FACTOR AUTHENTICATION (MEDIA PRIORIDAD)
  // =================================================================================

  /**
   * Obtiene un token usando autenticación multi-factor (usuario, contraseña y código MFA). Utiliza
   * el endpoint de autenticación por defecto.
   *
   * @param username el nombre de usuario
   * @param password la contraseña
   * @param mfaCode el código de autenticación multi-factor
   * @return el token de acceso
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getMFAToken(String username, String password, String mfaCode)
      throws FrameworkBusinessException;

  /**
   * Obtiene un token usando autenticación multi-factor con endpoint específico.
   *
   * @param endpoint la URL del endpoint de autenticación
   * @param username el nombre de usuario
   * @param password la contraseña
   * @param mfaCode el código de autenticación multi-factor
   * @return el token de acceso
   * @throws FrameworkBusinessException si hay problemas obteniendo el token
   */
  String getMFAToken(String endpoint, String username, String password, String mfaCode)
      throws FrameworkBusinessException;

  /**
   * Configura el nombre del campo para el código MFA en las peticiones.
   *
   * @param fieldName el nombre del campo para el código MFA
   */
  void setMFAFieldName(String fieldName);

  /**
   * Obtiene el nombre del campo configurado para el código MFA.
   *
   * @return el nombre del campo configurado
   */
  String getMFAFieldName();
}
