package com.scotia.qa.common.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("DataUtilities - Funcionalidades Avanzadas")
class DataUtilitiesAdvancedTest {

    // =============================================================
    // GENERACIÓN AVANZADA
    // =============================================================
    @Test
    @DisplayName("Genera boolean aleatorio")
    void testGenerateRandomBoolean() {
        boolean v1 = DataUtilities.generateRandomBoolean();
        boolean v2 = DataUtilities.generateRandomBoolean();
        assertTrue(v1 == true || v1 == false);
        assertTrue(v2 == true || v2 == false);
    }

    @Test
    @DisplayName("Genera número dentro de rango")
    void testGenerateNumberInRange() {
        int n = DataUtilities.generateNumberInRange(5, 10);
        assertTrue(n >= 5 && n <= 10);
    }

    @Test
    @DisplayName("Genera lorem ipsum con longitud aproximada")
    void testGenerateLoremIpsum() {
        String text = DataUtilities.generateLoremIpsum(120);
        assertNotNull(text);
        assertEquals(120, text.length());
    }

    @Test
    @DisplayName("Genera edad en rango")
    void testGenerateAgeInRange() {
        int age = DataUtilities.generateAgeInRange(18, 65);
        assertTrue(age >= 18 && age <= 65);
    }

    @Test
    @DisplayName("Genera monto monetario en rango")
    void testGenerateRandomAmount() {
        double amount = DataUtilities.generateRandomAmount(100.0, 500.0, 2);
        assertTrue(amount >= 100.0 && amount <= 500.0);
        String formatted = String.valueOf(amount);
        assertTrue(formatted.matches("\\d+\\.\\d{1,2}"));
    }

    @Test
    @DisplayName("Genera código con prefijo")
    void testGenerateCode() {
        String code = DataUtilities.generateCode("USR-", 6);
        assertTrue(code.startsWith("USR-"));
        assertEquals(10, code.length());
    }

    // =============================================================
    // JSONPATH AVANZADO
    // =============================================================
    private static final String SAMPLE_JSON = "{" +
            "\"data\": {" +
            "  \"users\": [" +
            "    {\"id\": 1, \"name\": \"Alice\", \"active\": true}," +
            "    {\"id\": 2, \"name\": \"Bob\", \"active\": false}," +
            "    {\"id\": 3, \"name\": \"Carol\", \"active\": true}" +
            "  ]" +
            "}," +
            "\"meta\": {\"total\": 3}" +
            "}";

    @Test
    @DisplayName("JSONPath - Obtener primer elemento por índice")
    void testJsonPathIndex() {
        Object name = DataUtilities.getByJsonPath(SAMPLE_JSON, "data.users[0].name");
        assertEquals("Alice", name);
    }

    @Test
    @DisplayName("JSONPath - Wildcard para IDs")
    void testJsonPathWildcard() {
        Object ids = DataUtilities.getByJsonPath(SAMPLE_JSON, "data.users[*].id");
        assertTrue(ids instanceof List);
        assertEquals(List.of(1,2,3), ids);
    }

    @Test
    @DisplayName("JSONPath - Filtro por active == true")
    void testJsonPathFilter() {
        Object activeNames = DataUtilities.getByJsonPath(SAMPLE_JSON, "data.users[?(@.active == true)].name");
        assertTrue(activeNames instanceof List);
        assertEquals(List.of("Alice","Carol"), activeNames);
    }

    @Test
    @DisplayName("JSONPath - hasJsonPath retorna true cuando existe")
    void testHasJsonPath() {
        assertTrue(DataUtilities.hasJsonPath(SAMPLE_JSON, "data.users[2].name"));
        assertFalse(DataUtilities.hasJsonPath(SAMPLE_JSON, "data.users[5].name"));
    }

    @Test
    @DisplayName("JSONPath - Obtener lista tipada")
    void testGetListByJsonPath() {
        List<String> names = DataUtilities.getListByJsonPath(SAMPLE_JSON, "data.users[*].name", String.class);
        assertEquals(3, names.size());
        assertTrue(names.contains("Alice"));
    }

    // =============================================================
    // COMPARACIÓN JSON
    // =============================================================
    @Test
    @DisplayName("areJsonEqual retorna true para JSON equivalentes")
    void testAreJsonEqual() {
        String j1 = "{\"a\":1,\"b\":2}";
        String j2 = "{\"b\":2,\"a\":1}"; // orden diferente
        assertTrue(DataUtilities.areJsonEqual(j1, j2));
    }

    @Test
    @DisplayName("areJsonEqual retorna false para JSON diferentes")
    void testAreJsonEqualFalse() {
        String j1 = "{\"a\":1,\"b\":2}";
        String j2 = "{\"a\":1,\"b\":3}";
        assertFalse(DataUtilities.areJsonEqual(j1, j2));
    }

    @Test
    @DisplayName("diffJson muestra diferencias")
    void testDiffJson() {
        String expected = "{\"a\":1,\"b\":2}";
        String actual = "{\"a\":1,\"b\":3,\"c\":4}";
        String diff = DataUtilities.diffJson(expected, actual);
        assertTrue(diff.contains("b => expected: 2, actual: 3"));
        assertTrue(diff.contains("c => expected:"));
    }

    @Test
    @DisplayName("jsonContainsAllFields true cuando están todos los campos")
    void testJsonContainsAllFieldsTrue() {
        String expected = "{\"a\":1,\"b\":2}";
        String actual = "{\"a\":1,\"b\":2,\"c\":3}";
        assertTrue(DataUtilities.jsonContainsAllFields(expected, actual));
    }

    @Test
    @DisplayName("jsonContainsAllFields false cuando falta campo")
    void testJsonContainsAllFieldsFalse() {
        String expected = "{\"a\":1,\"b\":2,\"d\":9}";
        String actual = "{\"a\":1,\"b\":2,\"c\":3}";
        assertFalse(DataUtilities.jsonContainsAllFields(expected, actual));
    }
}

