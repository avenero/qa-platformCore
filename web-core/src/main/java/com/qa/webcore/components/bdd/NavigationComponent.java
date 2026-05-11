package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.navigation.NavigationSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Navegacion.
 * Fase BDD: WHEN. Categoria: Navegacion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.navigation")
public class NavigationComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 30;

    @Override
    public String getName() {
        return "Navegacion";
    }

    @Override
    public String getDisplayName() {
        return "Navegacion";
    }

    @Override
    public String getDescription() {
        return "Navegar a URL, historial, refresh, flujos complejos";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }

    @Override
    public String getCategory() {
        return "Navegacion";
    }

    @Override
    public String getIcon() {
        return "navigation";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return NavigationSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "navigate", "navegar", "go-to", "open", "url",
            "page", "load", "back", "forward", "refresh", "pagina"
        );
    }

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
