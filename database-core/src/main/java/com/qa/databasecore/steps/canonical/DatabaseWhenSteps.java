package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.databasecore.context.QueryResultContext;
import com.qa.databasecore.helper.DatabaseHelper;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.java.en.When;

/**
 * Canonical English database step vocabulary — WHEN phase (query and DML execution).
 *
 * <p>TASK-D08: introduces stable English execution steps. Existing Spanish step definitions
 * in {@link com.qa.databasecore.steps.DatabaseConnectionSteps} are kept for backward compatibility.
 *
 * <p>Every SELECT step stores two context variables:
 * <ul>
 *   <li>{@code __queryResultContext} — {@link QueryResultContext} with all rows
 *       (used by canonical THEN steps for row-indexed assertions)</li>
 *   <li>{@code queryResult} — {@code Map&lt;String,Object&gt;} first row + metadata
 *       (backward compatibility with Spanish THEN steps)</li>
 * </ul>
 *
 * <p>Every DML step stores {@code rowsAffected} as an {@code int}.
 *
 * @since 3.0.0
 */
public class DatabaseWhenSteps {

    // =========================================================================
    // SELECT — inline string
    // =========================================================================

    /**
     * Executes a SQL query without parameters.
     *
     * <p>Example:
     * <pre>
     * When I query "SELECT * FROM users WHERE status = 'ACTIVE'"
     * </pre>
     */
    @When("I query {string}")
    public void iQuery(String query) throws FrameworkBusinessException {
        iQueryWithParams(query, null);
    }

    /**
     * Executes a SQL query with positional parameters.
     *
     * <p>Example:
     * <pre>
     * When I query "SELECT * FROM users WHERE id = ?" with parameters "42"
     * When I query "SELECT * FROM orders WHERE user_id = ? AND status = ?"
     *      with parameters "42,ACTIVE"
     * </pre>
     */
    @When("I query {string} with parameters {string}")
    public void iQueryWithParams(String query, String params) throws FrameworkBusinessException {
        DatabaseConnector connector = requireConnector();
        QueryResultContext ctx = QueryResultContext.execute(connector, resolve(query), resolve(params));
        storeQueryResult(ctx);
    }

    // =========================================================================
    // SELECT — DocString body
    // =========================================================================

    /**
     * Executes a multi-line SQL query supplied as a DocString.
     *
     * <p>Example:
     * <pre>
     * When I query:
     *   """
     *   SELECT u.id, u.email, a.balance
     *   FROM   users u
     *   JOIN   accounts a ON a.user_id = u.id
     *   WHERE  u.status = 'ACTIVE'
     *   """
     * </pre>
     */
    @When("I query:")
    public void iQueryDocString(String query) throws FrameworkBusinessException {
        iQueryWithParams(query.strip(), null);
    }

    // =========================================================================
    // DML — inline string
    // =========================================================================

    /**
     * Executes a DML statement (INSERT / UPDATE / DELETE) without parameters.
     *
     * <p>Example:
     * <pre>
     * When I execute "DELETE FROM temp_data WHERE created_at &lt; '2025-01-01'"
     * </pre>
     */
    @When("I execute {string}")
    public void iExecute(String sql) throws FrameworkBusinessException {
        iExecuteWithParams(sql, null);
    }

    /**
     * Executes a DML statement with positional parameters.
     *
     * <p>Example:
     * <pre>
     * When I execute "UPDATE users SET status = ? WHERE id = ?" with parameters "INACTIVE,42"
     * </pre>
     */
    @When("I execute {string} with parameters {string}")
    public void iExecuteWithParams(String sql, String params) throws FrameworkBusinessException {
        DatabaseConnector connector = requireConnector();
        int affected = DatabaseHelper.executeStatement(connector, resolve(sql), resolve(params));
        ExecutionContext.requireCurrent().variables().set("rowsAffected", affected);
    }

    // =========================================================================
    // DML — DocString body
    // =========================================================================

    /**
     * Executes a multi-line DML statement supplied as a DocString.
     *
     * <p>Example:
     * <pre>
     * When I execute:
     *   """
     *   INSERT INTO audit_log (event, user_id, created_at)
     *   VALUES ('LOGIN', 42, NOW())
     *   """
     * </pre>
     */
    @When("I execute:")
    public void iExecuteDocString(String sql) throws FrameworkBusinessException {
        iExecuteWithParams(sql.strip(), null);
    }

