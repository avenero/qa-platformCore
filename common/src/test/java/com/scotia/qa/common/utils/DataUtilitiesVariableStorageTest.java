package com.scotia.qa.common.utils;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests unitarios para DataUtilities - PARTE 1: Variable Storage.
 *
 * <p><b>Clase CRÍTICA P0:</b> Usada por TODOS los módulos del framework</p>
 * <p><b>Scope:</b> Variable storage, retrieval y replacement</p>
 * <p><b>Cobertura objetivo:</b> 85%</p>
 *
 * <p><b>Grupos de tests:</b>
 * <ul>
 *   <li>Variable Storage (storeValue, getValue)</li>
 *   <li>Variable Replacement (replaceVariables)</li>
 *   <li>Thread Safety (concurrent access)</li>
 *   <li>Clear Operations (cleanup)</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.1
 */
@DisplayName("DataUtilities - Variable Storage Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataUtilitiesVariableStorageTest {

    @BeforeEach
    void setUp() {
        // Limpiar todas las variables antes de cada test
        DataUtilities.clearVariables();
    }

    @AfterEach
    void tearDown() {
        // Limpiar después de cada test
        DataUtilities.clearVariables();
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
            // Given
            String key = "testKey";
            String value = "testValue";

            // When
            DataUtilities.storeValue(key, value);
            String retrieved = DataUtilities.getValue(key);

            // Then
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("Debe retornar null para variable inexistente")
        void testGetNonExistentVariable() {
            // When
            String value = DataUtilities.getValue("noExiste");

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("Debe sobrescribir variable existente")
        void testOverwriteVariable() {
            // Given
            String key = "key";
            DataUtilities.storeValue(key, "valorOriginal");

            // When
            DataUtilities.storeValue(key, "valorNuevo");
            String result = DataUtilities.getValue(key);

            // Then
            assertThat(result).isEqualTo("valorNuevo");
        }

        @Test
        @DisplayName("Debe almacenar valores numéricos como strings")
        void testStoreNumericValues() {
            // When
            DataUtilities.storeValue("int", 123);
            DataUtilities.storeValue("long", 999999999L);
            DataUtilities.storeValue("double", 3.14);

            // Then
            assertThat(DataUtilities.getValue("int")).isEqualTo("123");
            assertThat(DataUtilities.getValue("long")).isEqualTo("999999999");
            assertThat(DataUtilities.getValue("double")).isEqualTo("3.14");
        }

        @Test
        @DisplayName("Debe almacenar valores booleanos como strings")
        void testStoreBooleanValues() {
            // When
            DataUtilities.storeValue("true", true);
            DataUtilities.storeValue("false", false);

            // Then
            assertThat(DataUtilities.getValue("true")).isEqualTo("true");
            assertThat(DataUtilities.getValue("false")).isEqualTo("false");
        }

        @Test
        @DisplayName("Debe manejar claves con caracteres especiales")
        void testStoreKeysWithSpecialChars() {
            // When
            DataUtilities.storeValue("api.user-id_123", "value");
            DataUtilities.storeValue("web:session@token", "token123");

            // Then
            assertThat(DataUtilities.getValue("api.user-id_123")).isEqualTo("value");
            assertThat(DataUtilities.getValue("web:session@token")).isEqualTo("token123");
        }

        @Test
        @DisplayName("Debe manejar valores con caracteres especiales")
        void testStoreValuesWithSpecialChars() {
            // When
            DataUtilities.storeValue("url", "https://test.com/path?param=value&other=123");
            DataUtilities.storeValue("json", "{\"key\":\"value\"}");
            DataUtilities.storeValue("sql", "SELECT * FROM users WHERE id = ?");

            // Then
            assertThat(DataUtilities.getValue("url")).contains("?", "&", "=");
            assertThat(DataUtilities.getValue("json")).contains("{", "}", ":");
            assertThat(DataUtilities.getValue("sql")).contains("SELECT", "WHERE");
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
            // Given
            DataUtilities.storeValue("nombre", "Abel");
            String template = "Hola ${nombre}!";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Hola Abel!");
        }

        @Test
        @DisplayName("Debe reemplazar múltiples variables en texto")
        void testReplaceMultipleVariables() {
            // Given
            DataUtilities.storeValue("user", "testuser");
            DataUtilities.storeValue("pass", "pass123");
            String template = "Usuario: ${user}, Password: ${pass}";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Usuario: testuser, Password: pass123");
        }

        @Test
        @DisplayName("Debe reemplazar variables repetidas en texto")
        void testReplaceRepeatedVariable() {
            // Given
            DataUtilities.storeValue("token", "abc123");
            String template = "Auth: ${token}, Backup: ${token}, Verify: ${token}";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Auth: abc123, Backup: abc123, Verify: abc123");
        }

        @Test
        @DisplayName("Debe mantener ${VAR} si variable no existe")
        void testReplaceNonExistentVariable() {
            // Given
            DataUtilities.storeValue("existe", "valor");
            String template = "Existe: ${existe}, No existe: ${noExiste}";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("Existe: valor, No existe: ${noExiste}");
        }

        @Test
        @DisplayName("Debe buscar en System Properties si no está en store")
        void testReplaceFromSystemProperty() {
            // Given
            System.setProperty("sysProp", "systemValue");
            String template = "System: ${sysProp}";

            try {
                // When
                String result = DataUtilities.replaceVariables(template);

                // Then
                assertThat(result).isEqualTo("System: systemValue");
            } finally {
                // Cleanup
                System.clearProperty("sysProp");
            }
        }

        @Test
        @DisplayName("Debe buscar en Environment Variables si no está en store ni System Props")
        void testReplaceFromEnvironmentVariable() {
            // Given - Simulamos env var con System Property
            System.setProperty("ENV_VAR_TEST", "envValue");
            String template = "Env: ${ENV_VAR_TEST}";

            try {
                // When
                String result = DataUtilities.replaceVariables(template);

                // Then
                assertThat(result).isEqualTo("Env: envValue");
            } finally {
                System.clearProperty("ENV_VAR_TEST");
            }
        }

        @Test
        @DisplayName("Store debe tener máxima prioridad sobre System Props")
        void testStorePriorityOverSystemProps() {
            // Given
            System.setProperty("key", "systemValue");
            DataUtilities.storeValue("key", "storeValue");
            String template = "Value: ${key}";

            try {
                // When
                String result = DataUtilities.replaceVariables(template);

                // Then - Store debe ganar
                assertThat(result).isEqualTo("Value: storeValue");
            } finally {
                System.clearProperty("key");
            }
        }

        @Test
        @DisplayName("Debe manejar texto sin variables")
        void testReplaceWithoutVariables() {
            // Given
            String text = "Texto sin variables";

            // When
            String result = DataUtilities.replaceVariables(text);

            // Then
            assertThat(result).isEqualTo(text);
        }

        @Test
        @DisplayName("Debe manejar texto null")
        void testReplaceVariablesNullText() {
            // When
            String result = DataUtilities.replaceVariables(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Debe reemplazar variables en URLs complejas")
        void testReplaceInComplexUrl() {
            // Given
            DataUtilities.storeValue("host", "api.scotia.com");
            DataUtilities.storeValue("port", "8443");
            DataUtilities.storeValue("path", "/v1/users");
            String template = "https://${host}:${port}${path}?token=${token}";
            DataUtilities.storeValue("token", "abc123");

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result)
                .isEqualTo("https://api.scotia.com:8443/v1/users?token=abc123")
                .startsWith("https://")
                .contains("?token=");
        }

        @Test
        @DisplayName("Debe reemplazar variables en JSON")
        void testReplaceInJson() {
            // Given
            DataUtilities.storeValue("userId", "12345");
            DataUtilities.storeValue("userName", "testuser");
            String template = "{\"id\":\"${userId}\",\"name\":\"${userName}\"}";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("{\"id\":\"12345\",\"name\":\"testuser\"}");
        }

        @Test
        @DisplayName("Debe reemplazar variables en SQL queries")
        void testReplaceInSqlQuery() {
            // Given
            DataUtilities.storeValue("tableName", "users");
            DataUtilities.storeValue("userId", "999");
            String template = "SELECT * FROM ${tableName} WHERE id = ${userId}";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result).isEqualTo("SELECT * FROM users WHERE id = 999");
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
        @DisplayName("clearVariables debe limpiar todo el store")
        void testClearAllVariables() {
            // Given
            DataUtilities.storeValue("key1", "value1");
            DataUtilities.storeValue("key2", "value2");
            DataUtilities.storeValue("key3", "value3");

            // When
            DataUtilities.clearVariables();

            // Then
            assertThat(DataUtilities.getValue("key1")).isNull();
            assertThat(DataUtilities.getValue("key2")).isNull();
            assertThat(DataUtilities.getValue("key3")).isNull();
        }

        @Test
        @DisplayName("clear debe funcionar sin errores incluso si store está vacío")
        void testClearEmptyStore() {
            // When/Then - No debe lanzar excepción
            assertThatCode(() -> DataUtilities.clearVariables())
                .doesNotThrowAnyException();
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
        @DisplayName("Debe ser thread-safe en escritura concurrente")
        void testConcurrentWrites() throws InterruptedException {
            // Given
            int threadCount = 20;
            int writesPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // When - Múltiples threads escriben simultáneamente
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < writesPerThread; j++) {
                        String key = "thread" + threadId + "_var" + j;
                        String value = "value" + threadId + "_" + j;
                        DataUtilities.storeValue(key, value);
                        String retrieved = DataUtilities.getValue(key);
                        if (value.equals(retrieved)) {
                            successCount.incrementAndGet();
                        }
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // Then - Todas las escrituras/lecturas deben ser correctas
            assertThat(successCount.get()).isEqualTo(threadCount * writesPerThread);
        }

        @Test
        @DisplayName("Debe ser thread-safe en lecturas concurrentes")
        void testConcurrentReads() throws InterruptedException {
            // Given - Preparar datos
            for (int i = 0; i < 100; i++) {
                DataUtilities.storeValue("key" + i, "value" + i);
            }

            ExecutorService executor = Executors.newFixedThreadPool(10);
            AtomicInteger correctReads = new AtomicInteger(0);

            // When - Múltiples threads leen simultáneamente
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < 100; j++) {
                        String value = DataUtilities.getValue("key" + j);
                        if (("value" + j).equals(value)) {
                            correctReads.incrementAndGet();
                        }
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Then
            assertThat(correctReads.get()).isEqualTo(1000); // 10 threads × 100 reads
        }

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
                            DataUtilities.storeValue("shared" + j, "writer" + writerId);
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
                            DataUtilities.getValue("shared" + j);
                        } catch (Exception e) {
                            exceptions.incrementAndGet();
                        }
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // No debe haber excepciones de concurrencia
            assertThat(exceptions.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("clear debe ser thread-safe")
        void testClearThreadSafe() throws InterruptedException {
            // Given
            for (int i = 0; i < 100; i++) {
                DataUtilities.storeValue("key" + i, "value" + i);
            }

            ExecutorService executor = Executors.newFixedThreadPool(5);

            // When - Algunos threads escriben, otros limpian
            executor.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    DataUtilities.storeValue("new" + i, "value");
                }
            });

            Thread.sleep(10); // Pequeño delay

            executor.submit(DataUtilities::clearVariables);

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Then - No debe lanzar excepciones
            assertThatCode(() -> DataUtilities.getValue("key1"))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // EDGE CASES & SPECIAL SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases Tests")
    @Order(5)
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar valores vacíos")
        void testStoreEmptyValue() {
            // When
            DataUtilities.storeValue("empty", "");

            // Then
            assertThat(DataUtilities.getValue("empty")).isEqualTo("");
        }

        @Test
        @DisplayName("Debe manejar valores con espacios")
        void testStoreValueWithSpaces() {
            // When
            DataUtilities.storeValue("spaces", "   value with spaces   ");

            // Then
            assertThat(DataUtilities.getValue("spaces")).isEqualTo("   value with spaces   ");
        }

        @Test
        @DisplayName("Debe manejar valores muy largos")
        void testStoreLargeValue() {
            // Given
            String largeValue = "x".repeat(10000);

            // When
            DataUtilities.storeValue("large", largeValue);

            // Then
            assertThat(DataUtilities.getValue("large"))
                .hasSize(10000)
                .startsWith("xxx");
        }

        @Test
        @DisplayName("Debe manejar muchas variables simultáneas")
        void testStoreManyVariables() {
            // When
            for (int i = 0; i < 1000; i++) {
                DataUtilities.storeValue("key" + i, "value" + i);
            }

            // Then
            assertThat(DataUtilities.getValue("key0")).isEqualTo("value0");
            assertThat(DataUtilities.getValue("key500")).isEqualTo("value500");
            assertThat(DataUtilities.getValue("key999")).isEqualTo("value999");
        }

        @Test
        @DisplayName("Debe manejar caracteres unicode en valores")
        void testStoreUnicodeValues() {
            // When
            DataUtilities.storeValue("emoji", "✅ 🚀 💥");
            DataUtilities.storeValue("spanish", "Año Niño Señor");
            DataUtilities.storeValue("chinese", "中文测试");

            // Then
            assertThat(DataUtilities.getValue("emoji")).isEqualTo("✅ 🚀 💥");
            assertThat(DataUtilities.getValue("spanish"))
                .isNotNull()
                .contains("Año", "Niño", "Señor");  // Verificar palabras completas
            assertThat(DataUtilities.getValue("chinese")).isEqualTo("中文测试");
        }
    }

    // =========================================================================
    // REPLACEMENT PRIORITY TESTS (Store > System Props > Env Vars)
    // =========================================================================

    @Nested
    @DisplayName("Replacement Priority Tests")
    @Order(6)
    class ReplacementPriorityTests {

        @Test
        @DisplayName("Store debe tener máxima prioridad sobre System Property")
        void testStorePriority() {
            System.setProperty("priority", "system");
            DataUtilities.storeValue("priority", "store");

            try {
                String result = DataUtilities.replaceVariables("${priority}");
                assertThat(result).isEqualTo("store");
            } finally {
                System.clearProperty("priority");
            }
        }
    }

    // =========================================================================
    // REAL-WORLD USE CASES
    // =========================================================================

    @Nested
    @DisplayName("Real-World Use Cases")
    @Order(7)
    class RealWorldUseCasesTests {

        @Test
        @DisplayName("Caso de uso: Construcción de URL dinámica")
        void testDynamicUrlConstruction() {
            // Given - Configuración típica de API
            DataUtilities.storeValue("baseUrl", "https://api.scotia.com");
            DataUtilities.storeValue("version", "v2");
            DataUtilities.storeValue("endpoint", "users");
            DataUtilities.storeValue("userId", "12345");

            // When
            String url = DataUtilities.replaceVariables("${baseUrl}/${version}/${endpoint}/${userId}");

            // Then
            assertThat(url).isEqualTo("https://api.scotia.com/v2/users/12345");
        }

        @Test
        @DisplayName("Caso de uso: Request body con variables")
        void testRequestBodyWithVariables() {
            // Given
            DataUtilities.storeValue("userName", "testuser");
            DataUtilities.storeValue("userEmail", "test@scotia.com");
            DataUtilities.storeValue("userId", "999");

            String template = """
                {
                  "id": "${userId}",
                  "name": "${userName}",
                  "email": "${userEmail}"
                }
                """;

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result)
                .contains("\"id\": \"999\"")
                .contains("\"name\": \"testuser\"")
                .contains("\"email\": \"test@scotia.com\"");
        }

        @Test
        @DisplayName("Caso de uso: Database connection string")
        void testDatabaseConnectionString() {
            // Given
            DataUtilities.storeValue("dbHost", "qa-db.scotia.com");
            DataUtilities.storeValue("dbPort", "5432");
            DataUtilities.storeValue("dbName", "banking");
            DataUtilities.storeValue("dbUser", "qauser");

            String template = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}?user=${dbUser}";

            // When
            String result = DataUtilities.replaceVariables(template);

            // Then
            assertThat(result)
                .isEqualTo("jdbc:postgresql://qa-db.scotia.com:5432/banking?user=qauser")
                .startsWith("jdbc:")
                .contains("?user=");
        }

        @Test
        @DisplayName("Caso de uso: Variables encadenadas entre steps")
        void testChainedVariablesBetweenSteps() {
            // Simula flujo real de Cucumber steps:

            // Step 1: Almacenar token de autenticación
            DataUtilities.storeValue("authToken", "Bearer abc123xyz");

            // Step 2: Usar token en request
            String headerValue = DataUtilities.replaceVariables("${authToken}");
            assertThat(headerValue).isEqualTo("Bearer abc123xyz");

            // Step 3: Almacenar userId de response
            DataUtilities.storeValue("userId", "789");

            // Step 4: Usar userId en próximo request
            String url = DataUtilities.replaceVariables("/users/${userId}/profile");
            assertThat(url).isEqualTo("/users/789/profile");
        }
    }

    // =========================================================================
    // EDGE CASES WITH SPECIAL FORMATS
    // =========================================================================

    @Nested
    @DisplayName("Special Format Tests")
    @Order(8)
    class SpecialFormatTests {

        @Test
        @DisplayName("Debe manejar variables consecutivas sin separador")
        void testConsecutiveVariables() {
            // Given
            DataUtilities.storeValue("prefix", "test");
            DataUtilities.storeValue("suffix", "user");

            // When
            String result = DataUtilities.replaceVariables("${prefix}${suffix}");

            // Then
            assertThat(result).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Debe manejar variables al inicio y fin del texto")
        void testVariablesAtBoundaries() {
            // Given
            DataUtilities.storeValue("start", "BEGIN");
            DataUtilities.storeValue("end", "END");

            // When
            String result = DataUtilities.replaceVariables("${start} middle ${end}");

            // Then
            assertThat(result).isEqualTo("BEGIN middle END");
        }

        @Test
        @DisplayName("Debe manejar texto que es solo una variable")
        void testTextIsOnlyVariable() {
            // Given
            DataUtilities.storeValue("onlyVar", "completeValue");

            // When
            String result = DataUtilities.replaceVariables("${onlyVar}");

            // Then
            assertThat(result).isEqualTo("completeValue");
        }

        @Test
        @DisplayName("Debe manejar variables con números en el nombre")
        void testVariablesWithNumbers() {
            // Given
            DataUtilities.storeValue("var1", "value1");
            DataUtilities.storeValue("var2", "value2");
            DataUtilities.storeValue("var123", "value123");

            // When
            String result = DataUtilities.replaceVariables("${var1} ${var2} ${var123}");

            // Then
            assertThat(result).isEqualTo("value1 value2 value123");
        }

        @Test
        @DisplayName("Debe manejar variables con guiones y underscores")
        void testVariablesWithDashesAndUnderscores() {
            // Given
            DataUtilities.storeValue("api-token", "token1");
            DataUtilities.storeValue("user_id", "id123");
            DataUtilities.storeValue("db-connection_string", "jdbc:...");

            // When
            String token = DataUtilities.replaceVariables("${api-token}");
            String userId = DataUtilities.replaceVariables("${user_id}");
            String dbConn = DataUtilities.replaceVariables("${db-connection_string}");

            // Then
            assertThat(token).isEqualTo("token1");
            assertThat(userId).isEqualTo("id123");
            assertThat(dbConn).isEqualTo("jdbc:...");
        }
    }

    // =========================================================================
    // ERROR PREVENTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Error Prevention Tests")
    @Order(9)
    class ErrorPreventionTests {

        @Test
        @DisplayName("Debe prevenir infinite loop con variables circulares")
        void testPreventInfiniteLoopWithCircularVariables() {
            // Given - Escenario patológico: var1 → var2 → var1
            DataUtilities.storeValue("circular1", "${circular2}");
            DataUtilities.storeValue("circular2", "${circular1}");

            // When/Then - No debe colgar ni lanzar StackOverflowError
            assertThatCode(() -> {
                String result = DataUtilities.replaceVariables("${circular1}");
                // Debe mantener la variable sin resolver o hacer 1 nivel de resolución
                assertThat(result).isNotNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar variables con sintaxis malformada")
        void testMalformedVariableSyntax() {
            // Given
            DataUtilities.storeValue("var", "value");

            // When - Diferentes sintaxis incorrectas
            String incomplete1 = DataUtilities.replaceVariables("${var");     // Sin cerrar
            String incomplete2 = DataUtilities.replaceVariables("$var}");     // Sin abrir
            String nested = DataUtilities.replaceVariables("${${var}}");      // Anidado
            String empty = DataUtilities.replaceVariables("${}");             // Vacío

            // Then - No debe lanzar excepción, debe retornar texto original o parcialmente procesado
            assertThat(incomplete1).isNotNull();
            assertThat(incomplete2).isNotNull();
            assertThat(nested).isNotNull();
            assertThat(empty).isNotNull();
        }

        @Test
        @DisplayName("Debe manejar texto con muchas variables")
        void testManyVariablesInText() {
            // Given
            for (int i = 0; i < 50; i++) {
                DataUtilities.storeValue("var" + i, "val" + i);
            }

            StringBuilder template = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                template.append("${var").append(i).append("}");
                if (i < 49) template.append(",");
            }

            // When
            String result = DataUtilities.replaceVariables(template.toString());

            // Then
            assertThat(result).startsWith("val0");
            assertThat(result).endsWith("val49");
            assertThat(result).contains("val25");
        }
    }
}

