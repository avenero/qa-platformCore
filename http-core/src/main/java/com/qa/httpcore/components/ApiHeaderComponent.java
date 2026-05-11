package com.qa.httpcore.components;

import com.qa.httpcore.steps.config.HeaderSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Headers.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.headers")
public class ApiHeaderComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 30;
    @Override
    public String getName() {
        return "Headers";
    }
    @Override
    public String getDisplayName() {
        return "Headers";
    }
    @Override
    public String getDescription() {
        return "Gestion de cabeceras HTTP de la peticion";
    }
    @Override
    public BddPhase getPhase() {
        return BddPhase.GIVEN;
    }
    @Override
    public String getCategory() {
        return "Configuracion de Peticion";
    }
    @Override
    public String getIcon() {
        return "view_list";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return HeaderSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "header", "headers", "cabecera", "en-tete", "content-type",
            "accept", "authorization", "custom-header", "x-header", "http-header"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Headers",
            "en", "Headers",
            "fr", "En-tetes HTTP"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Gestion de cabeceras HTTP de la peticion",
            "en", "HTTP request headers management",
            "fr", "Gestion des en-tetes HTTP de la requete"
        );
    }
}
