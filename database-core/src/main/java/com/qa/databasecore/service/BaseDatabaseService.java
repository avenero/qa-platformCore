package com.qa.databasecore.service;

import com.qa.common.api.logging.TestLogger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Implementación consolidada del servicio de base de datos para el QA Automation Framework.
 *
 * <p>Esta clase proporciona una implementación completa y robusta del servicio de BD que utiliza
 * directamente la configuración consolidada sin depender del paquete dbConnector. Está optimizada
 * para ser utilizada directamente o extendida por frameworks específicos.
 *
 * <p><b>🔄 CONSOLIDACIÓN ARQUITECTURAL:</b> Esta implementación reemplaza y consolida:
 *
 * <ul>
 *   <li>Funcionalidades de {@code dbConnector/factory/DbConnectorFactory}
 *   <li>Capacidades de {@code dbConnector/connectors/*}
 *   <li>Operaciones de {@code dbConnector/repository/QueryRepository}
 * </ul>
 *
 * <p><b>Características principales:</b>
 *
 * <ul>
 *   <li>Implementación consolidada de DatabaseService
 *   <li>Configuración unificada usando BaseDatabaseConfiguration
 *   <li>Soporte multi-base de datos directo
 *   <li>Gestión automática de conexiones y pools
 *   <li>Logging detallado con sistema TestLogger
 *   <li>Thread-safe para uso concurrente
 *   <li>Operaciones optimizadas para testing
 * </ul>
 *
 * <p><b>🎯 ARQUITECTURA CONSOLIDADA:</b>
 *
 * <pre>
 * DatabaseService (Interface)
 *        ↓ IMPLEMENTA
 * BaseDatabaseService (Implementación Consolidada - ESTA CLASE)
 *        ↓ UTILIZA
 * BaseDatabaseConfiguration (Configuración Unificada)
 * </pre>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2.0.0
 * @see DatabaseService
 * @see BaseDatabaseConfiguration
 */
public class BaseDatabaseService implements DatabaseService {

  private static final TestLogger.LoggerWrapper LOG =
      TestLogger.getLogger(BaseDatabaseService.class);

  // =================================================================================
  // PROPIEDADES Y CONFIGURACIÓN
  // =================================================================================

  private final String databaseType;
  private final DataSource dataSource;
  private int connectionTimeout = 30000; // 30 segundos por defecto

  // =================================================================================
  // CONSTRUCTORES CONSOLIDADOS
  // =================================================================================

  /**
   * Constructor principal que crea DataSource usando configuración automática.
   *
   * @param databaseType tipo de BD ("oracle", "sqlserver")
   * @throws IllegalArgumentException si databaseType es null o no soportado
   */
  public BaseDatabaseService(String databaseType) {
    if (databaseType == null || databaseType.trim().isEmpty()) {
      throw new IllegalArgumentException("Database type no puede ser null o vacío");
    }

    this.databaseType = databaseType.toLowerCase().trim();

    try {
      // Crear DataSource usando configuración automática
      this.dataSource = createDataSourceForType(this.databaseType);
      LOG.debug("BaseDatabaseService inicializado para tipo: {}", this.databaseType);
    } catch (Exception e) {
      LOG.error(
          "Error inicializando BaseDatabaseService para tipo: {} - {}",
          this.databaseType,
          e.getMessage());
      throw e;
    }
  }

  /**
   * Constructor con configuración personalizada.
   *
   * @param databaseType tipo de BD
   * @param jdbcUrl URL JDBC personalizada
   * @param user usuario personalizado
   * @param password contraseña personalizada
   */
  public BaseDatabaseService(String databaseType, String jdbcUrl, String user, String password) {
    if (databaseType == null || databaseType.trim().isEmpty()) {
      throw new IllegalArgumentException("Database type no puede ser null o vacío");
    }

    this.databaseType = databaseType.toLowerCase().trim();

    try {
      // Crear DataSource con configuración personalizada
      this.dataSource =
          BaseDatabaseConfiguration.createDataSource(this.databaseType, jdbcUrl, user, password);
      LOG.debug(
          "BaseDatabaseService inicializado con configuración personalizada para tipo: {}",
          this.databaseType);
    } catch (Exception e) {
      LOG.error("Error inicializando BaseDatabaseService personalizado: {}", e.getMessage());
      throw e;
    }
  }

  // =================================================================================
  // CREACIÓN CONSOLIDADA DE DATASOURCE
  // =================================================================================

  /** Crea DataSource automáticamente según el tipo de base de datos. */
  private DataSource createDataSourceForType(String dbType) {
    switch (dbType) {
      case "oracle":
        return BaseDatabaseConfiguration.createDataSource(
            "oracle",
            BaseDatabaseConfiguration.getOracleJdbcUrl(),
            BaseDatabaseConfiguration.getOracleUser(),
            BaseDatabaseConfiguration.getOraclePassword());

      case "sqlserver":
      case "sql-server":
      case "mssql":
        return BaseDatabaseConfiguration.createDataSource(
            "sqlserver",
            BaseDatabaseConfiguration.getSqlServerJdbcUrl(),
            BaseDatabaseConfiguration.getSqlServerUser(),
            BaseDatabaseConfiguration.getSqlServerPassword());

      default:
        throw new IllegalArgumentException("Tipo de base de datos no soportado: " + dbType);
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN CONSOLIDADA DE OPERACIONES DE CONSULTA
  // =================================================================================

  @Override
  public Map<String, Object> queryForMap(String sql) {
    return queryForMap(sql, new Object[0]);
  }

  @Override
  public Map<String, Object> queryForMap(String sql, Object... parameters) {
    validateSql(sql);

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      LOG.info("Ejecutando consulta para Map en {}: {}", databaseType, truncateSqlForLog(sql));

      // Establecer parámetros
      setParameters(stmt, parameters);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          Map<String, Object> result = extractRowAsMap(rs);
          LOG.debug("Consulta para Map completada: {} columnas", result.size());
          return result;
        }
        return new HashMap<>();
      }

    } catch (SQLException e) {
      LOG.error("Error ejecutando consulta para Map: {} - {}", truncateSqlForLog(sql), e.getMessage());
      throw new RuntimeException(
          "Error ejecutando consulta en " + databaseType + ": " + e.getMessage(), e);
    }
  }

  @Override
  public List<Map<String, Object>> queryForList(String sql) {
    return queryForList(sql, new Object[0]);
  }

  @Override
  public List<Map<String, Object>> queryForList(String sql, Object... parameters) {
    validateSql(sql);

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      LOG.info("Ejecutando consulta para List en {}: {}", databaseType, truncateSqlForLog(sql));

      setParameters(stmt, parameters);

      try (ResultSet rs = stmt.executeQuery()) {
        List<Map<String, Object>> results = new ArrayList<>();
        while (rs.next()) {
          results.add(extractRowAsMap(rs));
        }
        LOG.debug("Consulta para List completada: {} filas", results.size());
        return results;
      }

    } catch (SQLException e) {
      LOG.error("Error ejecutando consulta para List: {} - {}", truncateSqlForLog(sql), e.getMessage());
      throw new RuntimeException(
          "Error ejecutando consulta en " + databaseType + ": " + e.getMessage(), e);
    }
  }

  @Override
  public Long queryForCount(String sql) {
    return queryForCount(sql, new Object[0]);
  }

  @Override
  public Long queryForCount(String sql, Object... parameters) {
    validateSql(sql);

    Map<String, Object> result = queryForMap(sql, parameters);
    if (result.isEmpty()) {
      return 0L;
    }

    // Obtener el primer valor (debería ser el COUNT)
    Object countValue = result.values().iterator().next();
    return convertToLong(countValue);
  }

  @Override
  public boolean exists(String sql) {
    return exists(sql, new Object[0]);
  }

  @Override
  public boolean exists(String sql, Object... parameters) {
    Long count = queryForCount(sql, parameters);
    return count != null && count > 0;
  }

  // =================================================================================
  // IMPLEMENTACIÓN CONSOLIDADA DE OPERACIONES DE MODIFICACIÓN
  // =================================================================================

  @Override
  public int executeUpdate(String sql) {
    return executeUpdate(sql, new Object[0]);
  }

  @Override
  public int executeUpdate(String sql, Object... parameters) {
    validateSql(sql);

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      LOG.info("Ejecutando update en {}: {}", databaseType, truncateSqlForLog(sql));

      setParameters(stmt, parameters);
      int rowsAffected = stmt.executeUpdate();

      LOG.debug("Update completado: {} filas afectadas", rowsAffected);
      return rowsAffected;

    } catch (SQLException e) {
      LOG.error("Error ejecutando update: {} - {}", truncateSqlForLog(sql), e.getMessage());
      throw new RuntimeException(
          "Error ejecutando update en " + databaseType + ": " + e.getMessage(), e);
    }
  }

  @Override
  public int[] executeBatch(List<String> sqlStatements) {
    if (sqlStatements == null || sqlStatements.isEmpty()) {
      return new int[0];
    }

    try (Connection conn = dataSource.getConnection()) {
      LOG.info("Ejecutando batch de {} statements en {}", sqlStatements.size(), databaseType);

      conn.setAutoCommit(false);

      try (Statement stmt = conn.createStatement()) {
        for (String sql : sqlStatements) {
          validateSql(sql);
          stmt.addBatch(sql);
        }

        int[] results = stmt.executeBatch();
        conn.commit();

        LOG.debug("Batch completado exitosamente: {} statements ejecutados", results.length);
        return results;

      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLException e) {
      LOG.error("Error ejecutando batch: {}", e.getMessage());
      throw new RuntimeException(
          "Error ejecutando batch en " + databaseType + ": " + e.getMessage(), e);
    }
  }

  @Override
  public int[] executeBatch(String sql, List<Object[]> parametersList) {
    if (parametersList == null || parametersList.isEmpty()) {
      return new int[0];
    }

    validateSql(sql);

    try (Connection conn = dataSource.getConnection()) {
      LOG.info(
          "Ejecutando batch parametrizado de {} statements en {}: {}",
          parametersList.size(),
          databaseType,
          truncateSqlForLog(sql));

      conn.setAutoCommit(false);

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        for (Object[] parameters : parametersList) {
          setParameters(stmt, parameters);
          stmt.addBatch();
        }

        int[] results = stmt.executeBatch();
        conn.commit();

        LOG.debug(
            "Batch parametrizado completado exitosamente: {} statements ejecutados",
            results.length);
        return results;

      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLException e) {
      LOG.error("Error ejecutando batch parametrizado: {} - {}", truncateSqlForLog(sql), e.getMessage());
      throw new RuntimeException(
          "Error ejecutando batch parametrizado en " + databaseType + ": " + e.getMessage(), e);
    }
  }

  // =================================================================================
  // IMPLEMENTACIÓN CONSOLIDADA DE OPERACIONES DE INFORMACIÓN
  // =================================================================================

  @Override
  public boolean testConnection() {
    try (Connection conn = dataSource.getConnection()) {
      boolean isValid = conn.isValid(5); // 5 segundos timeout
      LOG.debug("Test de conexión para {}: {}", databaseType, isValid ? "EXITOSO" : "FALLIDO");
      return isValid;
    } catch (Exception e) {
      LOG.warn("Test de conexión fallido para {}: {}", databaseType, e.getMessage());
      return false;
    }
  }

  @Override
  public Map<String, Object> getDatabaseInfo() {
    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData metaData = conn.getMetaData();
      Map<String, Object> info = new HashMap<>();

      info.put("databaseProductName", metaData.getDatabaseProductName());
      info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
      info.put("driverName", metaData.getDriverName());
      info.put("driverVersion", metaData.getDriverVersion());
      info.put("url", metaData.getURL());
      info.put("userName", metaData.getUserName());
      info.put("configuredType", databaseType);

      LOG.debug(
          "Información de BD obtenida para {}: {}", databaseType, info.get("databaseProductName"));
      return info;

    } catch (SQLException e) {
      LOG.error("Error obteniendo información de BD: {}", e.getMessage());
      throw new RuntimeException("Error obteniendo información de BD: " + e.getMessage(), e);
    }
  }

  @Override
  public List<Map<String, Object>> getAllTables() {
    // Usar consulta específica por tipo de BD
    String sql;
    switch (databaseType.toLowerCase()) {
      case "oracle":
        sql = "SELECT table_name FROM user_tables ORDER BY table_name";
        break;
      case "sqlserver":
      case "sql-server":
      case "mssql":
        sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"
            + " WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME";
        break;
      default:
        sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"
            + " WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME";
    }

    List<Map<String, Object>> tables = queryForList(sql);
    LOG.debug("Obtenidas {} tablas para {}", tables.size(), databaseType);
    return tables;
  }

  @Override
  public List<Map<String, Object>> getTableColumns(String tableName) {
    if (tableName == null || tableName.trim().isEmpty()) {
      throw new IllegalArgumentException("Table name no puede ser null o vacío");
    }

    // Usar consulta específica por tipo de BD
    String sql;
    switch (databaseType.toLowerCase()) {
      case "oracle":
        sql = "SELECT column_name, data_type, data_length, nullable"
            + " FROM user_tab_columns WHERE table_name = UPPER(?) ORDER BY column_id";
        break;
      case "sqlserver":
      case "sql-server":
      case "mssql":
        sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE"
            + " FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        break;
      default:
        sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE"
            + " FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
    }

    List<Map<String, Object>> columns = queryForList(sql, tableName);
    LOG.debug("Obtenidas {} columnas para tabla {} en {}", columns.size(), tableName, databaseType);
    return columns;
  }

  // =================================================================================
  // IMPLEMENTACIÓN CONSOLIDADA DE OPERACIONES DE CONFIGURACIÓN
  // =================================================================================

  @Override
  public String getDatabaseType() {
    return databaseType;
  }

  @Override
  public void setConnectionTimeout(int timeoutMs) {
    if (timeoutMs < 0) {
      throw new IllegalArgumentException("Connection timeout no puede ser negativo");
    }
    this.connectionTimeout = timeoutMs;
    LOG.debug("Connection timeout configurado: {} ms", timeoutMs);
  }

  @Override
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  public Map<String, Object> getConnectionPoolInfo() {
    Map<String, Object> poolInfo = new HashMap<>();
    poolInfo.put("databaseType", databaseType);
    poolInfo.put("connectionTimeout", connectionTimeout);
    poolInfo.put("poolType", "HikariCP");
    poolInfo.put("consolidatedArchitecture", true);
    return poolInfo;
  }

  @Override
  public void cleanup() {
    try {
      if (dataSource instanceof AutoCloseable) {
        ((AutoCloseable) dataSource).close();
        LOG.debug("DataSource cerrado para tipo: {}", databaseType);
      }
    } catch (Exception e) {
      LOG.warn("Error cerrando DataSource: {}", e.getMessage());
    }
  }

  @Override
  public void close() {
    cleanup();
  }

  @Override
  public boolean isActive() {
    return testConnection();
  }

  @Override
  public void reset() {
    // Reset statistics and state - no persistent state to reset
    LOG.debug("DatabaseService reset para tipo: {}", databaseType);
  }

  @Override
  public Map<String, Object> getUsageStatistics() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("databaseType", databaseType);
    stats.put("connectionTimeout", connectionTimeout);
    stats.put("isActive", isActive());
    stats.put("architectureVersion", "2.0-consolidated");
    return stats;
  }

  @Override
  public Map<String, Object> getPoolInfo() {
    return getConnectionPoolInfo();
  }

  @Override
  public String getJdbcUrl() {
    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData metaData = conn.getMetaData();
      String url = metaData.getURL();
      return sanitizeUrl(url);
    } catch (Exception e) {
      LOG.error("Error obteniendo JDBC URL: {}", e.getMessage());
      return "jdbc:unknown";
    }
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD CONSOLIDADOS
  // =================================================================================

  /** Valida que la consulta SQL no sea null o vacía. */
  private void validateSql(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      throw new IllegalArgumentException("SQL no puede ser null o vacío");
    }
  }

  /** Truncates the SQL string for safe log emission (does NOT redact inline values - only length-bounds). */
  private String truncateSqlForLog(String sql) {
    if (sql == null) {
      return "null";
    }
    if (sql.length() > 200) {
      return sql.substring(0, 200) + "...";
    }
    return sql;
  }

  /** Sanitiza URL JDBC eliminando credenciales. */
  private String sanitizeUrl(String url) {
    if (url == null) {
      return "jdbc:unknown";
    }
    // Remover credenciales de la URL si las hubiera
    return url.replaceAll("([?&])(user|password)=[^&]*", "$1$2=***");
  }

  /** Establece parámetros en un PreparedStatement. */
  private void setParameters(PreparedStatement stmt, Object[] parameters) throws SQLException {
    for (int i = 0; i < parameters.length; i++) {
      stmt.setObject(i + 1, parameters[i]);
    }
  }

  /** Extrae una fila de ResultSet como Map. */
  private Map<String, Object> extractRowAsMap(ResultSet rs) throws SQLException {
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();
    Map<String, Object> row = new HashMap<>();

    for (int i = 1; i <= columnCount; i++) {
      String columnName = metaData.getColumnName(i);
      Object value = rs.getObject(i);
      row.put(columnName, value);
    }

    return row;
  }

  /** Convierte un valor a Long para consultas de conteo. */
  private Long convertToLong(Object value) {
    if (value == null) {
      return 0L;
    }

    if (value instanceof Long) {
      return (Long) value;
    }

    if (value instanceof Number) {
      return ((Number) value).longValue();
    }

    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      LOG.warn("No se pudo convertir valor a Long: {} - retornando 0", value);
      return 0L;
    }
  }
}
