package com.qa.common.transport.wire;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Wire-protocol v1 — body de {@code POST /v1/runs} hacia el agente remoto
 * (TASK-I03 / espejo del DTO en {@code mobile-agent} TASK-I04).
 *
 * <p>El campo {@link #featureContents()} es un mapa de {@code basename → texto}
 * de cada {@code .feature}; el cliente {@code HttpAgentTransport} lo construye
 * leyendo los paths locales recibidos en
 * {@link com.qa.common.transport.ExecutionTransport#submit}. El agente
 * materializa los archivos en su workspace local — el cliente NUNCA envía
 * paths del filesystem (RFC-AGENT-01 §contrato featurePaths).
 *
 * <p>Este record vive en {@code common.transport.wire} para que tanto el
 * transport HTTP como el módulo {@code mobile-agent} puedan compartirlo
 * (mobile-agent define su propio {@code SubmitRequest} record con el mismo
 * contrato JSON; ambos son aditivos vía R-2 RFC-AGENT-01).
 *
 * @since TASK-I03
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitRequestDto(
        String environment,
        String browser,
        String tags,
        boolean parallelEnabled,
        int threadCount,
        String httpEngine,
        boolean trustAllSsl,
        Map<String, String> properties,
        Map<String, String> featureContents
) { }
