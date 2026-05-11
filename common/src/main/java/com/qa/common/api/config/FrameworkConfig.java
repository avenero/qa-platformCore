package com.qa.common.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Configuración transversal del framework — claves que no encajan en
 * Http/Web/Mobile/Logging/Concurrency (TASK-K03; identificada en gap audit
 * pre-K03-migrate).
 *
 * <p>Prefijo: {@code framework}.
 *
 * @param moduleName Identificador lógico del módulo activo (PLATFORM, MOBILE, …).
 *                   Usado por hooks (`WebHooksSteps`, `MobileHooksSteps`) para
 *                   etiquetar logs/MDC.
 * @param environment Ambiente lógico: dev | qa | uat | staging | prod.
 *                    Default `dev`.
 *
 * @since TASK-K03 (gap audit 2026-05-11)
 */
public record FrameworkConfig(
        @NotBlank String moduleName,
        @NotBlank @Pattern(regexp = "dev|qa|uat|staging|prod",
                message = "environment debe ser dev|qa|uat|staging|prod")
        String environment
) implements TypedConfig {

    public static FrameworkConfig defaults() {
        return new FrameworkConfig("PLATFORM", "dev");
    }
}
