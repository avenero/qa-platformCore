package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.databasecore.factory.DbConnectorFactory;
import com.qa.databasecore.helper.DatabaseHelper;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.ExecutionContext;
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

    /**
     * Connects to a database passing all connection parameters inline. No external
     * config file required — useful for self-contained feature files and ad-hoc
     * scenarios across any backend.
     *
     * <p>Supported {@code dbType} values (case-insensitive):
     * {@code oracle}, {@code postgresql} (alias {@code postgres}), {@code mysql},
     * {@code sqlserver} (aliases {@code sql-server}, {@code mssql}), {@code h2}.
     *
     * <p>The {@code password} parameter supports {@code ${ENV_VAR}} interpolation
     * via the framework {@code VariableInterpolator}; raw passwords in feature
     * files should be avoided in favor of environment variables.
     *
     * <p>Example:
     * <pre>
     * Given I connect to the "postgresql" database with url "jdbc:postgresql://localhost:5432/qa"
     *       user "qa_user" password "${QA_DB_PASS}"
     * </pre>
     */
    @Given("I connect to the {string} database with url {string} user {string} password {string}")
    public void iConnectToTheDatabaseWithParams(String dbType, String jdbcUrl,
                                                 String user, String password)
            throws FrameworkBusinessException {
        dbType   = resolvePlaceholders(dbType);
        jdbcUrl  = resolvePlaceholders(jdbcUrl);
        user     = resolvePlaceholders(user);
        password = resolvePlaceholders(password);
        if (dbType == null || dbType.isBlank()) {
            throw new FrameworkBusinessException("iConnectToTheDatabaseWithParams",
                "dbType cannot be null or blank");
        }
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new FrameworkBusinessException("iConnectToTheDatabaseWithParams",
                "jdbcUrl cannot be null or blank");
        }
        String type = dbType.toLowerCase().trim();
        DatabaseConnector connector = switch (type) {
            case "oracle" -> DbConnectorFactory.getOracleConnector(jdbcUrl, user, password);
            case "postgresql", "postgres" -> DbConnectorFactory.getPostgreSQLConnector(jdbcUrl, user, password);
            case "mysql" -> DbConnectorFactory.getMySQLConnector(jdbcUrl, user, password);
            case "sqlserver", "sql-server", "mssql" ->
                    DbConnectorFactory.getSQLServerConnector(jdbcUrl, user, password);
            case "h2" -> DbConnectorFactory.getH2Connector(jdbcUrl, user, password);
            default -> throw new FrameworkBusinessException("iConnectToTheDatabaseWithParams",
                "Unsupported dbType '" + dbType + "'. Supported: oracle, postgresql, mysql, sqlserver, h2");
        };
        ExecutionContext.requireCurrent().variables().set("currentDbConnector", connector);
        ExecutionContext.requireCurrent().variables().set("currentDbType", type);
    }

    /**
     * Connects to a database using a DataTable carrying all connection params.
     * Cleaner alternative when the connection definition is long or has comments.
     *
     * <p>Expected columns: {@code type | url | user | password} (order-independent;
     * accepts Spanish aliases {@code tipo}, {@code usuario}, {@code contraseña}).
     *
     * <p>Example:
     * <pre>
     * Given I connect to a database with parameters:
     *   | type     | url                                   | user    | password      |
     *   | postgres | jdbc:postgresql://localhost:5432/qa   | qa_user | ${QA_DB_PASS} |
     * </pre>
     */
    @Given("I connect to a database with parameters:")
    public void iConnectToADatabaseWithParameters(DataTable params)
            throws FrameworkBusinessException {
        List<List<String>> raw = params.cells();
        if (raw.size() != 2) {
            throw new FrameworkBusinessException("iConnectToADatabaseWithParameters",
                "Expected exactly 1 header row + 1 data row, got " + raw.size() + " row(s) total");
        }
        List<String> headers = raw.get(0);
        List<String> values  = raw.get(1);
        Map<String, String> row = new java.util.LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? values.get(i) : null);
        }
        String type = lookupParam(row, "type", "tipo", "dbType");
        String url  = lookupParam(row, "url", "jdbcUrl");
        String user = lookupParam(row, "user", "username", "usuario");
        String pass = lookupParam(row, "password", "pass", "contraseña");
        iConnectToTheDatabaseWithParams(type, url, user, pass);
    }

    /** Resolves {@code ${VAR}} placeholders against the active ExecutionContext variable store. */
    private static String resolvePlaceholders(String raw) {
        if (raw == null) { return null; }
        return ExecutionContext.current()
            .map(ctx -> ctx.variables().resolve(raw))
            .orElse(raw);
    }

    private static String lookupParam(Map<String, String> row, String... aliases)
            throws FrameworkBusinessException {
        for (String a : aliases) {
            for (Map.Entry<String, String> e : row.entrySet()) {
                if (e.getKey().equalsIgnoreCase(a)) { return e.getValue(); }
            }
        }
        throw new FrameworkBusinessException("lookupParam",
            "Missing column in connection DataTable. Expected one of: "
            + String.join(", ", aliases) + ". Got: " + row.keySet());
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
