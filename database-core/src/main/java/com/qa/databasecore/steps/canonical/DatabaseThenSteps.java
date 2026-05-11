package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.databasecore.context.QueryResultContext;
import com.qa.databasecore.factory.DbConnectorFactory;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Canonical English database step vocabulary — THEN phase (result assertions).
 *
 * <p>TASK-D08: introduces stable English assertion steps aligned with industry
 * conventions. Existing Spanish step definitions in {@link DatabaseConnectionSteps}
 * are kept for backward compatibility.
 *
 * <p>Row-indexed steps (e.g. {@code row {int}}) use the {@link QueryResultContext} stored
 * by {@link DatabaseWhenSteps} under the key {@code __queryResultContext}. Column-only
 * steps fall back to row 1.
 *
 * @since 3.0.0
 */
public class DatabaseThenSteps {

    // =========================================================================
    // Row count
    // =========================================================================

    @Then("the query should return {int} rows")
    public void theQueryShouldReturnRows(int expected) throws FrameworkBusinessException {
        int actual = requireContext().getRowCount();
        Assertions.assertThat(actual)
            .as("Expected query to return %d row(s) but got %d", expected, actual)
            .isEqualTo(expected);
    }

    @Then("the query should return at least {int} rows")
    public void theQueryShouldReturnAtLeastRows(int minimum) throws FrameworkBusinessException {
        int actual = requireContext().getRowCount();
        Assertions.assertThat(actual)
            .as("Expected query to return at least %d row(s) but got %d", minimum, actual)
            .isGreaterThanOrEqualTo(minimum);
    }

    @Then("the query should return results")
    public void theQueryShouldReturnResults() throws FrameworkBusinessException {
        Assertions.assertThat(requireContext().hasResults())
            .as("Expected the query to return at least one row")
            .isTrue();
    }

    @Then("the query should return no results")
    public void theQueryShouldReturnNoResults() throws FrameworkBusinessException {
        Assertions.assertThat(requireContext().hasResults())
            .as("Expected the query to return no rows")
            .isFalse();
    }

    // =========================================================================
    // Column value — row-indexed
    // =========================================================================

    @Then("the {string} column in row {int} should equal {string}")
    public void theColumnInRowShouldEqual(String column, int row, String expected)
            throws FrameworkBusinessException {
        Object actual = requireContext().getValue(row, column);
        String actualStr = actual == null ? null : actual.toString();
        Assertions.assertThat(actualStr)
            .as("Column '%s' in row %d", column, row)
            .isEqualTo(expected);
    }

    @Then("the {string} column in row {int} should contain {string}")
    public void theColumnInRowShouldContain(String column, int row, String substring)
            throws FrameworkBusinessException {
        Object actual = requireContext().getValue(row, column);
        String actualStr = actual == null ? "" : actual.toString();
        Assertions.assertThat(actualStr)
            .as("Column '%s' in row %d", column, row)
            .contains(substring);
    }

    // =========================================================================
    // Column value — first row shortcuts
    // =========================================================================

    @Then("the {string} column should equal {string}")
    public void theColumnShouldEqual(String column, String expected) throws FrameworkBusinessException {
        Object actual = requireContext().getFirstValue(column);
        String actualStr = actual == null ? null : actual.toString();
        Assertions.assertThat(actualStr)
            .as("Column '%s' (row 1)", column)
            .isEqualTo(expected);
    }

    @Then("the {string} column should not be null")
    public void theColumnShouldNotBeNull(String column) throws FrameworkBusinessException {
        Object actual = requireContext().getFirstValue(column);
        Assertions.assertThat(actual)
            .as("Column '%s' (row 1) should not be null", column)
            .isNotNull();
    }

    @Then("the {string} column should contain {string}")
    public void theColumnShouldContain(String column, String substring) throws FrameworkBusinessException {
        Object actual = requireContext().getFirstValue(column);
        String actualStr = actual == null ? "" : actual.toString();
        Assertions.assertThat(actualStr)
            .as("Column '%s' (row 1)", column)
            .contains(substring);
    }

    // =========================================================================
    // Table existence
    // =========================================================================

    @Then("the table {string} should exist")
    public void theTableShouldExist(String tableName) throws FrameworkBusinessException {
        Assertions.assertThat(tableExists(tableName))
            .as("Expected table '%s' to exist", tableName)
            .isTrue();
    }

    @Then("the table {string} should not exist")
    public void theTableShouldNotExist(String tableName) throws FrameworkBusinessException {
        Assertions.assertThat(tableExists(tableName))
            .as("Expected table '%s' to NOT exist", tableName)
            .isFalse();
    }

    // =========================================================================
    // DML affected rows
    // =========================================================================

    @Then("{int} rows should have been affected")
    public void rowsShouldHaveBeenAffected(int expected) {
        int actual = ExecutionContext.requireCurrent().variables()
            .require("rowsAffected", Integer.class);
        Assertions.assertThat(actual)
            .as("Expected %d row(s) affected but got %d", expected, actual)
            .isEqualTo(expected);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @After
    public void closeConnections() {
        if (DbConnectorFactory.hasActiveConnections()) {
            DbConnectorFactory.disconnectAll();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private QueryResultContext requireContext() throws FrameworkBusinessException {
        return ExecutionContext.requireCurrent().variables()
            .require("__queryResultContext", QueryResultContext.class);
    }

    private boolean tableExists(String tableName) throws FrameworkBusinessException {
        DatabaseConnector connector = ExecutionContext.requireCurrent().variables()
            .require("currentDbConnector", DatabaseConnector.class);
        try (Connection conn = connector.getConnection()) {
            try (ResultSet rs = conn.getMetaData().getTables(
                    null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) { return true; }
            }
            try (ResultSet rs = conn.getMetaData().getTables(
                    null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new FrameworkBusinessException("tableExists",
                "Error checking table existence for '" + tableName + "': " + e.getMessage());
        }
    }
}
