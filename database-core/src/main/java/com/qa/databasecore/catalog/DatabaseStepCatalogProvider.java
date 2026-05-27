package com.qa.databasecore.catalog;

import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.api.stepcatalog.StepCatalogProvider;
import com.qa.common.api.stepcatalog.StepLayer;

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

        entry("db.given.connect-with-params",
              "I connect to the {string} database with url {string} user {string} password {string}",
              "GIVEN", CAT_SETUP, "step.db.given.connect-with-params.description",
              List.of(param("type",     "step.db.given.connect.param.type"),
                      param("url",      "step.db.param.jdbc-url"),
                      param("user",     "step.db.param.db-user"),
                      param("password", "step.db.param.db-password"))),

        entry("db.given.connect-with-datatable",
              "I connect to a database with parameters:",
              "GIVEN", CAT_SETUP, "step.db.given.connect-with-datatable.description",
              List.of()),

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
              List.of(param("count", "step.db.then.rows-affected.param.count"))),

        // ── Numeric comparisons (THEN) ─────────────────────────────────────────
        entry("db.then.col-gt",
              "the {string} column should be greater than {string}",
              "THEN", CAT_VALID, "step.db.then.col-gt.description",
              List.of(param("column", "step.db.param.column"), param("value", "step.db.param.numeric"))),
        entry("db.then.col-lt",
              "the {string} column should be less than {string}",
              "THEN", CAT_VALID, "step.db.then.col-lt.description",
              List.of(param("column", "step.db.param.column"), param("value", "step.db.param.numeric"))),
        entry("db.then.col-gte",
              "the {string} column should be greater than or equal to {string}",
              "THEN", CAT_VALID, "step.db.then.col-gte.description",
              List.of(param("column", "step.db.param.column"), param("value", "step.db.param.numeric"))),
        entry("db.then.col-lte",
              "the {string} column should be less than or equal to {string}",
              "THEN", CAT_VALID, "step.db.then.col-lte.description",
              List.of(param("column", "step.db.param.column"), param("value", "step.db.param.numeric"))),
        entry("db.then.col-between",
              "the {string} column should be between {string} and {string}",
              "THEN", CAT_VALID, "step.db.then.col-between.description",
              List.of(param("column", "step.db.param.column"),
                      param("low",    "step.db.param.numeric"),
                      param("high",   "step.db.param.numeric"))),

        // ── Numeric comparisons row-indexed (THEN) ─────────────────────────────
        entry("db.then.col-row-gt",
              "the {string} column in row {int} should be greater than {string}",
              "THEN", CAT_VALID, "step.db.then.col-row-gt.description",
              List.of(param("column", "step.db.param.column"),
                      param("row",    "step.db.param.row"),
                      param("value",  "step.db.param.numeric"))),
        entry("db.then.col-row-lt",
              "the {string} column in row {int} should be less than {string}",
              "THEN", CAT_VALID, "step.db.then.col-row-lt.description",
              List.of(param("column", "step.db.param.column"),
                      param("row",    "step.db.param.row"),
                      param("value",  "step.db.param.numeric"))),
        entry("db.then.col-row-gte",
              "the {string} column in row {int} should be greater than or equal to {string}",
              "THEN", CAT_VALID, "step.db.then.col-row-gte.description",
              List.of(param("column", "step.db.param.column"),
                      param("row",    "step.db.param.row"),
                      param("value",  "step.db.param.numeric"))),
        entry("db.then.col-row-lte",
              "the {string} column in row {int} should be less than or equal to {string}",
              "THEN", CAT_VALID, "step.db.then.col-row-lte.description",
              List.of(param("column", "step.db.param.column"),
                      param("row",    "step.db.param.row"),
                      param("value",  "step.db.param.numeric"))),
        entry("db.then.col-row-between",
              "the {string} column in row {int} should be between {string} and {string}",
              "THEN", CAT_VALID, "step.db.then.col-row-between.description",
              List.of(param("column", "step.db.param.column"),
                      param("row",    "step.db.param.row"),
                      param("low",    "step.db.param.numeric"),
                      param("high",   "step.db.param.numeric"))),

        // ── Pattern / case-insensitive (THEN) ──────────────────────────────────
        entry("db.then.col-match",
              "the {string} column should match pattern {string}",
              "THEN", CAT_VALID, "step.db.then.col-match.description",
              List.of(param("column", "step.db.param.column"), param("regex", "step.db.param.regex"))),
        entry("db.then.col-eq-ci",
              "the {string} column should equal {string} ignoring case",
              "THEN", CAT_VALID, "step.db.then.col-eq-ci.description",
              List.of(param("column", "step.db.param.column"), param("expected", "step.db.param.value"))),
        entry("db.then.col-contains-ci",
              "the {string} column should contain {string} ignoring case",
              "THEN", CAT_VALID, "step.db.then.col-contains-ci.description",
              List.of(param("column", "step.db.param.column"), param("substring", "step.db.param.value"))),

        // ── NULL / empty (THEN) ────────────────────────────────────────────────
        entry("db.then.col-null",
              "the {string} column should be null",
              "THEN", CAT_VALID, "step.db.then.col-null.description",
              List.of(param("column", "step.db.param.column"))),
        entry("db.then.col-not-empty",
              "the {string} column should not be empty",
              "THEN", CAT_VALID, "step.db.then.col-not-empty.description",
              List.of(param("column", "step.db.param.column"))),

        // ── Multi-row predicates (THEN) ────────────────────────────────────────
        entry("db.then.every-row-gt",
              "every row {string} column should be greater than {string}",
              "THEN", CAT_VALID, "step.db.then.every-row-gt.description",
              List.of(param("column", "step.db.param.column"), param("value", "step.db.param.numeric"))),
        entry("db.then.at-least-one-row-eq",
              "at least one row {string} column should equal {string}",
              "THEN", CAT_VALID, "step.db.then.at-least-one-row-eq.description",
              List.of(param("column", "step.db.param.column"), param("expected", "step.db.param.value"))),

        // ── Schema column existence (THEN) ─────────────────────────────────────
        entry("db.then.table-has-col",
              "the {string} table should have column {string}",
              "THEN", CAT_VALID, "step.db.then.table-has-col.description",
              List.of(param("table",  "step.db.param.table"),
                      param("column", "step.db.param.column"))),
        entry("db.then.col-type",
              "the {string} column in table {string} should be of type {string}",
              "THEN", CAT_VALID, "step.db.then.col-type.description",
              List.of(param("column", "step.db.param.column"),
                      param("table",  "step.db.param.table"),
                      param("type",   "step.db.param.sql-type"))),

        // ── Extraction (THEN) ──────────────────────────────────────────────────
        entry("db.then.store-all",
              "I store all {string} column values as list {string}",
              "THEN", CAT_VALID, "step.db.then.store-all.description",
              List.of(param("column",   "step.db.param.column"),
                      param("variable", "step.db.param.variable"))),

        // ── Negative testing (WHEN + THEN) ─────────────────────────────────────
        entry("db.when.try-execute",
              "I try to execute {string} expecting failure",
              "WHEN", CAT_EXEC, "step.db.when.try-execute.description",
              List.of(param("sql", "step.db.param.sql"))),
        entry("db.then.sql-state",
              "the previous query should have failed with SQL state {string}",
              "THEN", CAT_VALID, "step.db.then.sql-state.description",
              List.of(param("state", "step.db.param.sql-state")))
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
