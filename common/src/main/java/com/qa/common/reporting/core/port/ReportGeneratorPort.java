package com.qa.common.reporting.core.port;

import com.qa.common.reporting.core.bridge.ExecutionData;
import com.qa.common.reporting.core.config.ExtentConfig;

/**
 * Driven port: generates a self-contained test execution report from bridge model data.
 *
 * <p>Consumers (BE, qa-module-test, FE-tests) convert their internal models to
 * {@link ExecutionData} and call this port without depending on any specific
 * reporting library. The implementation lives in Core and handles ExtentReports internally.
 *
 * <p>The returned String is always a fully self-contained HTML document — no external
 * CSS, JS, or image files are referenced; all resources are inlined.
 */
public interface ReportGeneratorPort {

    /**
     * Generates an HTML report using default configuration.
     *
     * @param execution aggregated execution data
     * @return self-contained HTML string, never null or empty
     * @throws ReportGenerationException if generation fails
     */
    String generateHtml(ExecutionData execution) throws ReportGenerationException;

    /**
     * Generates an HTML report with a custom ExtentConfig (title, theme, etc.).
     *
     * @param execution aggregated execution data
     * @param config    report customisation options
     * @return self-contained HTML string, never null or empty
     * @throws ReportGenerationException if generation fails
     */
    String generateHtml(ExecutionData execution, ExtentConfig config) throws ReportGenerationException;

    /**
     * Generates a report in the requested format.
     *
     * @param execution aggregated execution data
     * @param format    target output format
     * @return report content as a String (HTML, JSON, etc.)
     * @throws ReportGenerationException        if generation fails
     * @throws UnsupportedOperationException if the format is not yet implemented
     */
    String generate(ExecutionData execution, ReportFormat format) throws ReportGenerationException;
}
