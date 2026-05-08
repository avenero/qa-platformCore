package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.interaction.NativeElementSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Mobile: Elementos Nativos.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("mobile.element")
public class NativeElementComponent implements StepComponent {

    private static final int DISPLAY_ORDER = 40;

    @Override public String getName()                  { return "Elementos Nativos"; }
    @Override public String getDisplayName()           { return "Elementos Nativos"; }
    @Override public String getDescription()           { return "Interaccion con elementos de la UI nativa"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "widgets"; }
    @Override public int getDisplayOrder()             { return DISPLAY_ORDER; }
    @Override public Class<?> getStepDefinitionClass() { return NativeElementSteps.class; }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "native", "element", "interact", "accessibility-id",
            "xpath", "class-name", "id", "ui-element", "nativo"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Elementos Nativos",
            "en", "Native Elements",
            "fr", "Elements natifs"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Interaccion con elementos de la UI nativa",
            "en", "Interaction with native UI elements",
            "fr", "Interaction avec les elements de l'interface utilisateur native"
        );
    }
}
