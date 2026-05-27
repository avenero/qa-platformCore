package com.qa.databasecore.steps.canonical;

import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.datatable.DataTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the generic connect-with-params Given steps. Uses H2 in-memory
 * as the backing engine to exercise the full code path end-to-end without external
 * infrastructure — the steps themselves dispatch on {@code dbType} so the same
 * test design covers the wiring for the other supported engines.
 */
@DisplayName("DatabaseGivenSteps — generic connect-with-params")
class DatabaseGivenStepsConnectTest {

    private final DatabaseGivenSteps steps = new DatabaseGivenSteps();

    @BeforeEach
    void setUp() {
        ExecutionContext.builder().build().activate();
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("inline params: H2 connector stored under currentDbConnector")
    void inline_params_h2() throws FrameworkBusinessException {
        steps.iConnectToTheDatabaseWithParams(
            "h2", "jdbc:h2:mem:conn_test_inline_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1",
            "sa", "");
        DatabaseConnector connector = ExecutionContext.requireCurrent().variables()
            .require("currentDbConnector", DatabaseConnector.class);
        assertThat(connector).isNotNull();
        assertThat(ExecutionContext.requireCurrent().variables().require("currentDbType", String.class))
            .isEqualTo("h2");
    }

    @Test
    @DisplayName("DataTable params: same result as inline variant")
    void datatable_params_h2() throws FrameworkBusinessException {
        DataTable params = DataTable.create(List.of(
            List.of("type", "url", "user", "password"),
            List.of("h2",
                    "jdbc:h2:mem:conn_test_dt_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1",
                    "sa", "")
        ));
        steps.iConnectToADatabaseWithParameters(params);
        assertThat(ExecutionContext.requireCurrent().variables()
            .require("currentDbConnector", DatabaseConnector.class)).isNotNull();
    }

    @Test
    @DisplayName("DataTable accepts Spanish aliases (tipo / usuario / contraseña)")
    void datatable_spanish_aliases() throws FrameworkBusinessException {
        DataTable params = DataTable.create(List.of(
            List.of("tipo", "url", "usuario", "contraseña"),
            List.of("h2",
                    "jdbc:h2:mem:conn_test_es_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1",
                    "sa", "")
        ));
        steps.iConnectToADatabaseWithParameters(params);
        assertThat(ExecutionContext.requireCurrent().variables()
            .require("currentDbType", String.class)).isEqualTo("h2");
    }

    @Test
    @DisplayName("unsupported dbType is rejected with explicit error listing valid values")
    void unsupported_type_rejected() {
        assertThatThrownBy(() -> steps.iConnectToTheDatabaseWithParams(
            "mongo", "jdbc:whatever", "u", "p"))
            .isInstanceOf(FrameworkBusinessException.class)
            .hasMessageContaining("Unsupported dbType")
            .hasMessageContaining("oracle")
            .hasMessageContaining("postgresql");
    }

    @Test
    @DisplayName("blank dbType is rejected")
    void blank_type_rejected() {
        assertThatThrownBy(() -> steps.iConnectToTheDatabaseWithParams(
            "  ", "jdbc:h2:mem:x", "u", ""))
            .isInstanceOf(FrameworkBusinessException.class)
            .hasMessageContaining("dbType");
    }

    @Test
    @DisplayName("blank url is rejected")
    void blank_url_rejected() {
        assertThatThrownBy(() -> steps.iConnectToTheDatabaseWithParams(
            "h2", "  ", "u", ""))
            .isInstanceOf(FrameworkBusinessException.class)
            .hasMessageContaining("jdbcUrl");
    }

    @Test
    @DisplayName("DataTable with missing required column reports which alias was expected")
    void datatable_missing_column() {
        DataTable params = DataTable.create(List.of(
            List.of("type", "url", "user"),         // password missing
            List.of("h2",  "jdbc:h2:mem:x", "sa")
        ));
        assertThatThrownBy(() -> steps.iConnectToADatabaseWithParameters(params))
            .isInstanceOf(FrameworkBusinessException.class)
            .hasMessageContaining("password");
    }

    @Test
    @DisplayName("DataTable with more than 1 row is rejected")
    void datatable_multiple_rows_rejected() {
        DataTable params = DataTable.create(List.of(
            List.of("type", "url", "user", "password"),
            List.of("h2",   "jdbc:h2:mem:a", "sa", ""),
            List.of("h2",   "jdbc:h2:mem:b", "sa", "")
        ));
        assertThatThrownBy(() -> steps.iConnectToADatabaseWithParameters(params))
            .isInstanceOf(FrameworkBusinessException.class)
            .hasMessageContaining("Expected exactly 1");
    }

    @Test
    @DisplayName("postgres alias resolves to PostgreSQL connector (dispatch test)")
    void postgres_alias_dispatched() {
        // Connection will fail because no PG running, but we only care that the dispatch
        // reached the PostgreSQL connector code path (not the unsupported-type branch).
        assertThatThrownBy(() -> steps.iConnectToTheDatabaseWithParams(
            "postgres", "jdbc:postgresql://localhost:1/none", "u", ""))
            .isNotInstanceOf(FrameworkBusinessException.class);  // dispatched OK; pool init fails downstream
    }
}
