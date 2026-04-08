package com.qa.common.reporting.extent.generator;

import com.qa.common.logging.TestLogger;
import com.qa.common.reporting.core.config.ReportingConfig;
import com.qa.common.reporting.manager.pipeline.PipelineResult;
import com.qa.common.reporting.manager.pipeline.ReportingPipeline;
import com.qa.common.reporting.manager.pipeline.ReportingStep;
import com.qa.common.reporting.manager.pipeline.steps.ConversionStep;
import com.qa.common.reporting.manager.pipeline.steps.ExtentGenerationStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade principal del sistema de reporting.
 * Punto de entrada único para los módulos.
 *
 * USO:
 * <pre>
 * ReportingConfig config = ReportingConfig.fromConfigManager();
 * ReportingManager.initialize(config);
 *
 * String cucumberJson = loadCucumberResults();
 * PipelineResult result = ReportingManager.processTestResults(cucumberJson);
 *
 * if (result.isSuccess()) {
 *     System.out.println("Reporte generado: " + result.getExtentReportPath());
 * }
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class ReportingManager {

    private static ReportingConfig config;
    private static boolean initialized = false;

    private ReportingManager() {
        // Utility class - no instances
    }

    /**
     * Inicializa el ReportingManager con configuración.
     * Debe llamarse antes de procesar resultados.
     *
     * @param reportingConfig configuración de reporting
     */
    public static void initialize(ReportingConfig reportingConfig) {
        if (reportingConfig == null) {
            throw new IllegalArgumentException("ReportingConfig no puede ser null");
        }

        config = reportingConfig;
        initialized = true;

        TestLogger.logInfo("REPORTING_MANAGER", "✅ ReportingManager inicializado", null);
        TestLogger.logInfo("REPORTING_MANAGER", "   Reporting habilitado: " + config.isEnabled(), null);
        TestLogger.logInfo("REPORTING_MANAGER", "   Extent habilitado: " + config.getExtent().isEnabled(), null);
    }

    /**
     * Procesa resultados de tests y genera reportes.
     *
     * @param rawResults resultados en formato nativo (Cucumber JSON, JUnit XML, etc.)
     * @return resultado del pipeline
     */
    public static PipelineResult processTestResults(String rawResults) {
        validateInitialized();

        if (!config.isEnabled()) {
            TestLogger.logInfo("REPORTING_MANAGER",
                "⏭️  Reporting deshabilitado (reporting.enabled=false), omitiendo", null);
            return PipelineResult.failure("Reporting", "Reporting deshabilitado en configuración");
        }

        TestLogger.logInfo("REPORTING_MANAGER",
            "🚀 Procesando resultados de tests con ReportingManager", null);

        try {
            // Construir pipeline
            ReportingPipeline pipeline = buildPipeline();

            // Ejecutar
            PipelineResult result = pipeline.execute(rawResults);

            if (result.isSuccess()) {
                TestLogger.logInfo("REPORTING_MANAGER", "✅ Reporting completado exitosamente", null);
                logResults(result);
            } else {
                TestLogger.logError("REPORTING_MANAGER",
                    String.format("❌ Reporting falló en step: %s", result.getFailedStep()), null);
                TestLogger.logError("REPORTING_MANAGER",
                    "   Error: " + result.getErrorMessage(), null);
            }

            return result;

        } catch (Exception e) {
            TestLogger.logException("REPORTING_MANAGER",
                "Error procesando resultados: " + e.getMessage(), e);
            return PipelineResult.failure("ReportingManager", e.getMessage());
        }
    }

    /**
     * Construye el pipeline con los steps configurados.
     */
    private static ReportingPipeline buildPipeline() {
        List<ReportingStep> steps = new ArrayList<>();

        // Step 1: Conversión (siempre habilitado)
        steps.add(new ConversionStep());

        // Step 2: Extent Reports (si está habilitado)
        steps.add(new ExtentGenerationStep());

        // Nota: Las integraciones externas (Jira/Xray) se manejan en el Backend.

        return new ReportingPipeline.Builder()
                .withConfig(config)
                .addSteps(steps)
                .build();
    }

    /**
     * Valida que el manager esté inicializado.
     */
    private static void validateInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                "ReportingManager no inicializado. Llama a ReportingManager.initialize(config) primero."
            );
        }
    }

    /**
     * Log de resultados finales.
     */
    private static void logResults(PipelineResult result) {
        if (result.getExtentReportPath() != null) {
            TestLogger.logInfo("REPORTING_MANAGER",
                "   📄 Extent Report: " + result.getExtentReportPath(), null);
        }

        // Log de steps ejecutados
        result.getStepResults().forEach((stepName, stepResult) -> {
            String status = stepResult.isSuccess() ? "✅" : "❌";
            TestLogger.logDebug("REPORTING_MANAGER",
                String.format("   %s Step [%s]: %s", status, stepName,
                    stepResult.getMessage() != null ? stepResult.getMessage() : "OK"), null);
        });
    }

    /**
     * Obtiene la configuración actual.
     */
    public static ReportingConfig getConfig() {
        validateInitialized();
        return config;
    }

    /**
     * Verifica si está inicializado.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset del manager (útil para tests).
     */
    public static void reset() {
        config = null;
        initialized = false;
        TestLogger.logDebug("REPORTING_MANAGER", "ReportingManager reseteado", null);
    }
}

