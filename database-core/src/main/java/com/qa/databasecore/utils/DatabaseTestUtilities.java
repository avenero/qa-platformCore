package com.qa.databasecore.utils;

import com.qa.databasecore.service.BaseDatabaseService;
import com.qa.databasecore.service.DatabaseService;
import com.qa.common.api.logging.TestLogger;
import java.util.List;
import java.util.Map;

/**
 * Clase de utilidad que simplifica el uso del sistema de base de datos del QA Automation Framework.
 *
 * <p>Esta clase actúa como facade de conveniencia sobre el sistema DatabaseService, proporcionando
 * métodos estáticos para operaciones comunes de testing. Mantiene backward compatibility con el API
 * existente mientras usa internamente la nueva arquitectura de interfaces.
 *
 * <p><b>Arquitectura interna:</b>
 *
 * <ul>
 *   <li>Usa {@link BaseDatabaseService} internamente
 *   <li>Actúa como facade estática para fácil uso
 *   <li>Mantiene backward compatibility total
 *   <li>Integrado con sistema de logging TestLogger
 * </ul>
 *
 * <p><b>Uso típico (backward compatible):</b>
 *
 * <pre>
 * // API existente se mantiene igual
 * Map&lt;String, Object&gt; user = DatabaseTestUtilities.executeQueryForMap(
 *     "oracle", "SELECT * FROM users WHERE id = 1");
 * List&lt;Map&lt;String, Object&gt;&gt; users = DatabaseTestUtilities.executeQueryForList(
 *     "sqlserver", "SELECT * FROM users");
 * Long count = DatabaseTestUtilities.executeQueryForCount("oracle", "SELECT COUNT(*) FROM users");
 *
 * // Nuevas funcionalidades disponibles
 * boolean connected = DatabaseTestUtilities.testConnection("oracle");
 * Map&lt;String, Object&gt; info = DatabaseTestUtilities.getDatabaseInfo("sqlserver");
 * </pre>
 *
 * <p><b>Recomendación:</b> Para uso avanzado o en frameworks específicos, considere usar
 * directamente {@link DatabaseService} y {@link BaseDatabaseService} para mayor control y
 * funcionalidades adicionales.
 *
 * @author Abel Venero
 * @since 1.0.0
 * @see DatabaseService
 * @see BaseDatabaseService
 */
public class DatabaseTestUtilities {

  private static final TestLogger.LoggerWrapper LOG =
      TestLogger.getLogger(DatabaseTestUtilities.class);

  /** Maximum number of characters from a SQL string to include in sanitized log output. */
  private static final int SQL_LOG_MAX_LENGTH = 150;

  private DatabaseTestUtilities() {
    // Utility class
  }

  // =================================================================================
  // MÉTODOS PÚBLICOS PRINCIPALES (BACKWARD COMPATIBLE)
  // =================================================================================

