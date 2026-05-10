package com.qa.common.transport.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire-protocol v1 — respuesta de {@code POST /v1/runs}.
 *
 * @param executionId UUID asignado por el agente
 * @param eventsUrl   ruta relativa al stream SSE
 *
 * @since TASK-I03
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitResponseDto(String executionId, String eventsUrl) { }
