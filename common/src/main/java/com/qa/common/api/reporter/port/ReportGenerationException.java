package com.qa.common.api.reporter.port;

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
