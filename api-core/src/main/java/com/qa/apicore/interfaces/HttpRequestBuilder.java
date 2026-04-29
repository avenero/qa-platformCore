package com.qa.apicore.interfaces;


import java.util.Map;

/**
 * Contrato para configurar los datos de una petición HTTP antes de ejecutarla.
 *
 * <p>Agrupa: host, headers, query params, fields, body, cookies,
 * contexto de usuario/sesión y gestión de content-type.</p>
 *
 * <p>Segregado desde {@link HttpClient} para cumplir el
 * Interface Segregation Principle (ISP): los steps que sólo
 * necesitan construir la petición dependen únicamente de este contrato.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see HttpClient
 */
public interface HttpRequestBuilder {

    // =========================================================================
    // HOST
    // =========================================================================

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

    // =========================================================================
    // HEADERS
    // =========================================================================

    /**
     * Obtiene todos los headers actualmente configurados.
     * Implementación default retorna un mapa vacío.
     *
     * @return Mapa con todos los headers configurados (nunca null, puede estar vacío)
     * @since 2.0.0
     */
    default Map<String, String> getHeaders() {
        return Map.of();
    }

    /**
     * Agrega un header HTTP a la petición.
     *
     * @param key   Nombre del header (no puede ser null o vacío)
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
     * Elimina un header por nombre.
     * Implementación default: no-op. Las implementaciones concretas pueden sobreescribir.
     *
     * @param headerName nombre del header a eliminar
     * @since 2.0.0
     */
    default void removeHeader(String headerName) {
        // Las implementaciones concretas sobreescriben
    }

    // =========================================================================
    // QUERY PARAMS
    // =========================================================================

    /**
     * Agrega un query parameter a la URL de la petición.
     *
     * @param key   Nombre del parámetro (no puede ser null o vacío)
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
     * Limpia todos los query params configurados.
     * Implementación default: no-op. Las implementaciones concretas lo sobreescriben.
     *
     * @since 2.0.0
     */
    default void clearQueryParams() {
        // Las implementaciones concretas sobreescriben
    }

    // =========================================================================
    // PATH PARAMS
    // =========================================================================

    /**
     * Agrega un parámetro de path (URL template variable).
     * Implementación default: lo agrega como query param como fallback.
     *
     * @param name  nombre del parámetro (sin llaves)
     * @param value valor del parámetro
     * @since 2.0.0
     */
    default void addPathParam(String name, String value) {
        String host = getHost();
        if (host != null && host.contains("{" + name + "}")) {
            try {
                String encoded = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).
                    replace("+", "%20");
                setHost(host.replace("{" + name + "}", encoded));
            } catch (Exception e) {
                setHost(host.replace("{" + name + "}", value));
            }
        } else {
            addQueryParam(name, value);
        }
    }

    // =========================================================================
    // FIELDS (form data)
    // =========================================================================

    /**
     * Agrega un field al body de la petición (para form data).
     *
     * @param key   Nombre del field (no puede ser null o vacío)
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

    // =========================================================================
    // BODY
    // =========================================================================

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

    /**
     * Retorna el body del request actual (si está configurado).
     * Implementación default retorna null.
     *
     * @return body actual, o null si no está configurado
     * @since 2.0.0
     */
    default String getBody() {
        return null;
    }

    // =========================================================================
    // COOKIES
    // =========================================================================

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
     * @param name  Nombre de la cookie (no puede ser null o vacío)
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

    /** Limpia todas las cookies configuradas. */
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

    // =========================================================================
    // CONTEXTO DE USUARIO / SESIÓN
    // =========================================================================

    /**
     * Establece el contexto de usuario para las peticiones HTTP.
     * Permite compartir contexto entre frameworks (Web login → API calls → Mobile validation).
     *
     * @param userId    ID del usuario actual (no puede ser null o vacío)
     * @param sessionId ID de la sesión activa (no puede ser null o vacío)
     * @throws IllegalArgumentException Si userId o sessionId son null o vacíos
     */
    void setUserContext(String userId, String sessionId);

    /**
     * Establece el contexto de usuario con información adicional.
     *
     * @param userId            ID del usuario actual (no puede ser null o vacío)
     * @param sessionId         ID de la sesión activa (no puede ser null o vacío)
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
     * @param key   Clave del valor (no puede ser null o vacío)
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

    /** Limpia completamente el contexto de usuario. */
    void clearUserContext();

    /**
     * Aplica automáticamente el contexto de usuario a los headers de las peticiones.
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

    // =========================================================================
    // CONTENT TYPE
    // =========================================================================

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

    /** Configura headers para JSON: Content-Type y Accept → application/json. */
    void configureForJson();

    /** Configura headers para XML: Content-Type y Accept → application/xml. */
    void configureForXml();

    /** Configura Content-Type → application/x-www-form-urlencoded. */
    void configureForFormData();

    /** Configura Content-Type y Accept → text/plain. */
    void configureForPlainText();

    /** Configura Content-Type → multipart/form-data. */
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
}

