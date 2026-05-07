package com.qa.databasecore.factory;

import com.qa.databasecore.service.BaseDatabaseService;
import com.qa.databasecore.service.DatabaseService;
import com.qa.common.logging.TestLogger;

/**
 * Factory para crear instancias de DatabaseService del QA Automation Framework.
 *
 * <p>Esta factory proporciona métodos estáticos para crear instancias de DatabaseService
 * preconfiguradas para diferentes tipos de bases de datos. Simplifica la creación y configuración
 * de servicios de BD para testing y desarrollo.
 *
 * <p><b>Características principales:</b>
 *
 * <ul>
 *   <li>Creación estandarizada de servicios de BD
 *   <li>Soporte multi-base de datos (Oracle, SQL Server, PostgreSQL, MySQL)
 *   <li>Configuraciones predefinidas por tipo de BD
 *   <li>Validación automática de tipos soportados
 *   <li>Logging integrado para troubleshooting
 * </ul>
 *
 * <p><b>Bases de datos soportadas:</b>
 *
 * <ul>
 *   <li><b>Oracle</b> - Usando OracleConnector
 *   <li><b>SQL Server</b> - Usando SqlServerConnector
 *   <li><b>PostgreSQL</b> - Si se configura conector
 *   <li><b>MySQL</b> - Si se configura conector
 * </ul>
 *
 * <p><b>Uso típico:</b>
 *
 * <pre>
 * // Crear servicio para Oracle
 * DatabaseService oracleDB = DatabaseServiceFactory.getInstance("oracle");
 * Map&lt;String, Object&gt; user = oracleDB.queryForMap("SELECT * FROM users WHERE id = 1");
 *
 * // Crear servicio para SQL Server
 * DatabaseService sqlServerDB = DatabaseServiceFactory.getInstance("sqlserver");
 * List&lt;Map&lt;String, Object&gt;&gt; users = sqlServerDB.queryForList("SELECT * FROM users");
 *
 * // Crear con configuración específica
 * DatabaseService dbService = DatabaseServiceFactory.getInstanceWithTimeout("oracle", 15000);
 * </pre>
 *
 * <p><b>Configuraciones automáticas por BD:</b>
 *
 * <ul>
 *   <li><b>Oracle</b> - Timeout 30s, pool optimizado para consultas
 *   <li><b>SQL Server</b> - Timeout 25s, configuración para transacciones
 *   <li><b>PostgreSQL</b> - Timeout 20s, configuración para JSON
 *   <li><b>MySQL</b> - Timeout 15s, configuración para performance
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2.0.0
 * @see DatabaseService
 * @see BaseDatabaseService
 * @see com.qa.databasecore.connector.DatabaseConnector
 */
public final class DatabaseServiceFactory {

  private static final TestLogger.LoggerWrapper LOG =
      TestLogger.getLogger(DatabaseServiceFactory.class);

  // Configuraciones por defecto para cada tipo de BD
  private static final int ORACLE_DEFAULT_TIMEOUT = 30000; // 30 segundos
  private static final int SQLSERVER_DEFAULT_TIMEOUT = 25000; // 25 segundos
  private static final int POSTGRESQL_DEFAULT_TIMEOUT = 20000; // 20 segundos
  private static final int MYSQL_DEFAULT_TIMEOUT = 15000; // 15 segundos
  private static final int DEFAULT_TIMEOUT = 30000; // 30 segundos

  // Constructor privado para factory
  private DatabaseServiceFactory() {
    throw new UnsupportedOperationException("DatabaseServiceFactory es una clase factory");
  }

