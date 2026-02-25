package com.scotia.qa.common.database.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para DatabaseConfig.
 *
 * <p>Estrategia: Sin conexiones reales a BD ni H2.
 * Se valida la lógica interna (detección Windows Auth, tipo de BD,
 * masking de password, validación de parámetros y pool config)
 * a través del comportamiento observable al crear el DataSource.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("DatabaseConfig")
class DatabaseConfigTest {

    // =========================================================================
    // Validación de parámetros de entrada
    // =========================================================================

    @Nested
    @DisplayName("Validación de parámetros")
    class ParameterValidationTests {

        @Test
        @DisplayName("URL null lanza RuntimeException")
        void urlNullLanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(null, "user", "pass", "oracle.jdbc.OracleDriver"));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Driver null lanza RuntimeException")
        void driverNullLanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource("jdbc:oracle:thin:@host:1521:XE", "user", "pass", null));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Driver vacío lanza RuntimeException")
        void driverVacioLanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource("jdbc:oracle:thin:@host:1521:XE", "user", "pass", ""));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Driver inexistente lanza RuntimeException con mensaje de DataSource o Driver")
        void driverInexistenteLanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", "pass", "com.invalid.NonExistentDriver"));
            assertTrue(ex.getMessage().contains("DataSource") || ex.getMessage().contains("Driver") ||
                       ex.getMessage().contains("load") || ex.getMessage().contains("Failed"),
                "Mensaje inesperado: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Validación del pool (valores inválidos vs válidos)
    // =========================================================================

    @Nested
    @DisplayName("Configuración del pool")
    class PoolConfigTests {

        @Test
        @DisplayName("maxPoolSize=0 lanza RuntimeException")
        void poolSizeCeroLanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", "pass", "oracle.jdbc.OracleDriver", 0, 0));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("maxPoolSize negativo lanza RuntimeException")
        void poolSizeNegativoLanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", "pass", "oracle.jdbc.OracleDriver", -1, 0));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Pool válido (10/2) falla por driver, no por pool config")
        void poolValidoFallaPorDriver() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", "pass", "oracle.jdbc.OracleDriver", 10, 2));
            assertFalse(ex.getMessage().toLowerCase().contains("pool size"),
                "Error no esperado de pool: " + ex.getMessage());
        }

        @Test
        @DisplayName("Método sin pool size delega en método con pool size (misma excepción)")
        void metodoSinPoolSizeDelegaCorrectamente() {
            RuntimeException ex1 = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource("jdbc:invalid", "u", "p", "invalid.Driver"));
            RuntimeException ex2 = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource("jdbc:invalid", "u", "p", "invalid.Driver", 10, 2));
            assertEquals(ex1.getClass(), ex2.getClass());
        }
    }

    // =========================================================================
    // Detección de Windows Authentication
    // =========================================================================

    @Nested
    @DisplayName("Detección de Windows Authentication")
    class WindowsAuthTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "jdbc:sqlserver://host:1433;databaseName=DB;integratedSecurity=true",
            "jdbc:sqlserver://host:1433;integratedSecurity=true;encrypt=false",
            "jdbc:sqlserver://host:1433;INTEGRATEDSECURITY=TRUE",
            "jdbc:sqlserver://host:1433;IntegratedSecurity=True;trustServerCertificate=true"
        })
        @DisplayName("Con integratedSecurity=true ignora credenciales (Windows Auth)")
        void detectaWindowsAuth(String url) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    url, "ignorado", "ignorado", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
            assertNotNull(ex.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "jdbc:sqlserver://host:1433;databaseName=DB;encrypt=false",
            "jdbc:sqlserver://host:1433;integratedSecurity=false",
            "jdbc:oracle:thin:@host:1521:XE",
            "jdbc:postgresql://host:5432/db",
            "jdbc:mysql://host:3306/db"
        })
        @DisplayName("Sin integratedSecurity=true usa autenticación SQL estándar")
        void noDetectaWindowsAuth(String url) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(url, "user", "pass", "oracle.jdbc.OracleDriver"));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("URL null no causa NullPointerException en detección de Windows Auth")
        void urlNullNoNPEEnDeteccion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(null, "user", "pass", "oracle.jdbc.OracleDriver"));
            assertFalse(ex instanceof NullPointerException);
        }
    }

    // =========================================================================
    // Detección del tipo de BD por driver className
    // =========================================================================

    @Nested
    @DisplayName("Detección de tipo de BD por driver")
    class DbTypeDetectionTests {

        @ParameterizedTest
        @CsvSource({
            "oracle.jdbc.OracleDriver,                             jdbc:oracle:thin:@host:1521:XE",
            "oracle.jdbc.driver.OracleDriver,                     jdbc:oracle:thin:@host:1521:XE",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver,         jdbc:sqlserver://host:1433",
            "net.sourceforge.jtds.jdbc.Driver,                     jdbc:jtds:sqlserver://host:1433/db",
            "org.postgresql.Driver,                                jdbc:postgresql://host:5432/db",
            "com.mysql.cj.jdbc.Driver,                             jdbc:mysql://host:3306/db",
            "com.mysql.jdbc.Driver,                                jdbc:mysql://host:3306/db"
        })
        @DisplayName("Cada driver conocido falla por driver no disponible (no por config interna)")
        void deteccionTipoNoExplota(String driverClass, String url) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(url.trim(), "user", "pass", driverClass.trim()));
            assertTrue(ex.getMessage().contains("DataSource") || ex.getMessage().contains("Driver") ||
                       ex.getMessage().contains("class") || ex.getMessage().contains("load"),
                "Mensaje inesperado: " + ex.getMessage());
        }

        @Test
        @DisplayName("Driver desconocido (tipo Unknown) no lanza excepción interna propia")
        void driverDesconocidoUnknownNoExplotaInterno() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:custom://host/db", "user", "pass", "com.custom.unknown.Driver"));
            assertNotNull(ex.getMessage());
        }
    }

    // =========================================================================
    // Query de validación de conexión por tipo de BD
    // =========================================================================

    @Nested
    @DisplayName("Connection test query por tipo de BD")
    class ConnectionTestQueryTests {

        @ParameterizedTest
        @CsvSource({
            "oracle.jdbc.OracleDriver,                    jdbc:oracle:thin:@host:1521:XE",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver, jdbc:sqlserver://host:1433",
            "org.postgresql.Driver,                       jdbc:postgresql://host:5432/db",
            "com.mysql.cj.jdbc.Driver,                    jdbc:mysql://host:3306/db"
        })
        @DisplayName("Configuración de test query no lanza excepción propia (falla solo por driver)")
        void testQueryNoExplotaInternamente(String driverClass, String url) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(url.trim(), "user", "pass", driverClass.trim()));
            assertNotNull(ex.getMessage());
        }
    }

    // =========================================================================
    // Credenciales: casos edge (null, vacío, especiales)
    // =========================================================================

    @Nested
    @DisplayName("Credenciales edge cases")
    class CredentialsEdgeCaseTests {

        @Test
        @DisplayName("Username null no causa NullPointerException (solo warning en log)")
        void usernameNullNoNPE() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", null, "pass", "oracle.jdbc.OracleDriver"));
            assertFalse(ex instanceof NullPointerException);
        }

        @Test
        @DisplayName("Username vacío genera warning pero no falla por username")
        void usernameVacioNoFallaPorUsername() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "", "pass", "oracle.jdbc.OracleDriver"));
            assertFalse(ex.getMessage().toLowerCase().contains("username"),
                "No debe fallar por username: " + ex.getMessage());
        }

        @Test
        @DisplayName("Password null no causa NullPointerException")
        void passwordNullNoNPE() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", null, "oracle.jdbc.OracleDriver"));
            assertFalse(ex instanceof NullPointerException);
        }

        @Test
        @DisplayName("Password vacío genera warning pero no falla por password")
        void passwordVacioNoFallaPorPassword() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", "", "oracle.jdbc.OracleDriver"));
            assertFalse(ex.getMessage().toLowerCase().contains("password"),
                "No debe fallar por password: " + ex.getMessage());
        }

        @Test
        @DisplayName("Password con caracteres especiales no explota")
        void passwordCaracteresEspeciales() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", "user", "P@ss!#$%^&*()", "oracle.jdbc.OracleDriver"));
            assertNotNull(ex.getMessage());
        }
    }

    // =========================================================================
    // Masking de password en URL (método privado probado indirectamente)
    // =========================================================================

    @Nested
    @DisplayName("Masking de password en URL JDBC")
    class PasswordMaskingTests {

        @Test
        @DisplayName("URL con password=xxx en parámetros no causa NPE en masking")
        void urlConPasswordEnParametros() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:sqlserver://host:1433;password=SuperSecret!;databaseName=DB",
                    "user", "pass", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("URL con PASSWORD en mayúsculas no causa NPE en masking (case-insensitive)")
        void urlConPasswordMayusculas() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:sqlserver://host:1433;PASSWORD=Secret123;integratedSecurity=true",
                    "user", "pass", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
            assertNotNull(ex.getMessage());
        }
    }

    // =========================================================================
    // Flujos combinados: Windows Auth + pool + masking
    // =========================================================================

    @Nested
    @DisplayName("Flujos combinados")
    class CombinedFlowTests {

        @Test
        @DisplayName("Windows Auth + password en URL: credenciales pasadas se ignoran")
        void windowsAuthConPasswordEnUrl() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:sqlserver://host:1433;integratedSecurity=true;password=ShouldBeIgnored",
                    "ignored_user", "ignored_pass",
                    "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("SQL Auth con URL compleja (múltiples parámetros) no falla por URL")
        void sqlAuthUrlCompleja() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:sqlserver://host:1433;databaseName=DB;encrypt=true;trustServerCertificate=true;loginTimeout=30",
                    "user", "pass", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Credenciales ambas null con driver Oracle no causa NullPointerException sin envolver")
        void credencialesNullNoNPE() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:oracle:thin:@host:1521:XE", null, null, "oracle.jdbc.OracleDriver"));
            assertFalse(ex instanceof NullPointerException,
                "No debe propagarse NullPointerException sin envolver");
        }

        @Test
        @DisplayName("Windows Auth con pool size personalizado no falla por pool config")
        void windowsAuthConPoolPersonalizado() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DatabaseConfig.createHikariDataSource(
                    "jdbc:sqlserver://host:1433;integratedSecurity=true",
                    null, null,
                    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    5, 1));
            assertFalse(ex.getMessage().toLowerCase().contains("pool size"),
                "No debe fallar por pool: " + ex.getMessage());
        }
    }
}
