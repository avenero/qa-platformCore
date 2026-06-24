package com.qa.databasecore.factory;

import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.databasecore.config.JdbcDriverAllowlist;
import com.qa.databasecore.connector.DatabaseConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para DbConnectorFactory.
 *
 * <p>Cubre la lógica pura de la factory sin conexiones reales:</p>
 * <ul>
 *   <li>Resolución de tipo de BD → clase driver JDBC</li>
 *   <li>Detección de driver desde URL JDBC</li>
 *   <li>Validaciones de parámetros de entrada</li>
 *   <li>Gestión de cache (connectAndCache / disconnect / disconnectAll)</li>
 *   <li>Aliases de tipos soportados</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> Se testea exclusivamente la lógica interna observable
 * (excepciones, tipos retornados, estado del cache) sin levantar pools reales.</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("DbConnectorFactory")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DbConnectorFactoryTest {

    @AfterEach
    void tearDown() {
        // Limpiar cache entre tests
        DbConnectorFactory.disconnectAll();
    }

    // =========================================================================
    // getConnector() - validación de tipo
    // =========================================================================

    @Nested
    @DisplayName("getConnector() - validación de tipo")
    @Order(1)
    class GetConnectorValidationTests {

        @Test
        @DisplayName("Lanza excepción cuando dbType es null")
        void nullDbTypeLanzaExcepcion() {
            assertThatThrownBy(() -> DbConnectorFactory.getConnector(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Lanza excepción cuando dbType es vacío")
        void dbTypeVacioLanzaExcepcion() {
            assertThatThrownBy(() -> DbConnectorFactory.getConnector(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Lanza excepción cuando dbType es solo espacios")
        void dbTypeSoloEspaciosLanzaExcepcion() {
            assertThatThrownBy(() -> DbConnectorFactory.getConnector("   "))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"mongodb", "cassandra", "redis", "h2", "sqlite", "tipoInvalido"})
        @DisplayName("Lanza excepción para tipos de BD no soportados")
        void tipoNoSoportadoLanzaExcepcion(String tipoInvalido) {
            assertThatThrownBy(() -> DbConnectorFactory.getConnector(tipoInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado");
        }

        @ParameterizedTest
        @ValueSource(strings = {"oracle", "postgresql", "postgres", "mysql", "sqlserver", "sql-server", "mssql"})
        @DisplayName("Tipos soportados no lanzan excepción por tipo desconocido")
        void tiposSoportadosNoLanzanTipoDesconocido(String tipo) {
            // Los constructores de cada conector intentan leer config de BD
            // y fallan con "xxx.db.url no configurada" — eso es correcto y esperado.
            // Lo que NO debe pasar es que fallen con "tipo no soportado".
            try {
                DbConnectorFactory.getConnector(tipo.trim());
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage())
                    .as("No debe fallar con 'tipo no soportado' para: " + tipo)
                    .doesNotContain("no soportado");
            } catch (Exception ignored) {
                // Cualquier otra excepción (falta de config, etc.) es aceptable
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"ORACLE", "Oracle", "PostgreSQL", "MYSQL", "SQLServer", "MSSQL"})
        @DisplayName("Tipos soportados en mayúsculas/mixto no lanzan excepción por tipo desconocido")
        void tiposCaseInsensitiveNoLanzanTipoDesconocido(String tipo) {
            try {
                DbConnectorFactory.getConnector(tipo);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage())
                    .as("No debe fallar con 'tipo no soportado' para: " + tipo)
                    .doesNotContain("no soportado");
            } catch (Exception ignored) {
                // Falta de config es aceptable
            }
        }
    }

    // =========================================================================
    // detectDriverFromUrl() - via createFromConfig con URL inválida
    // =========================================================================

    @Nested
    @DisplayName("Detección de driver por URL JDBC")
    @Order(2)
    class DeteccionDriverPorUrlTests {

        @Test
        @DisplayName("createFromConfig lanza excepción si faltan propiedades requeridas de BD")
        void createFromConfigSinConfigLanzaExcepcion() {
            // ConfigManager puede tener alguna propiedad precargada de los archivos del proyecto.
            // Independientemente, sin una BD real disponible debe lanzar alguna excepción.
            // Lo importante es que falle con IllegalArgumentException o RuntimeException con
            // mensaje descriptivo, nunca con NullPointerException.
            assertThatThrownBy(() -> DbConnectorFactory.createFromConfig())
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @CsvSource({
            "jdbc:oracle:thin:@//host:1521/SVC",
            "jdbc:sqlserver://host:1433;databaseName=DB",
            "jdbc:jtds:sqlserver://host:1433/DB",
            "jdbc:postgresql://host:5432/db",
            "jdbc:mysql://host:3306/db"
        })
        @DisplayName("createFromConfig detecta driver correcto según URL JDBC (no falla por tipo desconocido)")
        void detectaDriverCorrectoSegunUrl(String jdbcUrl) {
            String originalUrl = System.getProperty("db.url");
            String originalDriver = System.getProperty("db.driver");
            String originalUser = System.getProperty("db.username");
            String originalPass = System.getProperty("db.password");

            try {
                System.setProperty("db.url", jdbcUrl.trim());
                System.clearProperty("db.driver"); // forzar auto-detección
                System.setProperty("db.username", "testuser");
                System.setProperty("db.password", "testpass");

                // Intentar crear — fallará al conectar, pero NO por driver desconocido
                try {
                    DbConnectorFactory.createFromConfig();
                } catch (Exception e) {
                    assertThat(e.getMessage())
                        .doesNotContain("No se pudo detectar el tipo de base de datos");
                }
            } finally {
                restoreProperty("db.url", originalUrl);
                restoreProperty("db.driver", originalDriver);
                restoreProperty("db.username", originalUser);
                restoreProperty("db.password", originalPass);
            }
        }

        @Test
        @DisplayName("createFromConfig lanza excepción para URL JDBC con tipo desconocido")
        void urlJdbcDesconocidaLanzaExcepcion() {
            String originalUrl = System.getProperty("db.url");
            String originalDriver = System.getProperty("db.driver");
            String originalUser = System.getProperty("db.username");
            String originalPass = System.getProperty("db.password");

            try {
                // System Properties tienen prioridad sobre el ConfigManager
                System.setProperty("db.url", "jdbc:unknowndb://host:9999/db");
                System.clearProperty("db.driver");
                System.setProperty("db.username", "user");
                System.setProperty("db.password", "pass");

                assertThatThrownBy(() -> DbConnectorFactory.createFromConfig())
                    .isInstanceOf(Exception.class)
                    .isNotInstanceOf(NullPointerException.class);
            } finally {
                restoreProperty("db.url", originalUrl);
                restoreProperty("db.driver", originalDriver);
                restoreProperty("db.username", originalUser);
                restoreProperty("db.password", originalPass);
            }
        }
    }

    // =========================================================================
    // create() - parámetros explícitos
    // =========================================================================

    @Nested
    @DisplayName("create() - parámetros explícitos")
    @Order(3)
    class CreateExplicitoTests {

        @Test
        @DisplayName("create() con parámetros válidos retorna conector no null")
        void createConParametrosValidosRetornaConector() {
            // Puede fallar al intentar conectar pero debe retornar un objeto
            try {
                var conector = DbConnectorFactory.create(
                    "jdbc:oracle:thin:@//localhost:1521/TESTDB",
                    "user", "pass",
                    "oracle.jdbc.OracleDriver"
                );
                // Si llega aquí, el conector fue creado (no conectado aún)
                assertThat(conector).isNotNull();
            } catch (Exception e) {
                // Esperado - no hay BD real. Lo importante es que no sea NullPointerException
                assertThat(e).isNotInstanceOf(NullPointerException.class);
            }
        }

        @Test
        @DisplayName("create() con pool size personalizado no lanza NullPointerException")
        void createConPoolSizePersonalizado() {
            try {
                DbConnectorFactory.create(
                    "jdbc:postgresql://localhost:5432/testdb",
                    "user", "pass",
                    "org.postgresql.Driver",
                    5
                );
            } catch (Exception e) {
                assertThat(e).isNotInstanceOf(NullPointerException.class);
            }
        }
    }

    // =========================================================================
    // connectAndCache() - gestión de cache
    // =========================================================================

    @Nested
    @DisplayName("connectAndCache() - gestión de cache")
    @Order(4)
    class CacheTests {

        @Test
        @DisplayName("connectAndCache() lanza excepción cuando dbType es null")
        void connectAndCacheNullLanzaExcepcion() {
            assertThatThrownBy(() -> DbConnectorFactory.connectAndCache(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("connectAndCache() lanza excepción cuando dbType es vacío")
        void connectAndCacheVacioLanzaExcepcion() {
            assertThatThrownBy(() -> DbConnectorFactory.connectAndCache(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("connectAndCache() lanza excepción cuando no hay configuración de BD")
        void connectAndCacheSinConfigLanzaExcepcion() {
            // Sin configurar oracle.db.url en properties debe fallar con mensaje útil
            assertThatThrownBy(() -> DbConnectorFactory.connectAndCache("oracle"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("oracle.db.url");
        }

        @Test
        @DisplayName("getCachedConnector() retorna null cuando no hay conector cacheado")
        void getCachedConnectorSinCacheRetornaNull() {
            assertThat(DbConnectorFactory.getCachedConnector("oracle")).isNull();
        }

        @Test
        @DisplayName("getCachedConnector() retorna null para dbType null")
        void getCachedConnectorNullRetornaNull() {
            assertThat(DbConnectorFactory.getCachedConnector(null)).isNull();
        }

        @Test
        @DisplayName("getCachedConnector() retorna null para dbType vacío")
        void getCachedConnectorVacioRetornaNull() {
            assertThat(DbConnectorFactory.getCachedConnector("")).isNull();
        }

        @Test
        @DisplayName("disconnect() con dbType inexistente no lanza excepción")
        void disconnectInexistenteNoLanzaExcepcion() {
            assertThatCode(() -> DbConnectorFactory.disconnect("oracle"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("disconnect() con null no lanza excepción")
        void disconnectNullNoLanzaExcepcion() {
            assertThatCode(() -> DbConnectorFactory.disconnect(null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("disconnectAll() con cache vacío no lanza excepción")
        void disconnectAllSinConectoresNoLanzaExcepcion() {
            assertThatCode(() -> DbConnectorFactory.disconnectAll())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("disconnectAll() múltiples veces no lanza excepción")
        void disconnectAllMultiplesVecesNoLanzaExcepcion() {
            assertThatCode(() -> {
                DbConnectorFactory.disconnectAll();
                DbConnectorFactory.disconnectAll();
                DbConnectorFactory.disconnectAll();
            }).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Constructores específicos por tipo
    // =========================================================================

    @Nested
    @DisplayName("Constructores específicos por tipo")
    @Order(5)
    class ConstructoresEspecificosTests {

        @Test
        @DisplayName("getOracleConnector() retorna conector no null")
        void oracleConectorNoNull() {
            try {
                var c = DbConnectorFactory.getOracleConnector(
                    "jdbc:oracle:thin:@//localhost:1521/SVC", "user", "pass");
                assertThat(c).isNotNull();
            } catch (Exception e) {
                assertThat(e).isNotInstanceOf(NullPointerException.class);
            }
        }

        @Test
        @DisplayName("getPostgreSQLConnector() retorna conector no null")
        void postgresConectorNoNull() {
            try {
                var c = DbConnectorFactory.getPostgreSQLConnector(
                    "jdbc:postgresql://localhost:5432/db", "user", "pass");
                assertThat(c).isNotNull();
            } catch (Exception e) {
                assertThat(e).isNotInstanceOf(NullPointerException.class);
            }
        }

        @Test
        @DisplayName("getMySQLConnector() retorna conector no null")
        void mysqlConectorNoNull() {
            try {
                var c = DbConnectorFactory.getMySQLConnector(
                    "jdbc:mysql://localhost:3306/db", "user", "pass");
                assertThat(c).isNotNull();
            } catch (Exception e) {
                assertThat(e).isNotInstanceOf(NullPointerException.class);
            }
        }

        @Test
        @DisplayName("getSQLServerConnector() retorna conector no null")
        void sqlServerConectorNoNull() {
            try {
                var c = DbConnectorFactory.getSQLServerConnector(
                    "jdbc:sqlserver://localhost:1433;databaseName=DB", "user", "pass");
                assertThat(c).isNotNull();
            } catch (Exception e) {
                assertThat(e).isNotInstanceOf(NullPointerException.class);
            }
        }
    }

    // =========================================================================
    // Allowlist de driver JDBC (W2-M2-DB-DRIVER) — path db.* / createFromConfig()
    // =========================================================================

    @Nested
    @DisplayName("Allowlist de driver JDBC (db.driver)")
    @Order(6)
    class DriverAllowlistTests {

        // createFromConfig() resuelve el driver vía el TypedConfig DatabaseConfig, que el
        // ConfigLoader cachea por clase. Estos tests fuerzan ConfigLoaderHolder.reset() antes
        // (para que el loader nuevo lea las System Properties recién seteadas) y después (para
        // descartar el DatabaseConfig que cachean y no contaminar otros tests del mismo JVM).

        @Test
        @DisplayName("createFromConfig rechaza un db.driver fuera de la allowlist con MSG_DRIVER_NOT_ALLOWED")
        void createFromConfigDriverNoPermitidoEsRechazado() {
            String originalUrl = System.getProperty("db.url");
            String originalDriver = System.getProperty("db.driver");
            String originalUser = System.getProperty("db.username");
            String originalPass = System.getProperty("db.password");

            try {
                System.setProperty("db.url", "jdbc:h2:mem:allowlist_evil_" + System.nanoTime());
                System.setProperty("db.driver", "com.acme.EvilDriver"); // input-driven, no permitido
                System.setProperty("db.username", "sa");
                System.setProperty("db.password", "");
                ConfigLoaderHolder.reset();

                assertThatThrownBy(DbConnectorFactory::createFromConfig)
                    .isInstanceOf(SecurityException.class)
                    .hasMessage(JdbcDriverAllowlist.MSG_DRIVER_NOT_ALLOWED);
            } finally {
                restoreProperty("db.url", originalUrl);
                restoreProperty("db.driver", originalDriver);
                restoreProperty("db.username", originalUser);
                restoreProperty("db.password", originalPass);
                ConfigLoaderHolder.reset();
            }
        }

        @Test
        @DisplayName("createFromConfig acepta un db.driver permitido (H2) y abre el pool")
        void createFromConfigDriverPermitidoConecta() throws Exception {
            String originalUrl = System.getProperty("db.url");
            String originalDriver = System.getProperty("db.driver");
            String originalUser = System.getProperty("db.username");
            String originalPass = System.getProperty("db.password");

            DatabaseConnector connector = null;
            try {
                System.setProperty("db.url",
                    "jdbc:h2:mem:allowlist_ok_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
                System.setProperty("db.driver", "org.h2.Driver"); // permitido por defecto
                System.setProperty("db.username", "sa");
                System.setProperty("db.password", "");
                ConfigLoaderHolder.reset();

                connector = DbConnectorFactory.createFromConfig();
                assertThat(connector).isNotNull();
                assertThat(connector.getDataSource()).isNotNull();
            } finally {
                if (connector != null) {
                    connector.close();
                }
                restoreProperty("db.url", originalUrl);
                restoreProperty("db.driver", originalDriver);
                restoreProperty("db.username", originalUser);
                restoreProperty("db.password", originalPass);
                ConfigLoaderHolder.reset();
            }
        }
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private void restoreProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
    }
}

