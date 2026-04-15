package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponsePerformanceSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps: Performance.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.performance")
public class ApiPerformanceComponent implements StepComponent {
    @Override public String getName()                  { return "Performance"; }
    @Override public String getDisplayName()           { return "Performance"; }
    @Override public String getDescription()           { return "Validacion de tiempo de respuesta y tamano del body"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion de Respuesta"; }
    @Override public String getIcon()                  { return "speed"; }
    @Override public int getDisplayOrder()             { return 110; }
    @Override public Class<?> getStepDefinitionClass() { return ResponsePerformanceSteps.class; }

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
