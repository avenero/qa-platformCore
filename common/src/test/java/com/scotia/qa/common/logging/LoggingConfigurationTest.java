package com.scotia.qa.common.logging;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para LoggingConfiguration - Configurador de logging del framework.
 *
 * <p><b>Clase P0:</b> Configuración central de logging
 * <p><b>Cobertura objetivo:</b> 70%
 * <p><b>Total tests:</b> 15
 *
 * <p><b>Validaciones:</b>
 * <ul>
 *   <li>Configuración por defecto</li>
 *   <li>Configuración personalizada</li>
 *   <li>Estado de configuración</li>
 *   <li>Recuperación de configuración actual</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("LoggingConfiguration Tests - Configurador de Logging")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoggingConfigurationTest {

    // =========================================================================
    // DEFAULT CONFIGURATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Default Configuration Tests")
    @Order(1)
    class DefaultConfigurationTests {

        @Test
        @DisplayName("Debe configurar logging por defecto para API")
        void testConfigureDefaultAPI() {
            // Given
            String framework = "API";

            // When/Then - No debe lanzar excepciones
            assertThatCode(() -> LoggingConfiguration.configureDefault(framework))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe configurar logging por defecto para WEB")
        void testConfigureDefaultWEB() {
            // Given
            String framework = "WEB";

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configureDefault(framework))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe configurar logging por defecto para MOBILE")
        void testConfigureDefaultMOBILE() {
            // Given
            String framework = "MOBILE";

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configureDefault(framework))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar framework con minúsculas")
        void testConfigureDefaultLowercase() {
            // Given
            String framework = "api";

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configureDefault(framework))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe marcar como configurado después de configuración por defecto")
        void testIsConfiguredAfterDefault() {
            // Given
            String framework = "API";

            // When
            LoggingConfiguration.configureDefault(framework);

            // Then
            assertThat(LoggingConfiguration.isConfigured()).isTrue();
        }
    }

    // =========================================================================
    // CUSTOM CONFIGURATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Custom Configuration Tests")
    @Order(2)
    class CustomConfigurationTests {

        @Test
        @DisplayName("Debe configurar con configuración personalizada básica")
        void testConfigureCustomBasic() {
            // Given
            LoggingConfiguration.LoggingConfig config = LoggingConfiguration.LoggingConfig.builder()
                .framework("API")
                .baseLogDirectory("custom-logs")
                .enableConsoleLogging(true)
                .enableFileLogging(false)
                .logLevel("DEBUG")
                .build();

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configure(config))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe configurar con todas las opciones habilitadas")
        void testConfigureAllEnabled() {
            // Given
            LoggingConfiguration.LoggingConfig config = LoggingConfiguration.LoggingConfig.builder()
                .framework("WEB")
                .baseLogDirectory("logs/web")
                .enableConsoleLogging(true)
                .enableFileLogging(true)
                .enableStructuredLogging(true)
                .enableEvidenceLogging(true)
                .logLevel("INFO")
                .maxFileSize("5MB")
                .maxBackupFiles(5)
                .build();

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configure(config))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe configurar con opciones mínimas")
        void testConfigureMinimalOptions() {
            // Given
            LoggingConfiguration.LoggingConfig config = LoggingConfiguration.LoggingConfig.builder()
                .framework("MOBILE")
                .baseLogDirectory("logs")
                .enableConsoleLogging(false)
                .enableFileLogging(false)
                .logLevel("ERROR")
                .build();

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configure(config))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe marcar como configurado después de configuración personalizada")
        void testIsConfiguredAfterCustom() {
            // Given
            LoggingConfiguration.LoggingConfig config = LoggingConfiguration.LoggingConfig.builder()
                .framework("API")
                .baseLogDirectory("logs")
                .logLevel("INFO")
                .build();

            // When
            LoggingConfiguration.configure(config);

            // Then
            assertThat(LoggingConfiguration.isConfigured()).isTrue();
        }
    }

    // =========================================================================
    // CONFIGURATION STATE TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Configuration State Tests")
    @Order(3)
    class ConfigurationStateTests {

        @Test
        @DisplayName("isConfigured debe retornar true después de configuración")
        void testIsConfiguredTrue() {
            // Given
            LoggingConfiguration.configureDefault("API");

            // When
            boolean configured = LoggingConfiguration.isConfigured();

            // Then
            assertThat(configured).isTrue();
        }

        @Test
        @DisplayName("Debe obtener configuración actual como Map")
        void testGetCurrentConfig() {
            // Given
            String framework = "API";
            LoggingConfiguration.configureDefault(framework);

            // When
            Map<String, Object> config = LoggingConfiguration.getCurrentConfig();

            // Then
            assertThat(config).isNotNull();
            assertThat(config).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("getCurrentConfig debe retornar copia, no referencia")
        void testGetCurrentConfigReturnsCopy() {
            // Given
            LoggingConfiguration.configureDefault("API");

            // When
            Map<String, Object> config1 = LoggingConfiguration.getCurrentConfig();
            Map<String, Object> config2 = LoggingConfiguration.getCurrentConfig();

            // Then - Deben ser diferentes instancias
            assertThat(config1).isNotSameAs(config2);
        }

        @Test
        @DisplayName("Modificar config retornado no debe afectar configuración interna")
        void testModifyingReturnedConfigDoesNotAffectInternal() {
            // Given
            LoggingConfiguration.configureDefault("API");
            Map<String, Object> config = LoggingConfiguration.getCurrentConfig();

            // When
            config.put("test_key", "test_value");

            // Then - La siguiente llamada no debe contener nuestra modificación
            Map<String, Object> freshConfig = LoggingConfiguration.getCurrentConfig();
            assertThat(freshConfig).doesNotContainKey("test_key");
        }
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Nested
    @DisplayName("4. Edge Cases")
    @Order(4)
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar reconfiguración múltiple")
        void testMultipleConfigurations() {
            // When/Then - No debe fallar al reconfigurar
            assertThatCode(() -> {
                LoggingConfiguration.configureDefault("API");
                LoggingConfiguration.configureDefault("WEB");
                LoggingConfiguration.configureDefault("MOBILE");
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar framework con caracteres especiales")
        void testFrameworkWithSpecialCharacters() {
            // Given
            String framework = "API-REST/v1";

            // When/Then
            assertThatCode(() -> LoggingConfiguration.configureDefault(framework))
                .doesNotThrowAnyException();
        }
    }
}

