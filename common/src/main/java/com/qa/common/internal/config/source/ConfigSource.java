package com.qa.common.internal.config.source;

import com.qa.common.api.Internal;

import java.util.Optional;

/**
 * Fuente abstracta de configuración para el {@code TypedConfig} pipeline
 * (TASK-K03). Cada implementación lee de una source concreta (SystemProperty,
 * env var, YAML, …) y resuelve claves por nombre.
 *
 * <p>Las implementaciones son <strong>read-only</strong>, thread-safe, y
 * tolerantes a ausencia (retornan {@link Optional#empty()} si la key no existe).
 *
 * @since TASK-K03
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader para acceder")
public interface ConfigSource {

    /**
     * Resuelve una key en formato {@code prefix.componentName} (camelCase
     * canónico). Implementaciones pueden traducir a su nomenclatura nativa
     * (e.g. EnvVarSource convierte a UPPER_SNAKE_CASE).
     *
     * @param key clave canónica camelCase (e.g. {@code "http.connectTimeout"})
     * @return valor crudo en string, o vacío si la fuente no tiene la key
     */
    Optional<String> get(String key);

    /** Nombre breve de la fuente para mensajes de error y logs. */
    String name();
}
