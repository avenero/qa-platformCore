package com.qa.common.api.config;

import com.qa.common.api.runtime.HttpEngine;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Duration;

/**
 * Configuración HTTP del Core (TASK-K03).
 *
 * <p>Prefijo de configuración: {@code http}. Ejemplos de keys:
 * <ul>
 *   <li>SystemProperty: {@code -Dhttp.engine=APACHE -Dhttp.connectTimeout=PT5S}</li>
 *   <li>Env Var: {@code HTTP_ENGINE=APACHE  HTTP_CONNECT_TIMEOUT=PT5S}</li>
 *   <li>YAML: {@code http: { engine: APACHE, connectTimeout: PT5S }}</li>
 * </ul>
 *
 * @param engine          Motor HTTP — PLAYWRIGHT default, APACHE legacy.
 * @param baseUrl         URL base canónica para steps relativos. Debe ser http(s).
 * @param connectTimeout  Timeout de conexión al servidor.
 * @param readTimeout     Timeout de lectura por request.
 * @param maxRetries      Reintentos extra ante fallo transitorio (0..10).
 * @param followRedirects Si seguir redirects 3xx automáticamente.
 *
 * @since TASK-K03
 */
public record HttpConfig(
        @NotNull HttpEngine engine,
        @NotNull @Pattern(regexp = "^https?://.*", message = "baseUrl debe empezar con http:// o https://")
        String baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(0) @Max(10) int maxRetries,
        boolean followRedirects
) implements TypedConfig {

    /**
     * Valores razonables por defecto. Sobre estos defaults se aplica el merge
     * de SystemProperty → Env → YAML.
     */
    public static HttpConfig defaults() {
        return new HttpConfig(
                HttpEngine.resolveDefault(),
                "http://localhost",
                Duration.ofSeconds(10),
                Duration.ofSeconds(60),
                0,
                true
        );
    }
}
