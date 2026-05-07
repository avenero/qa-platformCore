package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponseBodySteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps: Response Body.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.response.body")
public class ApiResponseBodyComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 90;
    @Override
    public String getName() { return "Response Body"; }
    @Override
    public String getDisplayName() { return "Response Body"; }
    @Override
    public String getDescription() {
        return "Validacion y extraccion del cuerpo de respuesta JSON";
    }
    @Override
    public BddPhase getPhase() { return BddPhase.THEN; }
    @Override
    public String getCategory() { return "Validacion de Respuesta"; }
    @Override
    public String getIcon() { return "data_object"; }
    @Override
    public int getDisplayOrder() { return DISPLAY_ORDER; }
    @Override
    public Class<?> getStepDefinitionClass() { return ResponseBodySteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Response Body",
            "en", "Response Body",
            "fr", "Corps de la reponse"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Validacion y extraccion del cuerpo de respuesta JSON",
            "en", "JSON response body validation and extraction",
            "fr", "Validation et extraction du corps de reponse JSON"
        );
    }
}
