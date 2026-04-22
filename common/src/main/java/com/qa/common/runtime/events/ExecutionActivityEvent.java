package com.qa.common.runtime.events;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Evento de actividad de ejecución — stream seguro para consumo externo (BE, FE).
 *
 * <p>A diferencia de {@link ExecutionEvent} (usado internamente por el runtime),
 * este evento está diseñado para viajar hacia el Backend y, eventualmente, al Frontend.
 * Por eso sus campos son:
 * <ul>
 *   <li>Estables y neutros respecto a proveedores (sin Jira, Azure, etc.)</li>
 *   <li>Sanitizados: sin stack traces, nombres de clase, tokens, URLs con secretos</li>
 *   <li>Orientados al usuario funcional, no al operador técnico</li>
 * </ul>
 *
 * <p>El detalle técnico (excepción, stack trace, mensajes raw) debe permanecer
 * en el sink de logging ({@link EventBus} + SLF4J), nunca en este evento.
 */
public final class ExecutionActivityEvent {

    private final String executionId;
    private final String activityType;
    private final ActivityPhase phase;
    private final ActivitySeverity severity;
    private final String messageCode;
    private final String message;
    private final String scenarioName;
    private final String stepName;
    private final Instant timestamp;
    private final Map<String, String> attributes;
    private final FailureDescriptor failure;

    private ExecutionActivityEvent(Builder b) {
        this.executionId  = b.executionId;
        this.activityType = Objects.requireNonNull(b.activityType, "activityType no puede ser null");
        this.phase        = Objects.requireNonNull(b.phase, "phase no puede ser null");
        this.severity     = b.severity != null ? b.severity : ActivitySeverity.INFO;
        this.messageCode  = b.messageCode;
        this.message      = b.message;
        this.scenarioName = b.scenarioName;
        this.stepName     = b.stepName;
        this.timestamp    = b.timestamp != null ? b.timestamp : Instant.now();
        this.attributes   = b.attributes != null
                            ? Collections.unmodifiableMap(new HashMap<>(b.attributes))
                            : Collections.emptyMap();
        this.failure      = b.failure;
    }

    public static Builder builder(String activityType, ActivityPhase phase) {
        return new Builder(activityType, phase);
    }

    public String            getExecutionId()  { return executionId; }
    public String            getActivityType() { return activityType; }
    public ActivityPhase     getPhase()        { return phase; }
    public ActivitySeverity  getSeverity()     { return severity; }
    public String            getMessageCode()  { return messageCode; }
    public String            getMessage()      { return message; }
    public String            getScenarioName() { return scenarioName; }
    public String            getStepName()     { return stepName; }
    public Instant           getTimestamp()    { return timestamp; }
    public Map<String,String> getAttributes()  { return attributes; }
    public FailureDescriptor getFailure()      { return failure; }

    /**
     * Constructor fluido de {@link ExecutionActivityEvent}.
     */
    public static final class Builder {
        private String executionId;
        private final String activityType;
        private final ActivityPhase phase;
        private ActivitySeverity severity;
        private String messageCode;
        private String message;
        private String scenarioName;
        private String stepName;
        private Instant timestamp;
        private Map<String, String> attributes;
        private FailureDescriptor failure;

        private Builder(String activityType, ActivityPhase phase) {
            this.activityType = activityType;
            this.phase        = phase;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder severity(ActivitySeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder messageCode(String code) {
            this.messageCode = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder scenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder stepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder attributes(Map<String, String> attrs) {
            this.attributes = attrs;
            return this;
        }

        public Builder failure(FailureDescriptor failure) {
            this.failure = failure;
            return this;
        }

        public ExecutionActivityEvent build() {
            return new ExecutionActivityEvent(this);
        }
    }

    @Override
    public String toString() {
        return "ExecutionActivityEvent{type=" + activityType
               + ", phase=" + phase + ", severity=" + severity
               + ", code=" + messageCode + "}";
    }
}
