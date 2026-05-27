package com.qa.common.internal.config.source;

import com.qa.common.api.Internal;

import java.util.Optional;

/**
 * Source de mayor prioridad: lee {@link System#getProperty(String)} con la
 * clave canónica camelCase tal cual.
 *
 * @since TASK-K03
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader para acceder")
public final class SystemPropertySource implements ConfigSource {

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank()) { return Optional.empty(); }
        String v = System.getProperty(key);
        return (v == null || v.isEmpty()) ? Optional.empty() : Optional.of(v);
    }

    @Override
    public String name() {
        return "SystemProperty";
    }
}
