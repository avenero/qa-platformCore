package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.wait.WaitSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Esperas.
 * Fase BDD: WHEN. Categoria: Esperas.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.wait")
public class WaitComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 110;

    @Override
    public String getName() {
        return "Esperas";
    }

    @Override
    public String getDisplayName() {
        return "Esperas";
    }

    @Override
    public String getDescription() {
        return "Esperas explicitas sobre elementos y condiciones";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }

    @Override
    public String getCategory() {
        return "Esperas";
    }

    @Override
    public String getIcon() {
        return "hourglass_empty";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return WaitSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "wait", "esperar", "pause", "pausa", "attendre",
            "timeout", "visible", "presence", "loading", "spinner"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Esperas",
            "en", "Waits",
            "fr", "Attentes"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Esperas explicitas sobre elementos y condiciones",
            "en", "Explicit waits on elements and conditions",
            "fr", "Attentes explicites sur les elements et les conditions"
        );
    }
}
