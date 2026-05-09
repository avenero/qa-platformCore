package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.databasecore.factory.DbConnectorFactory;
import com.qa.databasecore.helper.DatabaseHelper;
import com.qa.common.exception.FrameworkBusinessException;
import com.qa.common.runtime.ExecutionContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;

import java.util.List;
import java.util.Map;

/**
 * Canonical English database step vocabulary — GIVEN phase (connection and fixture setup).
 *
 * <p>TASK-D08: introduces stable English setup steps. Existing Spanish step definitions
 * in {@link DatabaseConnectionSteps} are kept for backward compatibility.
 *
 * <p>Connection name resolution: the {@code connectionName} parameter is used as the
 * cache key AND as the config property prefix ({@code {connectionName}.db.url/username/password}).
 * The {@code dbType} parameter is informational (e.g. "oracle", "postgresql") and drives
 * no logic in the current implementation — it exists for documentation clarity in feature files.
 *
 * <p>Example config-{env}.properties for connection name "reporting":
 * <pre>
 * reporting.db.url=jdbc:postgresql://host:5432/reports
 * reporting.db.username=qa_user
 * reporting.db.password=${REPORT_DB_PASS}
 * </pre>
 *
 * @since 3.0.0
 */
public class DatabaseGivenSteps {

    // =========================================================================
    // Connection
    // =========================================================================

    /**
     * Connects to a database and stores the connector under {@code connectionName}.
     *
     * <p>Examples:
     * <pre>
     * Given I connect to the "postgresql" database "reporting"
     * Given I connect to the "oracle" database "prod"
     * </pre>
     *
     * @param dbType         informational label (oracle/postgresql/mysql/sqlserver)
     * @param connectionName config property prefix and cache key
     */
    @Given("I connect to the {string} database {string}")
    public void iConnectToTheDatabase(String dbType, String connectionName)
            throws FrameworkBusinessException {
        DatabaseConnector connector = DbConnectorFactory.connectAndCache(connectionName);
        ExecutionContext.requireCurrent().variables().set("currentDbConnector", connector);
        ExecutionContext.requireCurrent().variables().set("currentDbType", connectionName);
    }

    // =========================================================================
    // Table fixture helpers
    // =========================================================================

    /**
     * Empties the given table by executing {@code DELETE FROM {table}}.
     *
     * <p>Example:
     * <pre>
     * Given the "audit_log" table is empty
     * </pre>
     */
    @Given("the {string} table is empty")
    public void theTableIsEmpty(String table) throws FrameworkBusinessException {
        DatabaseConnector connector = requireConnector();
        DatabaseHelper.executeStatement(connector, "DELETE FROM " + table, null);
    }

    /**
     * Inserts the rows from the DataTable into the given table using
     * {@code INSERT INTO {table} ({cols}) VALUES ({placeholders})}.
     *
     * <p>The DataTable header row becomes the column names. All values are bound
     * as strings via {@code PreparedStatement} to prevent SQL injection.
     *
     * <p>Example:
     * <pre>
     * Given the "users" table contains:
     *   | id | name  | status |
     *   | 1  | Alice | ACTIVE |
     *   | 2  | Bob   | ACTIVE |
     * </pre>
     */
    @Given("the {string} table contains:")
    public void theTableContains(String table, DataTable dataTable) throws FrameworkBusinessException {
        DatabaseConnector connector = requireConnector();
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        if (rows.isEmpty()) {
            return;
        }
        List<String> columns = new java.util.ArrayList<>(rows.get(0).keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
        String sql = "INSERT INTO " + table
            + " (" + String.join(", ", columns) + ")"
            + " VALUES (" + placeholders + ")";

        for (Map<String, String> row : rows) {
            String params = columns.stream()
                .map(row::get)
                .collect(java.util.stream.Collectors.joining(","));
            DatabaseHelper.executeStatement(connector, sql, params);
        }
    }

    // =========================================================================
    // SQL script / statement setup
    // =========================================================================

    /**
     * Reads and executes a SQL script file (one statement per line; blank lines and
     * lines starting with {@code --} are skipped).
     *
     * <p>Example:
     * <pre>
     * Given I execute the SQL script "src/test/resources/fixtures/setup.sql"
     * </pre>
     */
    @Given("I execute the SQL script {string}")
    public void iExecuteTheSqlScript(String filePath) throws FrameworkBusinessException {
        DatabaseConnector connector = requireConnector();
        try {
            String content = java.nio.file.Files.readString(java.nio.file.Path.of(filePath));
            for (String line : content.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    DatabaseHelper.executeStatement(connector, trimmed, null);
                }
            }
        } catch (java.io.IOException e) {
            throw new FrameworkBusinessException("iExecuteTheSqlScript",
                "Could not read SQL script '" + filePath + "': " + e.getMessage());
        }
    }

    /**
     * Executes a single SQL DML statement as part of test setup.
     *
     * <p>Example:
     * <pre>
     * Given I run the SQL statement "DELETE FROM temp_tokens WHERE expires_at &lt; NOW()"
     * </pre>
     */
    @Given("I run the SQL statement {string}")
    public void iRunTheSqlStatement(String sql) throws FrameworkBusinessException {
        DatabaseHelper.executeStatement(requireConnector(), sql, null);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private DatabaseConnector requireConnector() throws FrameworkBusinessException {
        return ExecutionContext.requireCurrent().variables()
            .require("currentDbConnector", DatabaseConnector.class);
    }
}
