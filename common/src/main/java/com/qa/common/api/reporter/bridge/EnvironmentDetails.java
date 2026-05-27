package com.qa.common.api.reporter.bridge;

import java.util.Map;

/**
 * Immutable bridge record for execution environment metadata included in reports.
 */
public record EnvironmentDetails(
        String os,
        String osVersion,
        String javaVersion,
        String browser,
        String browserVersion,
        String device,
        String platform,
        Map<String, String> customInfo
) {}
