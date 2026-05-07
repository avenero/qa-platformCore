package com.qa.common.reporting.core.bridge;

import com.qa.common.reporting.core.model.TestStatus;

import java.time.Instant;
import java.util.List;

/**
 * Immutable bridge record for a test scenario and all its steps.
 * {@code outlineExample} is null when {@code isOutline} is false.
 */
public record ScenarioData(
        String scenarioName,
        String featureFile,
        TestStatus status,
        Instant startTime,
        Instant endTime,
        long durationMs,
        String errorMessage,
        String businessMessage,
        String stackTrace,
        List<StepData> steps,
        List<String> tags,
        boolean isOutline,
        String outlineExample,
        List<AttachmentData> screenshots
) {}
