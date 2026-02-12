package com.scotia.qa.common.cucumber.context;

import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests unitarios para ScenarioContext - Contexto thread-safe compartido.
 *
 * <p><b>Clase CRÍTICA:</b> Maneja comunicación entre capas (API ↔ Web ↔ Mobile)</p>
 * <p><b>Cobertura objetivo:</b> 85%</p>
 * <p><b>Total tests:</b> 28</p>
 *
 * @author Abel Venero
 * @since 1.0.1
 */
@DisplayName("ScenarioContext Tests")
class ScenarioContextTest {

    @BeforeEach
    void setUp() {
        // Limpiar contexto antes de cada test
        ScenarioContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Limpiar contexto después de cada test
        ScenarioContext.clear();
    }

    // =========================================================================
    // BASIC OPERATIONS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Basic Operations Tests")
    class BasicOperationsTests {

        @Test
        @DisplayName("Debe almacenar y recuperar valor simple")
        void testSetAndGet() throws FrameworkBusinessException {
            // Given
            String key = "testKey";
            String value = "testValue";

            // When
            ScenarioContext.set(key, value);
            Object retrieved = ScenarioContext.get(key);

            // Then
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("Debe retornar null para clave inexistente")
        void testGetNonExistentKey() throws FrameworkBusinessException {
            // When
            Object result = ScenarioContext.get("noExiste");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Debe lanzar excepción con clave null")
        void testSetWithNullKey() {
            // When/Then
            assertThatThrownBy(() ->
                ScenarioContext.set(null, "value")
            ).isInstanceOf(FrameworkBusinessException.class)
             .hasMessageContaining("clave no puede ser nula");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "\t", "\n"})
        @DisplayName("Debe lanzar excepción con clave vacía")
        void testSetWithEmptyKey(String emptyKey) {
            // When/Then
            assertThatThrownBy(() ->
                ScenarioContext.set(emptyKey, "value")
            ).isInstanceOf(FrameworkBusinessException.class)
             .hasMessageContaining("clave no puede ser nula o vacía");
        }

        @Test
        @DisplayName("Debe sobrescribir valor existente")
        void testOverwriteValue() throws FrameworkBusinessException {
            // Given
            String key = "key";
            ScenarioContext.set(key, "valorOriginal");

            // When
            ScenarioContext.set(key, "valorNuevo");
            Object result = ScenarioContext.get(key);

            // Then
            assertThat(result).isEqualTo("valorNuevo");
        }

        @Test
        @DisplayName("Debe almacenar diferentes tipos de objetos")
        void testStoreDifferentTypes() throws FrameworkBusinessException {
            // When
            ScenarioContext.set("string", "text");
            ScenarioContext.set("integer", 123);
            ScenarioContext.set("long", 999L);
            ScenarioContext.set("boolean", true);
            ScenarioContext.set("map", Map.of("key", "value"));

            // Then
            assertThat(ScenarioContext.get("string")).isInstanceOf(String.class);
            assertThat(ScenarioContext.get("integer")).isInstanceOf(Integer.class);
            assertThat(ScenarioContext.get("long")).isInstanceOf(Long.class);
            assertThat(ScenarioContext.get("boolean")).isInstanceOf(Boolean.class);
            assertThat(ScenarioContext.get("map")).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Debe retornar tamaño correcto del contexto")
        void testSize() throws FrameworkBusinessException {
            // Given
            assertThat(ScenarioContext.size()).isEqualTo(0);

            // When
            ScenarioContext.set("key1", "value1");
            ScenarioContext.set("key2", "value2");

            // Then
            assertThat(ScenarioContext.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("containsKey debe verificar existencia correctamente")
        void testContainsKey() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("exists", "value");

            // Then
            assertThat(ScenarioContext.containsKey("exists")).isTrue();
            assertThat(ScenarioContext.containsKey("noExiste")).isFalse();
        }

        @Test
        @DisplayName("remove debe eliminar valor y retornarlo")
        void testRemove() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key", "value");

            // When
            Object removed = ScenarioContext.remove("key");

            // Then
            assertThat(removed).isEqualTo("value");
            assertThat(ScenarioContext.containsKey("key")).isFalse();
        }

        @Test
        @DisplayName("clear debe limpiar todo el contexto")
        void testClear() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", "value1");
            ScenarioContext.set("key2", "value2");
            ScenarioContext.set("key3", "value3");

            // When
            ScenarioContext.clear();

            // Then
            assertThat(ScenarioContext.size()).isEqualTo(0);
            assertThat(ScenarioContext.get("key1")).isNull();
        }
    }

    // =========================================================================
    // LAYER OPERATIONS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Layer Operations Tests")
    class LayerOperationsTests {

