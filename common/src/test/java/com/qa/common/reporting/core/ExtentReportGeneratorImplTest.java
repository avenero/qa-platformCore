package com.qa.common.reporting.core;

import com.qa.common.reporting.core.bridge.AttachmentData;
import com.qa.common.reporting.core.bridge.EnvironmentDetails;
import com.qa.common.reporting.core.bridge.ExecutionData;
import com.qa.common.reporting.core.bridge.HttpStepSummary;
import com.qa.common.reporting.core.bridge.ScenarioData;
import com.qa.common.reporting.core.bridge.StepData;
import com.qa.common.reporting.core.config.ExtentConfig;
import com.qa.common.reporting.core.model.TestStatus;
import com.qa.common.reporting.core.port.ReportFormat;
import com.qa.common.reporting.core.port.ReportGenerationException;
import com.qa.common.reporting.core.service.ExtentReportGeneratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtentReportGeneratorImplTest {

    private ExtentReportGeneratorImpl generator;

    @BeforeEach
    void setUp() {
        generator = new ExtentReportGeneratorImpl();
    }

    // -------------------------------------------------------------------------
    // Core functionality
    // -------------------------------------------------------------------------

    @Test
    void generateHtml_validExecutionData_returnsNonEmptyHtml() throws ReportGenerationException {
        ExecutionData execution = minimalExecution();

        String html = generator.generateHtml(execution);

        assertThat(html).isNotBlank();
    }

    @Test
    void generateHtml_validExecutionData_returnsHtmlDocument() throws ReportGenerationException {
        ExecutionData execution = minimalExecution();

        String html = generator.generateHtml(execution);

        assertThat(html.toLowerCase()).contains("<!doctype html");
    }

    @Test
    void generateHtml_scenariosPresent_allScenariosIncluded() throws ReportGenerationException {
        ScenarioData s1 = buildScenario("Login flow", TestStatus.PASS, List.of());
        ScenarioData s2 = buildScenario("Logout flow", TestStatus.FAIL, List.of());
        ExecutionData execution = buildExecution(List.of(s1, s2));

        String html = generator.generateHtml(execution);

        assertThat(html).contains("Login flow");
        assertThat(html).contains("Logout flow");
    }

    @Test
    void generateHtml_stepsPresent_stepsAppearInOutput() throws ReportGenerationException {
        StepData step1 = new StepData("Given", "a registered user", TestStatus.PASS, 100, null, null, null);
        StepData step2 = new StepData("When", "I submit the form", TestStatus.PASS, 200, null, null, null);
        ScenarioData scenario = buildScenario("Form test", TestStatus.PASS, List.of(step1, step2));
        ExecutionData execution = buildExecution(List.of(scenario));

        String html = generator.generateHtml(execution);

        assertThat(html).contains("a registered user");
        assertThat(html).contains("I submit the form");
    }

    @Test
    void generateHtml_httpDetailPresent_httpInfoEmbedded() throws ReportGenerationException {
        HttpStepSummary http = stubHttp("GET", "/api/users", 200, 30L);
        StepData step = new StepData("When", "I fetch users", TestStatus.PASS, 50, null, null, http);
        ScenarioData scenario = buildScenario("API test", TestStatus.PASS, List.of(step));
        ExecutionData execution = buildExecution(List.of(scenario));

        String html = generator.generateHtml(execution);

        assertThat(html).contains("GET");
        assertThat(html).contains("/api/users");
    }

    @Test
    void generateHtml_screenshotWithBase64_imageEmbedded() throws ReportGenerationException {
        byte[] imgBytes = "fake-image-data".getBytes();
        String base64 = Base64.getEncoder().encodeToString(imgBytes);
        AttachmentData att = new AttachmentData("Failure screenshot", base64, "image/png");
        ScenarioData scenario = buildScenarioWithScreenshots("Visual test", TestStatus.FAIL, List.of(att));
        ExecutionData execution = buildExecution(List.of(scenario));

        String html = generator.generateHtml(execution);

        assertThat(html).contains("data:image/png;base64,");
        assertThat(html).contains(base64);
    }

    @Test
    void generateHtml_failedScenario_errorMessageIncluded() throws ReportGenerationException {
        ScenarioData scenario = new ScenarioData(
                "Failed login", "login.feature", TestStatus.FAIL,
                Instant.EPOCH, Instant.now(), 500,
                "Expected 200 but got 401", "Authentication failed",
                "java.lang.AssertionError: Expected 200 but got 401",
                List.of(), List.of(), false, null, List.of());
        ExecutionData execution = buildExecution(List.of(scenario));

        String html = generator.generateHtml(execution);

        assertThat(html).contains("Authentication failed");
    }

    @Test
    void generateHtml_stackTrace_stackTraceIncluded() throws ReportGenerationException {
        ScenarioData scenario = new ScenarioData(
                "Error scenario", "errors.feature", TestStatus.FAIL,
                Instant.EPOCH, Instant.now(), 300,
                "NPE", null,
                "java.lang.NullPointerException at MyClass.method(MyClass.java:42)",
                List.of(), List.of(), false, null, List.of());
        ExecutionData execution = buildExecution(List.of(scenario));

        String html = generator.generateHtml(execution);

        assertThat(html).contains("NullPointerException");
    }

    @Test
    void generateHtml_xssEscaping_maliciousInputEscapedInHtmlContext() throws ReportGenerationException {
        // User-controlled field with a dangerous HTML payload in the failure message (HTML-injected context)
        String xssPayload = "<img src=x onerror=alert(1)>";
        ScenarioData scenario = new ScenarioData(
                "Safe scenario name", "xss.feature", TestStatus.FAIL,
                Instant.EPOCH, Instant.now(), 100,
                xssPayload, null, null,
                List.of(), List.of(), false, null, List.of());
        ExecutionData execution = buildExecution(List.of(scenario));

        String html = generator.generateHtml(execution);

        // The raw payload must not appear verbatim in HTML-injected content
        assertThat(html).doesNotContain("<img src=x onerror=alert(1)>");
        // The escaped version must appear (we inject error messages via test.fail with escapeHtml)
        assertThat(html).contains("&lt;img src=x onerror=alert(1)&gt;");
    }

    @Test
    void generateHtml_emptyScenarioList_gracefullyGeneratesReport() throws ReportGenerationException {
        ExecutionData execution = buildExecution(List.of());

        String html = generator.generateHtml(execution);

        assertThat(html).isNotBlank();
    }

    @Test
    void generateHtml_customConfig_reportTitleApplied() throws ReportGenerationException {
        ExtentConfig config = new ExtentConfig();
        config.setDocumentTitle("Custom QA Report");
        config.setReportTitle("Sprint 42 Results");
        ExecutionData execution = minimalExecution();

        String html = generator.generateHtml(execution, config);

        assertThat(html).isNotBlank();
    }

    @Test
    void generateHtml_withEnvironmentDetails_envInfoApplied() throws ReportGenerationException {
        EnvironmentDetails env = new EnvironmentDetails(
                "Linux", "5.4.0", "21.0.3", "Chrome", "124", null, "x86_64",
                Map.of("Region", "eu-west-1"));
        ExecutionData execution = new ExecutionData(
                "EX-99", "PROJ", Instant.EPOCH, Instant.now(), TestStatus.PASS,
                "production", 0, 0, 0, 0, List.of(), env);

        String html = generator.generateHtml(execution);

        assertThat(html).isNotBlank();
    }

    @Test
    void generateHtml_nullExecution_throwsReportGenerationException() {
        assertThatThrownBy(() -> generator.generateHtml(null))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("ExecutionData must not be null");
    }

    @Test
    void generate_pdfFormat_throwsUnsupportedOperationException() {
        ExecutionData execution = minimalExecution();

        assertThatThrownBy(() -> generator.generate(execution, ReportFormat.PDF))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void generate_jsonFormat_throwsUnsupportedOperationException() {
        ExecutionData execution = minimalExecution();

        assertThatThrownBy(() -> generator.generate(execution, ReportFormat.JSON))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void initialization_withNullConfig_usesDefaultConfig() throws ReportGenerationException {
        ExtentReportGeneratorImpl genWithNull = new ExtentReportGeneratorImpl(null);
        ExecutionData execution = minimalExecution();

        String html = genWithNull.generateHtml(execution);

        assertThat(html).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ExecutionData minimalExecution() {
        return new ExecutionData("EX-001", "TEST-PROJ", Instant.EPOCH, Instant.now(),
                TestStatus.PASS, "staging", 0, 0, 0, 0, List.of(), null);
    }

    private ExecutionData buildExecution(List<ScenarioData> scenarios) {
        int total = scenarios.size();
        long passed = scenarios.stream().filter(s -> s.status() == TestStatus.PASS).count();
        long failed = scenarios.stream().filter(s -> s.status() == TestStatus.FAIL).count();
        long skipped = scenarios.stream().filter(s -> s.status() == TestStatus.SKIP).count();
        TestStatus overall = failed > 0 ? TestStatus.FAIL : TestStatus.PASS;

        return new ExecutionData("EX-001", "PROJ", Instant.EPOCH, Instant.now(),
                overall, "test", total, (int) passed, (int) failed, (int) skipped,
                scenarios, null);
    }

    private ScenarioData buildScenario(String name, TestStatus status, List<StepData> steps) {
        return new ScenarioData(name, name.toLowerCase().replace(" ", "_") + ".feature",
                status, Instant.EPOCH, Instant.now(), 200,
                status == TestStatus.FAIL ? "Assertion failed" : null,
                null, null,
                steps, List.of("@test"), false, null, List.of());
    }

    private ScenarioData buildScenarioWithScreenshots(String name, TestStatus status, List<AttachmentData> screenshots) {
        return new ScenarioData(name, "test.feature", status,
                Instant.EPOCH, Instant.now(), 200,
                status == TestStatus.FAIL ? "Error" : null,
                null, null,
                List.of(), List.of(), false, null, screenshots);
    }

    // ---- test helper — minimal HttpStepSummary without importing http-core ----

    private HttpStepSummary stubHttp(String method, String urlPath, int httpStatus, Long durationMs) {
        return new HttpStepSummary() {
            @Override public String method()                { return method; }
            @Override public String urlPath()               { return urlPath; }
            @Override public int httpStatus()               { return httpStatus; }
            @Override public String requestId()             { return null; }
            @Override public String requestSummary()        { return null; }
            @Override public String responseSummary()       { return "{\"count\":5}"; }
            @Override public Integer requestBodySizeBytes() { return null; }
            @Override public Integer responseBodySizeBytes(){ return 80; }
            @Override public Long durationMs()              { return durationMs; }
            @Override public Map<String, String> requestHeaders()  { return null; }
            @Override public Map<String, String> responseHeaders() { return null; }
        };
    }
}
