package com.qa.httpcore.execution;

import java.time.Instant;

/**
 * Registro inmutable de un cambio de base URL ocurrido durante la ejecución.
 * Se acumula en {@link BaseUrlTracker#getHistory()} y se envía al BE al finalizar el escenario.
 *
 * @param url         URL normalizada que se aplicó (siempre con schema, sin trailing slash).
 * @param source      Origen del cambio.
 * @param stepPattern Pattern Cucumber del step que lo provocó; {@code null} si fue el bootstrap automático.
 * @param at          Instante en que se aplicó el cambio.
 */
public record BaseUrlChange(
    String        url,
    BaseUrlSource source,
    String        stepPattern,
    Instant       at
) {}
