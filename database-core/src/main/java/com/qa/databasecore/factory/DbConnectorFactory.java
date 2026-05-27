package com.qa.databasecore.factory;


import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.databasecore.config.DatabaseConfig;
import com.qa.databasecore.connector.MySQLConnector;
import com.qa.databasecore.connector.OracleConnector;
import com.qa.databasecore.connector.PostgreSQLConnector;
import com.qa.databasecore.connector.SQLServerConnector;
import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.common.api.logging.TestLogger;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory y Manager para crear y gestionar conectores de base de datos.
 *
 * <p>Soporta múltiples tipos de bases de datos:</p>
 * <ul>
 *   <li>Oracle - {@link OracleConnector}</li>
 *   <li>PostgreSQL - {@link PostgreSQLConnector}</li>
 *   <li>MySQL - {@link MySQLConnector}</li>
 *   <li>SQL Server - {@link SQLServerConnector}</li>
 * </ul>
 *
 * <p><b>⭐ NUEVO (v1.1.0):</b> Gestión de conexiones con cache para uso en Steps de Cucumber</p>
 *
 * <p><b>Configuración multi-BD en config-{env}.properties:</b></p>
 * <pre>
 * # Oracle
 * oracle.db.url=jdbc:oracle:thin:@//host:port/service
 * oracle.db.username=${ORACLE_USER}
 * oracle.db.password=${ORACLE_PASSWORD}
 *
 * # SQL Server con Windows Auth
 * sqlserver.db.url=jdbc:sqlserver://host:port;databaseName=DB;integratedSecurity=true;encrypt=false
 * sqlserver.db.username=
 * sqlserver.db.password=
 *
 * # PostgreSQL
 * postgresql.db.url=jdbc:postgresql://host:port/db
 * postgresql.db.username=${PG_USER}
 * postgresql.db.password=${PG_PASSWORD}
 * </pre>
 *
 * <p><b>Uso en Steps (RECOMENDADO):</b></p>
 * <pre>
 * // Conectar y cachear (reutiliza si ya existe)
 * DatabaseConnector oracle = DbConnectorFactory.connectAndCache("oracle");
 *
 * // Desconectar cuando termines
 * DbConnectorFactory.disconnect("oracle");
 *
 * // O desconectar todas
 * DbConnectorFactory.disconnectAll();
 * </pre>
 *
 * <p><b>Uso legacy (sigue funcionando):</b></p>
 * <pre>
 * // Via TypedConfig (db.*)
 * DatabaseConnector generic = DbConnectorFactory.createFromConfig();
 *
 * // Desde System Properties (oracle.db.*)
 * DatabaseConnector oracle = DbConnectorFactory.getConnector("oracle");
 *
 * // Parámetros explícitos
 * DatabaseConnector custom = DbConnectorFactory.create(url, user, pass, driver);
 * </pre>
 *
 * @author Abel Venero
 * @version 1.1.0
 * @since 2025-11-26
 */
public class DbConnectorFactory {

    private static final String PROP_DB_URL = "db.url";
    private static final String PROP_DB_USERNAME = "db.username";
    private static final String PROP_DB_PASSWORD = "db.password";
    private static final String PROP_DB_DRIVER = "db.driver";
    private static final String PROP_DB_POOL_SIZE = "db.pool.size";

    /** Tamaño de pool por defecto cuando no se configura explícitamente. */
    private static final int DEFAULT_POOL_SIZE = 10;

    /** Mínimo de conexiones en el pool HikariCP. */
    private static final int MIN_POOL_SIZE = 2;

    // =========================================================================
    // CACHE DE CONEXIONES (NUEVO v1.1.0)
    // =========================================================================

