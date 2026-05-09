package com.qa.httpcore.components;

import com.qa.httpcore.steps.config.AuthenticationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps: Autenticacion.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.authentication")
public class ApiAuthComponent implements StepComponent {

    /** Display order for this component in the UI step palette. */
    private static final int DISPLAY_ORDER = 20;
    @Override
    public String getName() {
        return "Autenticacion";
    }
    @Override
    public String getDisplayName() {
        return "Autenticacion";
    }
    @Override
    public String getDescription() {
        return "Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT";
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
        return "lock";
    }
    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }
    @Override
    public Class<?> getStepDefinitionClass() {
        return AuthenticationSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "auth", "authentication", "autenticacion", "authentification",
            "token", "bearer", "jwt", "oauth", "oauth2",
            "login", "credentials", "credenciales", "apikey", "session"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Autenticacion",
            "en", "Authentication",
            "fr", "Authentification"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT",
            "en", "Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT",
            "fr", "Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT"
        );
    }
}
