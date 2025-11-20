package com.scotia.qa.common.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Tests unitarios para DataUtilities - Utilidades de Fechas
 *
 * @author Scotia QA Framework Team
 * @since 1.2.0
 */
@DisplayName("DataUtilities - Date Utilities Tests")
class DataUtilitiesDateTest {

    // =================================================================================
    // TESTS DE TIMESTAMP Y FORMATO
    // =================================================================================

    @Test
    @DisplayName("Debería obtener timestamp actual en formato ISO 8601")
    void testGetCurrentTimestamp() {
        // When
        String timestamp = DataUtilities.getCurrentTimestamp();

        // Then
        assertNotNull(timestamp, "Timestamp no debería ser null");
        assertTrue(timestamp.contains("T"), "Debería contener 'T' (formato ISO 8601)");
        assertTrue(timestamp.contains("Z") || timestamp.contains("+") || timestamp.contains("-"),
            "Debería contener indicador de zona horaria");
    }

    @Test
    @DisplayName("Debería obtener timestamp con formato personalizado")
    void testGetCurrentTimestampWithCustomFormat() {
        // Given
        String format = "yyyy-MM-dd";

        // When
        String timestamp = DataUtilities.getCurrentTimestamp(format);

        // Then
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}"),
            "Debería cumplir formato yyyy-MM-dd");
    }

    @Test
    @DisplayName("Debería manejar formato null retornando ISO 8601")
    void testGetCurrentTimestampWithNullFormat() {
        // When
        String timestamp = DataUtilities.getCurrentTimestamp(null);

        // Then
        assertNotNull(timestamp);
        assertTrue(timestamp.contains("T"), "Debería usar formato ISO 8601 por defecto");
    }

    // =================================================================================
    // TESTS DE PARSE Y FORMAT
    // =================================================================================

    @Test
    @DisplayName("Debería parsear fecha correctamente")
    void testParseDate() {
        // Given
        String dateString = "2025-11-19";
        String format = "yyyy-MM-dd";

        // When
        LocalDate parsed = DataUtilities.parseDate(dateString, format);

        // Then
        assertNotNull(parsed);
        assertEquals(2025, parsed.getYear());
        assertEquals(11, parsed.getMonthValue());
        assertEquals(19, parsed.getDayOfMonth());
    }

    @Test
    @DisplayName("Debería lanzar excepción con formato inválido al parsear")
    void testParseDateWithInvalidFormat() {
        // Given
        String dateString = "2025-11-19";
        String wrongFormat = "dd/MM/yyyy"; // Formato incorrecto para el string

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> DataUtilities.parseDate(dateString, wrongFormat),
            "Debería lanzar excepción con formato incorrecto");
    }

    @Test
    @DisplayName("Debería formatear fecha correctamente")
    void testFormatDate() {
        // Given
        LocalDate date = LocalDate.of(2025, 11, 19);
        String format = "dd/MM/yyyy";

        // When
        String formatted = DataUtilities.formatDate(date, format);

        // Then
        assertEquals("19/11/2025", formatted);
    }

    // =================================================================================
    // TESTS DE OPERACIONES CON FECHAS
    // =================================================================================

    @Test
    @DisplayName("Debería agregar días a una fecha")
    void testAddDaysToDate() {
        // Given
        LocalDate base = LocalDate.of(2025, 11, 19);

        // When
        LocalDate result = DataUtilities.addDaysToDate(base, 5);

        // Then
        assertEquals(LocalDate.of(2025, 11, 24), result);
    }

    @Test
    @DisplayName("Debería restar días a una fecha")
    void testSubtractDaysFromDate() {
        // Given
        LocalDate base = LocalDate.of(2025, 11, 19);

        // When
        LocalDate result = DataUtilities.addDaysToDate(base, -5);

        // Then
        assertEquals(LocalDate.of(2025, 11, 14), result);
    }

    @Test
    @DisplayName("Debería agregar meses a una fecha")
    void testAddMonthsToDate() {
        // Given
        LocalDate base = LocalDate.of(2025, 11, 19);

        // When
        LocalDate result = DataUtilities.addMonthsToDate(base, 2);

        // Then
        assertEquals(LocalDate.of(2026, 1, 19), result);
    }

    @Test
    @DisplayName("Debería agregar años a una fecha")
    void testAddYearsToDate() {
        // Given
        LocalDate base = LocalDate.of(2025, 11, 19);

        // When
        LocalDate result = DataUtilities.addYearsToDate(base, 1);

        // Then
        assertEquals(LocalDate.of(2026, 11, 19), result);
    }

    // =================================================================================
    // TESTS DE DIFERENCIAS ENTRE FECHAS
    // =================================================================================

    @Test
    @DisplayName("Debería calcular días entre dos fechas")
    void testGetDaysBetween() {
        // Given
        LocalDate date1 = LocalDate.of(2025, 11, 19);
        LocalDate date2 = LocalDate.of(2025, 11, 29);

        // When
        long days = DataUtilities.getDaysBetween(date1, date2);

        // Then
        assertEquals(10, days);
    }

    @Test
    @DisplayName("Debería retornar negativo si date2 es anterior")
    void testGetDaysBetweenNegative() {
        // Given
        LocalDate date1 = LocalDate.of(2025, 11, 29);
        LocalDate date2 = LocalDate.of(2025, 11, 19);

        // When
        long days = DataUtilities.getDaysBetween(date1, date2);

        // Then
        assertEquals(-10, days);
    }

    @Test
    @DisplayName("Debería calcular meses entre dos fechas")
    void testGetMonthsBetween() {
        // Given
        LocalDate date1 = LocalDate.of(2025, 1, 15);
        LocalDate date2 = LocalDate.of(2025, 11, 15);

        // When
        long months = DataUtilities.getMonthsBetween(date1, date2);

        // Then
        assertEquals(10, months);
    }

    // =================================================================================
    // TESTS DE GENERACIÓN DE FECHAS
    // =================================================================================

    @Test
    @DisplayName("Debería generar fecha de nacimiento para edad específica")
    void testGenerateBirthDateForAge() {
        // Given
        int age = 30;

        // When
        LocalDate birthDate = DataUtilities.generateBirthDateForAge(age);

        // Then
        assertNotNull(birthDate);
        LocalDate expectedDate = LocalDate.now().minusYears(age);
        assertEquals(expectedDate, birthDate);
    }

    @Test
    @DisplayName("Debería lanzar excepción con edad inválida")
    void testGenerateBirthDateForInvalidAge() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> DataUtilities.generateBirthDateForAge(-1),
            "Debería lanzar excepción con edad negativa");

        assertThrows(IllegalArgumentException.class,
            () -> DataUtilities.generateBirthDateForAge(200),
            "Debería lanzar excepción con edad > 150");
    }

    @Test
    @DisplayName("Debería generar fecha de nacimiento en rango de edad")
    void testGenerateBirthDateForAgeRange() {
        // Given
        int minAge = 25;
        int maxAge = 35;

        // When
        LocalDate birthDate = DataUtilities.generateBirthDateForAgeRange(minAge, maxAge);

        // Then
        assertNotNull(birthDate);

        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge).minusDays(1);
        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge).plusDays(1);

        assertTrue(birthDate.isAfter(minBirthDate) && birthDate.isBefore(maxBirthDate),
            "Fecha de nacimiento debería estar en el rango correcto");
    }

    @Test
    @DisplayName("Debería generar fecha en los últimos N días")
    void testGenerateDateInLastDays() {
        // Given
        int days = 30;

        // When
        LocalDate generatedDate = DataUtilities.generateDateInLastDays(days);

        // Then
        assertNotNull(generatedDate);
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusDays(days);

        assertTrue(!generatedDate.isBefore(minDate) && !generatedDate.isAfter(today),
            "Fecha debería estar en los últimos " + days + " días");
    }

    @Test
    @DisplayName("Debería generar fecha en los próximos N días")
    void testGenerateDateInNextDays() {
        // Given
        int days = 30;

        // When
        LocalDate generatedDate = DataUtilities.generateDateInNextDays(days);

        // Then
        assertNotNull(generatedDate);
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(days);

        assertTrue(!generatedDate.isBefore(today) && !generatedDate.isAfter(maxDate),
            "Fecha debería estar en los próximos " + days + " días");
    }

    // =================================================================================
    // TESTS DE VALIDACIONES
    // =================================================================================

    @Test
    @DisplayName("Debería verificar si fecha está en el pasado")
    void testIsDateInPast() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(5);
        LocalDate futureDate = LocalDate.now().plusDays(5);

        // When & Then
        assertTrue(DataUtilities.isDateInPast(pastDate),
            "Fecha pasada debería retornar true");
        assertFalse(DataUtilities.isDateInPast(futureDate),
            "Fecha futura debería retornar false");
    }

    @Test
    @DisplayName("Debería verificar si fecha está en el futuro")
    void testIsDateInFuture() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(5);
        LocalDate futureDate = LocalDate.now().plusDays(5);

        // When & Then
        assertTrue(DataUtilities.isDateInFuture(futureDate),
            "Fecha futura debería retornar true");
        assertFalse(DataUtilities.isDateInFuture(pastDate),
            "Fecha pasada debería retornar false");
    }

    @Test
    @DisplayName("Debería manejar null en validaciones de fecha")
    void testDateValidationsWithNull() {
        // When & Then
        assertFalse(DataUtilities.isDateInPast(null),
            "null debería retornar false");
        assertFalse(DataUtilities.isDateInFuture(null),
            "null debería retornar false");
    }
}

