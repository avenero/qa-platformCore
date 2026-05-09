package com.qa.common.runtime;

/**
 * Selector del motor HTTP que se utilizará durante una ejecución BDD.
 *
 * <p>Forma parte de {@link ExecutionConfig#getHttpEngine()} y es resuelto por
 * {@code HttpClientFactory} (TASK-E04) en el momento de crear la instancia de
 * {@code HttpClient} para el escenario.
 *
 * <h2>Trade-offs</h2>
 * <table>
 *   <caption>Comparativa de motores HTTP</caption>
 *   <tr><th>Aspecto</th><th>{@link #PLAYWRIGHT}</th><th>{@link #APACHE}</th></tr>
 *   <tr><td>Sesión compartida con browser</td><td>✅ vía {@code BrowserContext.request()}</td><td>❌</td></tr>
 *   <tr><td>Overhead inicial</td><td>~300ms (binario nativo)</td><td>~0ms</td></tr>
 *   <tr><td>HTTP/2 / TLS modernos</td><td>✅ delegado al binario</td><td>✅ HC5</td></tr>
 *   <tr><td>Configuración SSL granular</td><td>limitada (browser-level)</td><td>✅ por petición</td></tr>
 *   <tr><td>Dependencia adicional</td><td>requiere binario Playwright</td><td>ya en classpath</td></tr>
 * </table>
 *
 * <h2>Resolución del default</h2>
 * <p>Se resuelve en este orden:
 * <ol>
 *   <li>Valor explícito pasado a {@link ExecutionConfig.Builder#httpEngine(HttpEngine)}.</li>
 *   <li>System property {@code execution.http.engine} (case-insensitive).</li>
 *   <li>Variable de entorno {@code EXECUTION_HTTP_ENGINE} (case-insensitive).</li>
 *   <li>Constante {@link #PLAYWRIGHT}.</li>
 * </ol>
 *
 * <p>El FE expone un selector que el BE traduce al string de este enum y propaga
 * vía system property o directamente al Builder. Valores inválidos se ignoran y
 * caen al default sin lanzar excepciones (resolución best-effort).
 *
 * @author Abel Venero
 * @since TASK-E03
 * @see ExecutionConfig#getHttpEngine()
 */
public enum HttpEngine {

    /**
     * Motor basado en {@code com.microsoft.playwright.APIRequestContext} (default).
     * <p>Permite compartir cookies/storage/auth con el browser (login API → Page autenticada).
     */
    PLAYWRIGHT,

    /**
     * Motor basado en Apache HttpClient 5 (fallback).
     * <p>Sin overhead de binario nativo; usar cuando no se necesita sesión compartida con browser.
     */
    APACHE;

    /** System property que el FE/BE pueden configurar para forzar un motor. */
    public static final String SYSTEM_PROPERTY = "execution.http.engine";

    /** Variable de entorno equivalente a {@link #SYSTEM_PROPERTY}. */
    public static final String ENV_VARIABLE = "EXECUTION_HTTP_ENGINE";

    /**
     * Resuelve el motor HTTP a partir de system property y variables de entorno.
     * Valores inválidos o ausentes resuelven a {@link #PLAYWRIGHT}.
     *
     * @return motor HTTP resuelto, nunca {@code null}
     */
    public static HttpEngine resolveDefault() {
        HttpEngine fromProp = parseSilently(System.getProperty(SYSTEM_PROPERTY));
        if (fromProp != null) {
            return fromProp;
        }
        HttpEngine fromEnv = parseSilently(System.getenv(ENV_VARIABLE));
        if (fromEnv != null) {
            return fromEnv;
        }
        return PLAYWRIGHT;
    }

    /**
     * Parsea {@code value} como {@link HttpEngine} (case-insensitive). Retorna
     * {@code null} si {@code value} es null/blank o no coincide con ningún miembro.
     *
     * <p>API pública para consumidores (BE) que reciben strings desde la capa FE.
     *
     * @param value string a parsear, puede ser null
     * @return motor parseado o {@code null} si no coincide
     */
    public static HttpEngine parseSilently(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return HttpEngine.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
