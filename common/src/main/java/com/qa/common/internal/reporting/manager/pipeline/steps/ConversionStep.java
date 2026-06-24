package com.qa.common.internal.reporting.manager.pipeline.steps;
import com.qa.common.api.Internal;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.reporter.bridge.ExecutionData;
import com.qa.common.internal.reporting.adapter.ResultAdapter;
import com.qa.common.internal.reporting.adapter.CucumberResultAdapter;
import com.qa.common.internal.reporting.config.MutableReportingConfig;
import com.qa.common.internal.reporting.manager.pipeline.PipelineContext;
import com.qa.common.internal.reporting.manager.pipeline.ReportingStep;
import com.qa.common.internal.reporting.manager.pipeline.PipelineStepResult;

/**
 * Step 1: Conversión de resultados raw al modelo puente.
 *
 * Detecta automáticamente el formato (Cucumber JSON, JUnit XML, etc.)
 * y usa el adaptador apropiado para convertir a ExecutionData.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
@Internal
public class ConversionStep implements ReportingStep {

    @Override
    public String getName() {
        return "Conversion";
    }

    @Override
    public boolean isEnabled(MutableReportingConfig config) {
        // Siempre habilitado (requerido para todos los pipelines)
        return true;
    }

    @Override
    public boolean isRequired() {
        // Obligatorio: sin conversión no hay reporting
        return true;
    }

    @Override
    public PipelineStepResult execute(PipelineContext context) {
        TestLogger.logInfo("CONVERSION_STEP", "📝 Convirtiendo resultados a modelo estándar", null);

        String rawResults = context.getRawResults();
        if (rawResults == null || rawResults.trim().isEmpty()) {
            return PipelineStepResult.failure("Raw results vacíos o nulos");
        }

        try {
            // Encontrar adaptador apropiado
            ResultAdapter adapter = findAdapter(rawResults);

            if (adapter == null) {
                return PipelineStepResult.failure("No se encontró adaptador compatible para el formato");
            }

            TestLogger.logInfo("CONVERSION_STEP",
                "   Adaptador seleccionado: " + adapter.getName(), null);

            // Convertir
            ExecutionData result = adapter.convert(rawResults);

            if (result == null) {
                return PipelineStepResult.failure("El adaptador retornó null");
            }

            // Validar resultado
            if (result.scenarios() == null || result.scenarios().isEmpty()) {
                TestLogger.logWarning("CONVERSION_STEP",
                    "⚠️  No se encontraron scenarios en los resultados", null);
            } else {
                TestLogger.logInfo("CONVERSION_STEP",
                    String.format("   Scenarios procesados: %d", result.scenarios().size()), null);
            }

            // Guardar en contexto
            context.setExecutionData(result);

            return PipelineStepResult.success(
                String.format("Convertidos %d scenarios exitosamente",
                    result.scenarios() != null ? result.scenarios().size() : 0)
            );

        } catch (Exception e) {
            TestLogger.logException("CONVERSION_STEP",
                "Error convirtiendo resultados: " + e.getMessage(), e);
            return PipelineStepResult.failure("Error en conversión: " + e.getMessage(), e);
        }
    }

    /**
     * Encuentra el adaptador apropiado para el formato de resultados.
     *
     * Actualmente soporta:
     * - Cucumber JSON (default)
     * - JUnit XML (futuro)
     */
    private ResultAdapter findAdapter(String rawResults) {
        // Crear adaptadores disponibles
        ResultAdapter cucumberAdapter = new CucumberResultAdapter(true); // Con evidencias

        // Intentar cada adaptador
        if (cucumberAdapter.canHandle(rawResults)) {
            return cucumberAdapter;
        }

        // Agregar más adaptadores aquí cuando se implementen
        // ResultAdapter junitAdapter = new JUnitResultAdapter();
        // if (junitAdapter.canHandle(rawResults)) {
        //     return junitAdapter;
        // }

        TestLogger.logWarning("CONVERSION_STEP",
            "⚠️  No se encontró adaptador específico, usando Cucumber por defecto", null);

        return cucumberAdapter; // Default
    }
}

