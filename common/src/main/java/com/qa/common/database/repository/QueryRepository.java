package com.qa.common.database.repository;

import com.qa.common.database.interfaces.DatabaseConnector;
import com.qa.common.logging.TestLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio genérico para ejecutar consultas SQL de alto nivel.
 *
 * <p><b>⚠️ IMPORTANTE:</b> Esta clase es una <b>herramienta genérica</b> que NO contiene
 * queries específicas. TÚ le pasas el SQL que necesites ejecutar.</p>
 *
 * <p><b>Simplifica la ejecución de queries SQL:</b></p>
 * <ul>
 *   <li>✅ Gestión automática de recursos (auto-close de Connection, Statement, ResultSet)</li>
 *   <li>✅ Conversión automática de ResultSet a Maps/Lists</li>
 *   <li>✅ Logging integrado con TestLogger</li>
 *   <li>✅ Prepared Statements (previene SQL injection)</li>
 *   <li>✅ Mappers funcionales para objetos custom</li>
 * </ul>
 *
 * <p><b>Casos de uso comunes:</b></p>
 * <pre>
 * DatabaseConnector connector = DbConnectorFactory.connectAndCache("oracle");
 * QueryRepository repo = new QueryRepository(connector);
 *
 * // ========== CASO 1: Obtener UNA fila (Map) ==========
 * Map&lt;String, Object&gt; user = repo.queryForMap(
 *     "SELECT * FROM users WHERE user_id = ?",
 *     "12345"
 * );
 * String firstName = (String) user.get("first_name");
 *
 * // ========== CASO 2: Obtener VARIAS filas (List) ==========
 * List&lt;Map&lt;String, Object&gt;&gt; accounts = repo.queryForList(
 *     "SELECT * FROM accounts WHERE user_id = ? AND status = ?",
 *     "12345", "ACTIVE"
 * );
 *
 * // ========== CASO 3: Contar registros ==========
 * Long totalUsers = repo.count("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
 *
 * // ========== CASO 4: Ejecutar UPDATE/INSERT/DELETE ==========
 * int rowsUpdated = repo.execute(
 *     "UPDATE accounts SET balance = ? WHERE account_id = ?",
 *     5000.00, "ACC-001"
 * );
 *
 * // ========== CASO 5: Mapear a objeto custom ==========
 * User user = repo.queryForObject(
 *     "SELECT * FROM users WHERE user_id = ?",
 *     rs -&gt; new User(
 *         rs.getString("user_id"),
 *         rs.getString("first_name"),
 *         rs.getString("last_name")
 *     ),
 *     "12345"
 * );
 * </pre>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2026-02-20
 */
public class QueryRepository {

    private final DatabaseConnector connector;

    public QueryRepository(DatabaseConnector connector) {
        if (connector == null) {
            throw new IllegalArgumentException("DatabaseConnector no puede ser null");
        }
        this.connector = connector;
    }

    // =========================================================================
    // QUERY FOR MAP - UNA FILA
    // =========================================================================

    /**
     * Ejecuta una consulta y retorna el resultado como un Map.
     * Útil para consultas que retornan una sola fila.
     *
     * @param sql Consulta SQL
     * @return Map con los resultados (columna → valor), vacío si no hay resultados
     * @throws SQLException Si ocurre un error en la consulta
     */
    public Map<String, Object> queryForMap(String sql) throws SQLException {
        TestLogger.logDebug("QUERY_REPOSITORY", "Ejecutando queryForMap: " + sql, null);

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    result.put(columnName, value);
                }

                TestLogger.logDebug("QUERY_REPOSITORY",
                    "Query ejecutado exitosamente, columnas: " + columnCount, null);
                return result;
            }

            TestLogger.logWarning("QUERY_REPOSITORY", "Query no retornó resultados", null);
            return new HashMap<>();

        } catch (SQLException e) {
            TestLogger.logError("QUERY_REPOSITORY",
                "Error ejecutando query: " + sql + " - " + e.getMessage(), null);
            throw e;
        }
    }

    /**
     * Ejecuta una consulta con parámetros y retorna el resultado como un Map.
     *
     * <p><b>⭐ Método RECOMENDADO</b> - Usa PreparedStatement para prevenir SQL injection.</p>
     *
     * @param sql Consulta SQL con placeholders (?)
     * @param parameters Parámetros para reemplazar los placeholders (en orden)
     * @return Map con los resultados (vacío si no hay resultados)
     * @throws SQLException Si ocurre un error en la consulta
     */
    public Map<String, Object> queryForMap(String sql, Object... parameters) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Establecer parámetros
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        result.put(columnName, value);
                    }

                    return result;
                }

                return new HashMap<>();
            }
        }
    }

    // =========================================================================
    // QUERY FOR LIST - MÚLTIPLES FILAS
    // =========================================================================

    /**
     * Ejecuta una consulta y retorna una lista de Maps.
     * Útil para consultas que retornan múltiples filas.
     *
     * @param sql Consulta SQL
     * @return Lista de Maps con los resultados (vacía si no hay resultados)
     * @throws SQLException Si ocurre un error en la consulta
     */
    public List<Map<String, Object>> queryForList(String sql) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }

                results.add(row);
            }
        }

        return results;
    }

    /**
     * Ejecuta una consulta con parámetros y retorna una lista de Maps.
     *
     * @param sql Consulta SQL con placeholders (?)
     * @param parameters Parámetros para reemplazar los placeholders
     * @return Lista de Maps (vacía si no hay resultados)
     * @throws SQLException Si ocurre un error
     */
    public List<Map<String, Object>> queryForList(String sql, Object... parameters) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }

                    results.add(row);
                }
            }
        }

        return results;
    }

    // =========================================================================
    // QUERY FOR OBJECT - MAPPER PERSONALIZADO
    // =========================================================================

    /**
     * Ejecuta una consulta y mapea el resultado usando un ResultSetMapper personalizado.
     *
     * @param sql Consulta SQL
     * @param mapper Función lambda o método de referencia para mapear el ResultSet
     * @param <T> Tipo del objeto resultado
     * @return Objeto mapeado, o null si no hay resultados
     * @throws SQLException Si ocurre un error en la consulta
     */
    public <T> T queryForObject(String sql, ResultSetMapper<T> mapper) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return mapper.mapRow(rs);
            }

            return null;
        }
    }

    /**
     * Ejecuta una consulta con parámetros y mapea el resultado.
     *
     * @param sql Consulta SQL con placeholders (?)
     * @param mapper Función para mapear ResultSet
     * @param parameters Parámetros
     * @param <T> Tipo del objeto resultado
     * @return Objeto mapeado, o null si no hay resultados
     * @throws SQLException Si ocurre un error
     */
    public <T> T queryForObject(String sql, ResultSetMapper<T> mapper, Object... parameters) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapper.mapRow(rs);
                }
                return null;
            }
        }
    }

    // =========================================================================
    // COUNT
    // =========================================================================

    /**
     * Ejecuta una consulta de conteo y retorna el resultado como Long.
     *
     * @param sql Consulta SQL (debe ser un SELECT COUNT(*) o similar)
     * @return El conteo como Long (0 si no hay resultados)
     * @throws SQLException Si ocurre un error en la consulta
     */
    public Long count(String sql) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }

            return 0L;
        }
    }

    /**
     * Ejecuta una consulta de conteo con parámetros.
     *
     * @param sql Consulta SQL con placeholders
     * @param parameters Parámetros
     * @return Conteo como Long
     * @throws SQLException Si ocurre un error
     */
    public Long count(String sql, Object... parameters) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        }
    }

    // =========================================================================
    // EXECUTE - INSERT/UPDATE/DELETE
    // =========================================================================

    /**
     * Ejecuta una sentencia SQL (INSERT, UPDATE, DELETE) y retorna el número de filas afectadas.
     *
     * @param sql Sentencia SQL (INSERT, UPDATE, DELETE)
     * @param parameters Parámetros para la sentencia
     * @return Número de filas afectadas (0 si ninguna fila fue modificada)
     * @throws SQLException Si ocurre un error en la ejecución
     */
    public int execute(String sql, Object... parameters) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Establecer parámetros
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            return stmt.executeUpdate();
        }
    }

    // =========================================================================
    // INTERFACES FUNCIONALES
    // =========================================================================

    /**
     * Interfaz funcional para mapear ResultSet a objetos.
     *
     * @param <T> tipo del objeto resultado
     */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
}

