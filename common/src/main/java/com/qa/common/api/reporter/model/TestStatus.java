package com.qa.common.api.reporter.model;


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

    /**
     * @return estado en formato Jira
     */
    public String getJiraStatus() {
        return jiraStatus;
    }

    /**
     * Convierte status de Cucumber a TestStatus.
     *
     * @param cucumberStatus estado devuelto por Cucumber (ej: "passed", "failed")
     * @return TestStatus equivalente
     */
    public static TestStatus fromCucumber(String cucumberStatus) {
        if (cucumberStatus == null) {
            return TODO;
        }

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
     * Convierte status de JUnit a TestStatus.
     *
     * @param junitStatus estado devuelto por JUnit (ej: "successful", "failed")
     * @return TestStatus equivalente
     */
    public static TestStatus fromJUnit(String junitStatus) {
        if (junitStatus == null) {
            return TODO;
        }

        return switch (junitStatus.toLowerCase()) {
            case "successful", "passed" -> PASS;
            case "failed" -> FAIL;
            case "skipped", "disabled", "aborted" -> SKIP;
            default -> TODO;
        };
    }
}
