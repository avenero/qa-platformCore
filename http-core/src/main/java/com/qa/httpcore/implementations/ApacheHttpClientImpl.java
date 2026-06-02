package com.qa.httpcore.implementations;

import com.qa.httpcore.interfaces.HttpClient;
import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.httpcore.model.HttpMethod;
import com.qa.httpcore.model.HttpResponse;
import com.qa.common.api.logging.TestLogger;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import com.qa.common.api.config.ExecutionProfile;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.internal.ssl.SSLContextFactory;

/**
 * Implementación de {@link HttpClient} basada en <b>Apache HttpClient 5</b>.
 *
 * <p>Reemplaza a {@link BaseHttpClient} (Unirest) como implementación por defecto del framework.
 * La interfaz pública {@link HttpClient} no cambia — solo la infraestructura HTTP interna.
 *
 * <h2>Ventajas respecto a la implementación Unirest</h2>
 * <ul>
 *   <li>Soporte activo y mantenido (Apache Foundation).</li>
 *   <li>Soporte nativo HTTP/2 y TLS 1.3.</li>
 *   <li>Configuración de timeouts granular (connect, response, read).</li>
 *   <li>Gestión de cookies robusta con {@code BasicCookieStore}.</li>
 *   <li>Sin dependencias de terceros adicionales — HC5 ya estaba en el classpath.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>Una instancia de esta clase NO es thread-safe por diseño: el estado interno
 * (headers, body, cookies, etc.) es mutable y pertenece a un único escenario BDD.
 * Para ejecuciones paralelas, cada hilo debe usar su propia instancia
 * (gestionada por {@link com.qa.httpcore.factories.HttpClientFactory} y el
 * {@link com.qa.common.api.runtime.ServiceRegistry}).
 *
 * @author Abel Venero
 * @version 2.2.0
 * @since 2.2.0
 * @see HttpClient
 * @see BaseHttpClient
 */
public class ApacheHttpClientImpl implements HttpClient {

    // ── Constantes ────────────────────────────────────────────────────────────

    private static final int DEFAULT_CONNECT_TIMEOUT_MS  = 30_000;
    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 60_000;
    private static final String DEFAULT_CONTENT_TYPE     = "application/json";
    private static final String DEFAULT_ACCEPT_TYPE      = "application/json";
    private static final int REQUEST_ID_LENGTH           = 8;

    // ── Estado de configuración HTTP ──────────────────────────────────────────

    private String host;
    private final Map<String, String>  headers     = new LinkedHashMap<>();
    private final Map<String, String>  queryParams = new LinkedHashMap<>();
    private final Map<String, String>  fields      = new LinkedHashMap<>();
    private final Map<String, String>  cookies     = new LinkedHashMap<>();
    private final Map<String, String>  userCtx     = new LinkedHashMap<>();
    private String  body;
    private String  contentType = DEFAULT_CONTENT_TYPE;
    private String  acceptType  = DEFAULT_ACCEPT_TYPE;
    private String  userId;
    private String  sessionId;
    private boolean autoApplyUserCtx    = false;
    private boolean automaticCookies    = true;

    // ── Configuración técnica ─────────────────────────────────────────────────

    private int  connectTimeoutMs  = DEFAULT_CONNECT_TIMEOUT_MS;
    private int  responseTimeoutMs = DEFAULT_RESPONSE_TIMEOUT_MS;
    private int  maxRetries        = 0;
    private int  retryDelayMs      = 0;

    // ── Configuración SSL ─────────────────────────────────────────────────────

    /** null = JVM default TLS (SYSTEM mode). */
    private SSLContext sslContext  = null;
    /** true = NoopHostnameVerifier + trust-all (solo entornos non-prod). */
    private boolean    trustAllSsl = false;

    // ── Estado de la última petición ──────────────────────────────────────────

    private HttpResponse lastResponse;
    private String       lastResponseBodyCache;
    private String       lastUrl;
    private String       lastMethod;
    private long         lastDuration;
    private String       lastRequestBody;
    private String       lastRequestId;
    private Map<String, String> lastRequestHeadersSnapshot = Collections.emptyMap();

    // ── Cookie store compartido entre redirects ───────────────────────────────

    private final BasicCookieStore cookieStore = new BasicCookieStore();

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

    @Override
    public String getHost() { return host; }

    @Override
    public boolean hasValidHost() {
        return host != null && !host.isBlank();
    }

    // =========================================================================
    // HttpRequestBuilder — HEADERS
    // =========================================================================

    @Override
    public Map<String, String> getHeaders() { return Collections.unmodifiableMap(headers); }

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

    @Override
    public void clearQueryParams() { queryParams.clear(); }

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

    @Override
    public void setBody(String body) { this.body = body; }

    @Override
    public boolean hasBody() { return body != null && !body.isBlank(); }

