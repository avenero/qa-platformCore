package com.qa.common.reporting.core;

import com.qa.common.reporting.core.bridge.AttachmentData;
import com.qa.common.reporting.core.bridge.EnvironmentDetails;
import com.qa.common.reporting.core.bridge.ExecutionData;
import com.qa.common.reporting.core.bridge.HttpStepSummary;
import com.qa.common.reporting.core.bridge.ScenarioData;
import com.qa.common.reporting.core.bridge.StepData;
import com.qa.common.reporting.core.model.TestStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BridgeModelsTest {

    @Test
    void executionData_createdWithAllFields_accessorsReturnCorrectValues() {
        EnvironmentDetails env = new EnvironmentDetails("Mac OS X", "15.0", "21", "Chrome", "124", null, null, null);
        ScenarioData scenario = buildScenario(TestStatus.PASS);
        ExecutionData execution = new ExecutionData(
                "EX-001", "PROJ-1", Instant.EPOCH, Instant.now(),
                TestStatus.PASS, "staging", 1, 1, 0, 0,
                List.of(scenario), env);

        assertThat(execution.executionKey()).isEqualTo("EX-001");
        assertThat(execution.projectKey()).isEqualTo("PROJ-1");
        assertThat(execution.overallStatus()).isEqualTo(TestStatus.PASS);
        assertThat(execution.totalScenarios()).isEqualTo(1);
        assertThat(execution.scenarios()).hasSize(1);
        assertThat(execution.environmentDetails().browser()).isEqualTo("Chrome");
    }

    @Test
    void scenarioData_createdWithSteps_stepsAreAccessible() {
        StepData step = new StepData("Given", "a logged-in user", TestStatus.PASS, 150, null, null, null);
        ScenarioData scenario = new ScenarioData(
                "Login flow", "login.feature", TestStatus.PASS,
                Instant.EPOCH, Instant.now(), 150L,
                null, null, null,
                List.of(step), List.of("@smoke"), false, null, List.of());

        assertThat(scenario.scenarioName()).isEqualTo("Login flow");
        assertThat(scenario.steps()).hasSize(1);
        assertThat(scenario.steps().get(0).keyword()).isEqualTo("Given");
        assertThat(scenario.tags()).containsExactly("@smoke");
        assertThat(scenario.isOutline()).isFalse();
    }

    @Test
    void stepData_withHttpDetail_httpDetailIsAccessible() {
        HttpStepSummary http = stubHttp("POST", "/api/login", 200, 75L);

        StepData step = new StepData("When", "I login", TestStatus.PASS, 100, null, null, http);

        assertThat(step.httpDetail()).isNotNull();
        assertThat(step.httpDetail().method()).isEqualTo("POST");
        assertThat(step.httpDetail().httpStatus()).isEqualTo(200);
        assertThat(step.httpDetail().durationMs()).isEqualTo(75L);
    }

    // ---- test helper — minimal HttpStepSummary without importing http-core ----

    private HttpStepSummary stubHttp(String method, String urlPath, int httpStatus, Long durationMs) {
        return new HttpStepSummary() {
            @Override public String method()               { return method; }
            @Override public String urlPath()              { return urlPath; }
            @Override public int httpStatus()              { return httpStatus; }
            @Override public String requestId()            { return null; }
            @Override public String requestSummary()       { return null; }
            @Override public String responseSummary()      { return null; }
            @Override public Integer requestBodySizeBytes(){ return null; }
            @Override public Integer responseBodySizeBytes(){ return null; }
            @Override public Long durationMs()             { return durationMs; }
            @Override public Map<String, String> requestHeaders()  { return null; }
            @Override public Map<String, String> responseHeaders() { return null; }
        };
    }

    @Test
    void attachmentData_withBase64Content_mimeTypePreserved() {
        AttachmentData att = new AttachmentData("screenshot-1", "aGVsbG8=", "image/png");

        assertThat(att.name()).isEqualTo("screenshot-1");
        assertThat(att.base64Content()).isEqualTo("aGVsbG8=");
        assertThat(att.mimeType()).isEqualTo("image/png");
    }

    @Test
    void environmentDetails_withCustomInfo_allFieldsAccessible() {
        Map<String, String> custom = Map.of("Region", "us-east-1", "BuildId", "42");
        EnvironmentDetails env = new EnvironmentDetails(
                "Linux", "5.4", "21.0.3", "Firefox", "125", "N/A", "x86_64", custom);

        assertThat(env.os()).isEqualTo("Linux");
        assertThat(env.javaVersion()).isEqualTo("21.0.3");
        assertThat(env.customInfo()).containsEntry("Region", "us-east-1");
    }

    // ---- helpers ----

    private ScenarioData buildScenario(TestStatus status) {
        return new ScenarioData("Scenario", "f.feature", status,
                Instant.EPOCH, Instant.now(), 100,
                null, null, null, List.of(), List.of(), false, null, List.of());
    }
}
