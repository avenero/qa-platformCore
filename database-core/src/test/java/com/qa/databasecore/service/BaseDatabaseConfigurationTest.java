package com.qa.databasecore.service;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para BaseDatabaseConfiguration.
 *
 * <p>Cubre la lógica de configuración sin levantar pools reales:</p>
 * <ul>
 *   <li>URLs y drivers por tipo de BD (oracle, sqlserver)</li>
 *   <li>Valores de pool por defecto son positivos</li>
 *   <li>Fallback a valores por defecto cuando env vars no existen</li>
 * </ul>
 *
 * @since 2.1.0 (TASK-A04)
 */
@DisplayName("BaseDatabaseConfiguration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BaseDatabaseConfigurationTest {

    // =========================================================================
    // Drivers
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("getOracleDriver retorna clase driver Oracle")
    void oracleDriver_isCorrect() {
        assertThat(BaseDatabaseConfiguration.getOracleDriver())
                .isEqualTo("oracle.jdbc.OracleDriver");
    }

    @Test
    @Order(2)
    @DisplayName("getSqlServerDriver retorna clase driver SQL Server")
    void sqlServerDriver_isCorrect() {
        assertThat(BaseDatabaseConfiguration.getSqlServerDriver())
                .isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    // =========================================================================
    // URLs por defecto (fallback cuando no hay env var ni properties)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("getOracleJdbcUrl retorna URL no-vacía con protocolo jdbc:oracle")
    void oracleJdbcUrl_hasOracleProtocol() {
        String url = BaseDatabaseConfiguration.getOracleJdbcUrl();
        assertThat(url).isNotBlank().startsWith("jdbc:oracle");
    }

    @Test
    @Order(4)
    @DisplayName("getSqlServerJdbcUrl retorna URL no-vacía con protocolo jdbc:sqlserver")
    void sqlServerJdbcUrl_hasSqlServerProtocol() {
        String url = BaseDatabaseConfiguration.getSqlServerJdbcUrl();
        assertThat(url).isNotBlank().startsWith("jdbc:sqlserver");
    }

    // =========================================================================
    // Pool por defecto (valores positivos)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("getMaxPoolSize retorna valor positivo")
    void maxPoolSize_isPositive() {
        assertThat(BaseDatabaseConfiguration.getMaxPoolSize()).isGreaterThan(0);
    }

    @Test
    @Order(6)
    @DisplayName("getMinIdleConnections retorna valor no-negativo")
    void minIdleConnections_isNonNegative() {
        assertThat(BaseDatabaseConfiguration.getMinIdleConnections()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(7)
    @DisplayName("maxPoolSize >= minIdleConnections (invariante del pool)")
    void maxPoolSize_greaterOrEqualToMinIdle() {
        assertThat(BaseDatabaseConfiguration.getMaxPoolSize())
                .isGreaterThanOrEqualTo(BaseDatabaseConfiguration.getMinIdleConnections());
    }

    // =========================================================================
    // Usuarios por defecto no-nulos
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("getOracleUser retorna valor no-vacío por defecto")
    void oracleUser_nonEmpty() {
        assertThat(BaseDatabaseConfiguration.getOracleUser()).isNotBlank();
    }

    @Test
    @Order(9)
    @DisplayName("getSqlServerUser retorna valor no-vacío por defecto")
    void sqlServerUser_nonEmpty() {
        assertThat(BaseDatabaseConfiguration.getSqlServerUser()).isNotBlank();
    }
}
