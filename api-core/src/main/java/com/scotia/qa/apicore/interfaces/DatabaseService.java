package com.scotia.qa.apicore.interfaces;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Interface consolidada para servicios de base de datos del framework Scotia QA.
 *
 * <p>Esta interface unifica tanto <b>operaciones funcionales</b> (queries, CRUD) como <b>gestión
 * técnica</b> (conexiones, pools) en una sola abstracción coherente. Es la interface principal que
 * deben usar todos los frameworks consumidores.
 *
 * <p><b>🎯 RESPONSABILIDADES UNIFICADAS:</b>
 *
 * <ul>
 *   <li><b>Operaciones CRUD</b> - Create, Read, Update, Delete simplificadas
 *   <li><b>Consultas de Testing</b> - Métodos optimizados para validaciones
 *   <li><b>Gestión de Conexiones</b> - Acceso a DataSource y conexiones cuando necesario
 *   <li><b>Operaciones de Negocio</b> - Funcionalidades específicas del dominio
 *   <li><b>Gestión Transaccional</b> - Operaciones multi-statement
 *   <li><b>Utilidades de Validación</b> - Verificaciones de datos para testing
 * </ul>
 *
 * <p><b>🏗️ ARQUITECTURA SIMPLIFICADA:</b>
 *
 * <pre>
 * DatabaseService (Interface Unificada - ESTA INTERFACE)
 *        ↓ IMPLEMENTA
 * BaseDatabaseService (Implementación Consolidada)
 *        ↓ UTILIZA
 * BaseDatabaseConfiguration (Configuración Unificada)
 * </pre>
 *
 * <p><b>🚀 CASOS DE USO PRINCIPALES:</b>
 *
 * <ul>
 *   <li>Validar datos en testing de APIs
 *   <li>Preparar data sets para pruebas
 *   <li>Verificar efectos de operaciones
 *   <li>Obtener datos de referencia
 *   <li>Limpiar datos después de tests
 *   <li>Acceso técnico a conexiones para casos especiales
 * </ul>
 *
 * <p><b>🛠️ USO TÍPICO (Por testers y desarrolladores):</b>
 *
 * <pre>
 * DatabaseService dbService = DatabaseServiceFactory.getInstance("oracle");
 *
 * // Operaciones de consulta para testing
 * Map&lt;String, Object&gt; user = dbService.queryForMap("SELECT * FROM users WHERE id = 1");
 * List&lt;Map&lt;String, Object&gt;&gt; activeUsers = dbService.queryForList("SELECT * FROM users WHERE active = 1");
 * Long userCount = dbService.queryForCount("SELECT COUNT(*) FROM users WHERE created_date > ?", lastMonth);
 *
 * // Operaciones de modificación para setup/cleanup
 * dbService.executeUpdate("UPDATE users SET active = 0 WHERE test_user = 1");
 * dbService.executeBatch(Arrays.asList(
 *     "INSERT INTO test_users VALUES (1, 'testuser1')",
 *     "INSERT INTO test_users VALUES (2, 'testuser2')"
 * ));
 *
 * // Verificaciones de testing
 * boolean userExists = dbService.exists("SELECT 1 FROM users WHERE email = ?", "test@example.com");
 * boolean isHealthy = dbService.testConnection();
 *
 * // Acceso técnico cuando es necesario (casos especiales)
 * try (Connection conn = dbService.getConnection()) {
 *     // JDBC directo para casos muy específicos
 *     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM complex_view");
 *     // ...
 * }
 * </pre>
 *
 * <p><b>🧪 USO EN TESTING DE APIs:</b>
 *
 * <pre>
 * // En step definitions de Cucumber
 * &#64;Given("user {string} exists in database")
 * public void user_exists_in_database(String userId) {
 *     DatabaseService db = DatabaseServiceFactory.getInstance("oracle");
 *     boolean exists = db.exists("SELECT 1 FROM users WHERE user_id = ?", userId);
 *     if (!exists) {
 *         db.executeUpdate("INSERT INTO users (user_id, name) VALUES (?, ?)", userId, "Test User");
 *     }
 * }
 *
 * &#64;Then("user {string} should be updated in database")
 * public void user_should_be_updated(String userId) {
 *     Map&lt;String, Object&gt; user = db.queryForMap("SELECT * FROM users WHERE user_id = ?", userId);
 *     assertEquals("Updated Name", user.get("name"));
 * }
 * </pre>
 *
 * <p><b>🏭 IMPLEMENTACIÓN:</b>
 *
 * <ul>
 *   <li><b>BaseDatabaseService:</b> Implementación principal consolidada
 *   <li><b>BaseDatabaseConfiguration:</b> Configuración unificada multi-fuente
 *   <li><b>Extensiones específicas:</b> ApiDatabaseService, WebDatabaseService, etc.
 *   <li><b>Integración automática:</b> Con logging, error handling y pools optimizados
 * </ul>
 *
 * <p><b>📋 NOTA DE CONSOLIDACIÓN:</b> Esta interface unifica lo que anteriormente estaba separado
 * en DatabaseConnector (aspectos técnicos) y DatabaseService (operaciones funcionales). La
 * consolidación simplifica la arquitectura sin perder funcionalidad.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 * @see com.scotia.qa.common.database.interfaces.DatabaseConnector
 * @see com.scotia.qa.common.database.config.DatabaseConfig
 */
