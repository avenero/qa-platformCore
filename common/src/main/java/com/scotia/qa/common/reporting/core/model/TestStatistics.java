package com.scotia.qa.common.reporting.core.model;

import java.util.List;

/**
 * Estadísticas de una ejecución de tests
 */
public class TestStatistics {

    private int total;
    private int passed;
    private int failed;
    private int skipped;
    private int todo;

    private double successRate;
    private long totalDurationMs;

    public TestStatistics() {
        // Constructor vacío
    }

    /**
     * Calcula estadísticas a partir de lista de scenarios
     */
    public void calculateFromScenarios(List<ScenarioResult> scenarios) {
        total = scenarios.size();
        passed = 0;
        failed = 0;
        skipped = 0;
        todo = 0;
        totalDurationMs = 0;

        for (ScenarioResult scenario : scenarios) {
            switch (scenario.getStatus()) {
                case PASS -> passed++;
                case FAIL -> failed++;
                case SKIP -> skipped++;
                case TODO -> todo++;
            }

            if (scenario.getDuration() != null) {
                totalDurationMs += scenario.getDuration().toMillis();
            }
        }

        successRate = total > 0 ? (double) passed / total * 100 : 0;
    }

    // Getters y setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getTodo() {
        return todo;
    }

    public void setTodo(int todo) {
        this.todo = todo;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }
}