    @Override
    public long getBodySize() { return body != null ? body.getBytes(StandardCharsets.UTF_8).length : 0L; }

    @Override
    public String getBody() { return body; }

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

    @Override
    public Map<String, String> getCookies() { return Collections.unmodifiableMap(cookies); }

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

    @Override
    public void clearCookies() {
        cookies.clear();
        cookieStore.clear();
    }

    @Override
    public void setAutomaticCookieHandling(boolean enabled) { this.automaticCookies = enabled; }

    @Override
    public boolean isAutomaticCookieHandlingEnabled() { return automaticCookies; }

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
        this.userId    = userId;
        this.sessionId = sessionId;
    }

    @Override
    public void setUserContext(String userId, String sessionId, Map<String, String> additional) {
        setUserContext(userId, sessionId);
        if (additional != null) {
            userCtx.putAll(additional);
        }
    }

    @Override
    public String getCurrentUserId()    { return userId; }

    @Override
    public String getCurrentSessionId() { return sessionId; }

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

    @Override
    public boolean hasUserContext() { return userId != null && sessionId != null; }

    @Override
    public void clearUserContext() {
        userId = null;
        sessionId = null;
        userCtx.clear();
    }

    @Override
    public void setAutoApplyUserContext(boolean enabled) { this.autoApplyUserCtx = enabled; }

    @Override
    public boolean isAutoApplyUserContextEnabled() { return autoApplyUserCtx; }

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

    @Override
    public String getContentType() { return contentType; }

    @Override
    public void setAcceptType(String at) {
        if (at == null || at.isBlank()) {
            throw new IllegalArgumentException("acceptType no puede ser null/vacío");
        }
        this.acceptType = at;
    }

    @Override
    public String getAcceptType() { return acceptType; }

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

    @Override
    public void configureForFormData() {
        contentType = "application/x-www-form-urlencoded";
    }

    @Override
    public void configureForPlainText() {
        contentType = "text/plain";
        acceptType  = "text/plain";
    }

    @Override
    public void configureForMultipart() {
        contentType = "multipart/form-data";
    }

    @Override
    public boolean isJsonContentType()     { return contentType != null && contentType.contains("json"); }

    @Override
    public boolean isXmlContentType()      { return contentType != null && contentType.contains("xml"); }

    @Override
    public boolean isFormDataContentType() {
        return contentType != null && contentType.contains("form");
    }

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

        String requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH);
        lastRequestId = requestId;

        String fullUrl = buildUrl(endpoint);
        lastUrl    = fullUrl;
        lastMethod = method.name();
        lastRequestBody = body;

        TestLogger.logInfo("HTTP_CLIENT",
            "[" + requestId + "] " + method + " → " + fullUrl, null);

        Map<String, String> headersSnapshot = buildEffectiveHeaders();
        lastRequestHeadersSnapshot = Collections.unmodifiableMap(new HashMap<>(headersSnapshot));

        long start = System.currentTimeMillis();
        HttpResponse response = executeWithRetry(
                method, fullUrl, headersSnapshot, followRedirects, timeoutMs, requestId);
        lastDuration = System.currentTimeMillis() - start;
        lastResponse = response;
        lastResponseBodyCache = response != null ? response.getBody() : null;

        TestLogger.logInfo("HTTP_CLIENT",
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

    @Override
    public HttpResponse getLastResponse()               { return lastResponse; }

    @Override
    public String getLastRequestUrl()                   { return lastUrl; }

    @Override
    public String getLastRequestMethod()                { return lastMethod; }

    @Override
    public long getLastRequestDuration()                { return lastResponse != null ? lastDuration : -1L; }

    @Override
    public String getLastRequestBody()                  { return lastRequestBody; }

    @Override
    public String getLastRequestId()                    { return lastRequestId; }

    @Override
    public Map<String, String> getLastRequestHeadersSnapshot() { return lastRequestHeadersSnapshot; }

    @Override
    public String getLastResponseBody() { return lastResponseBodyCache; }

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

    @Override
    public int getConnectionTimeout() { return connectTimeoutMs; }

    @Override
    public int getReadTimeout()       { return responseTimeoutMs; }

    @Override
    public void setRetryPolicy(int maxRetries, int retryDelayMs) {
        if (maxRetries < 0 || retryDelayMs < 0) {
            throw new IllegalArgumentException("maxRetries y retryDelayMs deben ser >= 0");
        }
        this.maxRetries   = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public boolean isRetryEnabled() { return maxRetries > 0; }

    @Override
    public int getMaxRetries()  { return maxRetries; }

    @Override
    public int getRetryDelay()  { return retryDelayMs; }

    @Override
    public void configureSsl(SSLContext ctx, boolean trustAll) {
        this.sslContext   = ctx;
        this.trustAllSsl  = trustAll;
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
        cookieStore.clear();
        userCtx.clear();
        userId    = null;
        sessionId = null;
        host      = null;
        lastResponse          = null;
        lastResponseBodyCache = null;
        lastUrl               = null;
        lastMethod            = null;
        lastDuration          = 0L;
        lastRequestBody       = null;
        lastRequestId         = null;
        lastRequestHeadersSnapshot = Collections.emptyMap();
    }

    @Override
    public String getDebugInfo() {
        return String.format(
            "ApacheHttpClientImpl{host='%s', headers=%d, queryParams=%d, cookies=%d, " +
            "body=%s, contentType='%s', connectTimeout=%dms, responseTimeout=%dms, maxRetries=%d}",
            host, headers.size(), queryParams.size(), cookies.size(),
            hasBody() ? getBodySize() + " bytes" : "none",
            contentType, connectTimeoutMs, responseTimeoutMs, maxRetries);
    }

    // =========================================================================
    // Implementación interna — construcción de petición
    // =========================================================================

    private String buildUrl(String endpoint) {
        String base = host.replaceAll("/+$", "");
        String path;
        if (endpoint == null || endpoint.isBlank()) {
            path = "";
        } else {
            path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        }

        if (queryParams.isEmpty()) {
            return base + path;
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(base + path);
            queryParams.forEach(uriBuilder::addParameter);
            return uriBuilder.build().toString();
        } catch (Exception e) {
            TestLogger.logWarning("HTTP_CLIENT",
                "Error construyendo URL con query params: " + e.getMessage(), null);
            // Fallback: append manually
            StringBuilder sb = new StringBuilder(base).append(path).append("?");
            queryParams.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            String result = sb.toString();
            return result.endsWith("&") ? result.substring(0, result.length() - 1) : result;
        }
    }

    private Map<String, String> buildEffectiveHeaders() {
        Map<String, String> effective = new LinkedHashMap<>(headers);
        effective.put("Content-Type", contentType);
        effective.put("Accept",       acceptType);
        if (autoApplyUserCtx && hasUserContext()) {
            effective.put("X-User-Id",    userId);
            effective.put("X-Session-Id", sessionId);
        }
        return effective;
    }

    private HttpUriRequestBase buildRequest(HttpMethod method, String url,
                                            Map<String, String> effectiveHeaders) {
        HttpUriRequestBase req = switch (method) {
            case GET     -> new HttpGet(url);
            case POST    -> new HttpPost(url);
            case PUT     -> new HttpPut(url);
            case DELETE  -> new HttpDelete(url);
            case PATCH   -> new HttpPatch(url);
            case HEAD    -> new HttpHead(url);
            case OPTIONS -> new HttpOptions(url);
        };

        effectiveHeaders.forEach(req::setHeader);

        // Cookie header manual (además del CookieStore para control explícito)
        if (!cookies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            cookies.forEach((k, v) -> sb.append(k).append("=").append(v).append("; "));
            String cookieHeader = sb.toString().trim();
            if (cookieHeader.endsWith(";")) {
                cookieHeader = cookieHeader.substring(0, cookieHeader.length() - 1);
            }
            req.setHeader("Cookie", cookieHeader);
        }

        // Body / form fields
        if (hasBody()) {
            ContentType ct = resolveApacheContentType(contentType);
            setEntityOnRequest(req, new StringEntity(body, ct));
        } else if (!fields.isEmpty() && isFormDataContentType()) {
            List<BasicNameValuePair> pairs = new ArrayList<>();
            fields.forEach((k, v) -> pairs.add(new BasicNameValuePair(k, v)));
            setEntityOnRequest(req, new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
        }

        return req;
    }

    private void setEntityOnRequest(HttpUriRequestBase req, org.apache.hc.core5.http.HttpEntity entity) {
        if (req instanceof HttpPost p) {
            p.setEntity(entity);
            return;
        }
        if (req instanceof HttpPut p) {
            p.setEntity(entity);
            return;
        }
        if (req instanceof HttpPatch p) {
            p.setEntity(entity);
            return;
        }
        if (req instanceof HttpDelete d) {
            d.setEntity(entity);
        }
        // GET/HEAD/OPTIONS — body ignorado por semántica HTTP
    }

    private ContentType resolveApacheContentType(String ct) {
        if (ct == null) {
            return ContentType.APPLICATION_JSON;
        }
        if (ct.contains("json")) {
            return ContentType.APPLICATION_JSON;
        }
        if (ct.contains("xml")) {
            return ContentType.APPLICATION_XML;
        }
        if (ct.contains("form")) {
            return ContentType.APPLICATION_FORM_URLENCODED;
        }
        if (ct.contains("text")) {
            return ContentType.TEXT_PLAIN;
        }
        try {
            return ContentType.parse(ct);
        } catch (Exception e) {
            return ContentType.DEFAULT_TEXT;
        }
    }

    // =========================================================================
    // Implementación interna — ejecución con retry
    // =========================================================================

    private HttpResponse executeWithRetry(
            HttpMethod method, String url, Map<String, String> headers,
            boolean followRedirects, int timeoutMs, String requestId)
            throws FrameworkTechnicalException {

        Exception lastEx = null;
        int attempts = maxRetries + 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (attempt > 1) {
                TestLogger.logInfo("HTTP_CLIENT",
                    "[" + requestId + "] Reintento " + (attempt - 1) + "/" + maxRetries, null);
                sleepQuietly(retryDelayMs);
            }
            try {
                return doExecute(method, url, headers, followRedirects, timeoutMs);
            } catch (Exception e) {
                lastEx = e;
                TestLogger.logWarning("HTTP_CLIENT",
                    "[" + requestId + "] Error en intento " + attempt + ": " + e.getMessage(), null);
            }
        }

        throw new FrameworkTechnicalException(
            "Petición " + method + " " + url + " falló después de " + attempts + " intento(s): "
            + (lastEx != null ? lastEx.getMessage() : "error desconocido"), lastEx);
    }

    private HttpResponse doExecute(
            HttpMethod method, String url, Map<String, String> effectiveHeaders,
            boolean followRedirects, int timeoutMs) throws Exception {

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.of(connectTimeoutMs, TimeUnit.MILLISECONDS))
            .setResponseTimeout(Timeout.of(timeoutMs, TimeUnit.MILLISECONDS))
            .setRedirectsEnabled(followRedirects)
            .build();

        // Sincronizar cookies manuales en el cookie store de Apache
        syncCookiesToStore(url);

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(automaticCookies ? cookieStore : null);

        if (trustAllSsl || sslContext != null) {
            javax.net.ssl.SSLContext ctx = trustAllSsl
                ? SSLContextFactory.createTrustAllContext(resolveProfileForTrustAllGuard(), url)
                : sslContext;
            var sslSFBuilder = SSLConnectionSocketFactoryBuilder.create().setSslContext(ctx);
            if (trustAllSsl) {
                sslSFBuilder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            var cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSFBuilder.build())
                .build();
            httpClientBuilder.setConnectionManager(cm);
        }

        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {

            HttpUriRequestBase request = buildRequest(method, url, effectiveHeaders);

            try (CloseableHttpResponse apacheResponse = httpClient.execute(request)) {
                return buildHttpResponse(apacheResponse);
            }
        }
    }

    /**
     * Resuelve el {@link ExecutionProfile} desde el {@link ExecutionContext} per-thread.
     * Fail-closed a {@link ExecutionProfile#PROD} cuando no hay contexto activo (CLI sin BE,
     * tests sin ctx) — alinea con el contrato fail-closed de IEP-1 y deja que el guard
     * de W1-T3-M3 enforce el rechazo si además el host es público.
     */
    private static ExecutionProfile resolveProfileForTrustAllGuard() {
        return ExecutionContext.current()
            .map(ctx -> ctx.config().getProfile())
            .orElse(ExecutionProfile.PROD);
    }

    private HttpResponse buildHttpResponse(CloseableHttpResponse apacheResponse) throws Exception {
        int statusCode = apacheResponse.getCode();

        String responseBody = "";
        var entity = apacheResponse.getEntity();
        if (entity != null) {
            responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }

        Map<String, String> responseHeaders = new HashMap<>();
        for (var header : apacheResponse.getHeaders()) {
            responseHeaders.put(header.getName(), header.getValue());
        }

        // Capturar cookies de respuesta en el mapa interno si el manejo automático está habilitado
        if (automaticCookies) {
            cookieStore.getCookies().forEach(c -> cookies.put(c.getName(), c.getValue()));
        }

        return new HttpResponse(statusCode, responseBody, responseHeaders, 0);
    }

    private void syncCookiesToStore(String requestUrl) {
        if (cookies.isEmpty()) {
            return;
        }
        try {
            String domain = URI.create(requestUrl).getHost();
            cookies.forEach((name, value) -> {
                BasicClientCookie cookie = new BasicClientCookie(name, value);
                cookie.setDomain(domain);
                cookie.setPath("/");
                cookieStore.addCookie(cookie);
            });
        } catch (Exception e) {
            TestLogger.logWarning("HTTP_CLIENT",
                "No se pudieron sincronizar cookies al store: " + e.getMessage(), null);
        }
    }

    // =========================================================================
    // Validaciones internas
    // =========================================================================

    private void validateHost() {
        if (!hasValidHost()) {
            throw new IllegalStateException(
                "Host no configurado. Llama a setHost(url) antes de ejecutar peticiones. " +
                "Ejemplo: client.setHost(\"https://api.example.com\")");
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
}
