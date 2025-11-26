package com.scotia.qa.common.jiraXray.model;

/**
 * Estados estándar de test compatibles con Jira.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public enum TestStatus {
    PASS("PASS"),
    FAIL("FAIL"),
    SKIP("SKIP"),
    TODO("TODO"),
    EXECUTING("EXECUTING");

    private final String jiraStatus;

    TestStatus(String jiraStatus) {
        this.jiraStatus = jiraStatus;
    }

    public String getJiraStatus() {
        return jiraStatus;
    }

    /**
     * Convierte status de Cucumber a TestStatus
     */
    public static TestStatus fromCucumber(String cucumberStatus) {
        if (cucumberStatus == null) return TODO;

        return switch (cucumberStatus.toLowerCase()) {
            case "passed" -> PASS;
            case "failed" -> FAIL;
            case "skipped" -> SKIP;
            case "pending" -> TODO;
            case "undefined" -> TODO;
            default -> TODO;
        };
    }

    /**
     * Convierte status de JUnit a TestStatus
     */
    public static TestStatus fromJUnit(String junitStatus) {
        if (junitStatus == null) return TODO;

        return switch (junitStatus.toLowerCase()) {
            case "successful", "passed" -> PASS;
            case "failed" -> FAIL;
            case "skipped", "disabled", "aborted" -> SKIP;
            default -> TODO;
        };
    }
}