public interface DatabaseService extends AutoCloseable {

  // =================================================================================
  // OPERACIONES DE CONSULTA - READ
  // =================================================================================

  /**
   * Ejecuta una consulta SQL y retorna el primer resultado como Map. Ideal para consultas que
   * retornan una sola fila (ej: buscar usuario por ID).
   *
   * @param sql consulta SQL a ejecutar
   * @return Map con los resultados (columna -> valor) o Map vacío si no hay resultados
   * @throws RuntimeException si hay errores en la ejecución
   */
  Map<String, Object> queryForMap(String sql);

  /**
   * Ejecuta una consulta SQL con parámetros y retorna el primer resultado como Map.
   *
   * @param sql consulta SQL con placeholders (?)
   * @param parameters parámetros para la consulta en orden
   * @return Map con los resultados o Map vacío si no hay resultados
   * @throws RuntimeException si hay errores en la ejecución
   */
  Map<String, Object> queryForMap(String sql, Object... parameters);

  /**
   * Ejecuta una consulta SQL y retorna todos los resultados como lista de Maps. Ideal para
   * consultas que pueden retornar múltiples filas.
   *
   * @param sql consulta SQL a ejecutar
   * @return Lista de Maps con todos los resultados, lista vacía si no hay resultados
   * @throws RuntimeException si hay errores en la ejecución
   */
  List<Map<String, Object>> queryForList(String sql);

  /**
   * Ejecuta una consulta SQL con parámetros y retorna todos los resultados como lista.
   *
   * @param sql consulta SQL con placeholders (?)
   * @param parameters parámetros para la consulta
   * @return Lista de Maps con todos los resultados
   * @throws RuntimeException si hay errores en la ejecución
   */
  List<Map<String, Object>> queryForList(String sql, Object... parameters);

  /**
   * Ejecuta una consulta de conteo y retorna el resultado como Long. Optimizada para consultas
   * COUNT, SUM, etc.
   *
   * @param sql consulta SQL que retorna un número (ej: SELECT COUNT(*))
   * @return resultado de la consulta como Long, 0 si no hay resultados
   * @throws RuntimeException si hay errores en la ejecución
   */
  Long queryForCount(String sql);

  /**
   * Ejecuta una consulta de conteo con parámetros.
   *
   * @param sql consulta SQL con placeholders
   * @param parameters parámetros para la consulta
   * @return resultado como Long
   * @throws RuntimeException si hay errores en la ejecución
   */
  Long queryForCount(String sql, Object... parameters);

  /**
   * Verifica si existe al menos un registro que cumpla la condición. Más eficiente que
   * queryForCount para verificaciones de existencia.
   *
   * @param sql consulta SQL de verificación (ej: SELECT 1 FROM table WHERE condition)
   * @return true si existe al menos un registro, false si no existe
   * @throws RuntimeException si hay errores en la ejecución
   */
  boolean exists(String sql);

  /**
   * Verifica existencia con parámetros.
   *
   * @param sql consulta SQL con placeholders
   * @param parameters parámetros para la consulta
   * @return true si existe, false si no existe
   * @throws RuntimeException si hay errores en la ejecución
   */
  boolean exists(String sql, Object... parameters);

  // =================================================================================
  // OPERACIONES DE MODIFICACIÓN - CREATE, UPDATE, DELETE
  // =================================================================================

  /**
   * Ejecuta una sentencia SQL de modificación (INSERT, UPDATE, DELETE).
   *
   * @param sql sentencia SQL a ejecutar
   * @return número de filas afectadas
   * @throws RuntimeException si hay errores en la ejecución
   */
  int executeUpdate(String sql);

  /**
   * Ejecuta una sentencia SQL de modificación con parámetros.
   *
   * @param sql sentencia SQL con placeholders (?)
   * @param parameters parámetros para la sentencia
   * @return número de filas afectadas
   * @throws RuntimeException si hay errores en la ejecución
   */
  int executeUpdate(String sql, Object... parameters);

  /**
   * Ejecuta múltiples sentencias SQL en una sola transacción (batch). Si alguna falla, se hace
   * rollback de todas.
   *
   * @param sqlStatements lista de sentencias SQL a ejecutar
   * @return array con el número de filas afectadas por cada sentencia
   * @throws RuntimeException si hay errores en la ejecución
   */
  int[] executeBatch(List<String> sqlStatements);