        @Test
        @DisplayName("Debe almacenar valor con prefijo de capa")
        void testSetByLayer() throws FrameworkBusinessException {
            // When
            ScenarioContext.setByLayer("api", "userId", "12345");

            // Then
            assertThat(ScenarioContext.get("api.userId")).isEqualTo("12345");
            assertThat(ScenarioContext.containsKey("api.userId")).isTrue();
        }

        @Test
        @DisplayName("Debe recuperar valor de capa específica")
        void testGetFromLayer() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("api.token", "abc123");
            ScenarioContext.set("web.token", "xyz789");

            // When
            Object apiToken = ScenarioContext.getFromLayer("api", "token");
            Object webToken = ScenarioContext.getFromLayer("web", "token");

            // Then
            assertThat(apiToken).isEqualTo("abc123");
            assertThat(webToken).isEqualTo("xyz789");
        }

        @Test
        @DisplayName("Debe aislar variables por capa")
        void testLayerIsolation() throws FrameworkBusinessException {
            // Given
            ScenarioContext.setByLayer("api", "userId", "API-123");
            ScenarioContext.setByLayer("web", "userId", "WEB-456");
            ScenarioContext.setByLayer("mobile", "userId", "MOBILE-789");

            // When
            Object apiValue = ScenarioContext.getFromLayer("api", "userId");
            Object webValue = ScenarioContext.getFromLayer("web", "userId");
            Object mobileValue = ScenarioContext.getFromLayer("mobile", "userId");

            // Then
            assertThat(apiValue).isEqualTo("API-123");
            assertThat(webValue).isEqualTo("WEB-456");
            assertThat(mobileValue).isEqualTo("MOBILE-789");
        }

        @Test
        @DisplayName("getFromAnyLayer debe buscar en orden correcto")
        void testGetFromAnyLayerPriority() throws FrameworkBusinessException {
            // Given - Orden: shared -> api -> web -> mobile -> sin prefijo
            ScenarioContext.set("shared.key", "SHARED");
            ScenarioContext.set("api.key", "API");
            ScenarioContext.set("web.key", "WEB");
            ScenarioContext.set("mobile.key", "MOBILE");
            ScenarioContext.set("key", "NO_PREFIX");

            // When
            Object result = ScenarioContext.getFromAnyLayer("key");

            // Then - Debe encontrar primero en shared
            assertThat(result).isEqualTo("SHARED");
        }

        @Test
        @DisplayName("getFromAnyLayer debe buscar en api si no está en shared")
        void testGetFromAnyLayerFallbackToApi() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("api.key", "API");
            ScenarioContext.set("web.key", "WEB");

            // When
            Object result = ScenarioContext.getFromAnyLayer("key");

            // Then - Debe encontrar en api
            assertThat(result).isEqualTo("API");
        }

        @Test
        @DisplayName("getByLayer debe retornar solo variables de esa capa")
        void testGetByLayer() throws FrameworkBusinessException {
            // Given
            ScenarioContext.setByLayer("api", "token", "token123");
            ScenarioContext.setByLayer("api", "userId", "user456");
            ScenarioContext.setByLayer("web", "sessionId", "session789");

            // When
            Map<String, Object> apiVars = ScenarioContext.getByLayer("api.");

            // Then
            assertThat(apiVars)
                .hasSize(2)
                .containsEntry("token", "token123")
                .containsEntry("userId", "user456")
                .doesNotContainKey("sessionId");
        }

