package com.qa.mobileagent.api.dto;

/**
 * Respuesta de {@code POST /v1/runs}.
 *
 * @param executionId UUID asignado por el agente (mismo que el del transport)
 * @param eventsUrl   URL relativa al stream SSE: {@code /v1/runs/&#123;id&#125;/events}
 *
 * @since TASK-I04
 */
public record SubmitResponse(String executionId, String eventsUrl) { }
