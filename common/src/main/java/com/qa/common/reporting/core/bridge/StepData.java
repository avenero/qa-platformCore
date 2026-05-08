package com.qa.common.reporting.core.bridge;

import com.qa.common.reporting.core.model.TestStatus;

/**
 * Immutable bridge record for a single test step (Given/When/Then/And/But).
 *
 * <p>{@code protocolDetail} is null when the step has no protocol-level interaction to display.
 * The concrete type is provided by the caller (e.g. {@code HttpDetailData} from {@code http-core},
 * or a future {@code DbQueryDetail} from {@code database-core});
 * {@code common} only knows the {@link StepDetail} contract.
 *
 * @since 2.3.0 (field renamed from {@code httpDetail} to {@code protocolDetail})
 */
public record StepData(
        String keyword,
        String name,
        TestStatus status,
        long durationMs,
        String errorMessage,
        String stackTrace,
        StepDetail protocolDetail
) {}
