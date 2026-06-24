package com.qa.httpcore.utils;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Utilidad para detectar parámetros de steps que contienen URLs absolutas hardcodeadas.
 *
 * <p>Se usa como fallback cuando un step no tiene la anotación
 * {@code StepMetadata}: si el parámetro en runtime parece
 * una URL absoluta, el {@link com.qa.httpcore.plugin.ApiPlugin} lo registra como
 * {@link com.qa.httpcore.execution.BaseUrlSource#STEP_LITERAL}.
 *
 * <p>No se usa para bloquear ejecuciones; solo para observabilidad y logging.
 */
public final class UrlLiteralDetector {

    private static final Pattern ABSOLUTE_URL =
        Pattern.compile("^https?://[^\\s/$.?#][^\\s]*$", Pattern.CASE_INSENSITIVE);

    private UrlLiteralDetector() {}

    /**
     * Retorna {@code true} si el valor parece ser una URL absoluta (comienza con
     * {@code http://} o {@code https://} y contiene al menos un carácter de host).
     */
    public static boolean isAbsoluteUrl(String value) {
        return value != null && ABSOLUTE_URL.matcher(value.trim()).matches();
    }

    /**
     * Si el valor contiene una URL absoluta, devuelve un mensaje de advertencia listo
     * para incluir en los warnings del escenario; si no, devuelve {@link Optional#empty()}.
     *
     * @param stepPattern pattern Cucumber del step
     * @param paramValue  valor del parámetro en runtime
     */
    public static Optional<String> buildWarningIfLiteral(String stepPattern, String paramValue) {
        if (!isAbsoluteUrl(paramValue)) { return Optional.empty(); }
        return Optional.of(String.format(
            "El step '%s' usó una URL absoluta literal: '%s'. " +
            "Considerá reemplazarla por un path relativo y configurar el host en el ambiente.",
            stepPattern, paramValue
        ));
    }
}
