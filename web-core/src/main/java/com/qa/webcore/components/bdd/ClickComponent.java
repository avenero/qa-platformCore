package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.ClickSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Clicks e Interacciones.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ClickComponent implements StepComponent {
    @Override public String getName()                  { return "Clicks e Interacciones"; }
    @Override public String getId()                    { return "web.click"; }
    @Override public String getDisplayName()           { return "Clicks e Interacciones"; }
    @Override public String getDescription()           { return "Click, doble click, click derecho, hover, shadow DOM"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "touch_app"; }
    @Override public int getDisplayOrder()             { return 60; }
    @Override public Class<?> getStepDefinitionClass() { return ClickSteps.class; }
}
