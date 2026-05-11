package com.qa.common.utils;


import com.qa.common.utils.text.TextUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para TextUtilities.capitalize().
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2026
 */
@DisplayName("TextUtilities.capitalize() Tests")
class TextUtilitiesCapitalizeTest {

    @Test
    @DisplayName("Debe capitalizar primera letra de palabra en minúsculas")
    void testCapitalizeWord() {
        assertThat(TextUtilities.capitalize("hello")).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Debe mantener palabra ya capitalizada")
    void testCapitalizeAlreadyCapitalized() {
        assertThat(TextUtilities.capitalize("Hello")).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Debe capitalizar palabra en mayúsculas (solo primera letra)")
    void testCapitalizeUpperCase() {
        assertThat(TextUtilities.capitalize("HELLO")).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Debe capitalizar string de una sola letra")
    void testCapitalizeSingleChar() {
        assertThat(TextUtilities.capitalize("h")).isEqualTo("H");
    }

    @Test
    @DisplayName("Debe retornar null si input es null")
    void testCapitalizeNull() {
        assertThat(TextUtilities.capitalize(null)).isNull();
    }

    @Test
    @DisplayName("Debe retornar string vacío si input es vacío")
    void testCapitalizeEmptyString() {
        assertThat(TextUtilities.capitalize("")).isEmpty();
    }

    @Test
    @DisplayName("Debe capitalizar string con espacios (solo primera letra)")
    void testCapitalizeWithSpaces() {
        assertThat(TextUtilities.capitalize("hello world")).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("Debe capitalizar string con números")
    void testCapitalizeWithNumbers() {
        assertThat(TextUtilities.capitalize("test123")).isEqualTo("Test123");
    }

    @Test
    @DisplayName("Debe capitalizar string que empieza con número (no cambia)")
    void testCapitalizeStartingWithNumber() {
        assertThat(TextUtilities.capitalize("123test")).isEqualTo("123test");
    }

    @Test
    @DisplayName("Debe capitalizar string con caracteres especiales")
    void testCapitalizeWithSpecialChars() {
        assertThat(TextUtilities.capitalize("hello-world")).isEqualTo("Hello-world");
    }
}

