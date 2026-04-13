package com.qa.apicore.components;

import com.qa.apicore.steps.config.RequestBodySteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps: Request Body.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiRequestBodyComponent implements StepComponent {
    @Override public String getName()                  { return "Request Body"; }
    @Override public String getId()                    { return "api.body"; }
    @Override public String getDisplayName()           { return "Request Body"; }
    @Override public String getDescription()           { return "Cuerpo de la peticion: JSON, XML, form-data, template"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "description"; }
    @Override public int getDisplayOrder()             { return 60; }
    @Override public Class<?> getStepDefinitionClass() { return RequestBodySteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Request Body",
            "en", "Request Body",
            "fr", "Corps de la requete"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Cuerpo de la peticion: JSON, XML, form-data, template",
            "en", "Request body: JSON, XML, form-data, template",
            "fr", "Corps de la requete : JSON, XML, form-data, template"
        );
    }
}
