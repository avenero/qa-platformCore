package com.scotia.qa.common.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para ConfigManager - Singleton de configuración global.
 *
 * <p><b>Clase CRÍTICA P0:</b> Si falla = Todo el framework falla</p>
 * <p><b>Cobertura objetivo:</b> 85%</p>
 * <p><b>Total tests:</b> 25</p>
 *
 * <p><b>Validaciones clave:</b>
 * <ul>
 *   <li>Singleton pattern correcto</li>
 *   <li>Precedencia: System Props > Env Vars > Config File</li>
 *   <li>Resolución de variables ${VAR}</li>
 *   <li>Type conversions (int, long, boolean)</li>
 *   <li>Error handling robusto</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.1
 */
@DisplayName("ConfigManager Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigManagerTest {

    private ConfigManager config;

    // Variables de entorno para tests
    private static final String ORIGINAL_ENV = System.getProperty("env");

    @BeforeEach
    void setUp() {
        // Limpiar system properties de tests anteriores
        System.clearProperty("test.override");
        System.clearProperty("test.number");

        // Configurar ambiente de test
        System.setProperty("env", "test");

        // Configurar variables de entorno simuladas
        System.setProperty("TEST_VAR", "resolvedValue");
        System.setProperty("VAR1", "part1");
        System.setProperty("VAR2", "part2");
        System.setProperty("DB_HOST", "localhost");
        System.setProperty("API_BASE_URL", "http://localhost:8080");
        System.setProperty("BASE_URL", "https://test.scotia.com");

        // Obtener instancia (puede reutilizar singleton existente)
        config = ConfigManager.getInstance();

        // Forzar recarga para ambiente de test
        config.reload();
    }

    @AfterEach
    void tearDown() {
        // Restaurar ambiente original
        if (ORIGINAL_ENV != null) {
            System.setProperty("env", ORIGINAL_ENV);
        } else {
            System.clearProperty("env");
        }

        // Limpiar properties de test
        System.clearProperty("test.override");
        System.clearProperty("test.number");
        System.clearProperty("TEST_VAR");
        System.clearProperty("VAR1");
        System.clearProperty("VAR2");
        System.clearProperty("DB_HOST");
        System.clearProperty("API_BASE_URL");
        System.clearProperty("BASE_URL");
    }

    // =========================================================================
    // SINGLETON PATTERN TESTS
    // =========================================================================

    @Nested
    @DisplayName("Singleton Pattern Tests")
    @Order(1)
    class SingletonTests {

        @Test
        @DisplayName("getInstance debe retornar siempre la misma instancia")
        void testSingletonSameInstance() {
            // When
            ConfigManager instance1 = ConfigManager.getInstance();
            ConfigManager instance2 = ConfigManager.getInstance();
            ConfigManager instance3 = ConfigManager.getInstance();

            // Then
            assertThat(instance1)
                .isNotNull()
                .isSameAs(instance2)
                .isSameAs(instance3);
        }

        @Test
        @DisplayName("getInstance no debe retornar null")
        void testSingletonNotNull() {
            // When
            ConfigManager instance = ConfigManager.getInstance();

            // Then
            assertThat(instance).isNotNull();
        }

        @Test
        @DisplayName("getInstance debe ser thread-safe")
        void testSingletonThreadSafe() throws InterruptedException {
            // Given
            ConfigManager[] instances = new ConfigManager[10];
            Thread[] threads = new Thread[10];

            // When - Múltiples threads obtienen instancia simultáneamente
            for (int i = 0; i < 10; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    instances[index] = ConfigManager.getInstance();
                });
                threads[i].start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }

            // Then - Todas deben ser la misma instancia
            ConfigManager first = instances[0];
            for (int i = 1; i < instances.length; i++) {
                assertThat(instances[i]).isSameAs(first);
            }
        }
    }

    // =========================================================================
    // BASIC GET OPERATIONS
    // =========================================================================

    @Nested
    @DisplayName("Basic Get Operations")
    @Order(2)
    class BasicGetTests {

        @Test
        @DisplayName("get debe retornar valor desde config file")
        void testGetFromConfigFile() {
            // When
            String value = config.get("test.string.simple");

            // Then
            assertThat(value).isEqualTo("testValue");
        }

        @Test
        @DisplayName("get debe retornar null para clave inexistente")
        void testGetNonExistent() {
            // When
            String value = config.get("clave.no.existe");

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("get con default debe retornar valor si existe")
        void testGetWithDefaultWhenExists() {
            // When
            String value = config.get("test.string.simple", "defaultValue");

            // Then
            assertThat(value).isEqualTo("testValue");
        }

        @Test
        @DisplayName("get con default debe retornar default si no existe")
        void testGetWithDefaultWhenNotExists() {
            // When
            String value = config.get("clave.no.existe", "valorPorDefecto");

            // Then
            assertThat(value).isEqualTo("valorPorDefecto");
        }

        @Test
        @DisplayName("contains debe retornar true para clave existente")
        void testContainsExisting() {
            // Then
            assertThat(config.contains("test.string.simple")).isTrue();
        }

        @Test
        @DisplayName("contains debe retornar false para clave inexistente")
        void testContainsNonExisting() {
            // Then
            assertThat(config.contains("no.existe")).isFalse();
        }
    }

    // =========================================================================
    // PRECEDENCE TESTS (System Props > ENV > Config File)
    // =========================================================================

    @Nested
    @DisplayName("Precedence Tests")
    @Order(3)
    class PrecedenceTests {

        @Test
        @DisplayName("System Property debe tener máxima prioridad sobre config file")
        void testSystemPropertyOverridesConfigFile() {
            // Given - config file tiene: test.string.simple=testValue
            System.setProperty("test.string.simple", "overriddenBySystemProp");

            // When
            String value = config.get("test.string.simple");

            // Then
            assertThat(value).isEqualTo("overriddenBySystemProp");

            // Cleanup
            System.clearProperty("test.string.simple");
        }

        @Test
        @DisplayName("Environment Variable debe tener prioridad sobre config file")
        void testEnvVarOverridesConfigFile() {
            // Given - Simulamos env var con System.setProperty (en test real sería System.getenv)
            // Como no podemos modificar env vars reales, usamos System properties para simular
            String key = "TEST_ENV_VAR";
            System.setProperty(key, "envValue");

            // When
            String value = config.get(key);

            // Then
            assertThat(value).isEqualTo("envValue");

            // Cleanup
            System.clearProperty(key);
        }

        @Test
        @DisplayName("System Property debe tener prioridad sobre Environment Variable")
        void testSystemPropertyOverridesEnvVar() {
            // Given
            String key = "TEST_PRECEDENCE";
            System.setProperty(key, "fromSystemProp");
            // Nota: En runtime real, env var tendría valor diferente

            // When
            String value = config.get(key);

            // Then - System Property gana
            assertThat(value).isEqualTo("fromSystemProp");

            // Cleanup
            System.clearProperty(key);
        }
    }

    // =========================================================================
    // ENVIRONMENT VARIABLE RESOLUTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Environment Variable Resolution Tests")
    @Order(4)
    class EnvVarResolutionTests {

        @Test
        @DisplayName("Debe resolver variable simple ${VAR}")
        void testResolveSimpleVariable() {
            // Given - config file tiene: test.env.resolved=${TEST_VAR}
            // Setup tiene: System.setProperty("TEST_VAR", "resolvedValue")

            // When
            String value = config.get("test.env.resolved");

            // Then
            assertThat(value).isEqualTo("resolvedValue");
        }

        @Test
        @DisplayName("Debe resolver múltiples variables en un valor")
        void testResolveMultipleVariables() {
            // Given - config file tiene: test.env.multiple=${VAR1}_${VAR2}

            // When
            String value = config.get("test.env.multiple");

            // Then
            assertThat(value).isEqualTo("part1_part2");
        }

        @Test
        @DisplayName("Debe resolver variables anidadas en URL")
        void testResolveNestedInUrl() {
            // Given - config file tiene: db.url=jdbc:postgresql://${DB_HOST}:5432/testdb

            // When
            String value = config.get("db.url");

            // Then
            assertThat(value)
                .isEqualTo("jdbc:postgresql://localhost:5432/testdb")
                .contains("localhost")
                .startsWith("jdbc:");
        }

        @Test
        @DisplayName("Debe mantener ${VAR} sin resolver si variable no existe")
        void testUnresolvedVariableKept() {
            // Given - config file tiene: test.env.notfound=${VAR_NOT_EXISTS}
            // Variable VAR_NOT_EXISTS NO está definida

            // When
            String value = config.get("test.env.notfound");

            // Then - Debe mantener el placeholder
            assertThat(value).isEqualTo("${VAR_NOT_EXISTS}");
        }

        @Test
        @DisplayName("Debe manejar valores sin variables")
        void testNoVariablesToResolve() {
            // When
            String value = config.get("test.string.simple");

            // Then
            assertThat(value).isEqualTo("testValue");
        }
    }

    // =========================================================================
    // TYPE CONVERSION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Type Conversion Tests")
    @Order(5)
    class TypeConversionTests {

        @Test
        @DisplayName("getInt debe convertir string a int correctamente")
        void testGetIntValid() {
            // When
            int value = config.getInt("test.number.int", 0);

            // Then
            assertThat(value).isEqualTo(42);
        }

        @Test
        @DisplayName("getInt debe retornar default si clave no existe")
        void testGetIntNotFound() {
            // When
            int value = config.getInt("no.existe", 999);

            // Then
            assertThat(value).isEqualTo(999);
        }

        @Test
        @DisplayName("getInt debe retornar default si valor no es número válido")
        void testGetIntInvalidFormat() {
            // Given
            System.setProperty("test.invalid.int", "notANumber");

            // When
            int value = config.getInt("test.invalid.int", 100);

            // Then
            assertThat(value).isEqualTo(100);

            // Cleanup
            System.clearProperty("test.invalid.int");
        }

        @Test
        @DisplayName("getLong debe convertir string a long correctamente")
        void testGetLongValid() {
            // When
            long value = config.getLong("test.number.long", 0L);

            // Then
            assertThat(value).isEqualTo(9999999999L);
        }

        @Test
        @DisplayName("getLong debe retornar default si no es número válido")
        void testGetLongInvalidFormat() {
            // Given
            System.setProperty("test.invalid.long", "invalid");

            // When
            long value = config.getLong("test.invalid.long", 5000L);

            // Then
            assertThat(value).isEqualTo(5000L);

            // Cleanup
            System.clearProperty("test.invalid.long");
        }

        @ParameterizedTest
        @CsvSource({
            "test.boolean.true, true",
            "test.boolean.false, false"
        })
        @DisplayName("getBoolean debe parsear valores correctamente")
        void testGetBooleanValid(String key, boolean expected) {
            // When
            boolean value = config.getBoolean(key, false);

            // Then
            assertThat(value).isEqualTo(expected);
        }

        @Test
        @DisplayName("getBoolean debe retornar default si clave no existe")
        void testGetBooleanNotFound() {
            // When
            boolean value = config.getBoolean("no.existe", true);

            // Then
            assertThat(value).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"yes", "no", "1", "0", "invalid"})
        @DisplayName("getBoolean debe manejar valores no booleanos")
        void testGetBooleanNonBooleanValues(String invalidValue) {
            // Given
            System.setProperty("test.invalid.bool", invalidValue);

            // When
            boolean value = config.getBoolean("test.invalid.bool", true);

            // Then - Boolean.parseBoolean retorna false para cualquier cosa que no sea "true"
            if ("true".equalsIgnoreCase(invalidValue)) {
                assertThat(value).isTrue();
            } else {
                assertThat(value).isFalse();
            }

            // Cleanup
            System.clearProperty("test.invalid.bool");
        }

        @Test
        @DisplayName("getInt debe hacer trim antes de parsear")
        void testGetIntWithWhitespace() {
            // Given
            System.setProperty("test.int.whitespace", "  123  ");

            // When
            int value = config.getInt("test.int.whitespace", 0);

            // Then
            assertThat(value).isEqualTo(123);

            // Cleanup
            System.clearProperty("test.int.whitespace");
        }
    }

    // =========================================================================
    // ENVIRONMENT DETECTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Environment Detection Tests")
    @Order(6)
    class EnvironmentTests {

        @Test
        @DisplayName("getEnvironment debe retornar ambiente configurado")
        void testGetEnvironment() {
            // Then - Debe ser 'test' como configuramos en setUp
            String env = config.getEnvironment();
            assertThat(env)
                .isNotNull()
                .isNotEmpty()
                .isIn("test", "qa", "dev", "uat", "prod");  // Valores válidos
        }

        @Test
        @DisplayName("Ambiente debe ser consistente entre llamadas")
        void testEnvironmentConsistent() {
            // When
            String env1 = config.getEnvironment();
            String env2 = config.getEnvironment();
            String env3 = config.getEnvironment();

            // Then
            assertThat(env1)
                .isEqualTo(env2)
                .isEqualTo(env3);
        }
    }

    // =========================================================================
    // RELOAD TESTS
    // =========================================================================

    @Nested
    @DisplayName("Reload Tests")
    @Order(7)
    class ReloadTests {

        @Test
        @DisplayName("reload debe recargar configuración sin lanzar excepción")
        void testReloadSuccessful() {
            // When/Then - No debe lanzar excepción
            assertThatCode(() -> config.reload())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("reload debe mantener instancia singleton")
        void testReloadKeepsSingleton() {
            // Given
            ConfigManager before = ConfigManager.getInstance();

            // When
            config.reload();
            ConfigManager after = ConfigManager.getInstance();

            // Then - Debe ser la misma instancia
            assertThat(after).isSameAs(before);
        }
    }

    // =========================================================================
    // EDGE CASES & ERROR HANDLING
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases Tests")
    @Order(8)
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar valores vacíos en config file")
        void testEmptyValue() {
            // When - config file tiene: test.string.empty=
            String value = config.get("test.string.empty");

            // Then
            assertThat(value).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Debe manejar claves con caracteres especiales")
        void testSpecialCharactersInValue() {
            // When
            String url = config.get("test.special.url");
            String json = config.get("test.special.json");

            // Then
            assertThat(url).contains("?", "&", "=");
            assertThat(json).contains("{", "}", ":");
        }

        @Test
        @DisplayName("Debe manejar valores numéricos como strings")
        void testNumericValuesAsStrings() {
            // When
            String port = config.get("db.port");

            // Then
            assertThat(port).isEqualTo("5432");
        }

        @Test
        @DisplayName("getInt debe manejar valores con espacios al inicio/fin")
        void testGetIntTrimsSpaces() {
            // Given
            System.setProperty("test.spaced.number", "   789   ");

            // When
            int value = config.getInt("test.spaced.number", 0);

            // Then
            assertThat(value).isEqualTo(789);

            // Cleanup
            System.clearProperty("test.spaced.number");
        }
    }

    // =========================================================================
    // REAL USE CASE TESTS
    // =========================================================================

    @Nested
    @DisplayName("Real Use Case Tests")
    @Order(9)
    class RealUseCaseTests {

        @Test
        @DisplayName("Debe cargar configuración típica de database")
        void testDatabaseConfig() {
            // When
            String host = config.get("db.host");
            String port = config.get("db.port");
            String name = config.get("db.name");
            String url = config.get("db.url");

            // Then
            assertThat(host).isEqualTo("localhost");
            assertThat(port).isEqualTo("5432");
            assertThat(name).isEqualTo("testdb");
            assertThat(url)
                .contains("localhost")
                .contains("5432")
                .contains("testdb");
        }

        @Test
        @DisplayName("Debe cargar configuración típica de API")
        void testApiConfig() {
            // When
            String baseUrl = config.get("api.base.url");
            int timeout = config.getInt("api.timeout", 0);
            int retry = config.getInt("api.retry.max", 0);

            // Then
            assertThat(baseUrl).isEqualTo("http://localhost:8080");
            assertThat(timeout).isEqualTo(30000);
            assertThat(retry).isEqualTo(3);
        }

        @Test
        @DisplayName("Debe construir configuración completa para módulo de testing")
        void testCompleteModuleConfig() {
            // Simulación de uso real en qa-banking

            // When - Leer todas las configs necesarias
            String apiUrl = config.get("api.base.url");
            String dbUrl = config.get("db.url");
            int apiTimeout = config.getInt("api.timeout", 10000);
            boolean hasAllRequiredConfigs =
                config.contains("api.base.url") &&
                config.contains("db.url") &&
                config.contains("api.timeout");

            // Then
            assertThat(apiUrl).isNotNull();
            assertThat(dbUrl).isNotNull();
            assertThat(apiTimeout).isPositive();
            assertThat(hasAllRequiredConfigs).isTrue();
        }
    }

    // =========================================================================
    // PROVIDER ACCESS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Provider Access Tests")
    @Order(10)
    class ProviderAccessTests {

        @Test
        @DisplayName("getUnderlyingProvider debe retornar provider no null")
        void testGetUnderlyingProvider() {
            // When
            var provider = config.getUnderlyingProvider();

            // Then
            assertThat(provider).isNotNull();
        }

        @Test
        @DisplayName("getUnderlyingProvider debe retornar mismo provider en múltiples llamadas")
        void testGetUnderlyingProviderConsistency() {
            // When
            var provider1 = config.getUnderlyingProvider();
            var provider2 = config.getUnderlyingProvider();

            // Then
            assertThat(provider1).isSameAs(provider2);
        }
    }

    // =========================================================================
    // INTEGRATION TESTS (Orden de carga de archivos)
    // =========================================================================

    @Nested
    @DisplayName("File Loading Strategy Tests")
    @Order(11)
    class FileLoadingTests {

        @Test
        @DisplayName("Debe cargar valores desde archivo de configuración")
        void testLoadConfigFile() {
            // When - Valores definidos en config-test.properties
            String stringValue = config.get("test.string.simple");
            String dbName = config.get("db.name");

            // Then
            assertThat(stringValue).isNotNull();
            assertThat(dbName).isNotNull();
        }

        @Test
        @DisplayName("Debe funcionar con solo System Properties si no hay archivo")
        void testWorksWithSystemPropertiesOnly() {
            // Given
            System.setProperty("manual.config", "manualValue");

            // When
            String value = config.get("manual.config");

            // Then
            assertThat(value).isEqualTo("manualValue");

            // Cleanup
            System.clearProperty("manual.config");
        }

        @Test
        @DisplayName("Debe priorizar System Property sobre archivo")
        void testSystemPropertyOverridesFile() {
            // Given - Archivo tiene test.string.simple=testValue
            System.setProperty("test.string.simple", "overriddenValue");

            // When
            String value = config.get("test.string.simple");

            // Then
            assertThat(value).isEqualTo("overriddenValue");

            // Cleanup
            System.clearProperty("test.string.simple");
        }
    }
}

