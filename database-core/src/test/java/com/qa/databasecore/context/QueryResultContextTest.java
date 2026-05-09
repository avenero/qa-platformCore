package com.qa.databasecore.context;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.common.exception.FrameworkBusinessException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QueryResultContext — full result set")
class QueryResultContextTest {

    private H2Connector connector;

    @BeforeEach
    void setUp() throws SQLException {
        connector = new H2Connector();
        try (Connection conn = connector.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS items " +
                "(id INT, name VARCHAR(50), status VARCHAR(20))");
            conn.createStatement().execute("DELETE FROM items");
            conn.createStatement().execute(
                "INSERT INTO items VALUES (1, 'Alpha', 'ACTIVE')");
            conn.createStatement().execute(
                "INSERT INTO items VALUES (2, 'Beta',  'ACTIVE')");
            conn.createStatement().execute(
                "INSERT INTO items VALUES (3, 'Gamma', 'INACTIVE')");
        }
    }

    @AfterEach
    void tearDown() {
        connector.close();
    }

    // =========================================================================
    // execute() — null connector guard
    // =========================================================================

    @Test
    @DisplayName("null connector throws FrameworkBusinessException")
    void execute_nullConnector_throws() {
        assertThatThrownBy(() -> QueryResultContext.execute(null, "SELECT 1", null))
            .isInstanceOf(FrameworkBusinessException.class);
    }

    // =========================================================================
    // Row count and hasResults
    // =========================================================================

    @Nested
    @DisplayName("Row count and hasResults")
    class RowCountTests {

        @Test
        @DisplayName("returns all rows from the query")
        void allRows_returned() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items", null);
            assertThat(ctx.getRowCount()).isEqualTo(3);
            assertThat(ctx.hasResults()).isTrue();
        }

        @Test
        @DisplayName("empty result set → rowCount=0, hasResults=false")
        void emptyResult_noRows() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items WHERE status = 'DELETED'", null);
            assertThat(ctx.getRowCount()).isEqualTo(0);
            assertThat(ctx.hasResults()).isFalse();
        }

        @Test
        @DisplayName("parameterized query filters correctly")
        void parameterized_filtersRows() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items WHERE status = ?", "ACTIVE");
            assertThat(ctx.getRowCount()).isEqualTo(2);
        }
    }

    // =========================================================================
    // getValue() — row-indexed access
    // =========================================================================

    @Nested
    @DisplayName("getValue() — row-indexed access")
    class GetValueTests {

        @Test
        @DisplayName("row 1 returns first row values")
        void getValue_row1_firstRow() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items ORDER BY id", null);
            assertThat(ctx.getValue(1, "NAME").toString()).isEqualTo("Alpha");
        }

        @Test
        @DisplayName("row 3 returns third row values")
        void getValue_row3_thirdRow() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items ORDER BY id", null);
            assertThat(ctx.getValue(3, "NAME").toString()).isEqualTo("Gamma");
        }

        @Test
        @DisplayName("column lookup is case-insensitive")
        void getValue_caseInsensitiveColumn() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items ORDER BY id", null);
            assertThat(ctx.getValue(1, "name").toString()).isEqualTo("Alpha");
            assertThat(ctx.getValue(1, "NAME").toString()).isEqualTo("Alpha");
        }

        @Test
        @DisplayName("row 0 throws FrameworkBusinessException")
        void getValue_row0_throws() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items", null);
            assertThatThrownBy(() -> ctx.getValue(0, "NAME"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("row beyond result set throws FrameworkBusinessException")
        void getValue_rowBeyondSize_throws() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items", null);
            assertThatThrownBy(() -> ctx.getValue(99, "NAME"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("unknown column throws FrameworkBusinessException")
        void getValue_unknownColumn_throws() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items", null);
            assertThatThrownBy(() -> ctx.getValue(1, "NONEXISTENT"))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // getFirstValue()
    // =========================================================================

    @Nested
    @DisplayName("getFirstValue()")
    class GetFirstValueTests {

        @Test
        @DisplayName("returns row 1 value")
        void getFirstValue_returnsRow1() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items ORDER BY id", null);
            assertThat(ctx.getFirstValue("STATUS").toString()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("empty result throws FrameworkBusinessException")
        void getFirstValue_emptyResult_throws() throws FrameworkBusinessException {
            QueryResultContext ctx = QueryResultContext.execute(
                connector, "SELECT * FROM items WHERE id = -1", null);
            assertThatThrownBy(() -> ctx.getFirstValue("NAME"))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // H2 connector (test-only)
    // =========================================================================

    private static final class H2Connector implements DatabaseConnector {
        private final JdbcDataSource ds;

        H2Connector() {
            ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:qrc_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
        }

        @Override
        public DataSource getDataSource() { return ds; }

        @Override
        public void close() { }
    }
}
