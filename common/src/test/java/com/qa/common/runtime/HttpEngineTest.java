package com.qa.common.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpEngine — selector de motor HTTP por ejecución")
class HttpEngineTest {

    private String previousProp;

    @BeforeEach
    void capturePrevious() {
        previousProp = System.getProperty(HttpEngine.SYSTEM_PROPERTY);
        System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
    }

    @AfterEach
    void restorePrevious() {
        if (previousProp == null) {
            System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
        } else {
            System.setProperty(HttpEngine.SYSTEM_PROPERTY, previousProp);
        }
    }

    @Test
    @DisplayName("enum tiene exactamente PLAYWRIGHT y APACHE")
    void enumContieneAmbosValores() {
        assertThat(HttpEngine.values()).containsExactly(HttpEngine.PLAYWRIGHT, HttpEngine.APACHE);
    }

    @Test
    @DisplayName("resolveDefault sin overrides retorna PLAYWRIGHT")
    void resolveDefaultSinOverrides() {
        assertThat(HttpEngine.resolveDefault()).isEqualTo(HttpEngine.PLAYWRIGHT);
    }

    @ParameterizedTest
    @EnumSource(HttpEngine.class)
    @DisplayName("system property fuerza el motor (case-insensitive)")
    void systemPropertyFuerzaMotor(HttpEngine engine) {
        System.setProperty(HttpEngine.SYSTEM_PROPERTY, engine.name().toLowerCase());
        assertThat(HttpEngine.resolveDefault()).isEqualTo(engine);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "  ", "playwrigh", "APACHE_HC"})
    @DisplayName("system property con valor inválido cae al default PLAYWRIGHT")
    void systemPropertyInvalidaCaeAlDefault(String invalid) {
        System.setProperty(HttpEngine.SYSTEM_PROPERTY, invalid);
        assertThat(HttpEngine.resolveDefault()).isEqualTo(HttpEngine.PLAYWRIGHT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "nope", "PLAYWRIGTH"})
    @DisplayName("parseSilently retorna null para valores inválidos/null/vacíos")
    void parseSilentlyTolerante(String value) {
        assertThat(HttpEngine.parseSilently(value)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "playwright, PLAYWRIGHT",
        "PLAYWRIGHT, PLAYWRIGHT",
        "  apache  , APACHE",
        "Apache,     APACHE"
    })
    @DisplayName("parseSilently trim + case-insensitive para valores válidos")
    void parseSilentlyValidos(String input, HttpEngine expected) {
        assertThat(HttpEngine.parseSilently(input)).isEqualTo(expected);
    }
}
