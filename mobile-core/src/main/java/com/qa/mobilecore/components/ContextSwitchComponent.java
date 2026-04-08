package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.interaction.ContextSwitchSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Cambio de Contexto.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ContextSwitchComponent implements StepComponent {
    @Override public String getName()                  { return "Cambio de Contexto"; }
    @Override public String getId()                    { return "mobile.context"; }
    @Override public String getDisplayName()           { return "Cambio de Contexto"; }
    @Override public String getDescription()           { return "Cambiar entre contexto nativo y WebView"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "swap_horiz"; }
    @Override public int getDisplayOrder()             { return 50; }
    @Override public Class<?> getStepDefinitionClass() { return ContextSwitchSteps.class; }
}
