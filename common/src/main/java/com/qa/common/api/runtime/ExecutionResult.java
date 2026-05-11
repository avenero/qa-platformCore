package com.qa.common.api.runtime;
import com.qa.common.utils.security.SecurityUtilities;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DTO inmutable que representa el resultado de una ejecucion BDD.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ExecutionResult {

    /**
     * Estado general de la ejecucion.
     */
    public enum Status {
        PASSED,
        FAILED,
        ERROR
    }

    private final Status status;
    private final int totalScenarios;
    private final int passedScenarios;
    private final int failedScenarios;
    private final Duration duration;
    private final Instant startTime;
    private final Instant endTime;
    private final List<String> errors;

    private ExecutionResult(Builder builder) {
        this.status = builder.status;
        this.totalScenarios = builder.totalScenarios;
        this.passedScenarios = builder.passedScenarios;
        this.failedScenarios = builder.failedScenarios;
        this.duration = builder.duration;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.errors = Collections.unmodifiableList(builder.errors);
    }

    /** @return estado general de la ejecución */
    public Status getStatus() {
        return status;
    }

    /** @return total de escenarios ejecutados */
    public int getTotalScenarios() {
        return totalScenarios;
    }

    /** @return escenarios que pasaron */
    public int getPassedScenarios() {
        return passedScenarios;
    }

    /** @return escenarios que fallaron */
    public int getFailedScenarios() {
        return failedScenarios;
    }

    /** @return duración total de la ejecución */
    public Duration getDuration() {
        return duration;
    }

    /** @return instante de inicio de la ejecución */
    public Instant getStartTime() {
        return startTime;
    }

    /** @return instante de fin de la ejecución */
    public Instant getEndTime() {
        return endTime;
    }

    /** @return lista de errores registrados */
    public List<String> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return status == Status.PASSED;
    }

    @Override
    public String toString() {
        return "ExecutionResult{status=" + status
                + ", total=" + totalScenarios
                + ", passed=" + passedScenarios
                + ", failed=" + failedScenarios
                + ", duration=" + duration + "}";
    }

    /**
     * Builder for constructing {@link ExecutionResult} instances.
     */
    public static final class Builder {
        private Status status = Status.PASSED;
        private int totalScenarios;
        private int passedScenarios;
        private int failedScenarios;
        private Duration duration = Duration.ZERO;
        private Instant startTime = Instant.now();
        private Instant endTime = Instant.now();
        private List<String> errors = List.of();

        public Builder status(Status status) {
            this.status = Objects.requireNonNull(status);
            return this;
        }

        public Builder totalScenarios(int total) {
            this.totalScenarios = total;
            return this;
        }

        public Builder passedScenarios(int passed) {
            this.passedScenarios = passed;
            return this;
        }

        public Builder failedScenarios(int failed) {
            this.failedScenarios = failed;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = Objects.requireNonNull(duration);
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = Objects.requireNonNull(startTime);
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = Objects.requireNonNull(endTime);
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = Objects.requireNonNull(errors);
            return this;
        }

        public ExecutionResult build() {
            return new ExecutionResult(this);
        }
    }
}
