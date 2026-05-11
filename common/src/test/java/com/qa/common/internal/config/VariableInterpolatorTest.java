package com.qa.common.internal.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-K03M-F3 — VariableInterpolator.
 */
class VariableInterpolatorTest {

    @AfterEach
    void cleanup() {
        System.clearProperty("k03m.f3.foo");
        System.clearProperty("k03m.f3.bar");
        System.clearProperty("db.url");
    }

    @Test
    @DisplayName("Interpola SystemProperty literal")
    void interpolates_singleSysProp() {
        System.setProperty("k03m.f3.foo", "x");
        assertThat(VariableInterpolator.interpolate("${k03m.f3.foo}/bar"))
                .isEqualTo("x/bar");
    }

    @Test
    @DisplayName("Resuelve env var traduciendo a UPPER_SNAKE")
    void interpolates_envVarUpperSnake() {
        // PATH es siempre una env var presente; usamos su valor para una assertion estable.
        String pathValue = System.getenv("PATH");
        org.junit.jupiter.api.Assumptions.assumeTrue(pathValue != null && !pathValue.isBlank(),
                "PATH env var no disponible — test skipped");
        // varName "path" → upper "PATH" → match
        assertThat(VariableInterpolator.interpolate("path-is:${path}"))
                .isEqualTo("path-is:" + pathValue);
    }

    @Test
    @DisplayName("Variable no existente queda literal ${MISSING}")
    void leavesUnresolvedAsIs() {
        assertThat(VariableInterpolator.interpolate("a-${k03m.does.not.exist.42}-b"))
                .isEqualTo("a-${k03m.does.not.exist.42}-b");
    }

    @Test
    @DisplayName("Sin '${' es passthrough sin alocaciones extras")
    void noInterpolation_whenNoDollarBrace() {
        assertThat(VariableInterpolator.interpolate("plain-string")).isEqualTo("plain-string");
        assertThat(VariableInterpolator.interpolate("")).isEqualTo("");
        assertThat(VariableInterpolator.interpolate(null)).isNull();
    }

    @Test
    @DisplayName("Múltiples placeholders mixtos resueltos y literales")
    void interpolates_multiplePlaceholders() {
        System.setProperty("k03m.f3.foo", "FOO");
        System.setProperty("k03m.f3.bar", "BAR");
        assertThat(VariableInterpolator.interpolate("${k03m.f3.foo}-${k03m.absent}-${k03m.f3.bar}"))
                .isEqualTo("FOO-${k03m.absent}-BAR");
    }
}
