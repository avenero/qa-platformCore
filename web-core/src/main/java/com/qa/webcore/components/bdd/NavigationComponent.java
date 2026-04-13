package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.navigation.NavigationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps Web: Navegacion.
 * Fase BDD: WHEN. Categoria: Navegacion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NavigationComponent implements StepComponent {
    @Override public String getName()                  { return "Navegacion"; }
    @Override public String getId()                    { return "web.navigation"; }
    @Override public String getDisplayName()           { return "Navegacion"; }
    @Override public String getDescription()           { return "Navegar a URL, historial, refresh, flujos complejos"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Navegacion"; }
    @Override public String getIcon()                  { return "navigation"; }
    @Override public int getDisplayOrder()             { return 30; }
    @Override public Class<?> getStepDefinitionClass() { return NavigationSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Navegacion",
            "en", "Navigation",
            "fr", "Navigation"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Navegar a URL, historial, refresh, flujos complejos",
            "en", "Navigate to URL, history, refresh, complex flows",
            "fr", "Naviguer vers l'URL, historique, actualisation, flux complexes"
        );
    }
}
