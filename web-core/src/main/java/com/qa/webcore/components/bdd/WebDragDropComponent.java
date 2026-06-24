package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.DragDropSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Drag and Drop — sucesor no-deprecado de {@code "web.dragdrop"}.
 *
 * <p>Expone los steps de arrastrar/soltar y redimensionar bajo el id canónico
 * {@code "web.drag.drop"}, cumpliendo la promesa de deprecación declarada por
 * {@link DragDropComponent} ({@code @StepId("web.dragdrop", replacedBy = "web.drag.drop")}).
 *
 * <p>Es el <strong>único</strong> componente que enruta a {@link DragDropSteps}: el componente
 * deprecado {@link DragDropComponent} permanece registrado como alias del id {@code "web.dragdrop"}
 * pero ya no expone la clase de steps, evitando que el catálogo (Mundo #2) registre dos veces los
 * mismos {@code @StepDef}.
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see DragDropComponent
 * @see DragDropSteps
 */
@StepId("web.drag.drop")
public class WebDragDropComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 95;

    @Override
    public String getName() {
        return "Drag and Drop";
    }

    @Override
    public String getDisplayName() {
        return "Drag and Drop";
    }

    @Override
    public String getDescription() {
        return "Arrastrar y soltar elementos";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }

    @Override
    public String getCategory() {
        return "Interaccion";
    }

    @Override
    public String getIcon() {
        return "open_with";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return DragDropSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "drag", "drop", "arrastrar", "soltar", "move",
            "mover", "reorder", "dnd", "drag-and-drop", "glisser-deposer",
            "resize", "redimensionar"
        );
    }

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
