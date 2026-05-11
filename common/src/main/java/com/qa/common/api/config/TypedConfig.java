package com.qa.common.api.config;

/**
 * Marker interface for **typed, validated, immutable configuration records**
 * (TASK-K03).
 *
 * <p>Implementaciones esperadas: Java {@code record}s en
 * {@code com.qa.common.api.config.*} (y futuras extensiones por dominio).
 * Cada record declara sus componentes con anotaciones de Bean Validation
 * ({@code @NotNull}, {@code @Min}, {@code @Pattern}, …) y debe exponer un
 * método estático {@code defaults()} que retorne una instancia con valores
 * razonables.
 *
 * <h2>Jerarquía de fuentes (priority order, post K03-migrate F4)</h2>
 *
 * <ol>
 *   <li><b>ExecutionContext:</b> {@code ExecutionContext.current().config().getProperty(key)}
 *       — per-thread, valores finales (sin {@code ${VAR}} interpolation).</li>
 *   <li><b>SystemProperty:</b> {@code -Dhttp.connectTimeout=PT30S}</li>
 *   <li><b>Env Var:</b> {@code HTTP_CONNECT_TIMEOUT=PT30S}</li>
 *   <li><b>Properties file (classpath):</b> cascade
 *       {@code config-{env}.properties → config-app.properties → config.properties}</li>
 *   <li><b>YAML file:</b> {@code application.yml} con {@code http.connectTimeout: PT30S}</li>
 *   <li><b>Default:</b> valor de {@code defaults()} del record</li>
 * </ol>
 *
 * <p>Los valores de {@code SystemProperty}, {@code EnvVar}, {@code Properties}
 * y {@code YAML} pasan por {@code VariableInterpolator} (resuelve {@code ${VAR}}
 * contra system props y env vars).
 *
 * <p>Resolución de claves: el prefijo del record se deriva de su nombre
 * (e.g., {@code HttpConfig} → {@code http}). Cada componente del record se
 * lee como {@code &lt;prefix&gt;.&lt;componentName&gt;} (camelCase) en sources
 * orientados a properties, o como {@code &lt;PREFIX&gt;_&lt;COMPONENT_NAME&gt;}
 * (UPPER_SNAKE_CASE) en env vars.
 *
 * <h2>Reglas inviolables (TASK-K03)</h2>
 *
 * <ul>
 *   <li><b>R-K03-1:</b> implementaciones de {@code TypedConfig} son
 *       <strong>siempre</strong> {@code record}s (inmutables).</li>
 *   <li><b>R-K03-2:</b> cada record DEBE declarar un método estático
 *       {@code public static T defaults()} retornando una instancia válida.</li>
 *   <li><b>R-K03-3:</b> los componentes del record DEBEN llevar
 *       anotaciones de Bean Validation cuando aplique. El loader rechaza
 *       construcciones inválidas con {@link ConfigValidationException}.</li>
 *   <li><b>R-K03-4:</b> NO incluir secretos directos en defaults() — usar
 *       placeholders + resolverlos desde env vars / secret manager.</li>
 * </ul>
 *
 * @author Abel Venero
 * @since TASK-K03 (2026-05-11)
 */
public interface TypedConfig {

    /**
     * Prefijo de configuración derivado del nombre de la clase.
     * Por defecto: el nombre simple del record en lowercase sin sufijo "Config".
     * <p>Ejemplos: {@code HttpConfig} → {@code "http"}, {@code MobileConfig} →
     * {@code "mobile"}.
     */
    default String configPrefix() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Config")) {
            simpleName = simpleName.substring(0, simpleName.length() - "Config".length());
        }
        return simpleName.toLowerCase();
    }
}
