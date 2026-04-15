package com.qa.apicore.components;

import com.qa.apicore.steps.config.UrlConfigSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps: URL / Ambiente.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("api.url")
public class ApiUrlComponent implements StepComponent {
    @Override public String getName()                  { return "URL / Ambiente"; }
    @Override public String getDisplayName()           { return "URL / Ambiente"; }
    @Override public String getDescription()           { return "Configuracion de base URL, ambiente y protocolo"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "link"; }
    @Override public int getDisplayOrder()             { return 10; }
    @Override public Class<?> getStepDefinitionClass() { return UrlConfigSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "URL / Ambiente",
            "en", "URL / Environment",
            "fr", "URL / Environnement"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Configuracion de base URL, ambiente y protocolo",
            "en", "Base URL, environment and protocol configuration",
            "fr", "Configuration de l'URL de base, de l'environnement et du protocole"
        );
    }
}
