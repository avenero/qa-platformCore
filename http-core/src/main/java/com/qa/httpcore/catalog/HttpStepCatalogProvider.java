package com.qa.httpcore.catalog;

import com.qa.common.stepcatalog.ParamSpec;
import com.qa.common.stepcatalog.StepCatalogEntry;
import com.qa.common.stepcatalog.StepCatalogProvider;
import com.qa.common.stepcatalog.StepLayer;

import java.util.List;

/**
 * Proveedor del catálogo i18n para la capa HTTP.
 *
 * <p>Registra todos los steps canónicos de http-core con sus claves de
 * descripción y hints de parámetros. Las traducciones viven en
 * {@code i18n/http_steps_*.properties} dentro de este JAR.
 */
public class HttpStepCatalogProvider implements StepCatalogProvider {

    private static final String CAT_AUTH   = "step.http.category.auth";
    private static final String CAT_REQ    = "step.http.category.request";
    private static final String CAT_EXEC   = "step.http.category.execution";
    private static final String CAT_STATUS = "step.http.category.status";
    private static final String CAT_BODY   = "step.http.category.body";
    private static final String CAT_HEADER = "step.http.category.headers";
    private static final String CAT_PERF   = "step.http.category.performance";
    private static final String CAT_SEC    = "step.http.category.security";

