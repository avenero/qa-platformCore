package com.scotia.qa.common.interfaces;

import com.scotia.qa.common.http.HttpResponse;
import com.scotia.qa.common.http.HttpMethod;
import com.scotia.qa.common.http.exceptions.FrameworkTechnicalException;
import java.util.Map;

/**
 * Interface que define el contrato para clientes HTTP del framework Scotia QA.
 *
 * Esta interface proporciona la funcionalidad base que deben implementar todos los clientes HTTP
 * específicos de cada framework (API, Web, Mobile), permitiendo especialización sin duplicación.
 *
 * Características principales:
 * - Configuración de host, headers, query parameters y body
 * - Ejecución de peticiones HTTP con diferentes métodos
 * - Acceso a información de la última petición para debugging
 * - Limpieza de datos de petición
 * - Manejo robusto de excepciones
 * - Validación de parámetros y type safety
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2024
 */
public interface HttpClient {

    // =================================================================================
    // SECCIÓN: CONFIGURACIÓN DE PETICIONES
    // =================================================================================

    /**
     * Establece el host base para las peticiones HTTP.
     *
     * @param host URL base del servicio (ej: "https://api.example.com") - no puede ser null o vacío
     * @throws IllegalArgumentException Si host es null, vacío o no es una URL válida
     */
    void setHost(String host);

    /**
     * Obtiene el host actualmente configurado.
     *
     * @return Host base configurado o null si no se ha establecido
     */
    String getHost();

    /**
     * Verifica si hay un host configurado y válido.
     *
     * @return true si hay un host configurado, false en caso contrario
     */
    boolean hasValidHost();

    /**
     * Agrega un header HTTP a la petición.
     *
     * @param key Nombre del header (no puede ser null o vacío)
     * @param value Valor del header (puede ser null para eliminar header)
     * @throws IllegalArgumentException Si key es null o vacío
     */
    void addHeader(String key, String value);

    /**
     * Agrega múltiples headers HTTP a la petición.
     *
     * @param headers Mapa de headers key-value (no puede ser null)
     * @throws IllegalArgumentException Si headers es null o contiene keys null/vacías
     */
    void addHeaders(Map<String, String> headers);

    /**
     * Agrega un query parameter a la URL de la petición.
     *
     * @param key Nombre del parámetro (no puede ser null o vacío)
     * @param value Valor del parámetro (puede ser null para eliminar parámetro)
     * @throws IllegalArgumentException Si key es null o vacío
     */
    void addQueryParam(String key, String value);

    /**
     * Agrega múltiples query parameters a la URL de la petición.
     *
     * @param queryParams Mapa de parámetros key-value (no puede ser null)
     * @throws IllegalArgumentException Si queryParams es null o contiene keys null/vacías
     */
    void addQueryParams(Map<String, Object> queryParams);

    /**
     * Agrega un field al body de la petición (para form data).
     *
     * @param key Nombre del field (no puede ser null o vacío)
     * @param value Valor del field (puede ser null para eliminar field)
     * @throws IllegalArgumentException Si key es null o vacío
     */
    void addField(String key, String value);

    /**
     * Agrega múltiples fields al body de la petición.
     *
     * @param fields Mapa de fields key-value (no puede ser null)
     * @throws IllegalArgumentException Si fields es null o contiene keys null/vacías
     */
    void addFields(Map<String, String> fields);

    /**
     * Establece el body de la petición como string (para JSON, XML, etc.).
     *
     * @param body Contenido del body de la petición (puede ser null para eliminar body)
     */
    void setBody(String body);

    /**
     * Verifica si hay contenido en el body configurado.
     *
     * @return true si hay body configurado (no null y no vacío), false en caso contrario
     */
    boolean hasBody();

    /**
     * Obtiene el tamaño actual del body en bytes.
     *
     * @return Tamaño del body en bytes, 0 si no hay body configurado
     */
    long getBodySize();

    // =================================================================================
    // SECCIÓN: CONFIGURACIÓN DE TIMEOUTS Y RETRY
    // =================================================================================

    /**
     * Configura el timeout de conexión por defecto.
     *
     * @param timeoutMs Timeout de conexión en millisegundos (debe ser > 0)
     * @throws IllegalArgumentException Si timeoutMs <= 0
     */
    void setConnectionTimeout(int timeoutMs);

    /**
     * Configura el timeout de lectura por defecto.
     *
     * @param timeoutMs Timeout de lectura en millisegundos (debe ser > 0)
     * @throws IllegalArgumentException Si timeoutMs <= 0
     */
    void setReadTimeout(int timeoutMs);

