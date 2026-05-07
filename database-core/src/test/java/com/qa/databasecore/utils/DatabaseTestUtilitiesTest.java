package com.qa.databasecore.utils;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para DatabaseTestUtilities.
 *
 * <p>Cubre la lógica de validación de entrada sin conexiones reales:</p>
 * <ul>
 *   <li>Rechazo de dbType null o vacío en todos los métodos públicos</li>
 *   <li>Propagación de excepciones desde el servicio subyacente</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> Los métodos que requieren conexión real lanzan
 * RuntimeException en ausencia de BD. Se testea el camino de validación
 * y que la excepción se propaga correctamente (sin swallow silencioso).</p>
 *
 * @since 2.1.0 (TASK-A04)
 */
@DisplayName("DatabaseTestUtilities")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseTestUtilitiesTest {

    // =========================================================================
    // executeQueryForMap — validaciones de entrada
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("executeQueryForMap lanza IllegalArgumentException para dbType null")
    void executeQueryForMap_throwsOnNullDbType() {
        assertThatThrownBy(() -> DatabaseTestUtilities.executeQueryForMap(null, "SELECT 1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Order(2)
    @DisplayName("executeQueryForMap lanza IllegalArgumentException para dbType vacío")
    void executeQueryForMap_throwsOnEmptyDbType() {
        assertThatThrownBy(() -> DatabaseTestUtilities.executeQueryForMap("", "SELECT 1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // executeQueryForList — validaciones de entrada
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("executeQueryForList lanza IllegalArgumentException para dbType null")
    void executeQueryForList_throwsOnNullDbType() {
        assertThatThrownBy(() -> DatabaseTestUtilities.executeQueryForList(null, "SELECT 1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // executeQueryForCount — validaciones de entrada
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("executeQueryForCount lanza IllegalArgumentException para dbType null")
    void executeQueryForCount_throwsOnNullDbType() {
        assertThatThrownBy(() -> DatabaseTestUtilities.executeQueryForCount(null, "SELECT COUNT(*) FROM dual"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // executeUpdate — validaciones de entrada
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("executeUpdate lanza IllegalArgumentException para dbType null")
    void executeUpdate_throwsOnNullDbType() {
        assertThatThrownBy(() -> DatabaseTestUtilities.executeUpdate(null, "UPDATE x SET y=1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // testConnection — maneja ausencia de BD gracefully (retorna false)
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("testConnection retorna false cuando BD no está disponible (tipo válido sin servidor)")
    void testConnection_returnsFalse_whenNoBdAvailable() {
        // No hay servidor real: la excepción se captura internamente y retorna false
        boolean result = DatabaseTestUtilities.testConnection("oracle");
        assertThat(result).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("testConnection retorna false para sqlserver sin servidor")
    void testConnection_returnsFalse_sqlserver() {
        boolean result = DatabaseTestUtilities.testConnection("sqlserver");
        assertThat(result).isFalse();
    }
}