    // =========================================================================
    // Error-capturing DML (negative testing)
    // =========================================================================

    /**
     * Executes a DML statement that is expected to fail, capturing the SQL state
     * for assertion. Required for testing constraint violations (FK, UK, NOT NULL).
     *
     * <p>On success, the assertion {@code the previous query should have failed
     * with SQL state ...} will fail because no state was captured.
     */
    @When("I try to execute {string} expecting failure")
    public void iTryToExecuteExpectingFailure(String sql) throws FrameworkBusinessException {
        DatabaseConnector connector = requireConnector();
        try (java.sql.Connection conn = connector.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            ExecutionContext.requireCurrent().variables().set("__lastSqlState", "");
        } catch (java.sql.SQLException e) {
            String state = e.getSQLState() == null ? "" : e.getSQLState();
            ExecutionContext.requireCurrent().variables().set("__lastSqlState", state);
        }
    }

    // =========================================================================
    // Stored procedure
    // =========================================================================

    /**
     * Calls a stored procedure via a {@code CALL} statement.
     *
     * <p>Example:
     * <pre>
     * When I call the stored procedure "cleanup_expired_tokens()"
     * </pre>
     */
    @When("I call the stored procedure {string}")
    public void iCallTheStoredProcedure(String procedure) throws FrameworkBusinessException {
        iExecute("CALL " + procedure);
    }

    // =========================================================================
    // Transaction control
    // =========================================================================

    /**
     * Begins an explicit transaction by disabling auto-commit on the active connection.
     *
     * <p>Example:
     * <pre>
     * When I begin a transaction
     * </pre>
     */
    @When("I begin a transaction")
    public void iBeginATransaction() throws FrameworkBusinessException {
        try {
            requireConnector().getConnection().setAutoCommit(false);
        } catch (java.sql.SQLException e) {
            throw new FrameworkBusinessException("iBeginATransaction", e.getMessage());
        }
    }

    /**
     * Commits the active transaction.
     *
     * <p>Example:
     * <pre>
     * When I commit the transaction
     * </pre>
     */
    @When("I commit the transaction")
    public void iCommitTheTransaction() throws FrameworkBusinessException {
        try {
            java.sql.Connection conn = requireConnector().getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (java.sql.SQLException e) {
            throw new FrameworkBusinessException("iCommitTheTransaction", e.getMessage());
        }
    }

    /**
     * Rolls back the active transaction.
     *
     * <p>Example:
     * <pre>
     * When I rollback the transaction
     * </pre>
     */
    @When("I rollback the transaction")
    public void iRollbackTheTransaction() throws FrameworkBusinessException {
        try {
            java.sql.Connection conn = requireConnector().getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (java.sql.SQLException e) {
            throw new FrameworkBusinessException("iRollbackTheTransaction", e.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Resolves {@code ${VAR}} placeholders against the active ExecutionContext variable store. */
    private static String resolve(String raw) {
        if (raw == null) { return null; }
        return ExecutionContext.current()
            .map(ctx -> ctx.variables().resolve(raw))
            .orElse(raw);
    }

    private DatabaseConnector requireConnector() throws FrameworkBusinessException {
        return ExecutionContext.requireCurrent().variables()
            .require("currentDbConnector", DatabaseConnector.class);
    }

    private void storeQueryResult(QueryResultContext ctx) throws FrameworkBusinessException {
        ExecutionContext.requireCurrent().variables().set("__queryResultContext", ctx);
        // backward-compat: build the legacy Map without re-running the query
        java.util.Map<String, Object> legacy = new java.util.HashMap<>();
        if (ctx.hasResults()) {
            legacy.putAll(ctx.getRows().get(0));
        }
        legacy.put("_rowCount", ctx.getRowCount());
        legacy.put("_hasResults", ctx.hasResults());
        ExecutionContext.requireCurrent().variables().set("queryResult", legacy);
        ExecutionContext.requireCurrent().variables().set("queryRowCount", ctx.getRowCount());
    }
}
