package com.qa.httpcore.implementations;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.model.HttpMethod;
import com.qa.httpcore.model.HttpResponse;
import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.common.api.logging.TestLogger;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Implementación de {@link HttpClient} que delega la ejecución HTTP en el
 * {@link APIRequestContext} de Playwright (TASK-E02).
 *
 * <h2>¿Por qué Playwright como motor HTTP?</h2>
 * <ul>
 *   <li>Permite compartir cookies/storage/auth entre flujos web (Page) y HTTP
 *       (login API → browser ya autenticado, o viceversa).</li>
 *   <li>Soporte HTTP/2, TLS moderno y proxy delegado al binario de Playwright.</li>
 *   <li>Habilita interceptación de tráfico cuando el escenario sea mixto web↔HTTP.</li>
 * </ul>
 *
 * <h2>Decisión arquitectural</h2>
 * <p>Para preservar la regla inviolable «ningún módulo especializado conoce a otro
 * módulo especializado», esta clase recibe un {@link Supplier} de
 * {@code APIRequestContext} en lugar de invocar directamente
 * {@code com.qa.webcore.driver.playwright.PlaywrightManager}. El cableado con
 * {@code PlaywrightManager::getApiContext} se realiza en la capa consumidora
 * (BE / tests integrados). De esta forma {@code http-core} sólo depende de la
 * librería {@code com.microsoft.playwright:playwright}, no de {@code web-core}.</p>
 *
 * <h2>Thread-safety</h2>
 * <p>Misma semántica que {@link ApacheHttpClientImpl}: una instancia por hilo /
 * escenario BDD. El estado interno (headers, cookies, etc.) es mutable.</p>
 *
 * <h2>Limitaciones conocidas</h2>
 * <ul>
 *   <li>Configuración SSL granular ({@link #configureSsl}) es no-op: Playwright
 *       gestiona TLS desde su binario nativo. Para "trust-all" se debe lanzar
 *       el browser con {@code ignoreHTTPSErrors=true} (ya configurado en
 *       {@code PlaywrightManager.startScenario}).</li>
 *   <li>Los timeouts se aplican por petición vía {@link RequestOptions#setTimeout}.</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since TASK-E02
 * @see HttpClient
 * @see APIRequestContext
 */
public class PlaywrightHttpEngine implements HttpClient {

    // ── Constantes ────────────────────────────────────────────────────────────

    private static final int DEFAULT_CONNECT_TIMEOUT_MS  = 30_000;
    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 60_000;
    private static final String DEFAULT_CONTENT_TYPE     = "application/json";
    private static final String DEFAULT_ACCEPT_TYPE      = "application/json";
    private static final int REQUEST_ID_LENGTH           = 8;

    // ── Estado de configuración HTTP ──────────────────────────────────────────

    private String host;
    private final Map<String, String> headers     = new LinkedHashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private final Map<String, String> fields      = new LinkedHashMap<>();
    private final Map<String, String> cookies     = new LinkedHashMap<>();
    private final Map<String, String> userCtx     = new LinkedHashMap<>();
    private String  body;
    private String  contentType = DEFAULT_CONTENT_TYPE;
    private String  acceptType  = DEFAULT_ACCEPT_TYPE;
    private String  userId;
    private String  sessionId;
    private boolean autoApplyUserCtx = false;
    private boolean automaticCookies = true;

    // ── Configuración técnica ─────────────────────────────────────────────────

    private int connectTimeoutMs  = DEFAULT_CONNECT_TIMEOUT_MS;
    private int responseTimeoutMs = DEFAULT_RESPONSE_TIMEOUT_MS;
    private int maxRetries        = 0;
    private int retryDelayMs      = 0;

    // ── Estado de la última petición ──────────────────────────────────────────

    private HttpResponse lastResponse;
    private String lastResponseBodyCache;
    private String lastUrl;
    private String lastMethod;
    private long lastDuration;
    private String lastRequestBody;
    private String lastRequestId;
    private Map<String, String> lastRequestHeadersSnapshot = Collections.emptyMap();

    // ── Proveedor de APIRequestContext ────────────────────────────────────────

    private final Supplier<APIRequestContext> apiContextSupplier;

    /**
     * Constructor que recibe el proveedor de {@link APIRequestContext}.
     *
     * @param apiContextSupplier proveedor — típicamente {@code PlaywrightManager::getApiContext}
     *                           cableado desde la capa consumidora. No puede ser {@code null}.
     */
    public PlaywrightHttpEngine(Supplier<APIRequestContext> apiContextSupplier) {
        if (apiContextSupplier == null) {
            throw new IllegalArgumentException("apiContextSupplier no puede ser null");
        }
        this.apiContextSupplier = apiContextSupplier;
    }

    // =========================================================================
    // HttpRequestBuilder — HOST
    // =========================================================================

    @Override
    public void setHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host no puede ser null o vacío");
        }
        this.host = host.trim();
    }

    @Override public String getHost() { return host; }

    @Override public boolean hasValidHost() { return host != null && !host.isBlank(); }

    // =========================================================================
    // HttpRequestBuilder — HEADERS
    // =========================================================================

    @Override public Map<String, String> getHeaders() { return Collections.unmodifiableMap(headers); }

    @Override
    public void addHeader(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("header key no puede ser null/vacío");
        }
        if (value == null) {
            headers.remove(key);
        } else {
            headers.put(key, value);
        }
    }

    @Override
    public void addHeaders(Map<String, String> h) {
        if (h == null) {
            throw new IllegalArgumentException("headers map no puede ser null");
        }
        h.forEach(this::addHeader);
    }

    @Override
    public void removeHeader(String headerName) {
        if (headerName != null) {
            headers.remove(headerName);
        }
    }

    // =========================================================================
    // HttpRequestBuilder — QUERY PARAMS
    // =========================================================================

    @Override
    public void addQueryParam(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("param key no puede ser null/vacío");
        }
        if (value == null) {
            queryParams.remove(key);
        } else {
            queryParams.put(key, value);
        }
    }

    @Override
    public void addQueryParams(Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("queryParams no puede ser null");
        }
        params.forEach((k, v) -> addQueryParam(k, v != null ? v.toString() : null));
    }

    @Override public void clearQueryParams() { queryParams.clear(); }

    // =========================================================================
    // HttpRequestBuilder — FIELDS (form data)
    // =========================================================================

    @Override
    public void addField(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("field key no puede ser null/vacío");
        }
        if (value == null) {
            fields.remove(key);
        } else {
            fields.put(key, value);
        }
    }

    @Override
    public void addFields(Map<String, String> f) {
        if (f == null) {
            throw new IllegalArgumentException("fields no puede ser null");
        }
        f.forEach(this::addField);
    }

    // =========================================================================
    // HttpRequestBuilder — BODY
    // =========================================================================

    @Override public void setBody(String body) { this.body = body; }

    @Override public boolean hasBody() { return body != null && !body.isBlank(); }

    @Override
    public long getBodySize() {
        return body != null ? body.getBytes(StandardCharsets.UTF_8).length : 0L;
    }

    @Override public String getBody() { return body; }

    // =========================================================================
    // HttpRequestBuilder — COOKIES
    // =========================================================================

    @Override
    public void setCookies(Map<String, String> c) {
        if (c == null) {
            throw new IllegalArgumentException("cookies no puede ser null");
        }
        cookies.clear();
        c.forEach(this::addCookie);
    }

    @Override public Map<String, String> getCookies() { return Collections.unmodifiableMap(cookies); }

    @Override
    public void addCookie(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("cookie name no puede ser null/vacío");
        }
        if (value == null) {
            cookies.remove(name);
        } else {
            cookies.put(name, value);
        }
    }

    @Override
    public String getCookie(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("cookie name no puede ser null/vacío");
        }
        return cookies.get(name);
    }

    @Override
    public boolean hasCookie(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("cookie name no puede ser null/vacío");
        }
        return cookies.containsKey(name);
    }

    @Override
    public void removeCookie(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("cookie name no puede ser null/vacío");
        }
        cookies.remove(name);
    }

    @Override public void clearCookies() { cookies.clear(); }

    @Override public void setAutomaticCookieHandling(boolean enabled) { this.automaticCookies = enabled; }

    @Override public boolean isAutomaticCookieHandlingEnabled() { return automaticCookies; }

    // =========================================================================
    // HttpRequestBuilder — CONTEXTO DE USUARIO
    // =========================================================================

    @Override
    public void setUserContext(String userId, String sessionId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId no puede ser null/vacío");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId no puede ser null/vacío");
        }
        this.userId = userId;
        this.sessionId = sessionId;
    }

    @Override
    public void setUserContext(String userId, String sessionId, Map<String, String> additional) {
        setUserContext(userId, sessionId);
        if (additional != null) {
            userCtx.putAll(additional);
        }
    }

    @Override public String getCurrentUserId() { return userId; }

    @Override public String getCurrentSessionId() { return sessionId; }

    @Override
    public Map<String, String> getUserContext() {
        Map<String, String> ctx = new HashMap<>(userCtx);
        if (userId != null) {
            ctx.put("userId", userId);
        }
        if (sessionId != null) {
            ctx.put("sessionId", sessionId);
        }
        return Collections.unmodifiableMap(ctx);
    }

    @Override
    public String getContextValue(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key no puede ser null/vacío");
        }
        return userCtx.get(key);
    }

    @Override
    public void setContextValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key no puede ser null/vacío");
        }
        if (value == null) {
            userCtx.remove(key);
        } else {
            userCtx.put(key, value);
        }
    }

    @Override public boolean hasUserContext() { return userId != null && sessionId != null; }

    @Override
    public void clearUserContext() {
        userId = null;
        sessionId = null;
        userCtx.clear();
    }

    @Override public void setAutoApplyUserContext(boolean enabled) { this.autoApplyUserCtx = enabled; }

    @Override public boolean isAutoApplyUserContextEnabled() { return autoApplyUserCtx; }

    // =========================================================================
    // HttpRequestBuilder — CONTENT TYPE
    // =========================================================================

    @Override
    public void setContentType(String ct) {
        if (ct == null || ct.isBlank()) {
            throw new IllegalArgumentException("contentType no puede ser null/vacío");
        }
        this.contentType = ct;
    }

    @Override public String getContentType() { return contentType; }

    @Override
    public void setAcceptType(String at) {
        if (at == null || at.isBlank()) {
            throw new IllegalArgumentException("acceptType no puede ser null/vacío");
        }
        this.acceptType = at;
    }

    @Override public String getAcceptType() { return acceptType; }

    @Override
    public void configureForJson() {
        contentType = "application/json";
        acceptType  = "application/json";
    }

    @Override
    public void configureForXml() {
        contentType = "application/xml";
        acceptType  = "application/xml";
    }

    @Override public void configureForFormData() { contentType = "application/x-www-form-urlencoded"; }

    @Override
    public void configureForPlainText() {
        contentType = "text/plain";
        acceptType  = "text/plain";
    }

    @Override public void configureForMultipart() { contentType = "multipart/form-data"; }

    @Override public boolean isJsonContentType() { return contentType != null && contentType.contains("json"); }

    @Override public boolean isXmlContentType() { return contentType != null && contentType.contains("xml"); }

    @Override public boolean isFormDataContentType() { return contentType != null && contentType.contains("form"); }

    @Override
    public String detectContentType(String b) {
        if (b == null) {
            throw new IllegalArgumentException("body no puede ser null");
        }
        String trimmed = b.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "application/json";
        }
        if (trimmed.startsWith("<")) {
            return "application/xml";
        }
        if (trimmed.contains("=") && trimmed.contains("&")) {
            return "application/x-www-form-urlencoded";
        }
        return "application/octet-stream";
    }

    @Override
    public void autoDetectAndSetContentType() {
        if (hasBody()) {
            contentType = detectContentType(body);
        }
    }

    // =========================================================================
    // HttpRequestExecutor
    // =========================================================================

    @Override
    public HttpResponse executeRequest(HttpMethod method, String endpoint)
            throws FrameworkTechnicalException {
        return executeRequest(method, endpoint, true, responseTimeoutMs);
    }

    @Override
    public HttpResponse executeRequest(HttpMethod method, String endpoint, boolean followRedirects)
            throws FrameworkTechnicalException {
        return executeRequest(method, endpoint, followRedirects, responseTimeoutMs);
    }

    @Override
    public HttpResponse executeRequest(HttpMethod method, String endpoint, int timeoutMs)
            throws FrameworkTechnicalException {
        return executeRequest(method, endpoint, true, timeoutMs);
    }

    @Override
    public HttpResponse executeRequest(HttpMethod method, String endpoint,
                                       boolean followRedirects, int timeoutMs)
            throws FrameworkTechnicalException {
        validateHost();
        validateEndpoint(endpoint);

        String requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH);
        lastRequestId = requestId;

        String fullUrl = buildUrl(endpoint);
        lastUrl = fullUrl;
        lastMethod = method.name();
        lastRequestBody = body;

        TestLogger.logInfo("HTTP_CLIENT_PW",
            "[" + requestId + "] " + method + " → " + fullUrl, null);

        Map<String, String> headersSnapshot = buildEffectiveHeaders();
        lastRequestHeadersSnapshot = Collections.unmodifiableMap(new HashMap<>(headersSnapshot));

        long start = System.currentTimeMillis();
        HttpResponse response = executeWithRetry(method, fullUrl, headersSnapshot,
                followRedirects, timeoutMs, requestId);
        lastDuration = System.currentTimeMillis() - start;
        lastResponse = response;
        lastResponseBodyCache = response != null ? response.getBody() : null;

        TestLogger.logInfo("HTTP_CLIENT_PW",
            "[" + requestId + "] ← HTTP " + response.getStatusCode() + " (" + lastDuration + "ms)", null);

        return response;
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

    // =========================================================================
    // HttpResponseAccessor
    // =========================================================================

    @Override public HttpResponse getLastResponse() { return lastResponse; }

    @Override public String getLastRequestUrl() { return lastUrl; }

    @Override public String getLastRequestMethod() { return lastMethod; }

    @Override public long getLastRequestDuration() { return lastResponse != null ? lastDuration : -1L; }

    @Override public String getLastRequestBody() { return lastRequestBody; }

    @Override public String getLastRequestId() { return lastRequestId; }

    @Override public Map<String, String> getLastRequestHeadersSnapshot() { return lastRequestHeadersSnapshot; }

    @Override public String getLastResponseBody() { return lastResponseBodyCache; }

    // =========================================================================
    // HttpClientConfigurator
    // =========================================================================

    @Override
    public void setConnectionTimeout(int ms) {
        if (ms <= 0) {
            throw new IllegalArgumentException("connectTimeout debe ser > 0");
        }
        this.connectTimeoutMs = ms;
    }

    @Override
    public void setReadTimeout(int ms) {
        if (ms <= 0) {
            throw new IllegalArgumentException("readTimeout debe ser > 0");
        }
        this.responseTimeoutMs = ms;
    }

    @Override public int getConnectionTimeout() { return connectTimeoutMs; }

    @Override public int getReadTimeout() { return responseTimeoutMs; }

    @Override
    public void setRetryPolicy(int maxRetries, int retryDelayMs) {
        if (maxRetries < 0 || retryDelayMs < 0) {
            throw new IllegalArgumentException("maxRetries y retryDelayMs deben ser >= 0");
        }
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    @Override public boolean isRetryEnabled() { return maxRetries > 0; }

    @Override public int getMaxRetries() { return maxRetries; }

    @Override public int getRetryDelay() { return retryDelayMs; }

    /**
     * No-op para Playwright: TLS lo gestiona el binario nativo.
     * Para "trust-all" usar {@code ignoreHTTPSErrors=true} al lanzar el browser
     * (configurado en {@code PlaywrightManager.startScenario}).
     */
    @Override
    public void configureSsl(SSLContext ctx, boolean trustAll) {
        // no-op intencional — ver javadoc.
    }

    @Override
    public void clearRequestData() {
        headers.clear();
        queryParams.clear();
        fields.clear();
        body = null;
        contentType = DEFAULT_CONTENT_TYPE;
        acceptType  = DEFAULT_ACCEPT_TYPE;
    }

    @Override
    public void reset() {
        clearRequestData();
        cookies.clear();
        userCtx.clear();
        userId = null;
        sessionId = null;
        host = null;
        lastResponse = null;
        lastResponseBodyCache = null;
        lastUrl = null;
        lastMethod = null;
        lastDuration = 0L;
        lastRequestBody = null;
        lastRequestId = null;
        lastRequestHeadersSnapshot = Collections.emptyMap();
    }

    @Override
    public String getDebugInfo() {
        return String.format(
            "PlaywrightHttpEngine{host='%s', headers=%d, queryParams=%d, cookies=%d, " +
            "body=%s, contentType='%s', connectTimeout=%dms, responseTimeout=%dms, maxRetries=%d}",
            host, headers.size(), queryParams.size(), cookies.size(),
            hasBody() ? getBodySize() + " bytes" : "none",
            contentType, connectTimeoutMs, responseTimeoutMs, maxRetries);
    }

    // =========================================================================
    // Implementación interna
    // =========================================================================

    private String buildUrl(String endpoint) {
        String base = host.replaceAll("/+$", "");
        String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        StringBuilder sb = new StringBuilder(base).append(path);
        if (!queryParams.isEmpty()) {
            sb.append('?');
            queryParams.forEach((k, v) -> {
                String enc = v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
                sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                  .append('=').append(enc).append('&');
            });
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private Map<String, String> buildEffectiveHeaders() {
        Map<String, String> effective = new LinkedHashMap<>(headers);
        effective.put("Content-Type", contentType);
        effective.put("Accept", acceptType);
        if (autoApplyUserCtx && hasUserContext()) {
            effective.put("X-User-Id", userId);
            effective.put("X-Session-Id", sessionId);
        }
        if (!cookies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            cookies.forEach((k, v) -> sb.append(k).append('=').append(v).append("; "));
            String cookieHeader = sb.toString().trim();
            if (cookieHeader.endsWith(";")) {
                cookieHeader = cookieHeader.substring(0, cookieHeader.length() - 1);
            }
            effective.put("Cookie", cookieHeader);
        }
        return effective;
    }

    private HttpResponse executeWithRetry(HttpMethod method, String url,
                                          Map<String, String> effectiveHeaders,
                                          boolean followRedirects, int timeoutMs,
                                          String requestId)
            throws FrameworkTechnicalException {
        Exception lastEx = null;
        int attempts = maxRetries + 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (attempt > 1) {
                TestLogger.logInfo("HTTP_CLIENT_PW",
                    "[" + requestId + "] Reintento " + (attempt - 1) + "/" + maxRetries, null);
                sleepQuietly(retryDelayMs);
            }
            try {
                return doExecute(method, url, effectiveHeaders, followRedirects, timeoutMs);
            } catch (Exception e) {
                lastEx = e;
                TestLogger.logWarning("HTTP_CLIENT_PW",
                    "[" + requestId + "] Error en intento " + attempt + ": " + e.getMessage(), null);
            }
        }
        throw new FrameworkTechnicalException(
            "Petición " + method + " " + url + " falló después de " + attempts + " intento(s): "
            + (lastEx != null ? lastEx.getMessage() : "error desconocido"), lastEx);
    }

    private HttpResponse doExecute(HttpMethod method, String url,
                                   Map<String, String> effectiveHeaders,
                                   boolean followRedirects, int timeoutMs) {
        APIRequestContext ctx = apiContextSupplier.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "apiContextSupplier devolvió null. ¿Olvidaste inicializar la suite Playwright?");
        }

        RequestOptions opts = RequestOptions.create()
                .setTimeout(timeoutMs)
                .setMaxRedirects(followRedirects ? 20 : 0);
        effectiveHeaders.forEach(opts::setHeader);

        // Body / form fields:
        if (hasBody()) {
            opts.setData(body);
        } else if (!fields.isEmpty() && isFormDataContentType()) {
            // Codifica campos como x-www-form-urlencoded.
            StringBuilder sb = new StringBuilder();
            fields.forEach((k, v) -> {
                if (sb.length() > 0) sb.append('&');
                sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                  .append('=')
                  .append(URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8));
            });
            opts.setData(sb.toString());
        }

        APIResponse pwResp = switch (method) {
            case GET     -> ctx.get(url, opts);
            case POST    -> ctx.post(url, opts);
            case PUT     -> ctx.put(url, opts);
            case DELETE  -> ctx.delete(url, opts);
            case PATCH   -> ctx.patch(url, opts);
            case HEAD    -> ctx.head(url, opts);
            case OPTIONS -> ctx.fetch(url, opts.setMethod("OPTIONS"));
        };

        try {
            int status = pwResp.status();
            String responseBody = safeText(pwResp);
            Map<String, String> respHeaders = new HashMap<>();
            Map<String, String> raw = pwResp.headers();
            if (raw != null) {
                respHeaders.putAll(raw);
            }
            return new HttpResponse(status, responseBody, respHeaders, 0L);
        } finally {
            try {
                pwResp.dispose();
            } catch (RuntimeException ignored) {
                // No propagamos errores en cleanup.
            }
        }
    }

    private static String safeText(APIResponse pw) {
        try {
            return pw.text();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void validateHost() {
        if (!hasValidHost()) {
            throw new IllegalStateException(
                "Host no configurado. Llama a setHost(url) antes de ejecutar peticiones.");
        }
    }

    private void validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint no puede ser null o vacío");
        }
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Acceso interno para tests sin exponer en la API pública.
    @SuppressWarnings("unused")
    private List<String> internalState() {
        return new ArrayList<>(); // marker — no se usa, evita warnings de compilación.
    }

    /**
     * Resuelve el dominio del host para validar la procedencia de cookies,
     * útil cuando el consumidor inyecta cookies y necesita verificar el ámbito.
     */
    public String resolvedHostDomain() {
        if (host == null) {
            return null;
        }
        try {
            return URI.create(host).getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
