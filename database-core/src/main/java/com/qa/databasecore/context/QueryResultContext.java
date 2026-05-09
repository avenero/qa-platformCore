package com.qa.databasecore.context;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.common.exception.FrameworkBusinessException;
import com.qa.common.logging.TestLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the full result set of a SQL query executed during a scenario step.
 *
 * <p>Unlike {@link com.qa.databasecore.helper.DatabaseHelper#executeQuery}, which only
 * captures the first row, {@code QueryResultContext} stores every row so that THEN
 * steps can assert on arbitrary rows (e.g. {@code row 3}).
 *
 * <p>Instances are stored in the Cucumber {@link com.qa.common.runtime.ExecutionContext}
 * under the key {@code __queryResultContext}.
 *
 * @since 3.0.0
 */
public final class QueryResultContext {

    private final List<Map<String, Object>> rows;

    private QueryResultContext(List<Map<String, Object>> rows) {
        this.rows = Collections.unmodifiableList(rows);
    }

    /**
     * Executes {@code query} using {@code connector} and returns a populated context.
     *
     * @param connector active database connector (must not be null)
     * @param query     SQL SELECT statement
     * @param params    comma-separated positional parameters, or null/empty for none
     * @throws FrameworkBusinessException on missing connector or SQL error
     */
    public static QueryResultContext execute(DatabaseConnector connector, String query, String params)
            throws FrameworkBusinessException {
        if (connector == null) {
            throw new FrameworkBusinessException("QueryResultContext.execute",
                "No active connection. Use: Given I connect to the \"<type>\" database \"<name>\"");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (params != null && !params.isBlank()) {
                String[] parts = params.split(",");
                for (int i = 0; i < parts.length; i++) {
                    stmt.setObject(i + 1, parts[i].trim());
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new FrameworkBusinessException("QueryResultContext.execute",
                "SQL error: " + e.getMessage());
        }

        TestLogger.logInfo("QUERY_RESULT_CTX",
            "Query returned " + rows.size() + " row(s)", null);
        return new QueryResultContext(rows);
    }

    /** Returns all rows (immutable). */
    public List<Map<String, Object>> getRows() { return rows; }

    /** Returns the total number of rows. */
    public int getRowCount() { return rows.size(); }

    /** Returns {@code true} when at least one row was returned. */
    public boolean hasResults() { return !rows.isEmpty(); }

    /**
     * Returns the value of {@code column} in the given 1-indexed {@code row}.
     *
     * @throws FrameworkBusinessException if the row or column does not exist
     */
    public Object getValue(int row, String column) throws FrameworkBusinessException {
        if (row < 1 || row > rows.size()) {
            throw new FrameworkBusinessException("QueryResultContext.getValue",
                String.format("Row %d does not exist (result has %d row(s))", row, rows.size()));
        }
        Map<String, Object> rowMap = rows.get(row - 1);
        for (Map.Entry<String, Object> entry : rowMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        throw new FrameworkBusinessException("QueryResultContext.getValue",
            String.format("Column '%s' not found in row %d. Available: %s",
                column, row, rowMap.keySet()));
    }

    /**
     * Convenience: returns the value of {@code column} in row 1.
     *
     * @throws FrameworkBusinessException if no rows returned or column missing
     */
    public Object getFirstValue(String column) throws FrameworkBusinessException {
        return getValue(1, column);
    }
}
