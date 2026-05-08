package com.qa.common.reporting.core.service;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.qa.common.reporting.core.bridge.AttachmentData;
import com.qa.common.reporting.core.bridge.EnvironmentDetails;
import com.qa.common.reporting.core.bridge.ExecutionData;
import com.qa.common.reporting.core.bridge.ScenarioData;
import com.qa.common.reporting.core.bridge.StepData;
import com.qa.common.reporting.core.config.ExtentConfig;
import com.qa.common.reporting.core.model.TestStatus;
import com.qa.common.reporting.core.port.ReportFormat;
import com.qa.common.reporting.core.port.ReportGenerationException;
import com.qa.common.reporting.core.port.ReportGeneratorPort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Core implementation of {@link ReportGeneratorPort} backed by ExtentReports 5.x.
 *
 * <p>Uses a temporary file to capture the HTML output from ExtentSparkReporter,
 * then returns the full HTML as a String — making report persistence the caller's concern.
 *
 * <p>All rendering logic operates on bridge models from
 * {@code com.qa.common.reporting.core.bridge}; no dependency on {@code TestExecutionResult}
 * or any BE domain model.
 *
 * <p>Thread safety: each call to {@code generateHtml} creates its own {@link ExtentReports}
 * instance, so concurrent invocations are safe.
 */
public class ExtentReportGeneratorImpl implements ReportGeneratorPort {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final long MS_PER_SECOND = 1_000L;
    private static final long MS_PER_MINUTE = 60_000L;

    private final ExtentConfig defaultConfig;

    public ExtentReportGeneratorImpl() {
        this.defaultConfig = new ExtentConfig();
    }

    public ExtentReportGeneratorImpl(ExtentConfig defaultConfig) {
        this.defaultConfig = defaultConfig != null ? defaultConfig : new ExtentConfig();
    }

    // -------------------------------------------------------------------------
    // ReportGeneratorPort
    // -------------------------------------------------------------------------

    @Override
    public String generateHtml(ExecutionData execution) throws ReportGenerationException {
        return generateHtml(execution, defaultConfig);
    }

    @Override
    public String generateHtml(ExecutionData execution, ExtentConfig config) throws ReportGenerationException {
        if (execution == null) {
            throw new ReportGenerationException("ExecutionData must not be null");
        }
        ExtentConfig effectiveConfig = config != null ? config : defaultConfig;
        try {
            return buildHtml(execution, effectiveConfig);
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate HTML report: " + e.getMessage(), e);
        }
    }

    @Override
    public String generate(ExecutionData execution, ReportFormat format) throws ReportGenerationException {
        if (format == ReportFormat.HTML) {
            return generateHtml(execution);
        }
        throw new UnsupportedOperationException("Format " + format + " is not implemented yet. Use ReportFormat.HTML.");
    }

    // -------------------------------------------------------------------------
    // Internal generation
    // -------------------------------------------------------------------------

