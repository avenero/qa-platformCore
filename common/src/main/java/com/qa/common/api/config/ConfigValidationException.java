package com.qa.common.api.config;

import jakarta.validation.ConstraintViolation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lanzada por {@link ConfigLoader} cuando un {@link TypedConfig} viola sus
 * constraints de Bean Validation (TASK-K03).
 *
 * <p>El mensaje agrega todas las violaciones para que el dev tenga el
 * panorama completo en un solo error (no aparece la primera y se va
 * pelando una por una).
 *
 * @author Abel Venero
 * @since TASK-K03
 */
public class ConfigValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Class<? extends TypedConfig> configClass;
    private final transient Set<? extends ConstraintViolation<?>> violations;

    public ConfigValidationException(Class<? extends TypedConfig> configClass,
                                     Set<? extends ConstraintViolation<?>> violations) {
        super(buildMessage(configClass, violations));
        this.configClass = configClass;
        this.violations = violations;
    }

    public Class<? extends TypedConfig> getConfigClass() {
        return configClass;
    }

    public Set<? extends ConstraintViolation<?>> getViolations() {
        return violations;
    }

    private static String buildMessage(Class<?> configClass,
                                       Set<? extends ConstraintViolation<?>> violations) {
        String detail = violations.stream()
                .map(v -> "  - " + v.getPropertyPath() + ": " + v.getMessage()
                        + " (rejected value: " + redact(v.getInvalidValue()) + ")")
                .collect(Collectors.joining("\n"));
        return "Invalid configuration for "
                + (configClass != null ? configClass.getSimpleName() : "<unknown>")
                + " (" + violations.size() + " violation"
                + (violations.size() == 1 ? "" : "s") + "):\n" + detail;
    }

    /** Redacta valores que podrían ser sensibles (passwords, tokens). */
    private static Object redact(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value);
        // Heurística pasiva: oculta strings que parecen tokens/passwords largos.
        // Las claves "password"/"secret"/"token" se redactan en logs, no aquí
        // (este método solo redacta valores largos sin estructura — last-resort).
        if (s.length() > 64) return "<redacted:" + s.length() + " chars>";
        return s;
    }
}