    private static final List<StepCatalogEntry> ENTRIES = List.of(

        // ── Autenticación (GIVEN) ─────────────────────────────────────────────
        entry("api.given.base-url",
              "I set the base url to {string}",
              "GIVEN", CAT_AUTH, "step.http.given.base-url.description",
              List.of(param("url", "step.http.given.base-url.param.url"))),

        entry("api.given.bearer",
              "I set the bearer token {string}",
              "GIVEN", CAT_AUTH, "step.http.given.bearer.description",
              List.of(param("token", "step.http.given.bearer.param.token"))),

        entry("api.given.basic-auth",
              "I set the basic auth with username {string} and password {string}",
              "GIVEN", CAT_AUTH, "step.http.given.basic-auth.description",
              List.of(param("username", "step.http.given.basic-auth.param.username"),
                      param("password", "step.http.given.basic-auth.param.password"))),

        // ── Configuración de request (GIVEN) ──────────────────────────────────
        entry("api.given.header",
              "I set the request header {string} to {string}",
              "GIVEN", CAT_REQ, "step.http.given.header.description",
              List.of(param("name",  "step.http.given.header.param.name"),
                      param("value", "step.http.given.header.param.value"))),

        entry("api.given.query-param",
              "I set the query parameter {string} to {string}",
              "GIVEN", CAT_REQ, "step.http.given.query-param.description",
              List.of(param("name",  "step.http.given.query-param.param.name"),
                      param("value", "step.http.given.query-param.param.value"))),

        entry("api.given.content-type",
              "I set the content type to {string}",
              "GIVEN", CAT_REQ, "step.http.given.content-type.description",
              List.of(param("contentType", "step.http.given.content-type.param.value"))),

        entry("api.given.body-inline",
              "I set the request body to:",
              "GIVEN", CAT_REQ, "step.http.given.body-inline.description",
              List.of()),

        entry("api.given.body-file",
              "I set the request body from file {string}",
              "GIVEN", CAT_REQ, "step.http.given.body-file.description",
              List.of(param("filename", "step.http.given.body-file.param.filename"))),

        // ── Ejecución (WHEN) ──────────────────────────────────────────────────
        entry("api.when.get",
              "I send a GET request to {string}",
              "WHEN", CAT_EXEC, "step.http.when.get.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.post",
              "I send a POST request to {string}",
              "WHEN", CAT_EXEC, "step.http.when.post.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.post-body",
              "I send a POST request to {string} with body:",
              "WHEN", CAT_EXEC, "step.http.when.post-body.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.put",
              "I send a PUT request to {string}",
              "WHEN", CAT_EXEC, "step.http.when.put.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.put-body",
              "I send a PUT request to {string} with body:",
              "WHEN", CAT_EXEC, "step.http.when.put-body.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.patch",
              "I send a PATCH request to {string}",
              "WHEN", CAT_EXEC, "step.http.when.patch.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.patch-body",
              "I send a PATCH request to {string} with body:",
              "WHEN", CAT_EXEC, "step.http.when.patch-body.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.delete",
              "I send a DELETE request to {string}",
              "WHEN", CAT_EXEC, "step.http.when.delete.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.head",
              "I send a HEAD request to {string}",
              "WHEN", CAT_EXEC, "step.http.when.head.description",
              List.of(param("path", "step.http.when.param.path"))),

        entry("api.when.get-params",
              "I send a GET request to {string} with query parameters:",
              "WHEN", CAT_EXEC, "step.http.when.get-params.description",
              List.of(param("path", "step.http.when.param.path"))),

        // ── Validación de status (THEN) ───────────────────────────────────────
        entry("api.then.status",
              "the response status should be {int}",
              "THEN", CAT_STATUS, "step.http.then.status.description",
              List.of(param("statusCode", "step.http.then.status.param.statusCode"))),

        // ── Validación de body (THEN) ─────────────────────────────────────────
        entry("api.then.body-contains",
              "the response body should contain {string}",
              "THEN", CAT_BODY, "step.http.then.body-contains.description",
              List.of(param("text", "step.http.then.body-contains.param.text"))),

        entry("api.then.field-equals",
              "the response field {string} should equal {string}",
              "THEN", CAT_BODY, "step.http.then.field-equals.description",
              List.of(param("jsonPath", "step.http.then.field.param.jsonPath"),
                      param("value",    "step.http.then.field.param.value"))),

        entry("api.then.field-not-null",
              "the response field {string} should not be null",
              "THEN", CAT_BODY, "step.http.then.field-not-null.description",
              List.of(param("jsonPath", "step.http.then.field.param.jsonPath"))),

        entry("api.then.field-null",
              "the response field {string} should be null",
              "THEN", CAT_BODY, "step.http.then.field-null.description",
              List.of(param("jsonPath", "step.http.then.field.param.jsonPath"))),

        entry("api.then.field-true",
              "the response field {string} should be true",
              "THEN", CAT_BODY, "step.http.then.field-true.description",
              List.of(param("jsonPath", "step.http.then.field.param.jsonPath"))),

        entry("api.then.field-false",
              "the response field {string} should be false",
              "THEN", CAT_BODY, "step.http.then.field-false.description",
              List.of(param("jsonPath", "step.http.then.field.param.jsonPath"))),

        entry("api.then.field-array-size",
              "the response array {string} should have {int} elements",
              "THEN", CAT_BODY, "step.http.then.field-array-size.description",
              List.of(param("jsonPath", "step.http.then.field.param.jsonPath"),
                      param("size",     "step.http.then.array-size.param.size"))),

        // ── Validación de headers (THEN) ──────────────────────────────────────
        entry("api.then.header-equals",
              "the response header {string} should equal {string}",
              "THEN", CAT_HEADER, "step.http.then.header-equals.description",
              List.of(param("header", "step.http.then.header.param.name"),
                      param("value",  "step.http.then.header.param.value"))),

        entry("api.then.header-present",
              "the response header {string} should be present",
              "THEN", CAT_HEADER, "step.http.then.header-present.description",
              List.of(param("header", "step.http.then.header.param.name"))),

        // ── Performance (THEN) ────────────────────────────────────────────────
        entry("api.then.response-time",
              "the response time should be less than {int} milliseconds",
              "THEN", CAT_PERF, "step.http.then.response-time.description",
              List.of(param("milliseconds", "step.http.then.response-time.param.ms")))
    );

    @Override
    public StepLayer layer() {
        return StepLayer.HTTP;
    }

    @Override
    public String bundleBaseName() {
        return "i18n/http_steps";
    }

    @Override
    public List<StepCatalogEntry> entries() {
        return ENTRIES;
    }

    private static StepCatalogEntry entry(String stepId, String pattern, String phase,
                                          String categoryKey, String descriptionKey,
                                          List<ParamSpec> params) {
        return new StepCatalogEntry(stepId, pattern, StepLayer.HTTP, phase,
                                    categoryKey, descriptionKey, params);
    }

    private static ParamSpec param(String name, String hintKey) {
        return new ParamSpec(name, hintKey);
    }
}
