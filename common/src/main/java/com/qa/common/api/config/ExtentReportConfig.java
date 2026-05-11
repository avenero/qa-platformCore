package com.qa.common.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Configuración específica de Extent Reports (TASK-K03M-F6.d).
 *
 * <p>Prefijo: {@code extent} (override del default que sería {@code "extentreport"}).
 * Mantiene compat con {@code config-app.properties} legacy que usa
 * {@code extent.outputPath}, {@code extent.theme}, etc.
 *
 * <p>El scan dinámico {@code extent.systemInfo.*} (Map de keys arbitrarias) NO
 * encaja en este record y queda pendiente para un helper aparte
 * ({@code ExtentSystemInfoLoader}, follow-up de M9).
 *
 * @since TASK-K03M-F6
 */
public record ExtentReportConfig(
        boolean enabled,
        @NotBlank String outputPath,
        @NotBlank String reportName,
        @NotBlank String documentTitle,
        @NotBlank String reportTitle,
        @NotBlank @Pattern(regexp = "STANDARD|DARK",
                message = "theme debe ser STANDARD o DARK")
        String theme,
        boolean includeScreenshots,
        boolean includeSystemInfo,
        boolean includeTimeline
) implements TypedConfig {

    public static ExtentReportConfig defaults() {
        return new ExtentReportConfig(
                true,
                "build/reports/extent/",
                "execution-report.html",
                "Test Execution Report",
                "Automated Tests",
                "STANDARD",
                true,
                true,
                true
        );
    }

    @Override
    public String configPrefix() {
        return "extent";
    }
}
