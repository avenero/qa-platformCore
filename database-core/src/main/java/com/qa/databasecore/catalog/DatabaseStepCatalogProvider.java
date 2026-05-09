package com.qa.databasecore.catalog;

import com.qa.common.stepcatalog.ParamSpec;
import com.qa.common.stepcatalog.StepCatalogEntry;
import com.qa.common.stepcatalog.StepCatalogProvider;
import com.qa.common.stepcatalog.StepLayer;

import java.util.List;

/**
 * Proveedor del catálogo i18n para la capa DATABASE.
 *
 * <p>Registra todos los steps canónicos de database-core con sus claves de
 * descripción y hints de parámetros. Las traducciones viven en
 * {@code i18n/db_steps_*.properties} dentro de este JAR.
 */
public class DatabaseStepCatalogProvider implements StepCatalogProvider {

    private static final String CAT_SETUP  = "step.db.category.setup";
    private static final String CAT_EXEC   = "step.db.category.execution";
    private static final String CAT_VALID  = "step.db.category.assertions";
    private static final String CAT_TX     = "step.db.category.transactions";

    private static final List<StepCatalogEntry> ENTRIES = List.of(

        // ── Setup / Conexión (GIVEN) ───────────────────────────────────────────
        entry("db.given.connect",
              "I connect to the {string} database {string}",
              "GIVEN", CAT_SETUP, "step.db.given.connect.description",
              List.of(param("type",       "step.db.given.connect.param.type"),
                      param("identifier", "step.db.given.connect.param.identifier"))),

        entry("db.given.execute-script",
              "I execute the SQL script {string}",
              "GIVEN", CAT_SETUP, "step.db.given.execute-script.description",
              List.of(param("filename", "step.db.given.execute-script.param.filename"))),

        entry("db.given.begin-tx",
              "I begin a transaction",
              "GIVEN", CAT_TX, "step.db.given.begin-tx.description",
              List.of()),

        // ── Ejecución de queries (WHEN) ────────────────────────────────────────
        entry("db.when.query",
              "I query {string}",
              "WHEN", CAT_EXEC, "step.db.when.query.description",
              List.of(param("sql", "step.db.param.sql"))),

        entry("db.when.query-params",
              "I query {string} with parameters {string}",
              "WHEN", CAT_EXEC, "step.db.when.query-params.description",
              List.of(param("sql",    "step.db.param.sql"),
                      param("params", "step.db.param.params"))),

        entry("db.when.query-docstring",
              "I query:",
              "WHEN", CAT_EXEC, "step.db.when.query-docstring.description",
              List.of()),

        entry("db.when.execute",
              "I execute {string}",
              "WHEN", CAT_EXEC, "step.db.when.execute.description",
              List.of(param("sql", "step.db.param.sql"))),

        entry("db.when.execute-params",
              "I execute {string} with parameters {string}",
              "WHEN", CAT_EXEC, "step.db.when.execute-params.description",
              List.of(param("sql",    "step.db.param.sql"),
                      param("params", "step.db.param.params"))),

        entry("db.when.execute-docstring",
              "I execute:",
              "WHEN", CAT_EXEC, "step.db.when.execute-docstring.description",
              List.of()),

        entry("db.when.run-statement",
              "I run the SQL statement {string}",
              "WHEN", CAT_EXEC, "step.db.when.run-statement.description",
              List.of(param("sql", "step.db.param.sql"))),

        entry("db.when.call-procedure",
              "I call the stored procedure {string}",
              "WHEN", CAT_EXEC, "step.db.when.call-procedure.description",
              List.of(param("procedure", "step.db.when.call-procedure.param.procedure"))),

        // ── Transacciones (WHEN) ──────────────────────────────────────────────
        entry("db.when.commit",
              "I commit the transaction",
              "WHEN", CAT_TX, "step.db.when.commit.description",
              List.of()),

        entry("db.when.rollback",
              "I rollback the transaction",
              "WHEN", CAT_TX, "step.db.when.rollback.description",
              List.of()),

        // ── Validación (THEN) ─────────────────────────────────────────────────
        entry("db.then.row-count",
              "the query result should have {int} rows",
              "THEN", CAT_VALID, "step.db.then.row-count.description",
              List.of(param("count", "step.db.then.row-count.param.count"))),

        entry("db.then.field-equals",
              "the result field {string} in row {int} should be {string}",
              "THEN", CAT_VALID, "step.db.then.field-equals.description",
              List.of(param("field", "step.db.then.field.param.field"),
                      param("row",   "step.db.then.field.param.row"),
                      param("value", "step.db.then.field.param.value"))),

        entry("db.then.field-not-null",
              "the result field {string} in row {int} should not be null",
              "THEN", CAT_VALID, "step.db.then.field-not-null.description",
              List.of(param("field", "step.db.then.field.param.field"),
                      param("row",   "step.db.then.field.param.row"))),

        entry("db.then.no-rows",
              "the query result should have no rows",
              "THEN", CAT_VALID, "step.db.then.no-rows.description",
              List.of()),

        entry("db.then.rows-affected",
              "the statement should have affected {int} rows",
              "THEN", CAT_VALID, "step.db.then.rows-affected.description",
              List.of(param("count", "step.db.then.rows-affected.param.count")))
    );

    @Override
    public StepLayer layer() {
        return StepLayer.DATABASE;
    }

    @Override
    public String bundleBaseName() {
        return "i18n/db_steps";
    }

    @Override
    public List<StepCatalogEntry> entries() {
        return ENTRIES;
    }

    private static StepCatalogEntry entry(String stepId, String pattern, String phase,
                                          String categoryKey, String descriptionKey,
                                          List<ParamSpec> params) {
        return new StepCatalogEntry(stepId, pattern, StepLayer.DATABASE, phase,
                                    categoryKey, descriptionKey, params);
    }

    private static ParamSpec param(String name, String hintKey) {
        return new ParamSpec(name, hintKey);
    }
}
