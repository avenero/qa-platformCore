package com.qa.common.utils;

import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.VariableStore;
import org.junit.jupiter.api.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para VariableStore — almacenamiento y resolución de variables.
 *
 * <p>Reemplaza DataUtilitiesVariableStorageTest después de la eliminación de DataUtilities.
 * Todas las operaciones se hacen a través de {@link VariableStore} obtenido del
 * {@link ExecutionContext} activo.</p>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2026
 */
@DisplayName("VariableStore - Variable Storage & Resolution Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VariableStoreResolveTest {

    private ExecutionContext ctx;
    private VariableStore store;

    @BeforeEach
    void setUp() {
        ExecutionContext.deactivate();
        ctx = ExecutionContext.builder()
                .scenarioId("test-" + System.nanoTime())
                .build();
        ctx.activate();
        store = ctx.variables();
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    // =========================================================================
    // BASIC STORAGE TESTS
    // =========================================================================

    @Nested
    @DisplayName("Basic Variable Storage Tests")
    @Order(1)
    class BasicStorageTests {

        @Test
        @DisplayName("Debe almacenar y recuperar valor simple")
        void testStoreAndRetrieveSimple() {
            store.set("testKey", "testValue");
            assertThat(store.get("testKey", String.class))
                    .isPresent().hasValue("testValue");
        }

        @Test
        @DisplayName("Debe retornar vacío para variable inexistente")
        void testGetNonExistentVariable() {
            assertThat(store.get("noExiste", String.class)).isEmpty();
        }

        @Test
        @DisplayName("Debe sobrescribir variable existente")
        void testOverwriteVariable() {
            store.set("key", "valorOriginal");
            store.set("key", "valorNuevo");
            assertThat(store.get("key", String.class))
                    .isPresent().hasValue("valorNuevo");
        }

        @Test
        @DisplayName("Debe almacenar valores numéricos")
        void testStoreNumericValues() {
            store.set("intVal", 123);
            store.set("doubleVal", 3.14);
            assertThat(store.get("intVal", Integer.class)).isPresent().hasValue(123);
            assertThat(store.get("doubleVal", Double.class)).isPresent().hasValue(3.14);
        }

        @Test
        @DisplayName("Debe manejar claves con caracteres especiales")
        void testStoreKeysWithSpecialChars() {
            store.set("api.user-id_123", "value");
            assertThat(store.get("api.user-id_123", String.class)).isPresent().hasValue("value");
        }

        @Test
        @DisplayName("Debe manejar valores con caracteres especiales")
        void testStoreValuesWithSpecialChars() {
            store.set("url", "https://test.com/path?param=value&other=123");
            store.set("json", "{\"key\":\"value\"}");
            assertThat(store.get("url", String.class)).isPresent()
                    .satisfies(v -> assertThat(v.get()).contains("?", "&", "="));
            assertThat(store.get("json", String.class)).isPresent()
                    .satisfies(v -> assertThat(v.get()).contains("{", "}", ":"));
        }
    }

    // =========================================================================
    // VARIABLE REPLACEMENT TESTS
    // =========================================================================

    @Nested
    @DisplayName("Variable Replacement Tests")
    @Order(2)
    class VariableReplacementTests {

        @Test
        @DisplayName("Debe reemplazar variable simple en texto")
        void testReplaceSimpleVariable() {
            store.set("nombre", "Abel");
            assertThat(store.resolve("Hola ${nombre}!")).isEqualTo("Hola Abel!");
        }

        @Test
        @DisplayName("Debe reemplazar múltiples variables en texto")
        void testReplaceMultipleVariables() {
            store.set("user", "testuser");
            store.set("pass", "pass123");
            assertThat(store.resolve("Usuario: ${user}, Password: ${pass}"))
                    .isEqualTo("Usuario: testuser, Password: pass123");
        }

        @Test
        @DisplayName("Debe reemplazar variables repetidas en texto")
        void testReplaceRepeatedVariable() {
            store.set("token", "abc123");
            assertThat(store.resolve("Auth: ${token}, Backup: ${token}, Verify: ${token}"))
                    .isEqualTo("Auth: abc123, Backup: abc123, Verify: abc123");
        }

        @Test
        @DisplayName("Debe mantener ${VAR} si variable no existe")
        void testReplaceNonExistentVariable() {
            store.set("existe", "valor");
            assertThat(store.resolve("Existe: ${existe}, No existe: ${noExiste}"))
                    .isEqualTo("Existe: valor, No existe: ${noExiste}");
        }

        @Test
        @DisplayName("Debe buscar en System Properties si no está en store")
        void testReplaceFromSystemProperty() {
            System.setProperty("sysProp", "systemValue");
            try {
                assertThat(store.resolve("System: ${sysProp}")).isEqualTo("System: systemValue");
            } finally {
                System.clearProperty("sysProp");
            }
        }

        @Test
        @DisplayName("Store debe tener máxima prioridad sobre System Props")
        void testStorePriorityOverSystemProps() {
            System.setProperty("key", "systemValue");
            store.set("key", "storeValue");
            try {
                assertThat(store.resolve("Value: ${key}")).isEqualTo("Value: storeValue");
            } finally {
                System.clearProperty("key");
            }
        }

        @Test
        @DisplayName("Debe manejar texto sin variables")
        void testReplaceWithoutVariables() {
            assertThat(store.resolve("Texto sin variables")).isEqualTo("Texto sin variables");
        }

        @Test
        @DisplayName("Debe manejar texto null")
        void testReplaceVariablesNullText() {
            assertThat(store.resolve(null)).isNull();
        }

        @Test
        @DisplayName("Debe reemplazar variables en URLs complejas")
        void testReplaceInComplexUrl() {
            store.set("host", "api.example.com");
            store.set("port", "8443");
            store.set("path", "/v1/users");
            store.set("token", "abc123");
            assertThat(store.resolve("https://${host}:${port}${path}?token=${token}"))
                    .isEqualTo("https://api.example.com:8443/v1/users?token=abc123");
        }

        @Test
        @DisplayName("Debe reemplazar variables en JSON")
        void testReplaceInJson() {
            store.set("userId", "12345");
            store.set("userName", "testuser");
            assertThat(store.resolve("{\"id\":\"${userId}\",\"name\":\"${userName}\"}"))
                    .isEqualTo("{\"id\":\"12345\",\"name\":\"testuser\"}");
        }
    }

    // =========================================================================
    // CLEAR OPERATIONS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Clear Operations Tests")
    @Order(3)
    class ClearOperationsTests {

        @Test
        @DisplayName("clear debe limpiar todo el store")
        void testClearAllVariables() {
            store.set("key1", "value1");
            store.set("key2", "value2");
            store.clear();
            assertThat(store.get("key1", String.class)).isEmpty();
            assertThat(store.get("key2", String.class)).isEmpty();
        }

        @Test
        @DisplayName("clear debe funcionar sin errores si store está vacío")
        void testClearEmptyStore() {
            assertThatCode(() -> store.clear()).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // THREAD SAFETY TESTS
    // =========================================================================

    @Nested
    @DisplayName("Thread Safety Tests")
    @Order(4)
    class ThreadSafetyTests {

        @Test
        @DisplayName("Debe ser thread-safe en escrituras/lecturas mixtas sin lanzar excepción")
        void testConcurrentMixedOperations() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            AtomicInteger exceptions = new AtomicInteger(0);

            for (int i = 0; i < 5; i++) {
                final int writerId = i;
                executor.submit(() -> {
                    for (int j = 0; j < 100; j++) {
                        try {
                            store.set("shared" + j, "writer" + writerId);
                        } catch (Exception e) {
                            exceptions.incrementAndGet();
                        }
                    }
                });
            }
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < 100; j++) {
                        try {
                            store.get("shared" + j, String.class);
                        } catch (Exception e) {
                            exceptions.incrementAndGet();
                        }
                    }
                });
            }

            executor.shutdown();
            boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
            assertThat(finished)
                .as("Todas las tareas deben completarse en el tiempo límite")
                .isTrue();
            assertThat(exceptions.get())
                .as("No deben ocurrir excepciones en operaciones concurrentes")
                .isEqualTo(0);
        }
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases Tests")
    @Order(5)
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar muchas variables simultáneas")
        void testStoreManyVariables() {
            for (int i = 0; i < 1000; i++) {
                store.set("key" + i, "value" + i);
            }
            assertThat(store.get("key0", String.class)).isPresent().hasValue("value0");
            assertThat(store.get("key500", String.class)).isPresent().hasValue("value500");
            assertThat(store.get("key999", String.class)).isPresent().hasValue("value999");
        }

        @Test
        @DisplayName("Debe manejar caracteres unicode en valores")
        void testStoreUnicodeValues() {
            store.set("emoji", "✅ 🚀 💥");
            store.set("spanish", "Año Niño Señor");
            assertThat(store.get("emoji", String.class)).isPresent().hasValue("✅ 🚀 💥");
            assertThat(store.get("spanish", String.class)).isPresent()
                    .satisfies(v -> assertThat(v.get()).contains("Año", "Niño", "Señor"));
        }

        @Test
        @DisplayName("Debe manejar variables con guiones y underscores en resolve")
        void testVariablesWithDashesAndUnderscores() {
            store.set("api-token", "token1");
            store.set("user_id", "id123");
            assertThat(store.resolve("${api-token}")).isEqualTo("token1");
            assertThat(store.resolve("${user_id}")).isEqualTo("id123");
        }
    }

    // =========================================================================
    // REAL-WORLD USE CASES
    // =========================================================================

    @Nested
    @DisplayName("Real-World Use Cases")
    @Order(6)
    class RealWorldUseCasesTests {

        @Test
        @DisplayName("Caso de uso: Construcción de URL dinámica")
        void testDynamicUrlConstruction() {
            store.set("baseUrl", "https://api.example.com");
            store.set("version", "v2");
            store.set("endpoint", "users");
            store.set("userId", "12345");
            assertThat(store.resolve("${baseUrl}/${version}/${endpoint}/${userId}"))
                    .isEqualTo("https://api.example.com/v2/users/12345");
        }

        @Test
        @DisplayName("Caso de uso: Variables encadenadas entre steps")
        void testChainedVariablesBetweenSteps() {
            store.set("authToken", "Bearer abc123xyz");
            assertThat(store.resolve("${authToken}")).isEqualTo("Bearer abc123xyz");

            store.set("userId", "789");
            assertThat(store.resolve("/users/${userId}/profile")).isEqualTo("/users/789/profile");
        }
    }
}

