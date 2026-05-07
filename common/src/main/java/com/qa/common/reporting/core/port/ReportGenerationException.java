package com.qa.common.reporting.core.port;

/**
 * Thrown when report generation fails due to an internal error (I/O, template, etc.).
 */
public class ReportGenerationException extends Exception {

    public ReportGenerationException(String message) {
        super(message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
