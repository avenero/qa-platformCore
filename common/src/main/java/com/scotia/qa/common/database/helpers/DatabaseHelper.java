package com.scotia.qa.common.database.helpers;

import com.scotia.qa.common.database.interfaces.DatabaseConnector;
import com.scotia.qa.common.logging.TestLogger;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper para operaciones de base de datos.
 *
 * <p>Encapsula la lógica de ejecución de queries y sentencias SQL,
 * manteniendo los steps de Cucumber limpios y sin lógica compleja.</p>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2026-02-20
 */
public class DatabaseHelper {

    /**
     * Ejecuta una consulta SQL y retorna los resultados.
     *
     * @param connector Conector de BD activo
     * @param query Consulta SQL
     * @param parameters Parámetros separados por coma (opcional)
     * @return Map con los resultados de la consulta
     * @throws SQLException Si ocurre un error ejecutando la query
     * @throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException Si el connector es null
     */
    public static Map<String, Object> executeQuery(DatabaseConnector connector, String query, String parameters)
            throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException {

        if (connector == null) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "executeQuery",
                "No hay conexión activa. Usa: Given establezco conexion a base de datos \"oracle\""
            );
        }

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // Establecer parámetros si existen
            if (parameters != null && !parameters.trim().isEmpty()) {
                String[] params = parameters.split(",");
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i].trim());
                }

                TestLogger.logInfo("DB_HELPER",
                    "Ejecutando query con parámetros",
                    Map.of("query", query, "params", parameters));
            } else {
                TestLogger.logInfo("DB_HELPER",
                    "Ejecutando query sin parámetros",
                    Map.of("query", query));
            }

            // Ejecutar y procesar resultados
            try (ResultSet rs = stmt.executeQuery()) {
                Map<String, Object> result = new HashMap<>();
                int rowCount = 0;

                if (rs.next()) {
                    // Obtener primera fila
                    int columnCount = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        result.put(columnName, value);
                    }
                    rowCount = 1;

                    // Contar el resto de filas
                    while (rs.next()) {
                        rowCount++;
                    }
                }

                // Agregar metadatos
                result.put("_rowCount", rowCount);
                result.put("_hasResults", rowCount > 0);

                TestLogger.logInfo("DB_HELPER",
                    "✅ Query ejecutado exitosamente",
                    Map.of("rowCount", rowCount, "columnsFound", result.size() - 2));

                return result;
            }

        } catch (SQLException e) {
            TestLogger.logError("DB_HELPER",
                "Error ejecutando query: " + e.getMessage(),
                Map.of("query", query, "sqlState", e.getSQLState()));
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "executeQuery",
                "Error ejecutando consulta SQL: " + e.getMessage()
            );
        }
    }

    /**
     * Ejecuta una sentencia SQL de modificación (INSERT, UPDATE, DELETE).
     *
     * @param connector Conector de BD activo
     * @param sql Sentencia SQL
     * @param parameters Parámetros separados por coma (opcional)
     * @return Número de filas afectadas
     * @throws SQLException Si ocurre un error ejecutando la sentencia
     * @throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException Si el connector es null
     */
    public static int executeStatement(DatabaseConnector connector, String sql, String parameters)
            throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException {

        if (connector == null) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "executeStatement",
                "No hay conexión activa. Usa: Given establezco conexion a base de datos \"oracle\""
            );
        }

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Establecer parámetros si existen
            if (parameters != null && !parameters.trim().isEmpty()) {
                String[] params = parameters.split(",");
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i].trim());
                }

                TestLogger.logInfo("DB_HELPER",
                    "Ejecutando sentencia con parámetros",
                    Map.of("sql", sql, "params", parameters));
            } else {
                TestLogger.logInfo("DB_HELPER",
                    "Ejecutando sentencia sin parámetros",
                    Map.of("sql", sql));
            }

            int rowsAffected = stmt.executeUpdate();

            TestLogger.logInfo("DB_HELPER",
                "✅ Sentencia ejecutada exitosamente",
                Map.of("rowsAffected", rowsAffected));

            return rowsAffected;

        } catch (SQLException e) {
            TestLogger.logError("DB_HELPER",
                "Error ejecutando sentencia: " + e.getMessage(),
                Map.of("sql", sql, "sqlState", e.getSQLState()));
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "executeStatement",
                "Error ejecutando sentencia SQL: " + e.getMessage()
            );
        }
    }

    /**
     * Obtiene el valor de una columna específica del último resultado de query.
     *
     * @param queryResult Map con el resultado de la última query
     * @param columnName Nombre de la columna a extraer
     * @return Valor de la columna o null si no existe
     */
    public static Object getColumnValue(Map<String, Object> queryResult, String columnName) {
        if (queryResult == null || queryResult.isEmpty()) {
            TestLogger.logWarning("DB_HELPER",
                "No hay resultados previos de query", null);
            return null;
        }

        // Buscar columna (case-insensitive)
        for (Map.Entry<String, Object> entry : queryResult.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(columnName)) {
                Object value = entry.getValue();
                TestLogger.logInfo("DB_HELPER",
                    "Valor de columna extraído",
                    Map.of("columna", columnName, "valor", value != null ? value.toString() : "null"));
                return value;
            }
        }

        TestLogger.logWarning("DB_HELPER",
            "Columna no encontrada en resultado",
            Map.of("columnaBuscada", columnName, "columnasDisponibles", queryResult.keySet()));
        return null;
    }

    /**
     * Valida si el resultado de una query tiene datos.
     *
     * @param queryResult Map con el resultado de la query
     * @return true si hay resultados, false si está vacío
     */
    public static boolean hasResults(Map<String, Object> queryResult) {
        if (queryResult == null) {
            return false;
        }

        Object hasResultsFlag = queryResult.get("_hasResults");
        if (hasResultsFlag instanceof Boolean) {
            return (Boolean) hasResultsFlag;
        }

        // Fallback: verificar si hay más keys que los metadatos
        return queryResult.size() > 2; // Más que _rowCount y _hasResults
    }

    /**
     * Obtiene el conteo de filas del último resultado de query.
     *
     * @param queryResult Map con el resultado de la query
     * @return Número de filas encontradas
     */
    public static int getRowCount(Map<String, Object> queryResult) {
        if (queryResult == null) {
            return 0;
        }

        Object rowCount = queryResult.get("_rowCount");
        if (rowCount instanceof Integer) {
            return (Integer) rowCount;
        }

        return 0;
    }

    /**
     * Valida que el resultado de una query tenga datos.
     * Lanza excepción si no hay resultados.
     *
     * @param queryResult Map con el resultado de la query
     * @throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException Si no hay resultados
     */
    public static void validateHasResults(Map<String, Object> queryResult)
            throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException {
        if (queryResult == null) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateHasResults",
                "No hay resultados previos de query. Ejecuta primero: When ejecuto la consulta \"...\""
            );
        }

        if (!hasResults(queryResult)) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateHasResults",
                "Se esperaba que la consulta retorne resultados, pero retornó 0 filas"
            );
        }

        int rowCount = getRowCount(queryResult);
        TestLogger.logInfo("DB_HELPER",
            "✅ Validación exitosa: La consulta retornó resultados",
            Map.of("rowCount", rowCount));
    }

    /**
     * Valida que el resultado de una query NO tenga datos.
     * Lanza excepción si hay resultados.
     *
     * @param queryResult Map con el resultado de la query
     * @throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException Si hay resultados
     */
    public static void validateNoResults(Map<String, Object> queryResult)
            throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException {
        if (queryResult == null) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateNoResults",
                "No hay resultados previos de query. Ejecuta primero: When ejecuto la consulta \"...\""
            );
        }

        if (hasResults(queryResult)) {
            int rowCount = getRowCount(queryResult);
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateNoResults",
                "Se esperaba que la consulta NO retorne resultados, pero retornó " + rowCount + " fila(s)"
            );
        }

        TestLogger.logInfo("DB_HELPER",
            "✅ Validación exitosa: La consulta no retornó resultados", null);
    }

    /**
     * Valida que el valor de una columna sea igual al esperado.
     *
     * @param queryResult Map con el resultado de la query
     * @param columnName Nombre de la columna a validar
     * @param expectedValue Valor esperado
     * @throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException Si el valor no coincide
     */
    public static void validateColumnValue(Map<String, Object> queryResult, String columnName, String expectedValue)
            throws com.scotia.qa.common.http.exceptions.FrameworkBusinessException {
        if (queryResult == null || queryResult.isEmpty()) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateColumnValue",
                "No hay resultados previos de query. Ejecuta primero: When ejecuto la consulta \"...\""
            );
        }

        Object actualValue = getColumnValue(queryResult, columnName);

        if (actualValue == null) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateColumnValue",
                "La columna '" + columnName + "' no existe en el resultado o es null"
            );
        }

        String actualValueStr = actualValue.toString();
        if (!expectedValue.equals(actualValueStr)) {
            throw new com.scotia.qa.common.http.exceptions.FrameworkBusinessException(
                "validateColumnValue",
                "El valor de la columna '" + columnName + "' no coincide. " +
                "Esperado: '" + expectedValue + "', Actual: '" + actualValueStr + "'"
            );
        }

        TestLogger.logInfo("DB_HELPER",
            "✅ Validación exitosa: Columna tiene el valor esperado",
            Map.of("columna", columnName, "valorEsperado", expectedValue, "valorActual", actualValueStr));
    }
}