        @Test
        @DisplayName("Debe manejar capas con case insensitive")
        void testLayerCaseInsensitive() throws FrameworkBusinessException {
            // When
            ScenarioContext.setByLayer("API", "key", "value");

            // Then
            assertThat(ScenarioContext.get("api.key")).isEqualTo("value");
        }
    }

    // =========================================================================
    // STRING OPERATIONS TESTS
    // =========================================================================

    @Nested
    @DisplayName("String Operations Tests")
    class StringOperationsTests {

        @Test
        @DisplayName("getString debe retornar valor como String")
        void testGetString() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key", 12345);

            // When
            String result = ScenarioContext.getString("key");

            // Then
            assertThat(result).isEqualTo("12345");
        }

        @Test
        @DisplayName("getString debe retornar null para clave inexistente")
        void testGetStringNotFound() throws FrameworkBusinessException {
            // When
            String result = ScenarioContext.getString("noExiste");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getString con default debe retornar default si no existe")
        void testGetStringWithDefault() throws FrameworkBusinessException {
            // When
            String result = ScenarioContext.getString("noExiste", "defaultValue");

            // Then
            assertThat(result).isEqualTo("defaultValue");
        }

        @Test
        @DisplayName("getString con default debe retornar valor si existe")
        void testGetStringWithDefaultWhenExists() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key", "actualValue");

            // When
            String result = ScenarioContext.getString("key", "defaultValue");

            // Then
            assertThat(result).isEqualTo("actualValue");
        }
    }

    // =========================================================================
    // VARIABLE REPLACEMENT TESTS
    // =========================================================================

    @Nested
    @DisplayName("Variable Replacement Tests")
    class VariableReplacementTests {

        @Test
        @DisplayName("Debe reemplazar variable simple en texto")
        void testReplaceVariableSimple() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("nombre", "Abel");
            String template = "Hola ${nombre}!";

            // When
            String result = ScenarioContext.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Hola Abel!");
        }

        @Test
        @DisplayName("Debe reemplazar múltiples variables")
        void testReplaceMultipleVariables() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("user", "testuser");
            ScenarioContext.set("pass", "pass123");
            String template = "Usuario: ${user}, Password: ${pass}";

            // When
            String result = ScenarioContext.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Usuario: testuser, Password: pass123");
        }

        @Test
        @DisplayName("Debe buscar variables en todas las capas")
        void testReplaceVariablesFromLayers() throws FrameworkBusinessException {
            // Given
            ScenarioContext.setByLayer("api", "token", "api-token-123");
            ScenarioContext.setByLayer("web", "sessionId", "web-session-456");
            String template = "API: ${token}, Web: ${sessionId}";

            // When
            String result = ScenarioContext.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("API: api-token-123, Web: web-session-456");
        }

        @Test
        @DisplayName("Debe mantener placeholder si variable no existe")
        void testReplaceVariablesNotFound() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("existe", "valor");
            String template = "Existe: ${existe}, No existe: ${noExiste}";

            // When
            String result = ScenarioContext.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Existe: valor, No existe: ${noExiste}");
        }

        @Test
        @DisplayName("Debe retornar texto sin cambios si no hay variables")
        void testReplaceVariablesNoPlaceholders() {
            // Given
            String text = "Texto sin variables";

            // When
            String result = ScenarioContext.replaceVariables(text);

            // Then
            assertThat(result).isEqualTo(text);
        }

        @Test
        @DisplayName("Debe manejar texto null")
        void testReplaceVariablesNullText() {
            // When
            String result = ScenarioContext.replaceVariables(null);

            // Then
            assertThat(result).isNull();
        }
    }

    // =========================================================================
    // COMPARISON TESTS
    // =========================================================================

    @Nested
    @DisplayName("Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("Debe comparar valores iguales correctamente")
        void testCompareEqualValues() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", "value");
            ScenarioContext.set("key2", "value");

            // When
            boolean result = ScenarioContext.compareValues("key1", "key2");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Debe comparar valores diferentes")
        void testCompareDifferentValues() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", "value1");
            ScenarioContext.set("key2", "value2");

            // When
            boolean result = ScenarioContext.compareValues("key1", "key2");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false si alguna clave no existe")
        void testCompareWithNonExistentKey() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", "value");

            // When
            boolean result = ScenarioContext.compareValues("key1", "noExiste");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe comparar valores de diferentes tipos como strings")
        void testCompareDifferentTypesAsStrings() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", 123);
            ScenarioContext.set("key2", "123");

            // When
            boolean result = ScenarioContext.compareValues("key1", "key2");

            // Then
            assertThat(result).isTrue();
        }
    }

    // =========================================================================
    // VALUE CONTAINS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Value Contains Tests")
    class ValueContainsTests {

        @Test
        @DisplayName("Debe verificar que valor contiene texto")
        void testValueContains() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("message", "Hola mundo desde Scotia QA");

            // When/Then
            assertThat(ScenarioContext.valueContains("message", "mundo")).isTrue();
            assertThat(ScenarioContext.valueContains("message", "Scotia")).isTrue();
            assertThat(ScenarioContext.valueContains("message", "NoEsta")).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false si clave no existe")
        void testValueContainsNonExistentKey() throws FrameworkBusinessException {
            // When
            boolean result = ScenarioContext.valueContains("noExiste", "texto");

            // Then
            assertThat(result).isFalse();
        }
    }

    // =========================================================================
    // UTILITY METHODS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("getAll debe retornar copia del contexto")
        void testGetAll() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", "value1");
            ScenarioContext.set("key2", "value2");

            // When
            Map<String, Object> all = ScenarioContext.getAll();

            // Then
            assertThat(all)
                .hasSize(2)
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("Modificar getAll no debe afectar contexto")
        void testGetAllImmutability() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key", "value");

            // When
            Map<String, Object> all = ScenarioContext.getAll();
            all.put("newKey", "newValue");

            // Then
            assertThat(ScenarioContext.containsKey("newKey")).isFalse();
        }

        @Test
        @DisplayName("getKeys debe retornar todas las claves")
        void testGetKeys() throws FrameworkBusinessException {
            // Given
            ScenarioContext.set("key1", "value1");
            ScenarioContext.set("api.token", "token");
            ScenarioContext.set("web.session", "session");

            // When
            Set<String> keys = ScenarioContext.getKeys();

            // Then
            assertThat(keys)
                .hasSize(3)
                .contains("key1", "api.token", "web.session");
        }

        @Test
        @DisplayName("printContextByLayers no debe lanzar excepción")
        void testPrintContextByLayers() throws FrameworkBusinessException {
            // Given
            ScenarioContext.setByLayer("api", "token", "123");
            ScenarioContext.setByLayer("web", "session", "456");

            // When/Then - No debe lanzar excepción
            assertThatCode(() ->
                ScenarioContext.printContextByLayers()
            ).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // THREAD SAFETY TESTS
    // =========================================================================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Debe ser thread-safe en acceso concurrente")
        void testConcurrentAccess() throws InterruptedException {
            // Given
            int threadCount = 10;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String key = "thread" + threadId + "_key" + j;
                            String value = "value" + j;
                            ScenarioContext.set(key, value);
                            Object retrieved = ScenarioContext.get(key);
                            if (value.equals(retrieved)) {
                                successCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        // Error de concurrencia
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // Then
            assertThat(successCount.get()).isEqualTo(threadCount * operationsPerThread);
        }

        @Test
        @DisplayName("Cada thread debe tener su propio contexto aislado")
        void testThreadLocalIsolation() throws InterruptedException {
            // Given
            ExecutorService executor = Executors.newFixedThreadPool(3);
            AtomicInteger correctIsolations = new AtomicInteger(0);

            // When - Cada thread guarda un valor diferente con la misma key
            for (int i = 0; i < 3; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        ScenarioContext.set("threadId", threadId);
                        Thread.sleep(50); // Permitir que otros threads también guarden
                        Object retrieved = ScenarioContext.get("threadId");

                        // Cada thread debe ver solo SU valor
                        if (threadId == ((Integer) retrieved)) {
                            correctIsolations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Error
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Then - Los 3 threads deben tener aislamiento correcto
            assertThat(correctIsolations.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("clear debe limpiar solo el thread actual")
        void testClearThreadLocal() throws InterruptedException, FrameworkBusinessException {
            // Given
            ScenarioContext.set("mainThread", "mainValue");

            ExecutorService executor = Executors.newSingleThreadExecutor();
            AtomicInteger otherThreadHasValue = new AtomicInteger(0);

            executor.submit(() -> {
                try {
                    ScenarioContext.set("otherThread", "otherValue");
                    // Main thread limpia su contexto
                } catch (Exception e) {
                    // Error
                }
            });

            // When - Main thread limpia
            ScenarioContext.clear();

            // Verificar en otro thread
            executor.submit(() -> {
                try {
                    if (ScenarioContext.get("otherThread") != null) {
                        otherThreadHasValue.set(1);
                    }
                } catch (Exception e) {
                    // Error
                }
            });

            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.SECONDS);

            // Then
            assertThat(ScenarioContext.size()).isEqualTo(0);  // Main thread limpio
            assertThat(otherThreadHasValue.get()).isEqualTo(1);  // Otro thread intacto
        }
    }

    // =========================================================================
    // EDGE CASES & ERROR HANDLING
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe lanzar excepción al intentar guardar null value")
        void testStoreNullValue() {
            // When/Then - ConcurrentHashMap no permite null values
            assertThatThrownBy(() ->
                ScenarioContext.set("key", null)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Debe manejar strings vacíos")
        void testStoreEmptyString() throws FrameworkBusinessException {
            // When
            ScenarioContext.set("key", "");

            // Then
            assertThat(ScenarioContext.getString("key")).isEqualTo("");
        }

        @Test
        @DisplayName("Debe manejar objetos complejos")
        void testStoreComplexObjects() throws FrameworkBusinessException {
            // Given
            Map<String, Object> complexObject = Map.of(
                "user", Map.of("id", 123, "name", "Abel"),
                "permissions", Set.of("read", "write")
            );

            // When
            ScenarioContext.set("userData", complexObject);
            Object retrieved = ScenarioContext.get("userData");

            // Then
            assertThat(retrieved).isEqualTo(complexObject);
        }

        @Test
        @DisplayName("Debe manejar claves con caracteres especiales")
        void testKeysWithSpecialCharacters() throws FrameworkBusinessException {
            // Given
            String specialKey = "api.user-data_123";

            // When
            ScenarioContext.set(specialKey, "value");

            // Then
            assertThat(ScenarioContext.get(specialKey)).isEqualTo("value");
        }
    }
}