  /**
   * Crea una nueva instancia de DatabaseService para el tipo de BD especificado. Utiliza
   * configuraciones optimizadas por defecto según el tipo de base de datos.
   *
   * @param databaseType tipo de base de datos ("oracle", "sqlserver", "postgresql", "mysql")
   * @return nueva instancia de DatabaseService configurada
   * @throws IllegalArgumentException si databaseType es null, vacío o no soportado
   */
  public static DatabaseService getInstance(String databaseType) {
    validateDatabaseType(databaseType);

    String normalizedType = databaseType.toLowerCase().trim();
    LOG.debug("Creando DatabaseService para tipo: {}", normalizedType);

    try {
      BaseDatabaseService dbService = new BaseDatabaseService(normalizedType);

      // Configurar timeout específico por tipo de BD
      int timeout = getDefaultTimeoutForType(normalizedType);
      dbService.setConnectionTimeout(timeout);

      LOG.debug("DatabaseService creado para {} con timeout: {} ms", normalizedType, timeout);
      return dbService;

    } catch (Exception e) {
      LOG.error("Error creando DatabaseService para tipo {}: {}", normalizedType, e.getMessage());
      throw new RuntimeException(
          "No se pudo crear DatabaseService para tipo: " + normalizedType, e);
    }
  }

  /**
   * Crea una instancia de DatabaseService con timeout personalizado. Útil cuando necesitas
   * configuraciones específicas de timeout.
   *
   * @param databaseType tipo de base de datos
   * @param timeoutMs timeout en milisegundos
   * @return DatabaseService configurado con el timeout especificado
   * @throws IllegalArgumentException si databaseType es inválido o timeout es negativo
   */
  public static DatabaseService getInstanceWithTimeout(String databaseType, int timeoutMs) {
    validateDatabaseType(databaseType);
    validateTimeout(timeoutMs);

    String normalizedType = databaseType.toLowerCase().trim();
    LOG.debug(
        "Creando DatabaseService para {} con timeout personalizado: {} ms",
        normalizedType,
        timeoutMs);

    try {
      BaseDatabaseService dbService = new BaseDatabaseService(normalizedType);
      dbService.setConnectionTimeout(timeoutMs);

      LOG.debug(
          "DatabaseService creado para {} con timeout personalizado: {} ms",
          normalizedType,
          timeoutMs);
      return dbService;

    } catch (Exception e) {
      LOG.error(
          "Error creando DatabaseService para tipo {} con timeout {}: {}",
          normalizedType,
          timeoutMs,
          e.getMessage());
      throw new RuntimeException(
          "No se pudo crear DatabaseService para tipo: " + normalizedType, e);
    }
  }

  /**
   * Crea una instancia de DatabaseService específicamente optimizada para Oracle. Incluye
   * configuraciones predefinidas para el mejor rendimiento con Oracle.
   *
   * @return DatabaseService optimizado para Oracle
   */
  public static DatabaseService getOracleInstance() {
    LOG.debug("Creando DatabaseService optimizado para Oracle");
    DatabaseService dbService = getInstance("oracle");

    // Configuraciones específicas adicionales para Oracle podrían ir aquí
    // Por ejemplo: configuraciones de pool específicas, hints SQL, etc.

    LOG.debug("DatabaseService para Oracle creado con configuraciones optimizadas");
    return dbService;
  }

  /**
   * Crea una instancia de DatabaseService específicamente optimizada para SQL Server. Incluye
   * configuraciones predefinidas para el mejor rendimiento con SQL Server.
   *
   * @return DatabaseService optimizado para SQL Server
   */
  public static DatabaseService getSqlServerInstance() {
    LOG.debug("Creando DatabaseService optimizado para SQL Server");
    DatabaseService dbService = getInstance("sqlserver");

    // Configuraciones específicas adicionales para SQL Server podrían ir aquí
    // Por ejemplo: configuraciones de aislamiento, bulk operations, etc.

    LOG.debug("DatabaseService para SQL Server creado con configuraciones optimizadas");
    return dbService;
  }

  /**
   * Crea una instancia de DatabaseService para PostgreSQL. Configurada para aprovechar
   * características específicas de PostgreSQL.
   *
   * @return DatabaseService configurado para PostgreSQL
   */
  public static DatabaseService getPostgreSQLInstance() {
    LOG.debug("Creando DatabaseService para PostgreSQL");
    DatabaseService dbService = getInstance("postgresql");

    // Configuraciones específicas adicionales para PostgreSQL podrían ir aquí
    // Por ejemplo: soporte JSON nativo, array types, etc.

    LOG.debug("DatabaseService para PostgreSQL creado con configuraciones optimizadas");
    return dbService;
  }

