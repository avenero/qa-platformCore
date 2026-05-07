package com.qa.common.reporting.core.bridge;

import com.qa.common.reporting.core.model.TestStatus;

/**
 * Immutable bridge record for a single test step (Given/When/Then/And/But).
 * {@code httpDetail} is null when the step has no HTTP interaction.
 */
public record StepData(
        String keyword,
        String name,
        TestStatus status,
        long durationMs,
        String errorMessage,
        String stackTrace,
        HttpDetailData httpDetail
) {}
