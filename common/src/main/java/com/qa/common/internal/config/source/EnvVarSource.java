package com.qa.common.internal.config.source;

import com.qa.common.api.Internal;

import java.util.Optional;
import java.util.function.Function;

/**
 * Source de prioridad 2: lee variables de entorno traduciendo la clave
 * canónica camelCase a {@code UPPER_SNAKE_CASE}.
 *
 * <p>Ejemplo: {@code "http.connectTimeout"} → {@code "HTTP_CONNECT_TIMEOUT"}.
 *
 * @since TASK-K03
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader para acceder")
public final class EnvVarSource implements ConfigSource {

    private final Function<String, String> envGetter;

    public EnvVarSource() {
        this(System::getenv);
    }

    /** Constructor para tests con env getter inyectable. */
    public EnvVarSource(Function<String, String> envGetter) {
        this.envGetter = envGetter;
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String envKey = toEnvVarName(key);
        String v = envGetter.apply(envKey);
        return (v == null || v.isEmpty()) ? Optional.empty() : Optional.of(v);
    }

    @Override
    public String name() {
        return "EnvVar";
    }

    /**
     * Traduce {@code "http.connectTimeout"} → {@code "HTTP_CONNECT_TIMEOUT"}.
     * <ul>
     *   <li>'.' (punto) → '_'</li>
     *   <li>Inserción de '_' antes de cada letra mayúscula precedida de
     *       letra minúscula o dígito (camelCase → SNAKE_CASE)</li>
     *   <li>Resultado a mayúsculas</li>
     * </ul>
     */
    static String toEnvVarName(String key) {
        StringBuilder out = new StringBuilder(key.length() + 4);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '.') {
                out.append('_');
            } else if (Character.isUpperCase(c) && i > 0) {
                char prev = key.charAt(i - 1);
                if (Character.isLowerCase(prev) || Character.isDigit(prev)) {
                    out.append('_');
                }
                out.append(c);
            } else {
                out.append(Character.toUpperCase(c));
            }
        }
        return out.toString();
    }
}
