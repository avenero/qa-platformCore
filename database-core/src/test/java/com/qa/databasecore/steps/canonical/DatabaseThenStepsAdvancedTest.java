package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.databasecore.context.QueryResultContext;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.ExecutionContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the 21 new canonical DB Then steps (Sprint 1 of DB coverage uplift).
 *
 * <p>Covers: numeric comparisons (5+5 row-indexed), pattern/case-insensitive (3),
 * NULL/empty (2), multi-row (2), schema column-existence (2), bulk extraction (1),
 * error capture via SQL state (1 + corresponding {@link DatabaseWhenSteps#iTryToExecuteExpectingFailure}).
 */
@DisplayName("DatabaseThenSteps — Sprint 1 advanced assertions")
class DatabaseThenStepsAdvancedTest {

    private final DatabaseThenSteps thenSteps = new DatabaseThenSteps();
    private final DatabaseWhenSteps whenSteps = new DatabaseWhenSteps();

    private H2Connector connector;

    @BeforeEach
    void setUp() throws SQLException {
        connector = new H2Connector();
        try (Connection conn = connector.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE accounts (id INT PRIMARY KEY, owner VARCHAR(50), balance DECIMAL(12,2), status VARCHAR(20))");
            conn.createStatement().execute("DELETE FROM accounts");
            conn.createStatement().execute("INSERT INTO accounts VALUES (1, 'Alice', 500.00,  'ACTIVE')");
            conn.createStatement().execute("INSERT INTO accounts VALUES (2, 'Bob',   1500.00, 'ACTIVE')");
            conn.createStatement().execute("INSERT INTO accounts VALUES (3, 'Carol', 2500.00, 'inactive')");
            conn.createStatement().execute("INSERT INTO accounts VALUES (4, 'Dave',  null,    'PENDING')");
        }
        ExecutionContext.builder().build().activate();
        ExecutionContext.requireCurrent().variables().set("currentDbConnector", connector);
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
        connector.close();
    }

    private void query(String sql) throws FrameworkBusinessException {
        QueryResultContext ctx = QueryResultContext.execute(connector, sql, null);
        ExecutionContext.requireCurrent().variables().set("__queryResultContext", ctx);
    }

    @Nested
    @DisplayName("Numeric comparisons — first row")
    class NumericFirstRow {

        @Test
        @DisplayName("greater than passes when actual > threshold")
        void greaterThan_pass() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 3");
            thenSteps.theColumnShouldBeGreaterThan("balance", "2000");
        }

        @Test
        @DisplayName("greater than fails when actual <= threshold")
        void greaterThan_fail() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 1");
            assertThatThrownBy(() -> thenSteps.theColumnShouldBeGreaterThan("balance", "1000"))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("between inclusive")
        void between_inclusive() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 2");
            thenSteps.theColumnShouldBeBetween("balance", "1000", "2000");
            thenSteps.theColumnShouldBeBetween("balance", "1500", "1500");
        }

        @Test
        @DisplayName("less than or equal at boundary")
        void lte_boundary() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 1");
            thenSteps.theColumnShouldBeLessThanOrEqual("balance", "500");
        }

        @Test
        @DisplayName("numeric op against NULL throws IllegalArgumentException")
        void numericOp_nullValue_throws() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 4");
            assertThatThrownBy(() -> thenSteps.theColumnShouldBeGreaterThan("balance", "0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NULL");
        }
    }

    @Nested
    @DisplayName("Numeric comparisons — row-indexed")
    class NumericRowIndexed {

        @Test
        @DisplayName("compares specified row correctly")
        void rowIndexed_pass() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE balance IS NOT NULL ORDER BY balance");
            thenSteps.theColumnInRowShouldBeLessThan("balance", 1, "1000");
            thenSteps.theColumnInRowShouldBeBetween("balance", 2, "1000", "2000");
            thenSteps.theColumnInRowShouldBeGreaterThan("balance", 3, "2000");
        }
    }

    @Nested
    @DisplayName("Pattern / case-insensitive")
    class PatternCase {

        @Test
        @DisplayName("regex match")
        void regex_match() throws FrameworkBusinessException {
            query("SELECT owner FROM accounts WHERE id = 1");
            thenSteps.theColumnShouldMatchPattern("owner", "^[A-Z][a-z]+$");
        }

        @Test
        @DisplayName("regex mismatch fails")
        void regex_mismatch() throws FrameworkBusinessException {
            query("SELECT owner FROM accounts WHERE id = 1");
            assertThatThrownBy(() -> thenSteps.theColumnShouldMatchPattern("owner", "^[0-9]+$"))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("case-insensitive equality matches differing case")
        void ignoreCase_equal() throws FrameworkBusinessException {
            query("SELECT status FROM accounts WHERE id = 3");
            thenSteps.theColumnShouldEqualIgnoringCase("status", "INACTIVE");
        }

        @Test
        @DisplayName("case-insensitive contains")
        void ignoreCase_contains() throws FrameworkBusinessException {
            query("SELECT status FROM accounts WHERE id = 1");
            thenSteps.theColumnShouldContainIgnoringCase("status", "activ");
        }
    }

    @Nested
    @DisplayName("NULL / empty")
    class NullEmpty {

        @Test
        @DisplayName("is null passes when value is NULL")
        void null_pass() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 4");
            thenSteps.theColumnShouldBeNull("balance");
        }

        @Test
        @DisplayName("is null fails when value is non-null")
        void null_fail() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE id = 1");
            assertThatThrownBy(() -> thenSteps.theColumnShouldBeNull("balance"))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("not empty passes for populated value")
        void notEmpty_pass() throws FrameworkBusinessException {
            query("SELECT owner FROM accounts WHERE id = 1");
            thenSteps.theColumnShouldNotBeEmpty("owner");
        }
    }

    @Nested
    @DisplayName("Multi-row predicates")
    class MultiRow {

        @Test
        @DisplayName("every row > threshold passes when all rows match")
        void every_pass() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE balance IS NOT NULL");
            thenSteps.everyRowColumnShouldBeGreaterThan("balance", "0");
        }

        @Test
        @DisplayName("every row > threshold fails when one row breaks the predicate")
        void every_fail() throws FrameworkBusinessException {
            query("SELECT balance FROM accounts WHERE balance IS NOT NULL");
            assertThatThrownBy(() -> thenSteps.everyRowColumnShouldBeGreaterThan("balance", "1000"))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("at least one row matches")
        void atLeastOne_pass() throws FrameworkBusinessException {
            query("SELECT status FROM accounts");
            thenSteps.atLeastOneRowColumnShouldEqual("status", "PENDING");
        }

        @Test
        @DisplayName("at least one row fails when none match")
        void atLeastOne_fail() throws FrameworkBusinessException {
            query("SELECT status FROM accounts");
            assertThatThrownBy(() -> thenSteps.atLeastOneRowColumnShouldEqual("status", "DELETED"))
                .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("Schema — column existence/type")
    class Schema {

        @Test
        @DisplayName("table has column passes when column exists")
        void columnExists() throws FrameworkBusinessException {
            thenSteps.theTableShouldHaveColumn("accounts", "balance");
        }

        @Test
        @DisplayName("table has column fails when column missing")
        void columnMissing() {
            assertThatThrownBy(() -> thenSteps.theTableShouldHaveColumn("accounts", "phantom"))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("column type detected (DECIMAL → NUMERIC family)")
        void columnType() throws FrameworkBusinessException {
            thenSteps.theColumnInTableShouldBeOfType("balance", "accounts", "DECIMAL");
        }
    }

    @Nested
    @DisplayName("Extraction")
    class Extraction {

        @Test
        @DisplayName("stores all values of column as list variable")
        @SuppressWarnings("unchecked")
        void storeAllValues() throws FrameworkBusinessException {
            query("SELECT owner FROM accounts ORDER BY id");
            thenSteps.iStoreAllColumnValuesAsList("owner", "owners");
            List<Object> owners = ExecutionContext.requireCurrent().variables().require("owners", List.class);
            assertThat(owners).containsExactly("Alice", "Bob", "Carol", "Dave");
        }
    }

    @Nested
    @DisplayName("Error capture (negative testing)")
    class ErrorCapture {

        @Test
        @DisplayName("captures SQL state on constraint violation (duplicate PK)")
        void captureSqlState_uniqueViolation() throws FrameworkBusinessException {
            whenSteps.iTryToExecuteExpectingFailure(
                "INSERT INTO accounts(id, owner, balance, status) VALUES (1, 'Dup', 0, 'X')");
            thenSteps.previousQueryShouldFailWithSqlState("23505");
        }

        @Test
        @DisplayName("successful query results in empty SQL state, assertion fails")
        void noFailure_blankState() throws FrameworkBusinessException {
            whenSteps.iTryToExecuteExpectingFailure(
                "INSERT INTO accounts(id, owner, balance, status) VALUES (99, 'New', 0, 'X')");
            assertThatThrownBy(() -> thenSteps.previousQueryShouldFailWithSqlState("23505"))
                .isInstanceOf(AssertionError.class);
        }
    }

    // =========================================================================
    // H2 connector (test-only)
    // =========================================================================

    private static final class H2Connector implements DatabaseConnector {
        private final JdbcDataSource ds;

        H2Connector() {
            ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:then_adv_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
        }

        @Override
        public DataSource getDataSource() { return ds; }

        @Override
        public void close() { }
    }
}
