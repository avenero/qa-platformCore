package com.qa.mobileagent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Evento del wire-protocol v1 emitido por SSE en
 * {@code GET /v1/runs/&#123;id&#125;/events}.
 *
 * <p>Tipos soportados (estables, R-2 RFC-AGENT-01: NUNCA renombrar/borrar):
 * <ul>
 *   <li>{@code SCENARIO_STARTED}  — { name }</li>
 *   <li>{@code STEP_STARTED}      — { stepText }</li>
 *   <li>{@code STEP_PASSED}       — { stepText, durationMs }</li>
 *   <li>{@code STEP_FAILED}       — { stepText, errorClass, errorMessage }</li>
 *   <li>{@code STEP_SKIPPED}      — { stepText }</li>
 *   <li>{@code SCENARIO_COMPLETED}— { outcome, durationMs }</li>
 *   <li>{@code EXECUTION_COMPLETED} — { status, total, passed, failed, durationMs }</li>
 *   <li>{@code EXECUTION_ERROR}   — { errorClass, errorMessage }</li>
 * </ul>
 *
 * <p>Para añadir un nuevo tipo de evento sólo se permite extender el enum y
 * añadir campos opcionales — nunca borrar campos existentes.
 *
 * @since TASK-I04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentEvent(
        String type,
        String executionId,
        Instant timestamp,
        String name,
        String stepText,
        Long durationMs,
        String outcome,
        String status,
        Integer total,
        Integer passed,
        Integer failed,
        String errorClass,
        String errorMessage
) {

    public static AgentEvent scenarioStarted(String execId, String name) {
        return new AgentEvent("SCENARIO_STARTED", execId, Instant.now(),
                name, null, null, null, null, null, null, null, null, null);
    }

    public static AgentEvent stepStarted(String execId, String stepText) {
        return new AgentEvent("STEP_STARTED", execId, Instant.now(),
                null, stepText, null, null, null, null, null, null, null, null);
    }

    public static AgentEvent stepPassed(String execId, String stepText, long durationMs) {
        return new AgentEvent("STEP_PASSED", execId, Instant.now(),
                null, stepText, durationMs, null, null, null, null, null, null, null);
    }

    public static AgentEvent stepFailed(String execId, String stepText, Throwable err) {
        return new AgentEvent("STEP_FAILED", execId, Instant.now(),
                null, stepText, null, null, null, null, null, null,
                err == null ? "Unknown" : err.getClass().getName(),
                err == null ? null : err.getMessage());
    }

    public static AgentEvent stepSkipped(String execId, String stepText) {
        return new AgentEvent("STEP_SKIPPED", execId, Instant.now(),
                null, stepText, null, null, null, null, null, null, null, null);
    }

    public static AgentEvent scenarioCompleted(String execId, String outcome, long durationMs) {
        return new AgentEvent("SCENARIO_COMPLETED", execId, Instant.now(),
                null, null, durationMs, outcome, null, null, null, null, null, null);
    }

    public static AgentEvent executionCompleted(String execId, String status,
                                                int total, int passed, int failed,
                                                long durationMs) {
        return new AgentEvent("EXECUTION_COMPLETED", execId, Instant.now(),
                null, null, durationMs, null, status, total, passed, failed, null, null);
    }

    public static AgentEvent executionError(String execId, Throwable err) {
        return new AgentEvent("EXECUTION_ERROR", execId, Instant.now(),
                null, null, null, null, null, null, null, null,
                err == null ? "Unknown" : err.getClass().getName(),
                err == null ? null : err.getMessage());
    }
}
