package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.databasecore.context.QueryResultContext;
import com.qa.databasecore.factory.DbConnectorFactory;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Canonical English database step vocabulary — THEN phase (result assertions).
 *
 * <p>TASK-D08: introduces stable English assertion steps aligned with industry
 * conventions. Existing Spanish step definitions in {@link com.qa.databasecore.steps.DatabaseConnectionSteps}
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
    // Numeric comparisons — first row
    // =========================================================================

    @Then("the {string} column should be greater than {string}")
    public void theColumnShouldBeGreaterThan(String column, String value) throws FrameworkBusinessException {
        assertCompare(column, 1, value, Compare.GT);
    }

    @Then("the {string} column should be less than {string}")
    public void theColumnShouldBeLessThan(String column, String value) throws FrameworkBusinessException {
        assertCompare(column, 1, value, Compare.LT);
    }

    @Then("the {string} column should be greater than or equal to {string}")
    public void theColumnShouldBeGreaterThanOrEqual(String column, String value) throws FrameworkBusinessException {
        assertCompare(column, 1, value, Compare.GTE);
    }

    @Then("the {string} column should be less than or equal to {string}")
    public void theColumnShouldBeLessThanOrEqual(String column, String value) throws FrameworkBusinessException {
        assertCompare(column, 1, value, Compare.LTE);
    }

    @Then("the {string} column should be between {string} and {string}")
    public void theColumnShouldBeBetween(String column, String lo, String hi) throws FrameworkBusinessException {
        BigDecimal actual = numericOrFail(requireContext().getFirstValue(column), column, 1);
        BigDecimal low = parseNumeric(lo);
        BigDecimal high = parseNumeric(hi);
        Assertions.assertThat(actual)
            .as("Column '%s' (row 1) between %s and %s", column, lo, hi)
            .isBetween(low, high);
    }

    // =========================================================================
    // Numeric comparisons — row-indexed
    // =========================================================================

    @Then("the {string} column in row {int} should be greater than {string}")
    public void theColumnInRowShouldBeGreaterThan(String column, int row, String value)
            throws FrameworkBusinessException {
        assertCompare(column, row, value, Compare.GT);
    }

    @Then("the {string} column in row {int} should be less than {string}")
    public void theColumnInRowShouldBeLessThan(String column, int row, String value)
            throws FrameworkBusinessException {
        assertCompare(column, row, value, Compare.LT);
    }

    @Then("the {string} column in row {int} should be greater than or equal to {string}")
    public void theColumnInRowShouldBeGreaterThanOrEqual(String column, int row, String value)
            throws FrameworkBusinessException {
        assertCompare(column, row, value, Compare.GTE);
    }

    @Then("the {string} column in row {int} should be less than or equal to {string}")
    public void theColumnInRowShouldBeLessThanOrEqual(String column, int row, String value)
            throws FrameworkBusinessException {
        assertCompare(column, row, value, Compare.LTE);
    }

    @Then("the {string} column in row {int} should be between {string} and {string}")
    public void theColumnInRowShouldBeBetween(String column, int row, String lo, String hi)
            throws FrameworkBusinessException {
        BigDecimal actual = numericOrFail(requireContext().getValue(row, column), column, row);
        Assertions.assertThat(actual)
            .as("Column '%s' (row %d) between %s and %s", column, row, lo, hi)
            .isBetween(parseNumeric(lo), parseNumeric(hi));
    }

    // =========================================================================
    // Pattern / case-insensitive
    // =========================================================================

    @Then("the {string} column should match pattern {string}")
    public void theColumnShouldMatchPattern(String column, String regex) throws FrameworkBusinessException {
        Object value = requireContext().getFirstValue(column);
        String actual = value == null ? "" : value.toString();
        Assertions.assertThat(actual)
            .as("Column '%s' (row 1) should match pattern %s", column, regex)
            .matches(Pattern.compile(regex));
    }

    @Then("the {string} column should equal {string} ignoring case")
    public void theColumnShouldEqualIgnoringCase(String column, String expected) throws FrameworkBusinessException {
        Object value = requireContext().getFirstValue(column);
        Assertions.assertThat(value == null ? null : value.toString())
            .as("Column '%s' (row 1) equalsIgnoreCase '%s'", column, expected)
            .isEqualToIgnoringCase(expected);
    }

    @Then("the {string} column should contain {string} ignoring case")
    public void theColumnShouldContainIgnoringCase(String column, String substring)
            throws FrameworkBusinessException {
        Object value = requireContext().getFirstValue(column);
        String actual = value == null ? "" : value.toString();
        Assertions.assertThat(actual)
            .as("Column '%s' (row 1) containsIgnoringCase '%s'", column, substring)
            .containsIgnoringCase(substring);
    }

    // =========================================================================
    // NULL / empty
    // =========================================================================

    @Then("the {string} column should be null")
    public void theColumnShouldBeNull(String column) throws FrameworkBusinessException {
        Object value = requireContext().getFirstValue(column);
        Assertions.assertThat(value)
            .as("Column '%s' (row 1) should be NULL", column)
            .isNull();
    }

    @Then("the {string} column should not be empty")
    public void theColumnShouldNotBeEmpty(String column) throws FrameworkBusinessException {
        Object value = requireContext().getFirstValue(column);
        Assertions.assertThat(value)
            .as("Column '%s' (row 1) should not be null", column)
            .isNotNull();
        Assertions.assertThat(value.toString())
            .as("Column '%s' (row 1) should not be empty", column)
            .isNotEmpty();
    }

    // =========================================================================
    // Multi-row predicates
    // =========================================================================

    @Then("every row {string} column should be greater than {string}")
    public void everyRowColumnShouldBeGreaterThan(String column, String value)
            throws FrameworkBusinessException {
        BigDecimal threshold = parseNumeric(value);
        QueryResultContext ctx = requireContext();
        for (int i = 1; i <= ctx.getRowCount(); i++) {
            BigDecimal actual = numericOrFail(ctx.getValue(i, column), column, i);
            Assertions.assertThat(actual)
                .as("Column '%s' in row %d should be greater than %s", column, i, value)
                .isGreaterThan(threshold);
        }
    }

    @Then("at least one row {string} column should equal {string}")
    public void atLeastOneRowColumnShouldEqual(String column, String expected)
            throws FrameworkBusinessException {
        QueryResultContext ctx = requireContext();
        for (int i = 1; i <= ctx.getRowCount(); i++) {
            Object v = ctx.getValue(i, column);
            if (v != null && v.toString().equals(expected)) {
                return;
            }
        }
        Assertions.fail("No row had column '%s' equal to '%s' (rows checked: %d)",
            column, expected, ctx.getRowCount());
    }

    // =========================================================================
    // Schema — column existence / type
    // =========================================================================

    @Then("the {string} table should have column {string}")
    public void theTableShouldHaveColumn(String tableName, String columnName) throws FrameworkBusinessException {
        Assertions.assertThat(findColumn(tableName, columnName).isPresent())
            .as("Table '%s' should have column '%s'", tableName, columnName)
            .isTrue();
    }

    @Then("the {string} column in table {string} should be of type {string}")
    public void theColumnInTableShouldBeOfType(String columnName, String tableName, String expectedType)
            throws FrameworkBusinessException {
        String actualType = findColumn(tableName, columnName)
            .orElseThrow(() -> new FrameworkBusinessException("theColumnInTableShouldBeOfType",
                "Column '" + columnName + "' not found in table '" + tableName + "'"));
        Assertions.assertThat(normalizeSqlType(actualType))
            .as("Column '%s.%s' type (expected family: %s, actual: %s)",
                tableName, columnName, expectedType, actualType)
            .isEqualTo(normalizeSqlType(expectedType));
    }

    /**
     * Maps SQL-standard type synonyms to a canonical family so assertions are
     * portable across vendors (DECIMAL ≡ NUMERIC; INTEGER ≡ INT; VARCHAR ≡
     * CHARACTER VARYING; TIMESTAMP ≡ TIMESTAMP WITHOUT TIME ZONE; …).
     */
    private static String normalizeSqlType(String raw) {
        if (raw == null) { return null; }
        String t = raw.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (t) {
            case "DECIMAL", "NUMERIC", "NUMBER"                        -> "NUMERIC";
            case "INT", "INTEGER", "INT4"                              -> "INTEGER";
            case "BIGINT", "INT8", "LONG"                              -> "BIGINT";
            case "SMALLINT", "INT2"                                    -> "SMALLINT";
            case "VARCHAR", "CHARACTER VARYING", "VARCHAR2"            -> "VARCHAR";
            case "CHAR", "CHARACTER", "BPCHAR"                         -> "CHAR";
            case "TEXT", "CLOB", "LONGTEXT"                            -> "TEXT";
            case "BOOLEAN", "BOOL", "BIT"                              -> "BOOLEAN";
            case "DATE"                                                -> "DATE";
            case "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE", "DATETIME" -> "TIMESTAMP";
            case "TIMESTAMPTZ", "TIMESTAMP WITH TIME ZONE"             -> "TIMESTAMPTZ";
            case "TIME", "TIME WITHOUT TIME ZONE"                      -> "TIME";
            case "BYTEA", "BLOB", "VARBINARY", "RAW"                   -> "BINARY";
            case "UUID"                                                -> "UUID";
            case "JSON", "JSONB"                                       -> "JSON";
            default -> t;
        };
    }

    // =========================================================================
    // Extraction
    // =========================================================================

    @Then("I store all {string} column values as list {string}")
    public void iStoreAllColumnValuesAsList(String column, String variableName) throws FrameworkBusinessException {
        QueryResultContext ctx = requireContext();
        List<Object> values = new ArrayList<>();
        for (int i = 1; i <= ctx.getRowCount(); i++) {
            values.add(ctx.getValue(i, column));
        }
        ExecutionContext.requireCurrent().variables().set(variableName, values);
    }

    // =========================================================================
    // Error expectations
    // =========================================================================

    @Then("the previous query should have failed with SQL state {string}")
    public void previousQueryShouldFailWithSqlState(String expectedSqlState) {
        String captured = ExecutionContext.requireCurrent().variables()
            .require("__lastSqlState", String.class);
        Assertions.assertThat(captured)
            .as("Expected previous query to fail with SQL state %s", expectedSqlState)
            .isEqualTo(expectedSqlState);
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

    private enum Compare { GT, LT, GTE, LTE }

    private void assertCompare(String column, int row, String value, Compare op)
            throws FrameworkBusinessException {
        BigDecimal actual = numericOrFail(requireContext().getValue(row, column), column, row);
        BigDecimal threshold = parseNumeric(value);
        var assertion = Assertions.assertThat(actual)
            .as("Column '%s' (row %d) %s %s", column, row, op, value);
        switch (op) {
            case GT  -> assertion.isGreaterThan(threshold);
            case LT  -> assertion.isLessThan(threshold);
            case GTE -> assertion.isGreaterThanOrEqualTo(threshold);
            case LTE -> assertion.isLessThanOrEqualTo(threshold);
            default  -> throw new IllegalStateException("unreachable");
        }
    }

    private static BigDecimal parseNumeric(String raw) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: '" + raw + "'", e);
        }
    }

    private static BigDecimal numericOrFail(Object value, String column, int row) {
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Column '%s' in row %d is NULL, cannot compare numerically", column, row));
        }
        if (value instanceof Number n) { return new BigDecimal(n.toString()); }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Column '%s' in row %d value '%s' is not numeric", column, row, value));
        }
    }

    private java.util.Optional<String> findColumn(String tableName, String columnName)
            throws FrameworkBusinessException {
        DatabaseConnector connector = ExecutionContext.requireCurrent().variables()
            .require("currentDbConnector", DatabaseConnector.class);
        try (Connection conn = connector.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            for (String tableCandidate : new String[]{tableName, tableName.toUpperCase(), tableName.toLowerCase()}) {
                try (ResultSet rs = md.getColumns(null, null, tableCandidate, null)) {
                    while (rs.next()) {
                        if (rs.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
                            return java.util.Optional.of(rs.getString("TYPE_NAME"));
                        }
                    }
                }
            }
            return java.util.Optional.empty();
        } catch (SQLException e) {
            throw new FrameworkBusinessException("findColumn",
                "Error inspecting column '" + tableName + "." + columnName + "': " + e.getMessage());
        }
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