    /**
     * Obtiene el timeout de conexión configurado.
     *
     * @return Timeout de conexión en ms, -1 si usa valores por defecto del sistema
     */
    int getConnectionTimeout();

    /**
     * Obtiene el timeout de lectura configurado.
     *
     * @return Timeout de lectura en ms, -1 si usa valores por defecto del sistema
     */
    int getReadTimeout();

    /**
     * Configura la política de retry para peticiones fallidas.
     *
     * @param maxRetries Número máximo de reintentos (debe ser >= 0)
     * @param retryDelayMs Delay entre reintentos en millisegundos (debe ser >= 0)
     * @throws IllegalArgumentException Si maxRetries < 0 o retryDelayMs < 0
     */
    void setRetryPolicy(int maxRetries, int retryDelayMs);

    /**
     * Verifica si el retry automático está habilitado.
     *
     * @return true si retry está habilitado (maxRetries > 0), false en caso contrario
     */
    boolean isRetryEnabled();

    /**
     * Obtiene el número máximo de reintentos configurado.
     *
     * @return Número máximo de reintentos, 0 si no hay retry
     */
    int getMaxRetries();

    /**
     * Obtiene el delay entre reintentos configurado.
     *
     * @return Delay en millisegundos, 0 si no hay retry configurado
     */
    int getRetryDelay();

    // =================================================================================
    // SECCIÓN: EJECUCIÓN DE PETICIONES
    // =================================================================================

    /**
     * Ejecuta una petición HTTP con el método y endpoint especificados.
     * Método principal type-safe usando enum HttpMethod.
     *
     * @param method Método HTTP usando enum type-safe
     * @param endpoint Endpoint relativo al host configurado (no puede ser null o vacío)
     * @return Respuesta HTTP encapsulada (nunca null)
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición (conectividad, timeout, etc.)
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint) throws FrameworkTechnicalException;


    /**
     * Ejecuta una petición HTTP con configuración de seguimiento de redirects.
     *
     * @param method Método HTTP usando enum type-safe
     * @param endpoint Endpoint relativo
     * @param followRedirects Si debe seguir redirects automáticamente
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint, boolean followRedirects) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición HTTP con timeout específico para esta petición.
     *
     * @param method Método HTTP usando enum type-safe
     * @param endpoint Endpoint relativo
     * @param timeoutMs Timeout específico para esta petición en millisegundos (debe ser > 0)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos o timeout
     * @throws IllegalArgumentException Si endpoint es inválido o timeout <= 0
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint, int timeoutMs) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición HTTP con configuración completa per-request.
     * Método más flexible que permite configurar todo sin afectar el cliente global.
     *
     * @param method Método HTTP usando enum type-safe
     * @param endpoint Endpoint relativo
     * @param followRedirects Si debe seguir redirects
     * @param timeoutMs Timeout específico en millisegundos (debe ser > 0)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si parámetros son inválidos
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse executeRequest(HttpMethod method, String endpoint, boolean followRedirects, int timeoutMs) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición GET.
     *
     * @param endpoint Endpoint relativo (no puede ser null o vacío)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse get(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición POST.
     *
     * @param endpoint Endpoint relativo (no puede ser null o vacío)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse post(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición PUT.
     *
     * @param endpoint Endpoint relativo (no puede ser null o vacío)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse put(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición DELETE.
     *
     * @param endpoint Endpoint relativo (no puede ser null o vacío)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse delete(String endpoint) throws FrameworkTechnicalException;

    /**
     * Ejecuta una petición PATCH.
     *
     * @param endpoint Endpoint relativo (no puede ser null o vacío)
     * @return Respuesta HTTP encapsulada
     * @throws FrameworkTechnicalException Si hay problemas técnicos con la petición
     * @throws IllegalArgumentException Si endpoint es null, vacío o inválido
     * @throws IllegalStateException Si no hay host configurado
     */
    HttpResponse patch(String endpoint) throws FrameworkTechnicalException;

    // =================================================================================
    // SECCIÓN: ACCESO A INFORMACIÓN DE PETICIÓN
    // =================================================================================

    /**
     * Obtiene la respuesta de la última petición ejecutada.
     *
     * @return Última respuesta HTTP o null si no se ha ejecutado ninguna petición
     */
    HttpResponse getLastResponse();

    /**
     * Obtiene la URL completa de la última petición ejecutada.
     *
     * @return URL de la última petición o null si no se ha ejecutado ninguna
     */
    String getLastRequestUrl();

