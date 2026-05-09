package com.qa.httpcore.steps.canonical;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.exception.FrameworkBusinessException;
import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Canonical English HTTP step vocabulary — GIVEN phase (request configuration).
 *
 * <p>TASK-D07: refines the previous HTTP step vocabulary by introducing a single,
 * stable English vocabulary aligned with industry conventions. Existing Spanish
 * step definitions are kept for backward compatibility but are deprecated.
 *
 * <p>All steps delegate to {@link ApiHelper}. Step IDs follow {@code api.given.*}.
 *
 * @since 3.0.0
 */
public class HttpGivenSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @StepDef(value = "api.given.base-url", displayName = "Set base URL")
    @Given("I set the base url to {string}")
    public void setBaseUrl(String url) {
        apiHelper().setBaseHost(url);
    }

    @StepDef(value = "api.given.header", displayName = "Set request header")
    @Given("I set the request header {string} to {string}")
    public void setRequestHeader(String name, String value) {
        apiHelper().addHeader(name, value);
    }

    @StepDef(value = "api.given.query-param", displayName = "Set query parameter")
    @Given("I set the query parameter {string} to {string}")
    public void setQueryParameter(String name, String value) {
        apiHelper().addQueryParam(name, value);
    }

    @StepDef(value = "api.given.bearer", displayName = "Set bearer token")
    @Given("I set the bearer token {string}")
    public void setBearerToken(String token) {
        apiHelper().addBearerToken(token);
    }

    @StepDef(value = "api.given.basic-auth", displayName = "Set basic auth")
    @Given("I set the basic auth with username {string} and password {string}")
    public void setBasicAuth(String username, String password) {
        apiHelper().addBasicAuthentication(username, password);
    }

    @StepDef(value = "api.given.body-inline", displayName = "Set request body (inline)")
    @Given("I set the request body to:")
    public void setRequestBody(String body) {
        apiHelper().setRequestBody(body);
    }

    @StepDef(value = "api.given.body-file", displayName = "Set request body from file")
    @Given("I set the request body from file {string}")
    public void setRequestBodyFromFile(String filename) throws FrameworkBusinessException {
        try {
            String content = Files.readString(Path.of(filename));
            apiHelper().setRequestBody(content);
        } catch (IOException e) {
            throw new FrameworkBusinessException(
                "Could not read request body file '" + filename + "': " + e.getMessage(), e);
        }
    }

    @StepDef(value = "api.given.content-type", displayName = "Set content type")
    @Given("I set the content type to {string}")
    public void setContentType(String contentType) {
        apiHelper().addHeader("Content-Type", contentType);
    }
}
