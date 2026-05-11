package com.qa.common.reporting.manager.pipeline.steps;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.reporting.core.config.ReportingConfig;
import com.qa.common.reporting.core.model.TestExecutionResult;
import com.qa.common.reporting.extent.generator.ExtentReportGenerator;
import com.qa.common.reporting.manager.pipeline.PipelineContext;
import com.qa.common.reporting.manager.pipeline.ReportingStep;
import com.qa.common.reporting.manager.pipeline.PipelineStepResult;

/**
 * Step 2: Generación de reporte Extent (HTML).
 *
 * Crea reporte HTML visual con screenshots embebidos y estadísticas.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class ExtentGenerationStep implements ReportingStep {

    @Override
    public String getName() {
        return "ExtentGeneration";
    }

    @Override
    public boolean isEnabled(ReportingConfig config) {
        return config.getExtent().isEnabled();
    }

    @Override
    public boolean isRequired() {
        // Opcional: si falla, no aborta pipeline
        return false;
    }

    @Override
    public PipelineStepResult execute(PipelineContext context) {
        TestLogger.logInfo("EXTENT_STEP", "📊 Generando reporte Extent Reports", null);

        TestExecutionResult result = context.getTestExecutionResult();
        if (result == null) {
            return PipelineStepResult.failure("TestExecutionResult no disponible en contexto");
        }

        try {
            ExtentReportGenerator generator = new ExtentReportGenerator(
                context.getConfig().getExtent()
            );

            String reportPath = generator.generate(result);

            // Guardar path en contexto
            context.setExtentReportPath(reportPath);

            return PipelineStepResult.success("Reporte generado en: " + reportPath);

        } catch (Exception e) {
            TestLogger.logException("EXTENT_STEP",
                "Error generando reporte Extent: " + e.getMessage(), e);
            return PipelineStepResult.failure("Error generando Extent Report: " + e.getMessage(), e);
        }
    }
}

