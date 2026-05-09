package com.qa.httpcore.execution;

import java.util.List;

/**
 * Metadatos de ejecución que el Core envía al BE al finalizar cada escenario.
 *
 * <p>El BE persiste estos datos en la tabla {@code executions} para que el FE
 * pueda mostrar de dónde provino la URL efectiva y si hubo cambios durante el escenario.
 *
 * @param effectiveBaseUrl URL base que se usó realmente en la última petición.
 * @param baseUrlSource    Origen de esa URL (nombre del enum {@link BaseUrlSource}).
 * @param baseUrlHistory   Historial cronológico de cambios de host en el escenario.
 */
public record ExecutionMeta(
    String             effectiveBaseUrl,
    String             baseUrlSource,
    List<BaseUrlChange> baseUrlHistory
) {

    /** Construye un {@code ExecutionMeta} a partir del tracker activo. */
    public static ExecutionMeta from(BaseUrlTracker tracker) {
        return new ExecutionMeta(
            tracker.getEffectiveBaseUrl(),
            tracker.getSource() != null ? tracker.getSource().name() : null,
            tracker.getHistory()
        );
    }

    /** {@code true} si hay URL efectiva disponible para persistir. */
    public boolean hasData() {
        return effectiveBaseUrl != null && !effectiveBaseUrl.isBlank();
    }
}
