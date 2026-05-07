package com.qa.apicore.reporting.bridge;

import com.qa.common.reporting.core.bridge.HttpStepSummary;

import java.util.Map;

/**
 * Registro puente inmutable de una interacción HTTP capturada durante un step de prueba.
 *
 * <p>Implementa {@link HttpStepSummary} para participar en el pipeline de reporting de
 * {@code common} sin crear dependencia {@code common → http-core}. La dirección de dependencia
 * correcta se mantiene: {@code http-core → common}.
 *
 * <p>Todos los datos sensibles deben estar <b>ya redactados</b> antes de construir este record.
 * Usar {@link com.qa.apicore.reporting.util.HttpDetailRedactor} como fábrica.
 *
 * @since 2.2.0
 * @see com.qa.apicore.reporting.util.HttpDetailRedactor
 */
public record HttpDetailData(
        String method,
        String urlPath,
        int httpStatus,
        String requestId,
        String requestSummary,
        String responseSummary,
        Integer requestBodySizeBytes,
        Integer responseBodySizeBytes,
        Long durationMs,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders
) implements HttpStepSummary {}
