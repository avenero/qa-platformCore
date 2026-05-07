package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponseSecuritySteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps: Seguridad.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.security")
public class ApiSecurityComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 120;
    @Override
    public String getName() {
        return "Seguridad";
    }
    @Override
    public String getDisplayName() {
        return "Seguridad";
    }
    @Override
    public String getDescription() {
        return "Validacion de controles de seguridad HTTP";
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
        return "security";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return ResponseSecuritySteps.class;
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Seguridad",
            "en", "Security",
            "fr", "Securite"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Validacion de controles de seguridad HTTP",
            "en", "HTTP security controls validation",
            "fr", "Validation des controles de securite HTTP"
        );
    }
}
