package com.qa.common.reporting.core;

import com.qa.common.reporting.core.bridge.ExecutionData;
import com.qa.common.reporting.core.bridge.EnvironmentDetails;
import com.qa.common.reporting.core.bridge.ScenarioData;
import com.qa.common.reporting.core.model.TestStatus;
import com.qa.common.reporting.core.port.ReportFormat;
import com.qa.common.reporting.core.port.ReportGenerationException;
import com.qa.common.reporting.core.port.ReportGeneratorPort;
import com.qa.common.reporting.core.service.ExtentReportGeneratorImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the ReportGeneratorPort contract using the concrete implementation.
 */
class ReportGeneratorPortTest {

    private final ReportGeneratorPort port = new ExtentReportGeneratorImpl();

    @Test
    void generateHtml_viaPortInterface_returnsNonEmptyString() throws ReportGenerationException {
        ExecutionData execution = minimalExecution(TestStatus.PASS);

        String html = port.generateHtml(execution);

        assertThat(html).isNotBlank();
    }

    @Test
    void generate_withHtmlFormat_delegatesToGenerateHtml() throws ReportGenerationException {
        ExecutionData execution = minimalExecution(TestStatus.FAIL);

        String html = port.generate(execution, ReportFormat.HTML);

        assertThat(html).isNotBlank();
    }

    @Test
    void generate_withUnsupportedFormat_throwsUnsupportedOperationException() {
        ExecutionData execution = minimalExecution(TestStatus.PASS);

        assertThatThrownBy(() -> port.generate(execution, ReportFormat.PDF))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("PDF");
    }

    // ---- helpers ----

    private ExecutionData minimalExecution(TestStatus status) {
        return new ExecutionData("EX-001", "PROJ", Instant.EPOCH, Instant.now(),
                status, "test", 0, 0, 0, 0, List.of(), null);
    }
}
