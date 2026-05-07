package com.qa.common.reporting.core.bridge;

import com.qa.common.reporting.core.model.TestStatus;

/**
 * Immutable bridge record for a single test step (Given/When/Then/And/But).
 * {@code httpDetail} is null when the step has no HTTP interaction.
 * The concrete type is provided by the caller (e.g. {@code HttpDetailData} from {@code http-core});
 * {@code common} only knows the {@link HttpStepSummary} contract.
 */
public record StepData(
        String keyword,
        String name,
        TestStatus status,
        long durationMs,
        String errorMessage,
        String stackTrace,
        HttpStepSummary httpDetail
) {}
