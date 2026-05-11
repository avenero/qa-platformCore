package com.qa.common.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Configuración de logging del Core (TASK-K03; suelo para K05 MDC + structured logging).
 *
 * <p>Prefijo: {@code logging}.
 *
 * @param rootLevel    Nivel raíz: TRACE | DEBUG | INFO | WARN | ERROR.
 * @param layout       text (dev) | json (CI/prod).
 * @param redactKeys   Claves de MDC cuyos valores se redactan en output (password, token, …).
 * @param mdcEnabled   Si MDC está activo para propagar correlation IDs.
 * @param qaLogsVerbose Activación de logs verbose del framework (`qa.logs.verbose`).
 *
 * @since TASK-K03
 */
public record LoggingConfig(
        @NotBlank @Pattern(regexp = "TRACE|DEBUG|INFO|WARN|ERROR",
                message = "rootLevel debe ser TRACE/DEBUG/INFO/WARN/ERROR")
        String rootLevel,
        @NotBlank @Pattern(regexp = "text|json",
                message = "layout debe ser text o json")
        String layout,
        @NotNull List<String> redactKeys,
        boolean mdcEnabled,
        boolean qaLogsVerbose
) implements TypedConfig {

    public static LoggingConfig defaults() {
        return new LoggingConfig(
                "INFO",
                "text",
                List.of("password", "token", "authorization", "secret", "apiKey"),
                true,
                false
        );
    }
}
