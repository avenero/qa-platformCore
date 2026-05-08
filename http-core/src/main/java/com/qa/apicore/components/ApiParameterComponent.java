package com.qa.apicore.components;

import com.qa.apicore.steps.config.ParameterSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Query Parameters.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.parameters")
public class ApiParameterComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 50;
    @Override
    public String getName() {
        return "Query Parameters";
    }
    @Override
    public String getDisplayName() {
        return "Query Parameters";
    }
    @Override
    public String getDescription() {
        return "Parametros de URL y path de la peticion";
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
        return "search";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return ParameterSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "param", "parameter", "parametro", "query", "querystring",
            "path-param", "path-variable", "url-param", "variable", "placeholder"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Query Parameters",
            "en", "Query Parameters",
            "fr", "Parametres de requete"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Parametros de URL y path de la peticion",
            "en", "URL and path parameters of the request",
            "fr", "Parametres d'URL et de chemin de la requete"
        );
    }
}
