package com.qa.common.internal.reporting.manager.pipeline.steps;
import com.qa.common.api.Internal;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.reporter.bridge.ExecutionData;
import com.qa.common.api.reporter.config.ExtentConfig;
import com.qa.common.api.reporter.port.ReportGeneratorPort;
import com.qa.common.internal.reporting.config.MutableReportingConfig;
import com.qa.common.internal.reporting.manager.pipeline.PipelineContext;
import com.qa.common.internal.reporting.manager.pipeline.ReportingStep;
import com.qa.common.internal.reporting.manager.pipeline.PipelineStepResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Step 2: Generación de reporte Extent (HTML).
 *
 * <p>Resuelve {@link ReportGeneratorPort} vía {@link ServiceLoader} (mismo
 * descubrimiento SPI que usa el Backend), proyecta el {@link ExecutionData} del
 * contexto a HTML autocontenido y lo escribe al path de reporte configurado —
 * preservando el contrato del consumidor CLI (espera un archivo en disco).
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
@Internal
public class ExtentGenerationStep implements ReportingStep {

    @Override
    public String getName() {
        return "ExtentGeneration";
    }

    @Override
    public boolean isEnabled(MutableReportingConfig config) {
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

        ExecutionData executionData = context.getExecutionData();
        if (executionData == null) {
            return PipelineStepResult.failure("ExecutionData no disponible en contexto");
        }

        try {
            ExtentConfig extentConfig = context.getConfig().getExtent();
            ReportGeneratorPort generator = loadGenerator();

            String html = generator.generateHtml(executionData, extentConfig);
            String reportPath = writeReport(extentConfig, html);

            // Guardar path en contexto
            context.setExtentReportPath(reportPath);

            return PipelineStepResult.success("Reporte generado en: " + reportPath);

        } catch (Exception e) {
            TestLogger.logException("EXTENT_STEP",
                "Error generando reporte Extent: " + e.getMessage(), e);
            return PipelineStepResult.failure("Error generando Extent Report: " + e.getMessage(), e);
        }
    }

    /**
     * Resuelve la implementación de {@link ReportGeneratorPort} publicada en
     * {@code META-INF/services} (idéntico a {@code ReportingInfrastructureConfig} del BE).
     */
    private ReportGeneratorPort loadGenerator() {
        return ServiceLoader.load(ReportGeneratorPort.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No ReportGeneratorPort SPI provider found in classpath. "
                        + "Check META-INF/services in common module."));
    }

    /**
     * Escribe el HTML autocontenido al path de reporte configurado, creando el
     * directorio de salida si no existe, y devuelve el path escrito.
     */
    private String writeReport(ExtentConfig extentConfig, String html) throws java.io.IOException {
        File outputDir = new File(extentConfig.getOutputPath());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        Path reportPath = Path.of(extentConfig.getFullReportPath());
        Files.writeString(reportPath, html, StandardCharsets.UTF_8);
        return reportPath.toString();
    }
}

