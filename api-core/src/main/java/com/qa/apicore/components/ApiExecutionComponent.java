package com.qa.apicore.components;

import com.qa.apicore.steps.execution.HttpExecutionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps: Ejecucion HTTP.
 * Fase BDD: WHEN. Categoria: Ejecucion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.execution")
public class ApiExecutionComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 70;
    @Override
    public String getName() {
        return "Ejecucion HTTP";
    }
    @Override
    public String getDisplayName() {
        return "Ejecucion HTTP";
    }
    @Override
    public String getDescription() {
        return "Envio de peticiones GET, POST, PUT, DELETE, PATCH";
    }
    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }
    @Override
    public String getCategory() {
        return "Ejecucion";
    }
    @Override
    public String getIcon() {
        return "send";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return HttpExecutionSteps.class;
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Ejecucion HTTP",
            "en", "HTTP Execution",
            "fr", "Execution HTTP"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Envio de peticiones GET, POST, PUT, DELETE, PATCH",
            "en", "Send GET, POST, PUT, DELETE, PATCH requests",
            "fr", "Envoi de requetes GET, POST, PUT, DELETE, PATCH"
        );
    }
}
