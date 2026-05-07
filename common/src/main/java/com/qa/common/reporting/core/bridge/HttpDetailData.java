package com.qa.common.reporting.core.bridge;

import java.util.Map;

/**
 * Immutable bridge record for a captured HTTP interaction in a test step.
 * All sensitive data must already be redacted before constructing this record.
 */
public record HttpDetailData(
        String method,
        String urlPath,
        int httpStatus,
        String requestId,
        String requestSummary,
        String responseSummary,
        Integer requestBodySizeBytes,
        Integer responseBodySizeBytes,
        Long durationMs,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders
) {}
