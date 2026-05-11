package com.qa.httpcore.components;

import com.qa.httpcore.steps.validation.ResponsePerformanceSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Performance.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.performance")
public class ApiPerformanceComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 110;
    @Override
    public String getName() {
        return "Performance";
    }
    @Override
    public String getDisplayName() {
        return "Performance";
    }
    @Override
    public String getDescription() {
        return "Validacion de tiempo de respuesta y tamano del body";
    }
    @Override
    public BddPhase getPhase() {
        return BddPhase.THEN;
    }
    @Override
    public String getCategory() {
        return "Validacion de Respuesta";
    }
    @Override
    public String getIcon() {
        return "speed";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return ResponsePerformanceSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "performance", "rendimiento", "latencia", "latency",
            "response-time", "tiempo-respuesta", "throughput", "tps",
            "sla", "timeout", "body-size", "tamano"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Performance",
            "en", "Performance",
            "fr", "Performance"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Validacion de tiempo de respuesta y tamano del body",
            "en", "Response time and body size validation",
            "fr", "Validation du temps de reponse et de la taille du corps"
        );
    }
}
