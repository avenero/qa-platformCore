package com.qa.common.api.config;

import java.util.Optional;

/**
 * Carga configs tipados desde la jerarquía de fuentes documentada en
 * {@link TypedConfig} (TASK-K03).
 *
 * <p>Contrato:
 * <ul>
 *   <li>{@link #load} es <strong>idempotente</strong> y <strong>cacheado</strong>:
 *       múltiples llamadas con la misma clase retornan la misma instancia.</li>
 *   <li>{@link #reload} fuerza una nueva lectura (útil en tests que cambian
 *       SystemProperties o env vars dinámicamente).</li>
 *   <li>Si la validación falla, lanza {@link ConfigValidationException} con
 *       todos los errores agregados.</li>
 *   <li>{@link #getRaw} ofrece un escape hatch sin parser ni validación para
 *       call sites que requieren claves dinámicas resueltas en runtime
 *       (TASK-K03M-F1).</li>
 * </ul>
 *
 * @author Abel Venero
 * @since TASK-K03 (2026-05-11)
 */
public interface ConfigLoader {

    /**
     * Carga el config tipado y lo cachea para llamadas subsiguientes.
     *
     * @param configClass clase del record que implementa {@link TypedConfig}
     * @param <T> tipo del config
     * @return instancia validada del config; nunca null
     * @throws ConfigValidationException si la validación Bean Validation falla
     * @throws IllegalArgumentException si la clase no es un record con
     *         constructor canónico y método estático {@code defaults()}
     */
    <T extends TypedConfig> T load(Class<T> configClass);

    /**
     * Re-lee las fuentes ignorando el cache. Útil cuando se cambian
     * propiedades de entorno en tests.
     */
    <T extends TypedConfig> T reload(Class<T> configClass);

    /**
     * Escape hatch para claves resueltas en runtime que no encajan en records
     * inmutables (e.g. {@code "{dbType}.db.url"} en {@code DbConnectorFactory}
     * o {@code propertyKey} desde un .feature en {@code ApiHelper}).
     *
     * <p>Recorre el mismo stack de {@code ConfigSource}s que {@link #load}
     * pero <strong>sin parser ni Bean Validation</strong> — devuelve el string
     * crudo del primer source que tenga la key.
     *
     * <p>Default {@link Optional#empty()} para no romper implementaciones
     * preexistentes; {@code DefaultConfigLoader} sobreescribe.
     *
     * @param key clave a resolver (formato libre — no se asume prefijo de record)
     * @return valor crudo o {@link Optional#empty()} si ningún source lo tiene
     * @since TASK-K03M-F1
     */
    default Optional<String> getRaw(String key) {
        return Optional.empty();
    }

    /**
     * Variante con default. Equivale a {@code getRaw(key).orElse(defaultValue)}.
     *
     * @since TASK-K03M-F1
     */
    default String getRaw(String key, String defaultValue) {
        return getRaw(key).orElse(defaultValue);
    }
}