  /**
   * Ejecuta una consulta simple y retorna el primer resultado como Map. Mantiene backward
   * compatibility con API existente.
   *
   * @param dbType tipo de base de datos ("oracle", "sqlserver", "postgresql", "mysql")
   * @param sql consulta SQL
   * @return Map con el resultado o empty map si no hay resultados
   */
  public static Map<String, Object> executeQueryForMap(String dbType, String sql) {
    LOG.info(
        "DatabaseTestUtils - Ejecutando consulta para Map en {}: {}", dbType, truncateSqlForLog(sql));

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      Map<String, Object> result = dbService.queryForMap(sql);
      dbService.cleanup();

      LOG.debug("Consulta para Map completada: {} columnas", result.size());
      return result;

    } catch (Exception e) {
      LOG.error("Error en executeQueryForMap: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Ejecuta una consulta y retorna múltiples resultados como lista de Maps. Mantiene backward
   * compatibility con API existente.
   *
   * @param dbType tipo de base de datos
   * @param sql consulta SQL
   * @return List de Maps con los resultados, lista vacía si no hay resultados
   */
  public static List<Map<String, Object>> executeQueryForList(String dbType, String sql) {
    LOG.info(
        "DatabaseTestUtils - Ejecutando consulta para List en {}: {}", dbType, truncateSqlForLog(sql));

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      List<Map<String, Object>> result = dbService.queryForList(sql);
      dbService.cleanup();

      LOG.debug("Consulta para List completada: {} filas", result.size());
      return result;

    } catch (Exception e) {
      LOG.error("Error en executeQueryForList: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Ejecuta una consulta de conteo y retorna el resultado como Long.
   *
   * @param dbType tipo de base de datos
   * @param sql consulta SQL de conteo
   * @return número de registros como Long
   */
  public static Long executeQueryForCount(String dbType, String sql) {
    LOG.info(
        "DatabaseTestUtils - Ejecutando consulta de conteo en {}: {}", dbType, truncateSqlForLog(sql));

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      Long result = dbService.queryForCount(sql);
      dbService.cleanup();

      LOG.debug("Consulta de conteo completada: {} registros", result);
      return result;

    } catch (Exception e) {
      LOG.error("Error en executeQueryForCount: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Ejecuta una sentencia de modificación (INSERT, UPDATE, DELETE).
   *
   * @param dbType tipo de base de datos
   * @param sql sentencia SQL
   * @return número de filas afectadas
   */
  public static int executeUpdate(String dbType, String sql) {
    LOG.info("DatabaseTestUtils - Ejecutando update en {}: {}", dbType, truncateSqlForLog(sql));

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      int result = dbService.executeUpdate(sql);
      dbService.cleanup();

      LOG.debug("Update completado: {} filas afectadas", result);
      return result;

    } catch (Exception e) {
      LOG.error("Error en executeUpdate: {}", e.getMessage());
      throw e;
    }
  }

  // =================================================================================
  // MÉTODOS NUEVOS DE CONVENIENCIA
  // =================================================================================

  /**
   * Prueba la conexión con la base de datos especificada.
   *
   * @param dbType tipo de base de datos
   * @return true si la conexión es exitosa
   */
  public static boolean testConnection(String dbType) {
    LOG.info("DatabaseTestUtils - Probando conexión para {}", dbType);

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      boolean result = dbService.testConnection();
      dbService.cleanup();

      LOG.debug("Test de conexión para {}: {}", dbType, result ? "EXITOSO" : "FALLIDO");
      return result;

    } catch (Exception e) {
      LOG.warn("Error en test de conexión para {}: {}", dbType, e.getMessage());
      return false;
    }
  }

  /**
   * Obtiene información básica de la base de datos.
   *
   * @param dbType tipo de base de datos
   * @return Map con información de la BD
   */
  public static Map<String, Object> getDatabaseInfo(String dbType) {
    LOG.info("DatabaseTestUtils - Obteniendo información de {}", dbType);

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      Map<String, Object> result = dbService.getDatabaseInfo();
      dbService.cleanup();

      LOG.debug("Información obtenida para {}: {}", dbType, result.get("databaseProductName"));
      return result;

    } catch (Exception e) {
      LOG.error("Error obteniendo información de {}: {}", dbType, e.getMessage());
      throw e;
    }
  }

  /**
   * Lista todas las tablas disponibles en la base de datos.
   *
   * @param dbType tipo de base de datos
   * @return List de Maps con información de las tablas
   */
  public static List<Map<String, Object>> getAllTables(String dbType) {
    LOG.info("DatabaseTestUtils - Obteniendo tablas de {}", dbType);

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      List<Map<String, Object>> result = dbService.getAllTables();
      dbService.cleanup();

      LOG.debug("Obtenidas {} tablas de {}", result.size(), dbType);
      return result;

    } catch (Exception e) {
      LOG.error("Error obteniendo tablas de {}: {}", dbType, e.getMessage());
      throw e;
    }
  }

  /**
   * Obtiene información de las columnas de una tabla específica.
   *
   * @param dbType tipo de base de datos
   * @param tableName nombre de la tabla
   * @return List de Maps con información de las columnas
   */
  public static List<Map<String, Object>> getTableColumns(String dbType, String tableName) {
    LOG.info("DatabaseTestUtils - Obteniendo columnas de tabla {} en {}", tableName, dbType);

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      List<Map<String, Object>> result = dbService.getTableColumns(tableName);
      dbService.cleanup();

      LOG.debug("Obtenidas {} columnas de tabla {} en {}", result.size(), tableName, dbType);
      return result;

    } catch (Exception e) {
      LOG.error(
          "Error obteniendo columnas de tabla {} en {}: {}", tableName, dbType, e.getMessage());
      throw e;
    }
  }

  // =================================================================================
  // MÉTODOS CON PARÁMETROS (NUEVOS)
  // =================================================================================

  /**
   * Ejecuta una consulta con parámetros y retorna el primer resultado como Map.
   *
   * @param dbType tipo de base de datos
   * @param sql consulta SQL con placeholders (?)
   * @param parameters parámetros para la consulta
   * @return Map con el resultado
   */
  public static Map<String, Object> executeQueryForMap(
      String dbType, String sql, Object... parameters) {
    LOG.info(
        "DatabaseTestUtils - Ejecutando consulta parametrizada para Map en {}: {}",
        dbType,
        truncateSqlForLog(sql));

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      Map<String, Object> result = dbService.queryForMap(sql, parameters);
      dbService.cleanup();

      LOG.debug("Consulta parametrizada para Map completada: {} columnas", result.size());
      return result;

    } catch (Exception e) {
      LOG.error("Error en executeQueryForMap parametrizada: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Ejecuta una sentencia de modificación con parámetros.
   *
   * @param dbType tipo de base de datos
   * @param sql sentencia SQL con placeholders (?)
   * @param parameters parámetros para la sentencia
   * @return número de filas afectadas
   */
  public static int executeUpdate(String dbType, String sql, Object... parameters) {
    LOG.info(
        "DatabaseTestUtils - Ejecutando update parametrizado en {}: {}", dbType, truncateSqlForLog(sql));

    try {
      DatabaseService dbService = createDatabaseService(dbType);
      int result = dbService.executeUpdate(sql, parameters);
      dbService.cleanup();

      LOG.debug("Update parametrizado completado: {} filas afectadas", result);
      return result;

    } catch (Exception e) {
      LOG.error("Error en executeUpdate parametrizado: {}", e.getMessage());
      throw e;
    }
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // =================================================================================

  /** Crea una instancia de DatabaseService para el tipo de BD especificado. */
  private static DatabaseService createDatabaseService(String dbType) {
    if (dbType == null || dbType.trim().isEmpty()) {
      throw new IllegalArgumentException("Database type no puede ser null o vacío");
    }
    return new BaseDatabaseService(dbType);
  }

  /** Truncates the SQL string for safe log emission (does NOT redact inline values - only length-bounds). */
  private static String truncateSqlForLog(String sql) {
    if (sql == null) {
      return "null";
    }
    if (sql.length() > SQL_LOG_MAX_LENGTH) {
      return sql.substring(0, SQL_LOG_MAX_LENGTH) + "...";
    }
    return sql;
  }
}
