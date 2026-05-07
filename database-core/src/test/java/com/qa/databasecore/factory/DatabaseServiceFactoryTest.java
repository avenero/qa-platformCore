package com.qa.databasecore.factory;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para DatabaseServiceFactory.
 *
 * <p>Cubre la lógica pura de la factory sin conexiones reales:</p>
 * <ul>
 *   <li>Validación de tipos de BD soportados</li>
 *   <li>Validación de parámetros de entrada (null, vacío, inválido)</li>
 *   <li>Timeout por defecto por tipo de BD</li>
 *   <li>isSupportedDatabaseType con aliases</li>
 *   <li>getFactoryInfo devuelve string informativo</li>
 *   <li>getSupportedDatabaseTypes devuelve los 4 tipos</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> Solo se testea lógica interna (excepciones, valores retornados)
 * sin levantar pools ni conexiones reales.</p>
 *
 * @since 2.1.0 (TASK-A04)
 */
@DisplayName("DatabaseServiceFactory")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseServiceFactoryTest {

    // =========================================================================
    // getSupportedDatabaseTypes
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("getSupportedDatabaseTypes devuelve exactamente 4 tipos")
    void supportedTypes_returnsFour() {
        String[] types = DatabaseServiceFactory.getSupportedDatabaseTypes();
        assertThat(types).hasSize(4).contains("oracle", "sqlserver", "postgresql", "mysql");
    }

    // =========================================================================
    // isSupportedDatabaseType
    // =========================================================================

    @ParameterizedTest
    @Order(2)
    @ValueSource(strings = {"oracle", "sqlserver", "postgresql", "mysql", "ORACLE", "SQLSERVER", "MySQL"})
    @DisplayName("isSupportedDatabaseType acepta los tipos soportados (case-insensitive)")
    void isSupportedType_returnsTrue(String dbType) {
        assertThat(DatabaseServiceFactory.isSupportedDatabaseType(dbType)).isTrue();
    }

    @ParameterizedTest
    @Order(3)
    @ValueSource(strings = {"mongodb", "cassandra", "redis", "db2", ""})
    @DisplayName("isSupportedDatabaseType rechaza tipos no soportados y cadena vacía")
    void isSupportedType_returnsFalse_forUnsupported(String dbType) {
        assertThat(DatabaseServiceFactory.isSupportedDatabaseType(dbType)).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("isSupportedDatabaseType rechaza null")
    void isSupportedType_returnsFalse_forNull() {
        assertThat(DatabaseServiceFactory.isSupportedDatabaseType(null)).isFalse();
    }

    // =========================================================================
    // getInstance — validaciones (sin conexión real)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("getInstance lanza IllegalArgumentException para tipo null")
    void getInstance_throwsOnNull() {
        assertThatThrownBy(() -> DatabaseServiceFactory.getInstance(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @Order(6)
    @DisplayName("getInstance lanza IllegalArgumentException para tipo vacío")
    void getInstance_throwsOnEmpty() {
        assertThatThrownBy(() -> DatabaseServiceFactory.getInstance(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Order(7)
    @DisplayName("getInstance lanza IllegalArgumentException para tipo no soportado")
    void getInstance_throwsOnUnsupportedType() {
        assertThatThrownBy(() -> DatabaseServiceFactory.getInstance("mongodb"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mongodb");
    }

    // =========================================================================
    // getInstanceWithTimeout — validaciones (sin conexión real)
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("getInstanceWithTimeout lanza IllegalArgumentException para timeout negativo")
    void getInstanceWithTimeout_throwsOnNegativeTimeout() {
        assertThatThrownBy(() -> DatabaseServiceFactory.getInstanceWithTimeout("oracle", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @Order(9)
    @DisplayName("getInstanceWithTimeout lanza IllegalArgumentException para tipo inválido")
    void getInstanceWithTimeout_throwsOnInvalidType() {
        assertThatThrownBy(() -> DatabaseServiceFactory.getInstanceWithTimeout("invalid", 5000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // getFactoryInfo
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("getFactoryInfo retorna string no-vacío con nombre de implementación")
    void getFactoryInfo_containsImplementationName() {
        String info = DatabaseServiceFactory.getFactoryInfo();
        assertThat(info)
                .isNotBlank()
                .contains("DatabaseServiceFactory")
                .contains("BaseDatabaseService");
    }
}
