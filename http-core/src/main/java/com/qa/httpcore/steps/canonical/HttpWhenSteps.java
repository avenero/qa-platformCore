package com.qa.httpcore.steps.canonical;

import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.model.HttpMethod;
import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.annotation.StepDef;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;

/**
 * Canonical English HTTP step vocabulary — WHEN phase (request execution).
 *
 * <p>TASK-D07: introduces stable English execution steps aligned with industry
 * conventions. Existing Spanish step definitions are kept for backward compatibility.
 *
 * <p>All steps delegate to {@link ApiHelper} / {@link HttpClient}.
 * Step IDs follow {@code api.when.*}.
 *
 * @since 3.0.0
 */
public class HttpWhenSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    private HttpClient httpClient() {
        return ExecutionContext.current()
            .map(ctx -> ctx.service(HttpClient.class))
            .orElseGet(com.qa.httpcore.factories.HttpClientFactory::getInstance);
    }

    // =========================================================================
    // Simple method + path steps
    // =========================================================================

    @StepDef(value = "api.when.get", displayName = "Send GET request")
    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        httpClient().get("");
    }

    @StepDef(value = "api.when.post", displayName = "Send POST request")
    @When("I send a POST request to {string}")
    public void sendPostRequest(String endpoint) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        httpClient().post("");
    }

    @StepDef(value = "api.when.put", displayName = "Send PUT request")
    @When("I send a PUT request to {string}")
    public void sendPutRequest(String endpoint) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        httpClient().put("");
    }

    @StepDef(value = "api.when.patch", displayName = "Send PATCH request")
    @When("I send a PATCH request to {string}")
    public void sendPatchRequest(String endpoint) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        httpClient().patch("");
    }

    @StepDef(value = "api.when.delete", displayName = "Send DELETE request")
    @When("I send a DELETE request to {string}")
    public void sendDeleteRequest(String endpoint) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        httpClient().delete("");
    }

    @StepDef(value = "api.when.head", displayName = "Send HEAD request")
    @When("I send a HEAD request to {string}")
    public void sendHeadRequest(String endpoint) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        httpClient().executeRequest(HttpMethod.HEAD, "");
    }

    // =========================================================================
    // Steps with DocString body
    // =========================================================================

    @StepDef(value = "api.when.post-body", displayName = "Send POST request with body")
    @When("I send a POST request to {string} with body:")
    public void sendPostRequestWithBody(String endpoint, String body) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        apiHelper().setRequestBody(body);
        httpClient().post("");
    }

    @StepDef(value = "api.when.put-body", displayName = "Send PUT request with body")
    @When("I send a PUT request to {string} with body:")
    public void sendPutRequestWithBody(String endpoint, String body) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        apiHelper().setRequestBody(body);
        httpClient().put("");
    }

    @StepDef(value = "api.when.patch-body", displayName = "Send PATCH request with body")
    @When("I send a PATCH request to {string} with body:")
    public void sendPatchRequestWithBody(String endpoint, String body) throws FrameworkTechnicalException {
        apiHelper().configureEndpoint(endpoint);
        apiHelper().setRequestBody(body);
        httpClient().patch("");
    }

    // =========================================================================
    // Step with DataTable query parameters
    // =========================================================================

    @StepDef(value = "api.when.get-params", displayName = "Send GET request with query parameters")
    @When("I send a GET request to {string} with query parameters:")
    public void sendGetRequestWithParams(String endpoint, DataTable table)
            throws FrameworkTechnicalException, FrameworkBusinessException {
        apiHelper().configureEndpoint(endpoint);
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String name = row.get("name");
            String value = row.get("value");
            if (name == null || value == null) {
                throw new FrameworkBusinessException(
                    "api.when.get-params",
                    "DataTable must have 'name' and 'value' columns");
            }
            apiHelper().addQueryParam(name, value);
        }
        httpClient().get("");
    }
}
