package com.scotia.qa.common.database.config;

import com.scotia.qa.common.logging.TestLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Configuración centralizada para la creación de DataSources con HikariCP.
 *
 * <p>HikariCP es el pool de conexiones más rápido y confiable para Java.
 * Proporciona alta performance y bajo overhead.</p>
 *
 * <p><b>Configuración óptima para testing:</b></p>
 * <ul>
 *   <li>Pool pequeño (10 conexiones máx)</li>
 *   <li>Leak detection habilitado</li>
 *   <li>Validación de conexiones automática</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class DatabaseConfig {

    // Configuraciones por defecto optimizadas para testing
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 segundos
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10 minutos
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30 minutos
    private static final long DEFAULT_LEAK_DETECTION_THRESHOLD = 60000; // 1 minuto

    /**
     * Crea un DataSource configurado usando HikariCP con configuración por defecto.
     *
     * @param jdbcUrl URL de conexión JDBC
     * @param user Usuario de la base de datos
     * @param password Contraseña del usuario
     * @param driverClassName Nombre de la clase del driver JDBC
     * @return DataSource configurado con HikariCP
     */
    public static DataSource createHikariDataSource(String jdbcUrl, String user, String password, String driverClassName) {
        return createHikariDataSource(jdbcUrl, user, password, driverClassName,
                                     DEFAULT_MAX_POOL_SIZE, DEFAULT_MIN_IDLE);
    }

    /**
     * Crea un DataSource con configuración personalizada del pool.
     *
     * @param jdbcUrl URL de conexión JDBC
     * @param user Usuario de la base de datos
     * @param password Contraseña del usuario
     * @param driverClassName Nombre de la clase del driver JDBC
     * @param maxPoolSize Tamaño máximo del pool
     * @param minIdle Mínimo de conexiones idle
     * @return DataSource configurado con HikariCP
     */
    public static DataSource createHikariDataSource(String jdbcUrl, String user, String password,
                                                   String driverClassName, int maxPoolSize, int minIdle) {
        try {
            HikariConfig config = new HikariConfig();

            // Configuración de conexión
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);

            // Configuración del pool
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
            config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
            config.setMaxLifetime(DEFAULT_MAX_LIFETIME);

            // Leak detection para detectar conexiones no cerradas
            config.setLeakDetectionThreshold(DEFAULT_LEAK_DETECTION_THRESHOLD);

            // Query para validar conexiones según tipo de BD
            String testQuery = getConnectionTestQuery(driverClassName);
            config.setConnectionTestQuery(testQuery);

            // Pool name para identificación en logs
            String dbType = extractDatabaseType(driverClassName);
            config.setPoolName("TestDataPool-" + dbType);

            HikariDataSource dataSource = new HikariDataSource(config);

            TestLogger.logInfo("DATABASE_CONFIG",
                "Pool de conexiones HikariCP creado exitosamente",
                Map.of(
                    "database", dbType,
                    "maxPoolSize", maxPoolSize,
                    "minIdle", minIdle,
                    "poolName", config.getPoolName()
                ));

            return dataSource;

        } catch (Exception e) {
            TestLogger.logError("DATABASE_CONFIG",
                "Error creando pool de conexiones HikariCP: " + e.getMessage(), null);
            throw new RuntimeException("No se pudo crear DataSource", e);
        }
    }

    /**
     * Extrae el tipo de base de datos del nombre del driver.
     *
     * @param driverClassName nombre de la clase del driver
     * @return tipo de base de datos
     */
    private static String extractDatabaseType(String driverClassName) {
        if (driverClassName.contains("oracle")) {
            return "Oracle";
        } else if (driverClassName.contains("sqlserver") || driverClassName.contains("jtds")) {
            return "SQLServer";
        } else if (driverClassName.contains("postgresql")) {
            return "PostgreSQL";
        } else if (driverClassName.contains("mysql")) {
            return "MySQL";
        } else {
            return "Unknown";
        }
    }

    /**
     * Obtiene la query de prueba de conexión apropiada según el tipo de base de datos.
     *
     * @param driverClassName nombre de la clase del driver
     * @return query de prueba de conexión
     */
    private static String getConnectionTestQuery(String driverClassName) {
        if (driverClassName.contains("oracle")) {
            return "SELECT 1 FROM DUAL";
        } else if (driverClassName.contains("sqlserver") || driverClassName.contains("jtds")) {
            return "SELECT 1";
        } else if (driverClassName.contains("postgresql")) {
            return "SELECT 1";
        } else if (driverClassName.contains("mysql")) {
            return "SELECT 1";
        } else {
            return "SELECT 1"; // Por defecto
        }
    }
}
