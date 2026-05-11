package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.config.WebEnvironmentSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Ambiente Web.
 * Fase BDD: GIVEN. Categoria: Configuracion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.environment")
public class WebEnvironmentComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 20;

    @Override
    public String getName() {
        return "Ambiente Web";
    }

    @Override
    public String getDisplayName() {
        return "Ambiente Web";
    }

    @Override
    public String getDescription() {
        return "URL base, timeouts, cookies de configuracion";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.GIVEN;
    }

    @Override
    public String getCategory() {
        return "Configuracion Web";
    }

    @Override
    public String getIcon() {
        return "settings";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return WebEnvironmentSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "environment", "entorno", "config", "variable", "env-var",
            "web-config", "ambiente", "setting"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Ambiente Web",
            "en", "Web Environment",
            "fr", "Environnement Web"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "URL base, timeouts, cookies de configuracion",
            "en", "Base URL, timeouts, configuration cookies",
            "fr", "URL de base, delais d'attente, cookies de configuration"
        );
    }
}
