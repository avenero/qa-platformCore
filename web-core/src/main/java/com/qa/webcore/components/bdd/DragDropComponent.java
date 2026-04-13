package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.DragDropSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps Web: Drag and Drop.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class DragDropComponent implements StepComponent {
    @Override public String getName()                  { return "Drag and Drop"; }
    @Override public String getId()                    { return "web.dragdrop"; }
    @Override public String getDisplayName()           { return "Drag and Drop"; }
    @Override public String getDescription()           { return "Arrastrar y soltar elementos"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "open_with"; }
    @Override public int getDisplayOrder()             { return 95; }
    @Override public Class<?> getStepDefinitionClass() { return DragDropSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Drag and Drop",
            "en", "Drag and Drop",
            "fr", "Glisser-deposer"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Arrastrar y soltar elementos",
            "en", "Drag and drop elements",
            "fr", "Faire glisser et deposer des elements"
        );
    }
}
