package com.scotia.qa.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para DataUtilities.capitalize().
 *
 * <p>Este método fue consolidado (eliminado de ApiSteps) y hecho público
 * para ser reutilizado en todo el framework.</p>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2025-02-15
 */
@DisplayName("DataUtilities.capitalize() Tests")
class DataUtilitiesCapitalizeTest {

    @Test
    @DisplayName("Debe capitalizar primera letra de palabra en minúsculas")
    void testCapitalizeWord() {
        // When
        String result = DataUtilities.capitalize("hello");

        // Then
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Debe mantener palabra ya capitalizada")
    void testCapitalizeAlreadyCapitalized() {
        // When
        String result = DataUtilities.capitalize("Hello");

        // Then
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Debe capitalizar palabra en mayúsculas (solo primera letra)")
    void testCapitalizeUpperCase() {
        // When
        String result = DataUtilities.capitalize("HELLO");

        // Then
        assertThat(result).isEqualTo("HELLO"); // Primera ya está en mayúscula
    }

    @Test
    @DisplayName("Debe capitalizar string de una sola letra")
    void testCapitalizeSingleChar() {
        // When
        String result = DataUtilities.capitalize("h");

        // Then
        assertThat(result).isEqualTo("H");
    }

    @Test
    @DisplayName("Debe retornar null si input es null")
    void testCapitalizeNull() {
        // When
        String result = DataUtilities.capitalize(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Debe retornar string vacío si input es vacío")
    void testCapitalizeEmptyString() {
        // When
        String result = DataUtilities.capitalize("");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe capitalizar string con espacios")
    void testCapitalizeWithSpaces() {
        // When
        String result = DataUtilities.capitalize("hello world");

        // Then
        assertThat(result).isEqualTo("Hello world"); // Solo primera letra
    }

    @Test
    @DisplayName("Debe capitalizar string con números")
    void testCapitalizeWithNumbers() {
        // When
        String result = DataUtilities.capitalize("test123");

        // Then
        assertThat(result).isEqualTo("Test123");
    }

    @Test
    @DisplayName("Debe capitalizar string que empieza con número (no cambia)")
    void testCapitalizeStartingWithNumber() {
        // When
        String result = DataUtilities.capitalize("123test");

        // Then
        assertThat(result).isEqualTo("123test"); // Números no se capitalizan
    }

    @Test
    @DisplayName("Debe capitalizar string con caracteres especiales")
    void testCapitalizeWithSpecialChars() {
        // When
        String result = DataUtilities.capitalize("hello-world");

        // Then
        assertThat(result).isEqualTo("Hello-world"); // Solo primera letra
    }
}

