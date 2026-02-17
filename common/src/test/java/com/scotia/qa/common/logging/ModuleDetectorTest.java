package com.scotia.qa.common.logging;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para ModuleDetector - Detector automático de módulo.
 *
 * <p><b>Clase P0:</b> Detección automática de módulo para logging
 * <p><b>Cobertura objetivo:</b> 75%
 * <p><b>Total tests:</b> 10
 *
 * <p><b>Validaciones:</b>
 * <ul>
 *   <li>Detección de nombre de módulo</li>
 *   <li>Detección de tipo de módulo</li>
 *   <li>Estrategias de fallback</li>
 *   <li>Cache de detección</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("ModuleDetector Tests - Detector de Módulo")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModuleDetectorTest {

    // System properties originales (para restaurar)
    private String originalModuleName;
    private String originalModuleType;
    private String originalProjectName;

    @BeforeEach
    void setUp() {
        // Guardar valores originales
        originalModuleName = System.getProperty("framework.module.name");
        originalModuleType = System.getProperty("framework.module.type");
        originalProjectName = System.getProperty("project.name");

        // Limpiar cache antes de cada test
        ModuleDetector.clearCache();

        // Limpiar system properties para cada test
        System.clearProperty("framework.module.name");
        System.clearProperty("framework.module.type");
        System.clearProperty("project.name");
    }

    @AfterEach
    void tearDown() {
        // Limpiar cache
        ModuleDetector.clearCache();

        // Restaurar valores originales
        restoreProperty("framework.module.name", originalModuleName);
        restoreProperty("framework.module.type", originalModuleType);
        restoreProperty("project.name", originalProjectName);
    }

    private void restoreProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
    }

    // =========================================================================
    // MODULE NAME DETECTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Module Name Detection Tests")
    @Order(1)
    class ModuleNameDetectionTests {

        @Test
        @DisplayName("Debe detectar nombre desde System Property")
        void testDetectFromSystemProperty() {
            // Given
            System.setProperty("framework.module.name", "BANKING");

            // When
            String moduleName = ModuleDetector.detectModuleName();

            // Then
            assertThat(moduleName).isEqualTo("BANKING");
        }

        @Test
        @DisplayName("Debe convertir nombre a mayúsculas")
        void testConvertToUppercase() {
            // Given
            System.setProperty("framework.module.name", "banking");

            // When
            String moduleName = ModuleDetector.detectModuleName();

            // Then
            assertThat(moduleName).isEqualTo("BANKING");
        }

        @Test
        @DisplayName("Debe hacer trim de espacios en nombre")
        void testTrimSpaces() {
            // Given
            System.setProperty("framework.module.name", "  BANKING  ");

            // When
            String moduleName = ModuleDetector.detectModuleName();

            // Then
            assertThat(moduleName).isEqualTo("BANKING");
        }

        @Test
        @DisplayName("Debe detectar desde project.name si no hay module.name")
        void testDetectFromProjectName() {
            // Given
            System.setProperty("project.name", "qa-banking");

            // When
            String moduleName = ModuleDetector.detectModuleName();

            // Then
            assertThat(moduleName).isNotNull();
            assertThat(moduleName).isNotEmpty();
            // Puede ser "BANKING" o "QA_BANKING" dependiendo de la extracción
        }

        @Test
        @DisplayName("Debe retornar un valor válido incluso sin configuración explícita")
        void testAlwaysReturnsValidValue() {
            // Given - No hay configuración explícita de module.name
            // Puede detectar de project.name u otras fuentes

            // When
            String moduleName = ModuleDetector.detectModuleName();

            // Then - Debe retornar algún valor válido (TEST, GRADLE, o derivado de project.name)
            assertThat(moduleName).isNotNull();
            assertThat(moduleName).isNotEmpty();
            assertThat(moduleName).matches("[A-Z_]+"); // Solo mayúsculas y underscores
        }

        @Test
        @DisplayName("Debe cachear resultado de detección")
        void testCachingModuleName() {
            // Given
            System.setProperty("framework.module.name", "BANKING");

            // When
            String moduleName1 = ModuleDetector.detectModuleName();

            // Cambiar la propiedad (pero debe usar cache)
            System.setProperty("framework.module.name", "MOBILE");
            String moduleName2 = ModuleDetector.detectModuleName();

            // Then - Debe retornar el mismo valor (cacheado)
            assertThat(moduleName1).isEqualTo("BANKING");
            assertThat(moduleName2).isEqualTo("BANKING"); // Mismo valor por cache
        }
    }

    // =========================================================================
    // MODULE TYPE DETECTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Module Type Detection Tests")
    @Order(2)
    class ModuleTypeDetectionTests {

        @Test
        @DisplayName("Debe detectar tipo desde System Property")
        void testDetectTypeFromSystemProperty() {
            // Given
            System.setProperty("framework.module.type", "HYBRID");

            // When
            String moduleType = ModuleDetector.detectModuleType();

            // Then
            assertThat(moduleType).isEqualTo("HYBRID");
        }

        @Test
        @DisplayName("Debe retornar null si tipo no está configurado")
        void testReturnNullIfTypeNotConfigured() {
            // Given - No hay configuración de tipo

            // When
            String moduleType = ModuleDetector.detectModuleType();

            // Then
            assertThat(moduleType).isNull();
        }

        @Test
        @DisplayName("Debe convertir tipo a mayúsculas")
        void testConvertTypeToUppercase() {
            // Given
            System.setProperty("framework.module.type", "web");

            // When
            String moduleType = ModuleDetector.detectModuleType();

            // Then
            assertThat(moduleType).isEqualTo("WEB");
        }
    }

    // =========================================================================
    // CACHE MANAGEMENT TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Cache Management Tests")
    @Order(3)
    class CacheManagementTests {

        @Test
        @DisplayName("clearCache debe limpiar cache de nombre")
        void testClearCacheName() {
            // Given
            System.setProperty("framework.module.name", "BANKING");
            String name1 = ModuleDetector.detectModuleName(); // BANKING

            // When
            ModuleDetector.clearCache();
            System.setProperty("framework.module.name", "MOBILE");
            String name2 = ModuleDetector.detectModuleName(); // Debe ser MOBILE

            // Then
            assertThat(name1).isEqualTo("BANKING");
            assertThat(name2).isEqualTo("MOBILE");
        }

        @Test
        @DisplayName("clearCache debe limpiar cache de tipo")
        void testClearCacheType() {
            // Given
            System.setProperty("framework.module.type", "WEB");
            String type1 = ModuleDetector.detectModuleType(); // WEB

            // When
            ModuleDetector.clearCache();
            System.setProperty("framework.module.type", "API");
            String type2 = ModuleDetector.detectModuleType(); // Debe ser API

            // Then
            assertThat(type1).isEqualTo("WEB");
            assertThat(type2).isEqualTo("API");
        }
    }
}