  /**
   * Crea una instancia de DatabaseService para MySQL. Configurada para aprovechar características
   * específicas de MySQL.
   *
   * @return DatabaseService configurado para MySQL
   */
  public static DatabaseService getMySQLInstance() {
    LOG.debug("Creando DatabaseService para MySQL");
    DatabaseService dbService = getInstance("mysql");

    // Configuraciones específicas adicionales para MySQL podrían ir aquí
    // Por ejemplo: configuraciones de charset, engine preferences, etc.

    LOG.debug("DatabaseService para MySQL creado con configuraciones optimizadas");
    return dbService;
  }

  /**
   * Prueba la conectividad con la base de datos especificada. Útil para validar configuraciones
   * antes de usar el servicio.
   *
   * @param databaseType tipo de base de datos a probar
   * @return true si la conexión es exitosa, false en caso contrario
   */
  public static boolean testConnection(String databaseType) {
    try {
      LOG.debug("Probando conectividad para tipo de BD: {}", databaseType);
      DatabaseService dbService = getInstance(databaseType);
      boolean result = dbService.testConnection();
      dbService.cleanup();

      LOG.debug("Test de conectividad para {}: {}", databaseType, result ? "EXITOSO" : "FALLIDO");
      return result;

    } catch (Exception e) {
      LOG.warn("Error en test de conectividad para {}: {}", databaseType, e.getMessage());
      return false;
    }
  }

  /**
   * Obtiene los tipos de base de datos soportados por el factory. Útil para validación y
   * documentación.
   *
   * @return array con los tipos de BD soportados
   */
  public static String[] getSupportedDatabaseTypes() {
    return new String[] {"oracle", "sqlserver", "postgresql", "mysql"};
  }

  /**
   * Verifica si un tipo de base de datos es soportado.
   *
   * @param databaseType tipo de BD a verificar
   * @return true si es soportado, false en caso contrario
   */
  public static boolean isSupportedDatabaseType(String databaseType) {
    if (databaseType == null || databaseType.trim().isEmpty()) {
      return false;
    }

    String normalizedType = databaseType.toLowerCase().trim();
    for (String supportedType : getSupportedDatabaseTypes()) {
      if (supportedType.equals(normalizedType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Obtiene información de debug sobre las capacidades del factory. Útil para troubleshooting y
   * verificación de configuración.
   *
   * @return string con información del factory
   */
  public static String getFactoryInfo() {
    return String.format(
        "DatabaseServiceFactory v1.0.0 - Implementación: %s - Tipos soportados: %s",
        BaseDatabaseService.class.getSimpleName(), String.join(", ", getSupportedDatabaseTypes()));
  }

  // =================================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // =================================================================================

  /** Valida que el tipo de base de datos sea válido. */
  private static void validateDatabaseType(String databaseType) {
    if (databaseType == null || databaseType.trim().isEmpty()) {
      throw new IllegalArgumentException("Database type no puede ser null o vacío");
    }

    if (!isSupportedDatabaseType(databaseType)) {
      throw new IllegalArgumentException(
          String.format(
              "Tipo de base de datos no soportado: %s. Tipos soportados: %s",
              databaseType, String.join(", ", getSupportedDatabaseTypes())));
    }
  }

  /** Valida que el timeout sea válido. */
  private static void validateTimeout(int timeoutMs) {
    if (timeoutMs < 0) {
      throw new IllegalArgumentException("Timeout no puede ser negativo: " + timeoutMs);
    }
  }

  /** Obtiene el timeout por defecto para un tipo de BD específico. */
  private static int getDefaultTimeoutForType(String normalizedType) {
    switch (normalizedType) {
      case "oracle":
        return ORACLE_DEFAULT_TIMEOUT;
      case "sqlserver":
      case "sql-server":
      case "mssql":
        return SQLSERVER_DEFAULT_TIMEOUT;
      case "postgresql":
      case "postgres":
        return POSTGRESQL_DEFAULT_TIMEOUT;
      case "mysql":
        return MYSQL_DEFAULT_TIMEOUT;
      default:
        return DEFAULT_TIMEOUT;
    }
  }
}
