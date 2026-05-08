package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.interaction.ContextSwitchSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Mobile: Cambio de Contexto.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("mobile.context")
public class ContextSwitchComponent implements StepComponent {

    private static final int DISPLAY_ORDER = 50;

    @Override public String getName()                  { return "Cambio de Contexto"; }
    @Override public String getDisplayName()           { return "Cambio de Contexto"; }
    @Override public String getDescription()           { return "Cambiar entre contexto nativo y WebView"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "swap_horiz"; }
    @Override public int getDisplayOrder()             { return DISPLAY_ORDER; }
    @Override public Class<?> getStepDefinitionClass() { return ContextSwitchSteps.class; }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "context", "webview", "native", "switch", "cambiar",
            "webkit", "hybrid", "contexto-nativo", "context-switch"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Cambio de Contexto",
            "en", "Context Switch",
            "fr", "Changement de contexte"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Cambiar entre contexto nativo y WebView",
            "en", "Switch between native context and WebView",
            "fr", "Basculer entre le contexte natif et WebView"
        );
    }
}
