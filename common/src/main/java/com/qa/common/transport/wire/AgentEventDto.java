package com.qa.common.transport.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire-protocol v1 — payload de cada evento SSE emitido por el agente
 * en {@code GET /v1/runs/&#123;id&#125;/events} (TASK-I03).
 *
 * <p>R-2 RFC-AGENT-01: estos campos son la fuente de verdad del contrato
 * remoto. **Solo cambios aditivos** — añadir campos nuevos opcionales o
 * tipos de evento nuevos, NUNCA renombrar / borrar.
 *
 * <p>Tipos canónicos: {@code SCENARIO_STARTED}, {@code STEP_STARTED},
 * {@code STEP_PASSED}, {@code STEP_FAILED}, {@code STEP_SKIPPED},
 * {@code SCENARIO_COMPLETED}, {@code EXECUTION_COMPLETED},
 * {@code EXECUTION_ERROR}.
 *
 * @since TASK-I03
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentEventDto(
        String type,
        String executionId,
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
) { }