    /**
     * Cache de conectores activos por tipo de BD.
     * Permite reutilizar conexiones en múltiples steps sin recrearlas.
     */
    private static final Map<String, DatabaseConnector> ACTIVE_CONNECTORS = new HashMap<>();

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
     * Crea un conector genérico desde configuración tipada.
     *
     * <p><b>⭐ MÉTODO RECOMENDADO:</b> Lee configuración vía ConfigLoaderHolder (TypedConfig),
     * soporta config-{env}.properties, variables de entorno y System Properties.</p>
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
        com.qa.common.api.config.DatabaseConfig db =
            ConfigLoaderHolder.get().load(com.qa.common.api.config.DatabaseConfig.class);

        String jdbcUrl = db.url();
        String username = db.username();
        String password = db.password();

        // Validar que al menos tengamos URL
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Propiedad 'db.url' no configurada en config-{env}.properties. " +
                "Para multi-BD usa: DbConnectorFactory.connectAndCache(\"oracle\") " +
                "y configura: oracle.db.url, sqlserver.db.url, etc."
            );
        }

        // Detectar driver automáticamente si no está especificado
        String driver = db.driver();

        if (driver == null || driver.trim().isEmpty()) {
            // Intentar detectar por db.type
            String dbType = db.type();
            if (dbType != null && !dbType.trim().isEmpty()) {
                driver = getDriverByType(dbType.trim().toLowerCase());
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "Driver detectado por db.type",
                    Map.of("dbType", dbType, "driver", driver));
            } else {
                // Detectar por URL JDBC
                driver = detectDriverFromUrl(jdbcUrl);
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "Driver detectado por URL JDBC",
                    Map.of("driver", driver));
            }
        }

        validateProperties(jdbcUrl, username, password, driver);

        int poolSize = db.poolSizeMax();

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "Creando conector desde configuración tipada",
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
        return create(jdbcUrl, username, password, driverClassName, DEFAULT_POOL_SIZE);
    }

    /**
     * Creates an H2 connector with explicit parameters. H2 is supported primarily for
     * self-contained QA scenarios and in-memory smoke tests; production workloads should
     * use a vendor-specific connector.
     */
    public static DatabaseConnector getH2Connector(String jdbcUrl, String username, String password) {
        return create(jdbcUrl, username, password, "org.h2.Driver", DEFAULT_POOL_SIZE);
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

    // =========================================================================
    // GESTIÓN DE CONEXIONES CON CACHE (NUEVO v1.1.0)
    // =========================================================================

    /**
     * Conecta a una BD y cachea la conexión para reutilización.
     *
     * <p><b>⭐ MÉTODO RECOMENDADO para Steps de Cucumber</b></p>
     *
     * <p>Lee configuración desde config-{env}.properties usando el prefijo {dbType}.db.*
     * Si ya existe una conexión activa para ese tipo, la reutiliza.</p>
     *
     * <p><b>Configuración requerida en config-{env}.properties:</b></p>
     * <pre>
     * # Oracle
     * oracle.db.url=jdbc:oracle:thin:@//servidor:1521/DB
     * oracle.db.username=${ORACLE_USER}
     * oracle.db.password=${ORACLE_PASSWORD}
     *
     * # SQL Server con Windows Auth
     * sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=DB;integratedSecurity=true;encrypt=false
     * sqlserver.db.username=
     * sqlserver.db.password=
     * </pre>
     *
     * <p><b>Uso en Steps:</b></p>
     * <pre>
     * &#64;Given("establezco conexion a base de datos {string}")
     * public void establecerConexion(String dbType) {
     *     DatabaseConnector connector = DbConnectorFactory.connectAndCache(dbType);
     *     ScenarioContext.set("currentDbConnector", connector);
     * }
     * </pre>
     *
     * @param dbType Tipo de BD: "oracle", "sqlserver", "postgresql", "mysql"
     * @return DatabaseConnector configurado y cacheado
     * @throws IllegalArgumentException Si faltan configuraciones o tipo no soportado
     */
    public static DatabaseConnector connectAndCache(String dbType) {
        if (dbType == null || dbType.trim().isEmpty()) {
            throw new IllegalArgumentException("dbType no puede ser null o vacío");
        }

        String type = dbType.toLowerCase().trim();

        // Reutilizar si ya existe
        if (ACTIVE_CONNECTORS.containsKey(type)) {
            TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                "♻️ Reutilizando conexión cacheada: " + type, null);
            return ACTIVE_CONNECTORS.get(type);
        }

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "🔌 Conectando a base de datos: " + type, null);

        // Crear nuevo conector
        DatabaseConnector connector = getConnectorFromConfigManager(type);

        // Cachear
        ACTIVE_CONNECTORS.put(type, connector);

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "✅ Conexión establecida y cacheada: " + type, null);

        return connector;
    }

    /**
     * Obtiene el conector activo de un tipo de BD desde el cache.
     *
     * @param dbType Tipo de BD
     * @return Conector activo o null si no está conectado
     */
    public static DatabaseConnector getCachedConnector(String dbType) {
        if (dbType == null || dbType.trim().isEmpty()) {
            return null;
        }
        return ACTIVE_CONNECTORS.get(dbType.toLowerCase().trim());
    }

    /**
     * Desconecta de una BD específica y la remueve del cache.
     *
     * @param dbType Tipo de BD a desconectar
     */
    public static void disconnect(String dbType) {
        if (dbType == null || dbType.trim().isEmpty()) {
            return;
        }

        String type = dbType.toLowerCase().trim();
        DatabaseConnector connector = ACTIVE_CONNECTORS.remove(type);

        if (connector != null) {
            try {
                connector.close();
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "✅ Desconectado de: " + type, null);
            } catch (Exception e) {
                TestLogger.logError("DB_CONNECTOR_FACTORY",
                    "Error desconectando de " + type + ": " + e.getMessage(), null);
            }
        }
    }

    /**
     * Desconecta de todas las BDs activas y limpia el cache.
     *
     * <p><b>Uso típico en hooks:</b></p>
     * <pre>
     * &#64;After
     * public void cerrarConexiones() {
     *     DbConnectorFactory.disconnectAll();
     * }
     * </pre>
     */
    public static boolean hasActiveConnections() {
        return !ACTIVE_CONNECTORS.isEmpty();
    }

    public static void disconnectAll() {
        if (ACTIVE_CONNECTORS.isEmpty()) {
            return;
        }

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "🧹 Cerrando todas las conexiones activas (" + ACTIVE_CONNECTORS.size() + ")...", null);

        ACTIVE_CONNECTORS.forEach((type, connector) -> {
            try {
                connector.close();
                TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                    "✅ Cerrado: " + type, null);
            } catch (Exception e) {
                TestLogger.logError("DB_CONNECTOR_FACTORY",
                    "Error cerrando " + type + ": " + e.getMessage(), null);
            }
        });

        ACTIVE_CONNECTORS.clear();
    }

    // =========================================================================
    // MÉTODOS PRIVADOS - LECTURA DE CONFIGURACIÓN
    // =========================================================================

    /**
     * Crea un conector leyendo configuración dinámica por prefijo de tipo de BD.
     *
     * <p>Lee {dbType}.db.url, {dbType}.db.username, {dbType}.db.password</p>
     *
     * @param dbType Tipo de BD
     * @return DatabaseConnector configurado
     */
    private static DatabaseConnector getConnectorFromConfigManager(String dbType) {
        com.qa.common.api.config.ConfigLoader loader = ConfigLoaderHolder.get();

        String urlKey = dbType + ".db.url";
        String usernameKey = dbType + ".db.username";
        String passwordKey = dbType + ".db.password";

        String jdbcUrl = loader.getRaw(urlKey).orElse(null);
        String username = loader.getRaw(usernameKey).orElse("");
        String password = loader.getRaw(passwordKey).orElse("");

        // Validar URL (obligatoria)
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Propiedad '%s' no configurada en config-{env}.properties. " +
                    "Ejemplo: %s=jdbc:oracle:thin:@//servidor:1521/DB", urlKey, urlKey)
            );
        }

        // Detectar driver automáticamente por URL
        String driver = detectDriverFromUrl(jdbcUrl);

        TestLogger.logInfo("DB_CONNECTOR_FACTORY",
            "Driver detectado automáticamente",
            Map.of("type", dbType, "driver", driver));

        // Detectar Windows Authentication
        boolean isWindowsAuth = jdbcUrl.toLowerCase().contains("integratedsecurity=true");

        if (isWindowsAuth) {
            TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                "✅ Windows Authentication detectada para " + dbType,
                Map.of("info", "Username/Password no requeridos"));
        } else {
            // Validar credenciales para SQL Auth
            if (username == null || username.trim().isEmpty()) {
                TestLogger.logWarning("DB_CONNECTOR_FACTORY",
                    String.format("⚠️ Username vacío para %s. " +
                        "Si usas Windows Auth, agrega 'integratedSecurity=true' a %s",
                        dbType, urlKey), null);
            }
        }

        // Obtener pool size (default DEFAULT_POOL_SIZE)
        int poolSize = loader.getRaw(dbType + ".db.pool.size.max")
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException e) {
                    return DEFAULT_POOL_SIZE;
                }
            })
            .orElse(DEFAULT_POOL_SIZE);

        // Crear conector
        return create(jdbcUrl, username, password, driver, poolSize);
    }

    // =========================================================================
    // VALIDACIONES Y UTILIDADES
    // =========================================================================

    /**
     * Valida que las propiedades requeridas estén presentes.
     */
    private static void validateProperties(String jdbcUrl, String username, String password, String driver) {
        // Validar URL (siempre obligatoria)
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Propiedad '" + PROP_DB_URL + "' no configurada. " +
                "Usa: -D" + PROP_DB_URL + "=jdbc:..."
            );
        }

        // Validar driver (siempre obligatorio)
        if (driver == null || driver.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Propiedad '" + PROP_DB_DRIVER + "' no configurada. " +
                "Ejemplos: oracle.jdbc.OracleDriver, com.microsoft.sqlserver.jdbc.SQLServerDriver"
            );
        }

        // Detectar Windows Authentication de SQL Server
        boolean isWindowsAuth = jdbcUrl.toLowerCase().contains("integratedsecurity=true");

        if (!isWindowsAuth) {
            // Autenticación SQL estándar: validar username/password
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Propiedad '" + PROP_DB_USERNAME + "' no configurada. " +
                    "Si usas SQL Server con Windows Authentication, " +
                    "agrega 'integratedSecurity=true' a la URL JDBC"
                );
            }
            if (password == null) {
                throw new IllegalArgumentException(
                    "Propiedad '" + PROP_DB_PASSWORD + "' no configurada"
                );
            }
        } else {
            // Windows Authentication detectada - credenciales no requeridas
            TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                "✅ Windows Authentication detectada (integratedSecurity=true) - " +
                "Username/Password no requeridos", null);
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
     * Implementación interna genérica de {@link DatabaseConnector} basada en HikariCP.
     *
     * <p>Creada por {@link #create(String, String, String, String, int)} y usada
     * cuando no hay un conector específico por tipo de BD disponible.</p>
     */
    private static class GenericDatabaseConnector implements DatabaseConnector {
        private final HikariDataSource dataSource;

        GenericDatabaseConnector(String jdbcUrl, String username, String password,
                                       String driverClassName, int maxPoolSize) {
            TestLogger.logInfo("DB_CONNECTOR_FACTORY",
                "Creando conector genérico de base de datos",
                Map.of("driver", driverClassName, "poolSize", maxPoolSize));

            this.dataSource = (HikariDataSource) DatabaseConfig.createHikariDataSource(
                jdbcUrl, username, password, driverClassName, maxPoolSize, MIN_POOL_SIZE
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

