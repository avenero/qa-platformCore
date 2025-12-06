package com.scotia.qa.common.reporting.manager.pipeline.steps;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.core.model.TestExecutionResult;
import com.scotia.qa.common.reporting.extent.generator.ExtentReportGenerator;
import com.scotia.qa.common.reporting.manager.pipeline.PipelineContext;
import com.scotia.qa.common.reporting.manager.pipeline.ReportingStep;
import com.scotia.qa.common.reporting.manager.pipeline.StepResult;

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
    public StepResult execute(PipelineContext context) {
        TestLogger.logInfo("EXTENT_STEP", "📊 Generando reporte Extent Reports", null);

        TestExecutionResult result = context.getTestExecutionResult();
        if (result == null) {
            return StepResult.failure("TestExecutionResult no disponible en contexto");
        }

        try {
            ExtentReportGenerator generator = new ExtentReportGenerator(
                context.getConfig().getExtent()
            );

            String reportPath = generator.generate(result);

            // Guardar path en contexto
            context.setExtentReportPath(reportPath);

            return StepResult.success("Reporte generado en: " + reportPath);

        } catch (Exception e) {
            TestLogger.logException("EXTENT_STEP",
                "Error generando reporte Extent: " + e.getMessage(), e);
            return StepResult.failure("Error generando Extent Report: " + e.getMessage(), e);
        }
    }
}

