package com.qa.httpcore.components;

import com.qa.httpcore.steps.config.CookieSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Cookies.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.cookies")
public class ApiCookieComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 40;
    @Override
    public String getName() {
        return "Cookies";
    }
    @Override
    public String getDisplayName() {
        return "Cookies";
    }
    @Override
    public String getDescription() {
        return "Gestion de cookies en la peticion HTTP";
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
        return "cookie";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return CookieSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "cookie", "cookies", "galleta", "session", "set-cookie",
            "http-only", "secure-cookie", "jar", "domain", "path"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Cookies",
            "en", "Cookies",
            "fr", "Cookies"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Gestion de cookies en la peticion HTTP",
            "en", "Cookie management in the HTTP request",
            "fr", "Gestion des cookies dans la requete HTTP"
        );
    }
}
