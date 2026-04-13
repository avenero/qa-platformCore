package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.navigation.WindowSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps Web: Ventanas y Pestanas.
 * Fase BDD: WHEN. Categoria: Navegacion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WindowComponent implements StepComponent {
    @Override public String getName()                  { return "Ventanas y Pestanas"; }
    @Override public String getId()                    { return "web.window"; }
    @Override public String getDisplayName()           { return "Ventanas y Pestanas"; }
    @Override public String getDescription()           { return "Gestion de multiples ventanas y pestanas"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Navegacion"; }
    @Override public String getIcon()                  { return "tab"; }
    @Override public int getDisplayOrder()             { return 50; }
    @Override public Class<?> getStepDefinitionClass() { return WindowSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Ventanas y Pestanas",
            "en", "Windows & Tabs",
            "fr", "Fenetres et onglets"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Gestion de multiples ventanas y pestanas",
            "en", "Multiple windows and tabs management",
            "fr", "Gestion de plusieurs fenetres et onglets"
        );
    }
}
