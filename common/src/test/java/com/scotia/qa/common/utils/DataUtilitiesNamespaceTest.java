package com.scotia.qa.common.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

/**
 * Tests unitarios para DataUtilities - Funcionalidad de Namespace
 *
 * @author Abel Venero
 * @since 1.2.0
 */
@DisplayName("DataUtilities - Namespace Tests")
class DataUtilitiesNamespaceTest {

    private static final String TEST_NAMESPACE_1 = "test_scenario_1";
    private static final String TEST_NAMESPACE_2 = "test_scenario_2";

    @BeforeEach
    void setUp() {
        // Limpiar todos los namespaces antes de cada test
        DataUtilities.clearAllNamespaces();
    }

    @AfterEach
    void tearDown() {
        // Limpiar después de cada test
        DataUtilities.clearAllNamespaces();
    }

    // =================================================================================
    // TESTS DE VARIABLES EN NAMESPACE
    // =================================================================================

    @Test
    @DisplayName("Debería almacenar y recuperar variable en namespace")
    void testStoreAndGetValueInNamespace() {
        // Given
        String key = "userId";
        String value = "user123";

        // When
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, key, value);
        String retrieved = DataUtilities.getValueFromNamespace(TEST_NAMESPACE_1, key);

        // Then
        assertNotNull(retrieved, "El valor debería existir");
        assertEquals(value, retrieved, "El valor debería ser el mismo");
    }

    @Test
    @DisplayName("Debería aislar variables entre diferentes namespaces")
    void testVariableIsolationBetweenNamespaces() {
        // Given
        String key = "token";
        String value1 = "token_namespace_1";
        String value2 = "token_namespace_2";

        // When
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, key, value1);
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_2, key, value2);

        String retrieved1 = DataUtilities.getValueFromNamespace(TEST_NAMESPACE_1, key);
        String retrieved2 = DataUtilities.getValueFromNamespace(TEST_NAMESPACE_2, key);

        // Then
        assertEquals(value1, retrieved1, "Namespace 1 debería tener su propio valor");
        assertEquals(value2, retrieved2, "Namespace 2 debería tener su propio valor");
        assertNotEquals(retrieved1, retrieved2, "Los valores deberían ser diferentes");
    }

    @Test
    @DisplayName("Debería retornar null para namespace inexistente")
    void testGetValueFromNonExistentNamespace() {
        // When
        String retrieved = DataUtilities.getValueFromNamespace("nonexistent_namespace", "someKey");

        // Then
        assertNull(retrieved, "Debería retornar null para namespace inexistente");
    }

    @Test
    @DisplayName("Debería retornar null para key inexistente en namespace")
    void testGetNonExistentKeyFromNamespace() {
        // Given
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, "existingKey", "value");

        // When
        String retrieved = DataUtilities.getValueFromNamespace(TEST_NAMESPACE_1, "nonExistentKey");

        // Then
        assertNull(retrieved, "Debería retornar null para key inexistente");
    }

    @Test
    @DisplayName("Debería verificar existencia de variable en namespace")
    void testHasValueInNamespace() {
        // Given
        String key = "email";
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, key, "test@test.com");

        // When & Then
        assertTrue(DataUtilities.hasValueInNamespace(TEST_NAMESPACE_1, key),
            "Debería existir la variable");
        assertFalse(DataUtilities.hasValueInNamespace(TEST_NAMESPACE_1, "nonExistentKey"),
            "No debería existir key inexistente");
        assertFalse(DataUtilities.hasValueInNamespace("nonExistentNamespace", key),
            "No debería existir en namespace inexistente");
    }

    @Test
    @DisplayName("Debería limpiar un namespace específico sin afectar otros")
    void testClearSpecificNamespace() {
        // Given
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, "key1", "value1");
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_2, "key2", "value2");

        // When
        DataUtilities.clearNamespace(TEST_NAMESPACE_1);

        // Then
        assertFalse(DataUtilities.hasValueInNamespace(TEST_NAMESPACE_1, "key1"),
            "Namespace 1 debería estar limpio");
        assertTrue(DataUtilities.hasValueInNamespace(TEST_NAMESPACE_2, "key2"),
            "Namespace 2 NO debería estar afectado");
    }

    @Test
    @DisplayName("Debería limpiar todos los namespaces")
    void testClearAllNamespaces() {
        // Given
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, "key1", "value1");
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_2, "key2", "value2");

        // When
        DataUtilities.clearAllNamespaces();

        // Then
        Set<String> activeNamespaces = DataUtilities.getActiveNamespaces();
        assertTrue(activeNamespaces.isEmpty(), "No debería haber namespaces activos");
    }

    @Test
    @DisplayName("Debería obtener lista de namespaces activos")
    void testGetActiveNamespaces() {
        // Given
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_1, "key1", "value1");
        DataUtilities.storeValueInNamespace(TEST_NAMESPACE_2, "key2", "value2");

        // When
        Set<String> activeNamespaces = DataUtilities.getActiveNamespaces();

        // Then
        assertEquals(2, activeNamespaces.size(), "Deberían haber 2 namespaces activos");
        assertTrue(activeNamespaces.contains(TEST_NAMESPACE_1), "Debería contener namespace 1");
        assertTrue(activeNamespaces.contains(TEST_NAMESPACE_2), "Debería contener namespace 2");
    }

    @Test
    @DisplayName("Debería manejar namespace null o vacío fallback a store global")
    void testNullOrEmptyNamespaceFallback() {
        // Given
        String key = "globalKey";
        String value = "globalValue";

        // When - namespace null debería usar store global
        DataUtilities.storeValueInNamespace(null, key, value);
        String retrieved = DataUtilities.getValue(key); // Obtener del store global

        // Then
        assertEquals(value, retrieved, "Debería almacenar en store global si namespace es null");
    }

    // =================================================================================
    // TESTS DE OBJETOS EN NAMESPACE
    // =================================================================================

    @Test
    @DisplayName("Debería almacenar y recuperar objeto en namespace")
    void testStoreAndGetObjectInNamespace() {
        // Given
        String key = "userData";
        TestUser user = new TestUser("John", "Doe", 30);

        // When
        DataUtilities.storeObjectInNamespace(TEST_NAMESPACE_1, key, user);
        TestUser retrieved = DataUtilities.getObjectFromNamespace(TEST_NAMESPACE_1, key, TestUser.class);

        // Then
        assertNotNull(retrieved, "El objeto debería existir");
        assertEquals(user.firstName, retrieved.firstName, "firstName debería ser igual");
        assertEquals(user.lastName, retrieved.lastName, "lastName debería ser igual");
        assertEquals(user.age, retrieved.age, "age debería ser igual");
    }

    @Test
    @DisplayName("Debería aislar objetos entre diferentes namespaces")
    void testObjectIsolationBetweenNamespaces() {
        // Given
        String key = "user";
        TestUser user1 = new TestUser("Alice", "Smith", 25);
        TestUser user2 = new TestUser("Bob", "Jones", 35);

        // When
        DataUtilities.storeObjectInNamespace(TEST_NAMESPACE_1, key, user1);
        DataUtilities.storeObjectInNamespace(TEST_NAMESPACE_2, key, user2);

        TestUser retrieved1 = DataUtilities.getObjectFromNamespace(TEST_NAMESPACE_1, key, TestUser.class);
        TestUser retrieved2 = DataUtilities.getObjectFromNamespace(TEST_NAMESPACE_2, key, TestUser.class);

        // Then
        assertEquals(user1.firstName, retrieved1.firstName, "Namespace 1 debería tener user1");
        assertEquals(user2.firstName, retrieved2.firstName, "Namespace 2 debería tener user2");
    }

    @Test
    @DisplayName("Debería verificar existencia de objeto en namespace")
    void testHasObjectInNamespace() {
        // Given
        String key = "config";
        TestUser user = new TestUser("Test", "User", 20);
        DataUtilities.storeObjectInNamespace(TEST_NAMESPACE_1, key, user);

        // When & Then
        assertTrue(DataUtilities.hasObjectInNamespace(TEST_NAMESPACE_1, key),
            "Debería existir el objeto");
        assertFalse(DataUtilities.hasObjectInNamespace(TEST_NAMESPACE_1, "nonExistent"),
            "No debería existir objeto inexistente");
    }

    @Test
    @DisplayName("Debería remover objeto al pasar null")
    void testRemoveObjectWithNull() {
        // Given
        String key = "tempObject";
        DataUtilities.storeObjectInNamespace(TEST_NAMESPACE_1, key, "someValue");

        // When
        DataUtilities.storeObjectInNamespace(TEST_NAMESPACE_1, key, null);

        // Then
        assertFalse(DataUtilities.hasObjectInNamespace(TEST_NAMESPACE_1, key),
            "El objeto debería estar removido");
    }

    // =================================================================================
    // CLASE HELPER PARA TESTS
    // =================================================================================

    /**
     * Clase simple para tests de objetos
     */
    static class TestUser {
        public String firstName;
        public String lastName;
        public int age;

        // Constructor por defecto (requerido por Jackson)
        public TestUser() {
        }

        public TestUser(String firstName, String lastName, int age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }
    }
}

