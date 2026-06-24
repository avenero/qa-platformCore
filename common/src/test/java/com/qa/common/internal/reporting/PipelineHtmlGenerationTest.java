package com.qa.common.internal.reporting;

import com.qa.common.internal.reporting.config.MutableReportingConfig;
import com.qa.common.internal.reporting.manager.pipeline.PipelineResult;
import com.qa.common.internal.reporting.manager.pipeline.ReportingPipeline;
import com.qa.common.internal.reporting.manager.pipeline.steps.ConversionStep;
import com.qa.common.internal.reporting.manager.pipeline.steps.ExtentGenerationStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test for the W2-CW1 SPI flip: runs the full reporting pipeline
 * ({@link ConversionStep} then {@link ExtentGenerationStep}) over a Cucumber fixture
 * and asserts that {@code ExtentGenerationStep} resolves {@code ReportGeneratorPort}
 * via {@link java.util.ServiceLoader}, renders self-contained HTML from
 * {@code ExecutionData}, and writes it to the configured report path.
 *
 * <p>This is the qa-module-test pure-path guard (no BE): the CLI consumer expects a
 * report file on disk, now produced by the SPI implementation instead of the deleted
 * legacy {@code ExtentReportGenerator}.
 */
class PipelineHtmlGenerationTest {

    private static final String FIXTURE = "fixtures/3-scenarios-cucumber.json";
    private static final String REPORT_NAME = "execution-report.html";

    @Test
    void pipelineRendersSelfContainedHtmlReportToConfiguredPath(@TempDir Path tempDir) throws Exception {
        String rawResults = readResource(FIXTURE);

        MutableReportingConfig config = new MutableReportingConfig();
        config.getExtent().setOutputPath(tempDir.toString());
        config.getExtent().setReportName(REPORT_NAME);

        PipelineResult result = new ReportingPipeline.Builder()
                .withConfig(config)
                .addStep(new ConversionStep())
                .addStep(new ExtentGenerationStep())
                .build()
                .execute(rawResults);

        assertThat(result.isSuccess()).as("full pipeline succeeds").isTrue();

        String reportPath = result.getContext().getExtentReportPath();
        assertThat(reportPath).as("report path recorded in context").isNotNull();

        Path report = tempDir.resolve(REPORT_NAME);
        assertThat(report).as("HTML report written to configured path").exists();
        assertThat(Path.of(reportPath)).isEqualTo(report);

        String html = Files.readString(report, StandardCharsets.UTF_8);
        assertThat(html).as("output is a non-empty HTML document").containsIgnoringCase("<html");
        // Each fixture scenario name must be rendered (the SPI is the renderer now).
        assertThat(html).contains("Passing scenario", "Failing scenario", "Skipped scenario");
    }

    private static String readResource(String path) throws Exception {
        try (InputStream in = PipelineHtmlGenerationTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("fixture resource present: " + path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
