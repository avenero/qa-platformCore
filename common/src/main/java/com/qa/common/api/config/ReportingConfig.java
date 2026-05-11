package com.qa.common.api.config;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuración de reporting transversal del Core (TASK-K03M-F6.d).
 *
 * <p>Prefijo: {@code reporting}. Cubre la pieza agnóstica al tooling
 * (enabled/environment/cucumberJsonPath). Las opciones específicas de Extent
 * viven en {@link ExtentReportConfig} (prefijo separado {@code extent}) para
 * preservar compat con {@code config-app.properties} legacy que usa keys
 * {@code extent.*}.
 *
 * @param enabled           Master switch. {@code false} omite generación de reportes.
 * @param environment       Etiqueta de ambiente para los reportes (ej. {@code "qa"}, {@code "prod"}).
 * @param cucumberJsonPath  Path relativo al {@code cucumber.json} desde {@code user.dir}.
 *
 * @since TASK-K03M-F6
 */
public record ReportingConfig(
        boolean enabled,
        @NotBlank String environment,
        @NotBlank String cucumberJsonPath
) implements TypedConfig {

    public static ReportingConfig defaults() {
        return new ReportingConfig(
                true,
                "local",
                "build/reports/cucumber/cucumber.json"
        );
    }
}
