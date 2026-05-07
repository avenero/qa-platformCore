package com.qa.common.reporting.core.port;

/**
 * Supported output formats for report generation.
 * Only HTML is implemented in v1; others throw UnsupportedOperationException.
 */
public enum ReportFormat {
    HTML,
    PDF,
    JSON,
    XML
}
