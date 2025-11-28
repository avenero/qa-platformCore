package com.scotia.qa.apicore.implementations;

import com.scotia.qa.common.logging.TestLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Gestor consolidado de configuración de base de datos para el framework Scotia QA.
 *
 * <p>Esta clase unifica y migra las funcionalidades de DatabaseConfig y DbConnectionConfig del
 * paquete dbConnector hacia la nueva arquitectura de implementations. Proporciona configuración
 * robusta con múltiples fuentes (properties, environment, defaults).
 *
 * <p><b>🎯 RESPONSABILIDADES CONSOLIDADAS:</b>
 *
 * <ul>
 *   <li><b>Configuración DataSource</b> - Creación optimizada con HikariCP
 *   <li><b>Multi-source config</b> - Properties, variables entorno, defaults
 *   <li><b>Configuraciones específicas</b> - Oracle, SQL Server optimizadas
 *   <li><b>Pool management</b> - Configuraciones de conexión robustas
 *   <li><b>Fallback strategies</b> - Configuración resiliente
 * </ul>
 *
 * <p><b>🏗️ MIGRACIÓN DESDE dbConnector:</b> Esta clase consolida y reemplaza:
 *
 * <ul>
 *   <li>{@code dbConnector/config/DatabaseConfig.java}
 *   <li>{@code dbConnector/config/DbConnectionConfig.java}
 * </ul>
 *
 * <p><b>🚀 USO EN IMPLEMENTATIONS:</b>
 *
 * <pre>
 * // Crear DataSource para Oracle
 * DataSource oracleDS = BaseDatabaseConfiguration.createDataSource(
 *     "oracle", "jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
 *
 * // Configuración desde múltiples fuentes
 * String oracleUrl = BaseDatabaseConfiguration.getOracleJdbcUrl();
 * String sqlServerUser = BaseDatabaseConfiguration.getSqlServerUser();
 * </pre>
 *
 * <p><b>📋 CONFIGURACIONES SOPORTADAS:</b>
 *
 * <ul>
 *   <li><b>Oracle Database</b> - URLs, drivers, credenciales
 *   <li><b>SQL Server</b> - URLs, drivers, credenciales optimizadas
 *   <li><b>Pool Configuration</b> - HikariCP con settings production-ready
 * </ul>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2.0.0
 * @see com.scotia.qa.common.database.interfaces.DatabaseConnector
 */
public class BaseDatabaseConfiguration {

  private static final TestLogger.LoggerWrapper log =
      TestLogger.getLogger(BaseDatabaseConfiguration.class);

  // =================================================================================
  // CONFIGURACIONES DE POOL POR DEFECTO (MIGRADAS DE DatabaseConfig)
  // =================================================================================

  private static final int DEFAULT_MAX_POOL_SIZE = 10;
  private static final int DEFAULT_MIN_IDLE = 2;
  private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 segundos
  private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10 minutos
  private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30 minutos

  // =================================================================================
  // CONFIGURACIONES DESDE MÚLTIPLES FUENTES (MIGRADAS DE DbConnectionConfig)
  // =================================================================================

  private static final String CONFIG_FILE = "db-config.properties";
  private static Properties properties;

  static {
    loadConfiguration();
  }

  private BaseDatabaseConfiguration() {
    // Utility class - no instances
  }

  // =================================================================================
  // CREACIÓN DE DATASOURCE CONSOLIDADA
  // =================================================================================

  /**
   * Crea un DataSource optimizado basado en el tipo de base de datos.
   *
   * @param databaseType tipo de BD ("oracle", "sqlserver")
   * @param jdbcUrl URL JDBC
   * @param user usuario
   * @param password contraseña
   * @return DataSource configurado y optimizado
   */
  public static DataSource createDataSource(
      String databaseType, String jdbcUrl, String user, String password) {
    String driverClassName = getDriverClassName(databaseType);
    return createHikariDataSource(jdbcUrl, user, password, driverClassName);
  }

  /**
   * Crea un DataSource con configuración de pool personalizada.
   *
   * @param databaseType tipo de BD
   * @param jdbcUrl URL JDBC
   * @param user usuario
   * @param password contraseña
   * @param maxPoolSize tamaño máximo del pool
   * @param minIdle conexiones mínimas idle
   * @return DataSource configurado
   */
  public static DataSource createDataSource(
      String databaseType,
      String jdbcUrl,
      String user,
      String password,
      int maxPoolSize,
      int minIdle) {
    String driverClassName = getDriverClassName(databaseType);
    return createHikariDataSource(jdbcUrl, user, password, driverClassName, maxPoolSize, minIdle);
  }

  /** Crea un HikariDataSource con configuración estándar. */
  public static DataSource createHikariDataSource(
      String jdbcUrl, String user, String password, String driverClassName) {
    return createHikariDataSource(
        jdbcUrl, user, password, driverClassName, DEFAULT_MAX_POOL_SIZE, DEFAULT_MIN_IDLE);
  }

  /** Crea un HikariDataSource con configuración personalizada. */
  public static DataSource createHikariDataSource(
      String jdbcUrl,
      String user,
      String password,
      String driverClassName,
      int maxPoolSize,
      int minIdle) {
    String dbType = extractDatabaseType(driverClassName);
    log.info("Configurando DataSource {} [MaxPool:{}, MinIdle:{}]", dbType, maxPoolSize, minIdle);

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

    // Configuraciones adicionales de rendimiento
    config.setLeakDetectionThreshold(60000);
    config.setPoolName("Scotia-" + dbType + "-Pool");

    // Validación de conexiones
    config.setConnectionTestQuery(getTestQuery(dbType));

    HikariDataSource dataSource = new HikariDataSource(config);
    log.info("DataSource {} configurado exitosamente", dbType);
    return dataSource;
  }

  // =================================================================================
  // CONFIGURACIONES ESPECÍFICAS POR BASE DE DATOS
  // =================================================================================

  /** Configuraciones para Oracle Database. */
  public static String getOracleJdbcUrl() {
    return getConfigValue(
        "oracle.jdbc.url", "ORACLE_JDBC_URL", "jdbc:oracle:thin:@localhost:1521:XE");
  }

  public static String getOracleUser() {
    return getConfigValue("oracle.user", "ORACLE_USER", "system");
  }

  public static String getOraclePassword() {
    return getConfigValue("oracle.password", "ORACLE_PASSWORD", "oracle");
  }

  public static String getOracleDriver() {
    return "oracle.jdbc.OracleDriver";
  }

  /** Configuraciones para SQL Server. */
  public static String getSqlServerJdbcUrl() {
    return getConfigValue(
        "sqlserver.jdbc.url",
        "SQLSERVER_JDBC_URL",
        "jdbc:sqlserver://localhost:1433;databaseName=TestDB");
  }

  public static String getSqlServerUser() {
    return getConfigValue("sqlserver.user", "SQLSERVER_USER", "sa");
  }

  public static String getSqlServerPassword() {
    return getConfigValue("sqlserver.password", "SQLSERVER_PASSWORD", "SQLPassword123!");
  }

  public static String getSqlServerDriver() {
    return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  }

  /** Configuraciones generales del pool. */
  public static int getMaxPoolSize() {
    String value =
        getConfigValue(
            "db.pool.max.size", "DB_POOL_MAX_SIZE", String.valueOf(DEFAULT_MAX_POOL_SIZE));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.warn("Invalid pool size: {}, using default: {}", value, DEFAULT_MAX_POOL_SIZE);
      return DEFAULT_MAX_POOL_SIZE;
    }
  }

  public static int getMinIdleConnections() {
    String value =
        getConfigValue("db.pool.min.idle", "DB_POOL_MIN_IDLE", String.valueOf(DEFAULT_MIN_IDLE));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.warn("Invalid min idle: {}, using default: {}", value, DEFAULT_MIN_IDLE);
      return DEFAULT_MIN_IDLE;
    }
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // =================================================================================

  /** Carga configuración desde archivo properties. */
  private static void loadConfiguration() {
    properties = new Properties();

    try (InputStream input =
        BaseDatabaseConfiguration.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
      if (input != null) {
        properties.load(input);
        log.info("Configuración cargada desde archivo: {}", CONFIG_FILE);
      } else {
        log.info("Archivo {} no encontrado, usando variables de entorno y defaults", CONFIG_FILE);
      }
    } catch (IOException e) {
      log.error("Error cargando configuración: {} - {}", CONFIG_FILE, e.getMessage());
    }
  }

  /** Obtiene valor de configuración con fallback strategy. */
  private static String getConfigValue(String propertyKey, String envKey, String defaultValue) {
    // 1. Properties file
    String value = properties.getProperty(propertyKey);
    if (value != null && !value.trim().isEmpty()) {
      return value.trim();
    }

    // 2. Environment variables
    value = System.getenv(envKey);
    if (value != null && !value.trim().isEmpty()) {
      return value.trim();
    }

    // 3. System properties
    value = System.getProperty(envKey);
    if (value != null && !value.trim().isEmpty()) {
      return value.trim();
    }

    // 4. Default value
    return defaultValue;
  }

  /** Obtiene el nombre de la clase driver según el tipo de BD. */
  private static String getDriverClassName(String databaseType) {
    switch (databaseType.toLowerCase()) {
      case "oracle":
        return getOracleDriver();
      case "sqlserver":
      case "sql-server":
      case "mssql":
        return getSqlServerDriver();
      default:
        throw new IllegalArgumentException("Tipo de base de datos no soportado: " + databaseType);
    }
  }

  /** Extrae el tipo de base de datos desde el nombre del driver. */
  private static String extractDatabaseType(String driverClassName) {
    if (driverClassName.contains("oracle")) {
      return "Oracle";
    } else if (driverClassName.contains("sqlserver")) {
      return "SQLServer";
    }
    return "Unknown";
  }

  /** Obtiene la consulta de test de conexión específica por BD. */
  private static String getTestQuery(String dbType) {
    switch (dbType.toLowerCase()) {
      case "oracle":
        return "SELECT 1 FROM DUAL";
      case "sqlserver":
        return "SELECT 1";
      default:
        return "SELECT 1";
    }
  }
}
