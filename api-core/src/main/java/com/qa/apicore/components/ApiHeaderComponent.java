package com.qa.apicore.components;

import com.qa.apicore.steps.config.HeaderSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps: Headers.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiHeaderComponent implements StepComponent {
    @Override public String getName()                  { return "Headers"; }
    @Override public String getId()                    { return "api.headers"; }
    @Override public String getDisplayName()           { return "Headers"; }
    @Override public String getDescription()           { return "Gestion de cabeceras HTTP de la peticion"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "view_list"; }
    @Override public int getDisplayOrder()             { return 30; }
    @Override public Class<?> getStepDefinitionClass() { return HeaderSteps.class; }

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
