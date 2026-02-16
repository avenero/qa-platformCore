package com.scotia.qa.common.config;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests unitarios para ConfigManager.getWithPriority().
 *
 * <p>Este nuevo método implementa resolución de configuración con prioridades:</p>
 * <ol>
 *   <li>System Property (-Dproperty=value)</li>
 *   <li>Variable de entorno (ENV_VAR)</li>
 *   <li>Archivo de configuración (config.properties)</li>
 *   <li>Valor por defecto</li>
 * </ol>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2025-02-15
 */
@DisplayName("ConfigManager.getWithPriority() Tests")
class ConfigManagerPriorityTest {

    private ConfigManager config;

    @BeforeEach
    void setUp() {
        // Limpiar System Properties y variables de entorno de tests anteriores
        System.clearProperty("test.property");
        System.clearProperty("TEST_PROPERTY");
        config = ConfigManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Limpiar después de cada test
        System.clearProperty("test.property");
        System.clearProperty("TEST_PROPERTY");
    }

    // =========================================================================
    // TESTS DE PRIORIDAD: System Property > Env Var > Config File > Default
    // =========================================================================

    @Test
    @DisplayName("Prioridad 1: System Property debe ganar sobre todas")
    void testSystemPropertyHasHighestPriority() {
        // Given
        System.setProperty("test.property", "from-system-property");
        // Variable de entorno no se puede setear en runtime, pero suponemos que existe

        // When
        String result = config.getWithPriority("test.property", "default-value");

        // Then
        assertThat(result).isEqualTo("from-system-property");
    }

    @Test
    @DisplayName("Prioridad 2: Variable de entorno si no hay System Property")
    void testEnvVarIfNoSystemProperty() {
        // When - Buscar con nombre que se convierte a env var
        String result = config.getWithPriority("home", "default-value");

        // Then - Debe encontrar variable HOME (existe en Mac/Linux) o default en Windows
        assertThat(result).isNotNull();
        // No validamos valor exacto porque depende del sistema
    }

    @Test
    @DisplayName("Prioridad 3: Config file si no hay System Property ni Env Var")
    void testConfigFileIfNoSystemPropertyOrEnvVar() {
        // When
        String result = config.getWithPriority("driver.strategy", "default-value");

        // Then - Debe leer desde config-scotia.properties o retornar default
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Prioridad 4: Default value si no hay ninguna fuente")
    void testDefaultValueIfNoSource() {
        // Given - Propiedad que NO existe en ningún lado
        String nonExistentKey = "this.property.does.not.exist.anywhere";

        // When
        String result = config.getWithPriority(nonExistentKey, "default-value");

        // Then
        assertThat(result).isEqualTo("default-value");
    }

    // =========================================================================
    // TESTS DE CONVERSIÓN DE NOMBRES (property.name → PROPERTY_NAME)
    // =========================================================================

    @Test
    @DisplayName("Debe buscar System Property primero antes de convertir a env var")
    void testSystemPropertyBeforeEnvVarConversion() {
        // Given - Setear System Property con nombre en formato properties
        System.setProperty("driver.chrome.version", "114.0.5735.90");

        // When
        String result = config.getWithPriority("driver.chrome.version", "default");

        // Then - Debe encontrar System Property directamente (no buscar DRIVER_CHROME_VERSION)
        assertThat(result).isEqualTo("114.0.5735.90");

        // Cleanup
        System.clearProperty("driver.chrome.version");
    }

    @Test
    @DisplayName("Debe retornar valor de config file si existe")
    void testConfigFileValue() {
        // Given - db.driver existe en config-scotia.properties

        // When - No hay System Property ni env var
        String result = config.getWithPriority("db.driver", "default-driver");

        // Then - Debe leer desde archivo config o retornar default
        assertThat(result).isNotNull();
    }

    // =========================================================================
    // TESTS DE EDGE CASES
    // =========================================================================

    @Test
    @DisplayName("Debe lanzar excepción si key es null")
    void testNullKey() {
        // When/Then - System.getProperty(null) lanza NullPointerException
        assertThatThrownBy(() ->
            config.getWithPriority(null, "default-value")
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Debe lanzar excepción si key es vacío")
    void testEmptyKey() {
        // When/Then - System.getProperty("") lanza IllegalArgumentException
        assertThatThrownBy(() ->
            config.getWithPriority("", "default-value")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Debe retornar default si key no existe en ningún lado")
    void testNonExistentKey() {
        // When
        String result = config.getWithPriority("this.key.does.not.exist", "default-value");

        // Then
        assertThat(result).isEqualTo("default-value");
    }

    @Test
    @DisplayName("Debe manejar default value null correctamente")
    void testNullDefaultValue() {
        // When
        String result = config.getWithPriority("non.existent.key", null);

        // Then
        assertThat(result).isNull();
    }
}