    private String buildHtml(ExecutionData execution, ExtentConfig config)
            throws IOException, ReportGenerationException {
        File tempFile = File.createTempFile("qa-report-", ".html");
        try {
            ExtentReports extent = initializeExtent(tempFile, config);
            addExecutionSystemInfo(extent, execution, config);

            List<ScenarioData> scenarios = execution.scenarios();
            if (scenarios != null) {
                for (ScenarioData scenario : scenarios) {
                    processScenario(extent, scenario, config);
                }
            }

            extent.flush();
            return Files.readString(tempFile.toPath());
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    private ExtentReports initializeExtent(File reportFile, ExtentConfig config) {
        ExtentSparkReporter spark = new ExtentSparkReporter(reportFile.getAbsolutePath());

        spark.config().setDocumentTitle(config.getDocumentTitle());
        spark.config().setReportName(config.getReportTitle());
        spark.config().setTheme("DARK".equalsIgnoreCase(config.getTheme()) ? Theme.DARK : Theme.STANDARD);
        spark.config().setTimelineEnabled(config.isIncludeTimeline());

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        return extent;
    }

    private void addExecutionSystemInfo(ExtentReports extent, ExecutionData execution, ExtentConfig config) {
        if (execution.executionKey() != null) {
            extent.setSystemInfo("Execution", execution.executionKey());
        }
        if (execution.projectKey() != null) {
            extent.setSystemInfo("Project", execution.projectKey());
        }
        if (execution.environment() != null) {
            extent.setSystemInfo("Environment", execution.environment());
        }
        if (execution.startTime() != null) {
            extent.setSystemInfo("Started", TIMESTAMP_FMT.format(execution.startTime()));
        }
        if (execution.endTime() != null) {
            extent.setSystemInfo("Finished", TIMESTAMP_FMT.format(execution.endTime()));
        }
        extent.setSystemInfo("Total Scenarios", String.valueOf(execution.totalScenarios()));
        extent.setSystemInfo("Passed", String.valueOf(execution.passedScenarios()));
        extent.setSystemInfo("Failed", String.valueOf(execution.failedScenarios()));
        extent.setSystemInfo("Skipped", String.valueOf(execution.skippedScenarios()));

        if (config.isIncludeSystemInfo() && execution.environmentDetails() != null) {
            addEnvironmentDetails(extent, execution.environmentDetails());
        }

        if (config.getSystemInfo() != null) {
            config.getSystemInfo().forEach(extent::setSystemInfo);
        }
    }

    private void addEnvironmentDetails(ExtentReports extent, EnvironmentDetails env) {
        if (env.os() != null) {
            String osInfo = env.os() + (env.osVersion() != null ? " " + env.osVersion() : "");
            extent.setSystemInfo("OS", osInfo);
        }
        if (env.javaVersion() != null) {
            extent.setSystemInfo("Java", env.javaVersion());
        }
        if (env.browser() != null) {
            String browserInfo = env.browser()
                    + (env.browserVersion() != null ? " " + env.browserVersion() : "");
            extent.setSystemInfo("Browser", browserInfo);
        }
        if (env.device() != null) {
            extent.setSystemInfo("Device", env.device());
        }
        if (env.platform() != null) {
            extent.setSystemInfo("Platform", env.platform());
        }
        if (env.customInfo() != null) {
            env.customInfo().forEach(extent::setSystemInfo);
        }
    }

    private void processScenario(ExtentReports extent, ScenarioData scenario, ExtentConfig config) {
        String testName = escapeHtml(scenario.scenarioName());
        if (scenario.isOutline() && scenario.outlineExample() != null) {
            testName = testName + " [" + escapeHtml(scenario.outlineExample()) + "]";
        }
        ExtentTest test = extent.createTest(testName);

        assignTags(test, scenario);

        if (scenario.featureFile() != null) {
            test.info("<span style='font-size:14px'><b>📁 Feature:</b> "
                    + escapeHtml(scenario.featureFile()) + "</span>");
        }

        addSteps(test, scenario);
        addDuration(test, scenario);
        addScreenshots(test, scenario, config);
        addFailureDetails(test, scenario);

        test.log(mapStatus(scenario.status()), "Scenario " + scenario.status().name().toLowerCase());
    }

    private void assignTags(ExtentTest test, ScenarioData scenario) {
        if (scenario.tags() != null) {
            scenario.tags().forEach(test::assignCategory);
        }
    }

    private void addSteps(ExtentTest test, ScenarioData scenario) {
        List<StepData> steps = scenario.steps();
        if (steps == null || steps.isEmpty()) {
            return;
        }

        long passed = steps.stream().filter(s -> s.status() == TestStatus.PASS).count();
        long failed = steps.stream().filter(s -> s.status() == TestStatus.FAIL).count();
        long skipped = steps.stream().filter(s -> s.status() == TestStatus.SKIP).count();

        test.info(String.format(
                "<span style='font-size:14px'><b>📋 Steps:</b> %d total "
                        + "<span style='color:#4CAF50'>(✔ %d)</span> "
                        + "<span style='color:#f44336'>(✘ %d)</span> "
                        + "<span style='color:#ff9800'>(⏭ %d)</span></span>",
                steps.size(), passed, failed, skipped));

        for (StepData step : steps) {
            logStep(test, step);
        }
    }

    private void logStep(ExtentTest test, StepData step) {
        String keyword = step.keyword() != null ? step.keyword() + " " : "";
        String name = step.name() != null ? step.name() : "";
        String duration = formatDuration(step.durationMs());

        String html = String.format(
                "<span style='font-size:14px'><b>%s</b>%s "
                        + "<span style='color:#aaa;font-size:12px'>(%s)</span></span>",
                escapeHtml(keyword), escapeHtml(name), duration);

        Status status = mapStatus(step.status());
        switch (status) {
            case PASS -> test.pass(html);
            case FAIL -> test.fail(html);
            case SKIP -> test.skip(html);
            default -> test.info(html);
        }

        if (step.protocolDetail() != null) {
            Status extStatus = step.status() == TestStatus.FAIL ? Status.FAIL : Status.INFO;
            test.log(extStatus, step.protocolDetail().renderHtml());
        }

        if (step.status() == TestStatus.FAIL && step.errorMessage() != null) {
            test.fail("<details style='font-size:13px;margin-top:4px'>"
                    + "<summary style='cursor:pointer;color:#aaa'><i>Step error</i></summary>"
                    + "<pre style='background:#1e1e1e;padding:10px;border-radius:4px;"
                    + "color:#d4d4d4;font-size:12px;overflow-x:auto'>"
                    + escapeHtml(step.errorMessage()) + "</pre></details>");
        }
    }

    private void addDuration(ExtentTest test, ScenarioData scenario) {
        if (scenario.durationMs() > 0) {
            test.info("<span style='font-size:14px'><b>⏱ Duration:</b> "
                    + formatDuration(scenario.durationMs()) + "</span>");
        }
    }

    private void addScreenshots(ExtentTest test, ScenarioData scenario, ExtentConfig config) {
        if (!config.isIncludeScreenshots()) {
            return;
        }
        List<AttachmentData> screenshots = scenario.screenshots();
        if (screenshots == null) {
            return;
        }
        for (AttachmentData att : screenshots) {
            if (att.base64Content() == null || att.base64Content().isBlank()) {
                continue;
            }
            String mimeType = att.mimeType() != null ? att.mimeType() : "image/png";
            String src = "data:" + mimeType + ";base64," + att.base64Content();
            String name = att.name() != null ? att.name() : "Screenshot";
            String html = "<details style='margin-top:8px'>"
                    + "<summary style='cursor:pointer;font-size:14px'><b>📸 "
                    + escapeHtml(name) + "</b> <i style='color:#aaa;font-size:12px'>(click to expand)</i></summary>"
                    + "<div style='margin-top:8px'><img src='" + src
                    + "' style='max-width:100%;border:1px solid #444;border-radius:4px'/></div></details>";
            test.fail(html);
        }
    }

    private void addFailureDetails(ExtentTest test, ScenarioData scenario) {
        if (scenario.status() != TestStatus.FAIL) {
            return;
        }
        String displayError = scenario.businessMessage() != null && !scenario.businessMessage().isBlank()
                ? scenario.businessMessage()
                : (scenario.errorMessage() != null ? scenario.errorMessage() : "Test failed");

        test.fail("<span style='font-size:14px'><b>Error:</b> " + escapeHtml(displayError) + "</span>");

        if (scenario.businessMessage() != null && scenario.errorMessage() != null
                && !scenario.errorMessage().equals(scenario.businessMessage())) {
            test.fail("<details style='font-size:13px;margin-top:8px'>"
                    + "<summary style='cursor:pointer;color:#aaa'><i>Technical details</i></summary>"
                    + "<pre style='background:#1e1e1e;padding:12px;border-radius:4px;"
                    + "color:#d4d4d4;font-size:12px;overflow-x:auto'>"
                    + escapeHtml(scenario.errorMessage()) + "</pre></details>");
        }
        if (scenario.stackTrace() != null) {
            test.fail("<details style='font-size:13px;margin-top:8px'>"
                    + "<summary style='cursor:pointer;color:#aaa'><i>Stack Trace</i></summary>"
                    + "<pre style='background:#1e1e1e;padding:12px;border-radius:4px;"
                    + "color:#d4d4d4;font-size:12px;overflow-x:auto'>"
                    + escapeHtml(scenario.stackTrace()) + "</pre></details>");
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private Status mapStatus(TestStatus status) {
        if (status == null) {
            return Status.INFO;
        }
        return switch (status) {
            case PASS -> Status.PASS;
            case FAIL -> Status.FAIL;
            case SKIP -> Status.SKIP;
            case TODO, EXECUTING -> Status.WARNING;
        };
    }

    private String formatDuration(long ms) {
        if (ms < MS_PER_SECOND) {
            return ms + "ms";
        } else if (ms < MS_PER_MINUTE) {
            return String.format("%.2fs", ms / (double) MS_PER_SECOND);
        } else {
            return String.format("%dm %ds", ms / MS_PER_MINUTE, (ms % MS_PER_MINUTE) / MS_PER_SECOND);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
