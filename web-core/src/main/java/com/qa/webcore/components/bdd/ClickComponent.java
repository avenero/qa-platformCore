package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.ClickSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Clicks e Interacciones.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.click")
public class ClickComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 60;

    @Override
    public String getName() {
        return "Clicks e Interacciones";
    }

    @Override
    public String getDisplayName() {
        return "Clicks e Interacciones";
    }

    @Override
    public String getDescription() {
        return "Click, doble click, click derecho, hover, shadow DOM";
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
        return "touch_app";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return ClickSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "click", "clic", "tap", "press", "pulsar",
            "boton", "button", "link", "enlace", "interact"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Clicks e Interacciones",
            "en", "Clicks & Interactions",
            "fr", "Clics et interactions"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Click, doble click, click derecho, hover, shadow DOM",
            "en", "Click, double click, right click, hover, shadow DOM",
            "fr", "Clic, double clic, clic droit, survol, shadow DOM"
        );
    }
}
