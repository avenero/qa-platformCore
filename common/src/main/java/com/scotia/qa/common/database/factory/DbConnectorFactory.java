package com.scotia.qa.common.database.factory;

import com.scotia.qa.common.database.config.DatabaseConfig;
import com.scotia.qa.common.database.connectors.MySQLConnector;
import com.scotia.qa.common.database.connectors.OracleConnector;
import com.scotia.qa.common.database.connectors.PostgreSQLConnector;
import com.scotia.qa.common.database.connectors.SQLServerConnector;
import com.scotia.qa.common.database.interfaces.DatabaseConnector;
import com.scotia.qa.common.logging.TestLogger;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Factory para crear conectores de base de datos específicos por tipo.
 *
 * <p>Soporta múltiples tipos de bases de datos:</p>
 * <ul>
 *   <li>Oracle - {@link OracleConnector}</li>
 *   <li>PostgreSQL - {@link PostgreSQLConnector}</li>
 *   <li>MySQL - {@link MySQLConnector}</li>
 *   <li>SQL Server - {@link SQLServerConnector}</li>
 * </ul>
 *
 * <p><b>Configuración específica por BD (System Properties):</b></p>
 * <pre>
 * # Oracle
 * -Doracle.db.url=jdbc:oracle:thin:@//host:port/service
 * -Doracle.db.username=user
 * -Doracle.db.password=pass
 *
 * # PostgreSQL
 * -Dpostgresql.db.url=jdbc:postgresql://host:port/db
 * -Dpostgresql.db.username=user
 * -Dpostgresql.db.password=pass
 *
 * # MySQL
 * -Dmysql.db.url=jdbc:mysql://host:port/db
 * -Dmysql.db.username=user
 * -Dmysql.db.password=pass
 *
 * # SQL Server
 * -Dsqlserver.db.url=jdbc:sqlserver://host:port;databaseName=db
 * -Dsqlserver.db.username=user
 * -Dsqlserver.db.password=pass
 * </pre>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * // Crear conector por tipo (desde System Properties)
 * DatabaseConnector oracle = DbConnectorFactory.getConnector("oracle");
 * DatabaseConnector postgres = DbConnectorFactory.getConnector("postgresql");
 *
 * // Crear con parámetros explícitos
 * DatabaseConnector oracle = DbConnectorFactory.getOracleConnector(url, user, pass);
 *
 * // Genérico (compatibilidad hacia atrás)
 * DatabaseConnector generic = DbConnectorFactory.createFromSystemProperties();
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class DbConnectorFactory {

    private static final String PROP_DB_URL = "db.url";
    private static final String PROP_DB_USERNAME = "db.username";
    private static final String PROP_DB_PASSWORD = "db.password";
    private static final String PROP_DB_DRIVER = "db.driver";
    private static final String PROP_DB_POOL_SIZE = "db.pool.size";

    /**
     * Crea un conector basado en el tipo de base de datos.
     *
     * <p>Lee configuración específica desde System Properties.</p>
     *
     * @param dbType Tipo de BD: "oracle", "postgresql", "mysql", "sqlserver"
     * @return DatabaseConnector configurado
     * @throws IllegalArgumentException Si el tipo no es soportado
     */
    public static DatabaseConnector getConnector(String dbType) {
        if (dbType == null || dbType.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de base de datos no puede ser null o vacío");
        }

        String type = dbType.toLowerCase().trim();

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "Creando conector para: " + type,
            Map.of("dbType", type));

        switch (type) {
            case "oracle":
                return new OracleConnector();

            case "postgresql":
            case "postgres":
                return new PostgreSQLConnector();

            case "mysql":
                return new MySQLConnector();

            case "sqlserver":
            case "sql-server":
            case "mssql":
                return new SQLServerConnector();

            default:
                throw new IllegalArgumentException(
                    "Tipo de base de datos no soportado: " + dbType + ". " +
                    "Tipos soportados: oracle, postgresql, mysql, sqlserver"
                );
        }
    }

    /**
     * Crea un conector Oracle con parámetros explícitos.
     */
    public static DatabaseConnector getOracleConnector(String jdbcUrl, String username, String password) {
        return new OracleConnector(jdbcUrl, username, password);
    }

    /**
     * Crea un conector PostgreSQL con parámetros explícitos.
     */
    public static DatabaseConnector getPostgreSQLConnector(String jdbcUrl, String username, String password) {
        return new PostgreSQLConnector(jdbcUrl, username, password);
    }

    /**
     * Crea un conector MySQL con parámetros explícitos.
     */
    public static DatabaseConnector getMySQLConnector(String jdbcUrl, String username, String password) {
        return new MySQLConnector(jdbcUrl, username, password);
    }

    /**
     * Crea un conector SQL Server con parámetros explícitos.
     */
    public static DatabaseConnector getSQLServerConnector(String jdbcUrl, String username, String password) {
        return new SQLServerConnector(jdbcUrl, username, password);
    }

    /**
     * Crea un conector genérico desde System Properties (para compatibilidad hacia atrás).
     *
     * <p>Lee configuración desde:</p>
     * <ul>
     *   <li>db.url</li>
     *   <li>db.username</li>
     *   <li>db.password</li>
     *   <li>db.driver</li>
     *   <li>db.pool.size (opcional, default: 10)</li>
     * </ul>
     *
     * @return DatabaseConnector configurado
     * @throws IllegalArgumentException Si faltan propiedades requeridas
     */
    /**
     * Crea un conector genérico desde ConfigManager.
     *
     * <p><b>⭐ MÉTODO RECOMENDADO:</b> Este método usa ConfigManager para leer configuraciones,
     * lo que permite usar archivos config-{env}.properties y variables de entorno.</p>
     *
     * <p><b>Configuración requerida en config-{env}.properties:</b></p>
     * <pre>
     * # Opción 1: Configuración explícita (RECOMENDADO)
     * db.url=jdbc:oracle:thin:@//host:port/service
     * db.username=${DB_USER}
     * db.password=${DB_PASS}
     * db.driver=oracle.jdbc.OracleDriver
     * db.pool.size.max=10
     *
     * # Opción 2: Usando db.type para auto-detección
     * db.type=sqlserver  # o oracle, postgresql, mysql
     * sqlserver.db.url=jdbc:sqlserver://host:port;databaseName=db
     * sqlserver.db.username=${DB_USER}
     * sqlserver.db.password=${DB_PASS}
     * </pre>
     *
     * @return DatabaseConnector configurado
     * @throws IllegalArgumentException Si faltan configuraciones requeridas
     */
    public static DatabaseConnector createFromConfig() {
        com.scotia.qa.common.config.ConfigManager config =
            com.scotia.qa.common.config.ConfigManager.getInstance();

        String jdbcUrl = config.get("db.url");
        String username = config.get("db.username");
        String password = config.get("db.password");

        // Detectar driver automáticamente si no está especificado
        String driver = config.get("db.driver");

        if (driver == null || driver.trim().isEmpty()) {
            // Intentar detectar por db.type
            String dbType = config.get("db.type");
            if (dbType != null && !dbType.trim().isEmpty()) {
                driver = getDriverByType(dbType.trim().toLowerCase());
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "Driver detectado por db.type",
                    Map.of("dbType", dbType, "driver", driver));
            } else if (jdbcUrl != null) {
                // Detectar por URL JDBC
                driver = detectDriverFromUrl(jdbcUrl);
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "Driver detectado por URL JDBC",
                    Map.of("driver", driver));
            } else {
                throw new IllegalArgumentException(
                    "No se pudo determinar el driver de base de datos. " +
                    "Especifica 'db.driver' o 'db.type' en config-{env}.properties"
                );
            }
        }

        validateProperties(jdbcUrl, username, password, driver);

        int poolSize = config.getInt("db.pool.size.max", 10);

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "Creando conector desde ConfigManager",
            Map.of("driver", driver, "poolSize", poolSize));

        return create(jdbcUrl, username, password, driver, poolSize);
    }

    /**
     * Crea un conector genérico con parámetros explícitos.
     *
     * @param jdbcUrl URL JDBC completa
     * @param username Usuario de BD
     * @param password Contraseña
     * @param driverClassName Clase del driver JDBC
     * @return DatabaseConnector configurado
     */
    public static DatabaseConnector create(String jdbcUrl, String username, String password, String driverClassName) {
        return create(jdbcUrl, username, password, driverClassName, 10);
    }

    /**
     * Crea un conector genérico con pool size personalizado.
     *
     * @param jdbcUrl URL JDBC
     * @param username Usuario
     * @param password Contraseña
     * @param driverClassName Driver JDBC
     * @param maxPoolSize Tamaño máximo del pool
     * @return DatabaseConnector configurado
     */
    public static DatabaseConnector create(String jdbcUrl, String username, String password,
                                          String driverClassName, int maxPoolSize) {
        return new GenericDatabaseConnector(jdbcUrl, username, password, driverClassName, maxPoolSize);
    }

    /**
     * Valida que las propiedades requeridas estén presentes.
     */
    private static void validateProperties(String jdbcUrl, String username, String password, String driver) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Propiedad '" + PROP_DB_URL + "' no configurada. " +
                "Usa: -D" + PROP_DB_URL + "=jdbc:..."
            );
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Propiedad '" + PROP_DB_USERNAME + "' no configurada"
            );
        }
        if (password == null) {
            throw new IllegalArgumentException(
                "Propiedad '" + PROP_DB_PASSWORD + "' no configurada"
            );
        }
        if (driver == null || driver.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Propiedad '" + PROP_DB_DRIVER + "' no configurada. " +
                "Ejemplos: oracle.jdbc.OracleDriver, com.microsoft.sqlserver.jdbc.SQLServerDriver"
            );
        }
    }

    /**
     * Obtiene el driver JDBC según el tipo de base de datos.
     *
     * @param dbType Tipo de BD: oracle, sqlserver, postgresql, mysql
     * @return Nombre de clase del driver JDBC
     * @throws IllegalArgumentException Si el tipo no es soportado
     */
    private static String getDriverByType(String dbType) {
        switch (dbType) {
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "sqlserver":
            case "mssql":
            case "sql-server":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "postgresql":
            case "postgres":
                return "org.postgresql.Driver";
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            default:
                throw new IllegalArgumentException(
                    "Tipo de base de datos no soportado: " + dbType + ". " +
                    "Tipos válidos: oracle, sqlserver, postgresql, mysql"
                );
        }
    }

    /**
     * Detecta el driver JDBC analizando la URL de conexión.
     *
     * @param jdbcUrl URL JDBC
     * @return Nombre de clase del driver JDBC
     * @throws IllegalArgumentException Si no se puede detectar el tipo de BD
     */
    private static String detectDriverFromUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL JDBC no puede ser null o vacía");
        }

        String url = jdbcUrl.toLowerCase();

        if (url.contains("jdbc:oracle")) {
            return "oracle.jdbc.OracleDriver";
        } else if (url.contains("jdbc:sqlserver") || url.contains("jdbc:jtds:sqlserver")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (url.contains("jdbc:postgresql")) {
            return "org.postgresql.Driver";
        } else if (url.contains("jdbc:mysql")) {
            return "com.mysql.cj.jdbc.Driver";
        } else {
            throw new IllegalArgumentException(
                "No se pudo detectar el tipo de base de datos desde la URL: " + jdbcUrl + ". " +
                "Especifica 'db.driver' explícitamente en config-{env}.properties"
            );
        }
    }

    /**
     * Implementación interna genérica de DatabaseConnector.
     */
    private static class GenericDatabaseConnector implements DatabaseConnector {
        private final HikariDataSource dataSource;

        public GenericDatabaseConnector(String jdbcUrl, String username, String password,
                                       String driverClassName, int maxPoolSize) {
            TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                "Creando conector genérico de base de datos",
                Map.of("driver", driverClassName, "poolSize", maxPoolSize));

            this.dataSource = (HikariDataSource) DatabaseConfig.createHikariDataSource(
                jdbcUrl, username, password, driverClassName, maxPoolSize, 2
            );
        }

        @Override
        public DataSource getDataSource() {
            return dataSource;
        }

        @Override
        public void close() {
            if (dataSource != null && !dataSource.isClosed()) {
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "Cerrando pool de conexiones: " + dataSource.getPoolName(), null);
                dataSource.close();
            }
        }
    }
}

