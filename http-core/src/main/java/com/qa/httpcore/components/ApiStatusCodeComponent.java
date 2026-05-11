package com.qa.httpcore.components;

import com.qa.httpcore.steps.validation.StatusCodeSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Status Code.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.status")
public class ApiStatusCodeComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 80;
    @Override
    public String getName() {
        return "Status Code";
    }
    @Override
    public String getDisplayName() {
        return "Status Code";
    }
    @Override
    public String getDescription() {
        return "Validacion del codigo de estado HTTP";
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
        return "check_circle";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return StatusCodeSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "status", "code", "codigo", "statut", "http-code",
            "response-code", "200", "201", "400", "401", "403", "404", "500"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Status Code",
            "en", "Status Code",
            "fr", "Code de statut"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Validacion del codigo de estado HTTP",
            "en", "HTTP status code validation",
            "fr", "Validation du code de statut HTTP"
        );
    }
}
