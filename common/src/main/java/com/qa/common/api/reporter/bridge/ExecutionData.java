package com.qa.common.api.reporter.bridge;

import com.qa.common.api.reporter.model.TestStatus;

import java.time.Instant;
import java.util.List;

/**
 * Immutable bridge record representing a full test execution, passed to ReportGeneratorPort.
 * Consumers (BE, qa-module-test) convert their internal models into this before calling the port.
 */
public record ExecutionData(
        String executionKey,
        String projectKey,
        Instant startTime,
        Instant endTime,
        TestStatus overallStatus,
        String environment,
        int totalScenarios,
        int passedScenarios,
        int failedScenarios,
        int skippedScenarios,
        List<ScenarioData> scenarios,
        EnvironmentDetails environmentDetails
) {}
