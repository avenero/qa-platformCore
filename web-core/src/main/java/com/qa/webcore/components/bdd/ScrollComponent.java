package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.ScrollSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Scroll.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.scroll")
public class ScrollComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 90;

    @Override
    public String getName() {
        return "Scroll";
    }

    @Override
    public String getDisplayName() {
        return "Scroll";
    }

    @Override
    public String getDescription() {
        return "Scroll hacia elementos o direcciones";
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
        return "swap_vert";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return ScrollSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "scroll", "desplazar", "bajar", "subir", "down",
            "up", "scroll-to", "wheel", "deslizar"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Scroll",
            "en", "Scroll",
            "fr", "Defilement"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Scroll hacia elementos o direcciones",
            "en", "Scroll to elements or directions",
            "fr", "Faire defiler vers des elements ou des directions"
        );
    }
}