  /**
   * Ejecuta múltiples sentencias SQL con parámetros en batch.
   *
   * @param sql sentencia SQL base con placeholders
   * @param parametersList lista de arrays de parámetros, uno por cada ejecución
   * @return array con el número de filas afectadas por cada ejecución
   * @throws RuntimeException si hay errores en la ejecución
   */
  int[] executeBatch(String sql, List<Object[]> parametersList);

  // =================================================================================
  // ACCESO TÉCNICO A CONEXIONES (Para casos especiales)
  // =================================================================================

  /**
   * Obtiene el DataSource configurado para casos técnicos especiales.
   *
   * <p><b>USO LIMITADO:</b> Solo usar cuando necesites acceso directo al pool de conexiones. Para
   * operaciones normales, usar los metodos query/execute de esta interface.
   *
   * @return DataSource configurado y listo para usar
   * @throws RuntimeException si hay problemas de configuracion
   */
  DataSource getDataSource();

  /**
   * Obtiene una conexión directa para casos técnicos muy específicos.
   *
   * <p><b>IMPORTANTE:</b> La conexión debe ser cerrada explícitamente por el caller usando
   * try-with-resources para evitar leaks de conexión.
   *
   * <p><b>USO LIMITADO:</b> Solo usar cuando necesites JDBC puro para casos muy específicos. Para
   * operaciones normales, usar los metodos query/execute de esta interface.
   *
   * @return Connection valida desde el pool
   * @throws SQLException si no se puede obtener conexion del pool
   */
  Connection getConnection() throws SQLException;

  // =================================================================================
  // OPERACIONES DE UTILIDAD Y INFORMACIÓN
  // =================================================================================

  /**
   * Verifica la conectividad con la base de datos. Útil para health checks y troubleshooting.
   *
   * @return true si la conexión está activa y funcional, false si hay problemas
   */
  boolean testConnection();

  /**
   * Obtiene información detallada de la base de datos. Incluye version, driver, URL, etc. Útil para
   * debugging.
   *
   * @return Map con información técnica de la BD
   */
  Map<String, Object> getDatabaseInfo();

  /**
   * Obtiene el tipo de base de datos que está manejando este servicio.
   *
   * @return tipo de BD (ej: "oracle", "sqlserver", "postgresql")
   */
  String getDatabaseType();

  /**
   * Obtiene estadísticas de uso del servicio. Incluye queries ejecutadas, tiempo total, errores,
   * etc.
   *
   * @return Map con estadísticas de uso
   */
  Map<String, Object> getUsageStatistics();

  /**
   * Obtiene información técnica del pool de conexiones. Útil para monitoring y troubleshooting.
   *
   * @return Map con información del pool (active connections, max pool size, etc.)
   */
  Map<String, Object> getPoolInfo();

  /**
   * Obtiene la URL JDBC configurada (sin credenciales por seguridad).
   *
   * @return URL JDBC sanitizada para logging/debugging
   */
  String getJdbcUrl();

  /**
   * Obtiene todas las tablas disponibles en la base de datos. Útil para explorar la estructura de
   * la BD en testing.
   *
   * @return Lista de Maps con información de las tablas
   */
  List<Map<String, Object>> getAllTables();

  /**
   * Obtiene información de las columnas de una tabla específica. Útil para validar estructura de
   * tablas en testing.
   *
   * @param tableName nombre de la tabla
   * @return Lista de Maps con información de las columnas
   */
  List<Map<String, Object>> getTableColumns(String tableName);

  /**
   * Configura el timeout de conexión en milisegundos.
   *
   * @param timeoutMs timeout en milisegundos
   */
  void setConnectionTimeout(int timeoutMs);

  /**
   * Obtiene el timeout de conexión configurado.
   *
   * @return timeout en milisegundos
   */
  int getConnectionTimeout();

  /**
   * Obtiene información del pool de conexiones. Incluye métricas como conexiones activas, tamaño
   * del pool, etc.
   *
   * @return Map con información del pool de conexiones
   */
  Map<String, Object> getConnectionPoolInfo();

  // =================================================================================
  // GESTIÓN DE RECURSOS Y LIFECYCLE
  // =================================================================================

  /**
   * Limpia recursos y cierra conexiones asociadas al servicio. Debe ser llamado al finalizar el uso
   * del servicio.
   */
  void cleanup();

  /**
   * Verifica si el servicio está en estado operativo.
   *
   * @return true si el servicio está activo y puede ejecutar operaciones
   */
  boolean isActive();

  /**
   * Reinicia las estadísticas y estado del servicio. Útil para testing o cuando se necesita limpiar
   * métricas.
   */
  void reset();

  /**
   * Verifica si el servicio/conector ha sido cerrado.
   *
   * @return true si el servicio está cerrado, false si está activo
   */
  default boolean isClosed() {
    return !isActive();
  }

  /**
   * Cierra el servicio y libera todos los recursos asociados. Implementa AutoCloseable para usar
   * con try-with-resources.
   */
  @Override
  void close();
}
