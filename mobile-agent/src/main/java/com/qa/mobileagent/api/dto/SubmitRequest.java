package com.qa.mobileagent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Payload del wire-protocol v1 para {@code POST /v1/runs}.
 *
 * <p>El BE NO envía paths locales por la red (RFC-AGENT-01 §contrato
 * featurePaths): manda el contenido de cada {@code .feature} en
 * {@link #featureContents()} y el agente los materializa en su workspace.
 *
 * <p>Los campos {@link #environment()}, {@link #browser()}, {@link #tags()} y
 * {@link #properties()} se mapean 1:1 a un {@link com.qa.common.api.runtime.ExecutionConfig}
 * vía su builder. {@link #httpEngine()} es opcional; si {@code null}, el agente
 * resuelve el default vía {@code HttpEngine.resolveDefault()}.
 *
 * <p>Nota: este DTO NO contiene {@code SSLContext} (no serializable). Si una
 * ejecución requiere mTLS, el BE pre-resuelve el SSLContext en su lado y
 * envía cualquier credencial necesaria como propiedad opaque. Esto es
 * consistente con TASK-2.7 (SSL/TLS) donde el BE es el único custodio de
 * material criptográfico.
 *
 * @param environment      ambiente lógico (no null, no blank)
 * @param browser          navegador para web; vacío si la ejecución es mobile
 * @param tags             expresión de tags Cucumber; vacío para "todos"
 * @param parallelEnabled  paralelismo de scenarios
 * @param threadCount      threads paralelos (≥1)
 * @param httpEngine       motor HTTP — null = default JVM
 * @param trustAllSsl      ignorar validación TLS (sólo entornos no-prod)
 * @param properties       map opaca de propiedades del runtime
 * @param featureContents  filename → texto del .feature, no vacío
 *
 * @since TASK-I04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitRequest(
        @NotNull String environment,
        String browser,
        String tags,
        boolean parallelEnabled,
        int threadCount,
        String httpEngine,
        boolean trustAllSsl,
        Map<String, String> properties,
        @NotEmpty Map<String, String> featureContents
) {
    public SubmitRequest {
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment requerido");
        }
        if (featureContents == null || featureContents.isEmpty()) {
            throw new IllegalArgumentException("featureContents requerido");
        }
        if (threadCount < 1) {
            threadCount = 1;
        }
        // null-coalesce a Map vacío para evitar NPEs río abajo.
        if (properties == null) {
            properties = Map.of();
        }
        if (browser == null) { browser = ""; }
        if (tags == null) { tags = ""; }
    }
}