    /**
     * Obtiene el método HTTP de la última petición ejecutada.
     *
     * @return Método de la última petición o null si no se ha ejecutado ninguna
     */
    String getLastRequestMethod();

    /**
     * Obtiene la duración en millisegundos de la última petición ejecutada.
     *
     * @return Duración de la última petición en ms, -1 si no se ha ejecutado ninguna
     */
    long getLastRequestDuration();

    // =================================================================================
    // SECCIÓN: UTILIDADES Y LIMPIEZA
    // =================================================================================

    /**
     * Limpia todos los datos de configuración de la petición actual
     * (headers, query params, fields, body) pero mantiene el host configurado.
     */
    void clearRequestData();

    /**
     * Limpia completamente el cliente, incluyendo host y datos de petición.
     */
    void reset();

    // =================================================================================
    // SECCIÓN: GESTIÓN DE SESIONES Y COOKIES (Cross-framework crítico)
    // =================================================================================

    /**
     * Establece múltiples cookies para las peticiones HTTP.
     * Crítico para casos cross-framework donde Web captura cookies y API los reutiliza.
     *
     * @param cookies Mapa de cookies nombre-valor (no puede ser null)
     * @throws IllegalArgumentException Si cookies es null o contiene nombres null/vacíos
     */
    void setCookies(Map<String, String> cookies);

    /**
     * Obtiene todas las cookies actualmente configuradas.
     *
     * @return Mapa con todas las cookies configuradas (nunca null, puede estar vacío)
     */
    Map<String, String> getCookies();

    /**
     * Agrega una cookie individual a la configuración.
     *
     * @param name Nombre de la cookie (no puede ser null o vacío)
     * @param value Valor de la cookie (puede ser null para eliminar la cookie)
     * @throws IllegalArgumentException Si name es null o vacío
     */
    void addCookie(String name, String value);

    /**
     * Obtiene el valor de una cookie específica.
     *
     * @param name Nombre de la cookie (no puede ser null o vacío)
     * @return Valor de la cookie o null si no existe
     * @throws IllegalArgumentException Si name es null o vacío
     */
    String getCookie(String name);

    /**
     * Verifica si existe una cookie específica.
     *
     * @param name Nombre de la cookie a verificar (no puede ser null o vacío)
     * @return true si la cookie existe, false en caso contrario
     * @throws IllegalArgumentException Si name es null o vacío
     */
    boolean hasCookie(String name);

    /**
     * Elimina una cookie específica.
     *
     * @param name Nombre de la cookie a eliminar (no puede ser null o vacío)
     * @throws IllegalArgumentException Si name es null o vacío
     */
    void removeCookie(String name);

    /**
     * Limpia todas las cookies configuradas.
     */
    void clearCookies();

    /**
     * Habilita o deshabilita el manejo automático de cookies.
     * Cuando está habilitado, las cookies de respuesta se almacenan automáticamente.
     *
     * @param enabled true para habilitar manejo automático, false para manual
     */
    void setAutomaticCookieHandling(boolean enabled);

    /**
     * Verifica si el manejo automático de cookies está habilitado.
     *
     * @return true si está habilitado, false en caso contrario
     */
    boolean isAutomaticCookieHandlingEnabled();

    // =================================================================================
    // SECCIÓN: GESTIÓN DE CONTEXTO DE USUARIO/SESIÓN
    // =================================================================================

    /**
     * Establece el contexto de usuario para las peticiones HTTP.
     * Permite compartir contexto entre frameworks (Web login → API calls → Mobile validation).
     *
     * @param userId ID del usuario actual (no puede ser null o vacío)
     * @param sessionId ID de la sesión activa (no puede ser null o vacío)
     * @throws IllegalArgumentException Si userId o sessionId son null o vacíos
     */
    void setUserContext(String userId, String sessionId);

    /**
     * Establece el contexto de usuario con información adicional.
     *
     * @param userId ID del usuario actual (no puede ser null o vacío)
     * @param sessionId ID de la sesión activa (no puede ser null o vacío)
     * @param additionalContext Información adicional del contexto (no puede ser null)
     * @throws IllegalArgumentException Si userId, sessionId son null/vacíos o additionalContext es null
     */
    void setUserContext(String userId, String sessionId, Map<String, String> additionalContext);

    /**
     * Obtiene el ID del usuario actual del contexto.
     *
     * @return ID del usuario actual o null si no hay contexto establecido
     */
    String getCurrentUserId();

    /**
     * Obtiene el ID de la sesión actual del contexto.
     *
     * @return ID de la sesión actual o null si no hay contexto establecido
     */
    String getCurrentSessionId();

