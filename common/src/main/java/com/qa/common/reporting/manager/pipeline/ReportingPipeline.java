package com.qa.common.reporting.manager.pipeline;

import com.qa.common.logging.TestLogger;
import com.qa.common.reporting.core.config.ReportingConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline de reporting que ejecuta steps en secuencia.
 * Implementa Chain of Responsibility pattern.
 *
 * FLUJO:
 * 1. ConversionStep → Convierte raw results a TestExecutionResult
 * 2. ExtentGenerationStep → Genera reporte HTML
 * 3. JiraUpdateStep → Actualiza status en Jira
 * 4. AttachmentUploadStep → Sube attachments a Jira
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class ReportingPipeline {

    private final List<ReportingStep> steps;
    private final ReportingConfig config;

    public ReportingPipeline(List<ReportingStep> steps, ReportingConfig config) {
        this.steps = steps;
        this.config = config;
    }

    /**
     * Ejecuta el pipeline completo.
     *
     * @param rawResults resultados en formato nativo (Cucumber JSON, JUnit XML, etc.)
     * @return resultado del pipeline
     */
    public PipelineResult execute(String rawResults) {
        TestLogger.logInfo("REPORTING_PIPELINE", "🚀 Iniciando pipeline de reporting", null);
        TestLogger.logInfo("REPORTING_PIPELINE",
            String.format("   Steps configurados: %d", steps.size()), null);

        PipelineContext context = new PipelineContext(rawResults, config);

        int executedSteps = 0;
        int skippedSteps = 0;

        for (ReportingStep step : steps) {
            String stepName = step.getName();

            // Verificar si el step está habilitado
            if (!step.isEnabled(config)) {
                TestLogger.logInfo("REPORTING_PIPELINE",
                    String.format("⏭️  Step [%s] deshabilitado, omitiendo", stepName), null);
                skippedSteps++;
                continue;
            }

            TestLogger.logInfo("REPORTING_PIPELINE",
                String.format("▶️  Ejecutando step: %s", stepName), null);

            try {
                StepResult result = step.execute(context);
                context.addStepResult(stepName, result);

                if (result.isSuccess()) {
                    TestLogger.logInfo("REPORTING_PIPELINE",
                        String.format("✅ Step [%s] completado exitosamente", stepName), null);
                    if (result.getMessage() != null) {
                        TestLogger.logDebug("REPORTING_PIPELINE",
                            "   " + result.getMessage(), null);
                    }
                    executedSteps++;
                } else {
                    TestLogger.logError("REPORTING_PIPELINE",
                        String.format("❌ Step [%s] falló: %s", stepName, result.getMessage()), null);

                    // Si el step es requerido, fallar el pipeline
                    if (step.isRequired()) {
                        TestLogger.logError("REPORTING_PIPELINE",
                            "🛑 Step requerido falló, abortando pipeline", null);
                        return PipelineResult.failure(stepName, result.getMessage(), context);
                    } else {
                        TestLogger.logWarning("REPORTING_PIPELINE",
                            "⚠️  Step opcional falló, continuando", null);
                    }
                }

            } catch (Exception e) {
                TestLogger.logException("REPORTING_PIPELINE",
                    String.format("💥 Excepción en step [%s]: %s", stepName, e.getMessage()), e);

                StepResult errorResult = StepResult.failure(e.getMessage(), e);
                context.addStepResult(stepName, errorResult);

                // Si el step es requerido, fallar el pipeline
                if (step.isRequired()) {
                    TestLogger.logError("REPORTING_PIPELINE",
                        "🛑 Step requerido lanzó excepción, abortando pipeline", null);
                    return PipelineResult.failure(stepName, e.getMessage(), context);
                } else {
                    TestLogger.logWarning("REPORTING_PIPELINE",
                        "⚠️  Step opcional lanzó excepción, continuando", null);
                }
            }
        }

        // Pipeline completado
        TestLogger.logInfo("REPORTING_PIPELINE", "🏁 Pipeline completado", null);
        TestLogger.logInfo("REPORTING_PIPELINE",
            String.format("   Steps ejecutados: %d", executedSteps), null);
        TestLogger.logInfo("REPORTING_PIPELINE",
            String.format("   Steps omitidos: %d", skippedSteps), null);

        return PipelineResult.success(context);
    }

    /**
     * Builder para construir pipelines con steps específicos.
     */
    public static class Builder {
        private final List<ReportingStep> steps = new ArrayList<>();
        private ReportingConfig config;

        public Builder withConfig(ReportingConfig config) {
            this.config = config;
            return this;
        }

        public Builder addStep(ReportingStep step) {
            this.steps.add(step);
            return this;
        }

        public Builder addSteps(List<ReportingStep> steps) {
            this.steps.addAll(steps);
            return this;
        }

        public ReportingPipeline build() {
            if (config == null) {
                throw new IllegalStateException("ReportingConfig es requerido");
            }
            return new ReportingPipeline(steps, config);
        }
    }
}

