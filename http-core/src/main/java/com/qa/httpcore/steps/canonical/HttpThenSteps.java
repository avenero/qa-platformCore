package com.qa.httpcore.steps.canonical;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.exception.FrameworkBusinessException;
import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Then;

/**
 * Canonical English HTTP step vocabulary — THEN phase (response assertions).
 *
 * <p>TASK-D07: introduces stable English assertion steps aligned with industry
 * conventions. Existing Spanish step definitions are kept for backward compatibility.
 *
 * <p>All steps delegate to {@link ApiHelper}.
 * Step IDs follow {@code api.then.*}.
 *
 * <p>Status codes use a single numeric step ({@code {int}}) rather than named aliases
 * (OK, Created, Not Found…). Boolean JSON fields use two independent step definitions
 * to avoid ambiguity.
 *
 * @since 3.0.0
 */
public class HttpThenSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    // =========================================================================
    // Status code
    // =========================================================================

    @StepDef(value = "api.then.status", displayName = "Assert response status code")
    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) throws FrameworkBusinessException {
        apiHelper().validateResponseStatusCode(expectedStatus);
    }

    // =========================================================================
    // Response body — text
    // =========================================================================

    @StepDef(value = "api.then.body-contains", displayName = "Assert response body contains text")
    @Then("the response body should contain {string}")
    public void theResponseBodyShouldContain(String text) throws FrameworkBusinessException {
        apiHelper().validateResponseContainsText(text);
    }

    // =========================================================================
    // Response body — JSON field assertions
    // =========================================================================

    @StepDef(value = "api.then.field-equals", displayName = "Assert JSON field equals value")
    @Then("the response field {string} should equal {string}")
    public void theResponseFieldShouldEqual(String jsonPath, String expectedValue)
            throws FrameworkBusinessException {
        apiHelper().validateJsonPathValue(jsonPath, expectedValue);
    }

    @StepDef(value = "api.then.field-not-null", displayName = "Assert JSON field is not null")
    @Then("the response field {string} should not be null")
    public void theResponseFieldShouldNotBeNull(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathNotNull(jsonPath);
    }

    @StepDef(value = "api.then.field-null", displayName = "Assert JSON field is null")
    @Then("the response field {string} should be null")
    public void theResponseFieldShouldBeNull(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathIsNull(jsonPath);
    }

    @StepDef(value = "api.then.field-true", displayName = "Assert JSON boolean field is true")
    @Then("the response field {string} should be true")
    public void theResponseFieldShouldBeTrue(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathValue(jsonPath, "true");
    }

    @StepDef(value = "api.then.field-false", displayName = "Assert JSON boolean field is false")
    @Then("the response field {string} should be false")
    public void theResponseFieldShouldBeFalse(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathValue(jsonPath, "false");
    }

    @StepDef(value = "api.then.field-matches", displayName = "Assert JSON field matches regex")
    @Then("the response field {string} should match {string}")
    public void theResponseFieldShouldMatch(String jsonPath, String regex)
            throws FrameworkBusinessException {
        apiHelper().validateJsonPathMatchesPattern(jsonPath, regex);
    }

    // =========================================================================
    // Response body — JSON array assertions
    // =========================================================================

    @StepDef(value = "api.then.array-size", displayName = "Assert JSON array has N elements")
    @Then("the response array {string} should have {int} elements")
    public void theResponseArrayShouldHaveElements(String jsonPath, int expectedSize)
            throws FrameworkBusinessException {
        apiHelper().validateJsonArraySize(jsonPath, expectedSize);
    }

    // =========================================================================
    // Schema validation
    // =========================================================================

    @StepDef(value = "api.then.schema", displayName = "Assert response matches JSON schema")
    @Then("the response should match the schema {string}")
    public void theResponseShouldMatchTheSchema(String schemaOrPath) throws FrameworkBusinessException {
        apiHelper().validateResponseSchema(schemaOrPath);
    }

    // =========================================================================
    // Response headers
    // =========================================================================

    @StepDef(value = "api.then.header-value", displayName = "Assert response header value")
    @Then("the response header {string} should be {string}")
    public void theResponseHeaderShouldBe(String headerName, String expectedValue)
            throws FrameworkBusinessException {
        apiHelper().validateHeaderValue(headerName, expectedValue);
    }

    @StepDef(value = "api.then.header-exists", displayName = "Assert response header exists")
    @Then("the response header {string} should exist")
    public void theResponseHeaderShouldExist(String headerName) throws FrameworkBusinessException {
        apiHelper().validateHeaderExists(headerName);
    }

    // =========================================================================
    // Performance
    // =========================================================================

    @StepDef(value = "api.then.response-time", displayName = "Assert response time under threshold")
    @Then("the response time should be less than {int} milliseconds")
    public void theResponseTimeShouldBeLessThan(int maxMillis) throws FrameworkBusinessException {
        long actual = apiHelper().getLastResponseTimeMs();
        if (actual >= maxMillis) {
            throw new FrameworkBusinessException(
                "api.then.response-time",
                String.format("Response time %d ms exceeded threshold of %d ms", actual, maxMillis));
        }
    }
}