    /**
     * Obtiene todo el contexto de usuario configurado.
     *
     * @return Map con todo el contexto de usuario (nunca null, puede estar vacío)
     */
    Map<String, String> getUserContext();

    /**
     * Obtiene un valor específico del contexto adicional.
     *
     * @param key Clave del valor a obtener (no puede ser null o vacío)
     * @return Valor del contexto o null si no existe
     * @throws IllegalArgumentException Si key es null o vacío
     */
    String getContextValue(String key);

    /**
     * Agrega o actualiza un valor en el contexto adicional.
     *
     * @param key Clave del valor (no puede ser null o vacío)
     * @param value Valor a almacenar (puede ser null para eliminar la clave)
     * @throws IllegalArgumentException Si key es null o vacío
     */
    void setContextValue(String key, String value);

    /**
     * Verifica si hay un contexto de usuario establecido.
     *
     * @return true si hay contexto establecido (userId y sessionId no null), false en caso contrario
     */
    boolean hasUserContext();

    /**
     * Limpia completamente el contexto de usuario.
     */
    void clearUserContext();

    /**
     * Aplica automáticamente el contexto de usuario a los headers de las peticiones.
     * Útil para agregar headers de autorización basados en el contexto.
     *
     * @param enabled true para aplicar automáticamente, false para manual
     */
    void setAutoApplyUserContext(boolean enabled);

    /**
     * Verifica si la aplicación automática del contexto está habilitada.
     *
     * @return true si está habilitado, false en caso contrario
     */
    boolean isAutoApplyUserContextEnabled();

    // =================================================================================
    // SECCIÓN: SOPORTE PARA DIFERENTES CONTENT TYPES
    // =================================================================================

    /**
     * Establece el Content-Type para las peticiones HTTP.
     *
     * @param contentType Tipo de contenido (no puede ser null o vacío)
     * @throws IllegalArgumentException Si contentType es null o vacío
     */
    void setContentType(String contentType);

    /**
     * Obtiene el Content-Type actualmente configurado.
     *
     * @return Content-Type configurado o null si no se ha establecido
     */
    String getContentType();

    /**
     * Establece el Accept header para las peticiones HTTP.
     *
     * @param acceptType Tipo de contenido aceptado (no puede ser null o vacío)
     * @throws IllegalArgumentException Si acceptType es null o vacío
     */
    void setAcceptType(String acceptType);

    /**
     * Obtiene el Accept type actualmente configurado.
     *
     * @return Accept type configurado o null si no se ha establecido
     */
    String getAcceptType();

    /**
     * Configura automáticamente headers comunes para JSON.
     * Establece Content-Type: application/json y Accept: application/json.
     */
    void configureForJson();

    /**
     * Configura automáticamente headers comunes para XML.
     * Establece Content-Type: application/xml y Accept: application/xml.
     */
    void configureForXml();

    /**
     * Configura automáticamente headers comunes para form data.
     * Establece Content-Type: application/x-www-form-urlencoded.
     */
    void configureForFormData();

    /**
     * Configura automáticamente headers comunes para texto plano.
     * Establece Content-Type: text/plain y Accept: text/plain.
     */
    void configureForPlainText();

    /**
     * Configura automáticamente headers comunes para multipart.
     * Establece Content-Type: multipart/form-data.
     */
    void configureForMultipart();

    /**
     * Verifica si el Content-Type actual es JSON.
     *
     * @return true si es JSON, false en caso contrario
     */
    boolean isJsonContentType();

    /**
     * Verifica si el Content-Type actual es XML.
     *
     * @return true si es XML, false en caso contrario
     */
    boolean isXmlContentType();

    /**
     * Verifica si el Content-Type actual es form data.
     *
     * @return true si es form data, false en caso contrario
     */
    boolean isFormDataContentType();

    /**
     * Detecta automáticamente el Content-Type basado en el contenido del body.
     *
     * @param body Contenido del body a analizar (no puede ser null)
     * @return Content-Type detectado o "application/octet-stream" si no se puede detectar
     * @throws IllegalArgumentException Si body es null
     */
    String detectContentType(String body);

    /**
     * Aplica automáticamente el Content-Type detectado basado en el body actual.
     * No hace nada si no hay body configurado.
     */
    void autoDetectAndSetContentType();

    /**
     * Obtiene una representación string del estado actual del cliente
     * para debugging (sin exponer datos sensibles).
     *
     * @return Estado del cliente como string
     */
    String getDebugInfo();
}
