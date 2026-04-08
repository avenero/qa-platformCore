package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.ScrollSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Scroll.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ScrollComponent implements StepComponent {
    @Override public String getName()                  { return "Scroll"; }
    @Override public String getId()                    { return "web.scroll"; }
    @Override public String getDisplayName()           { return "Scroll"; }
    @Override public String getDescription()           { return "Scroll hacia elementos o direcciones"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "swap_vert"; }
    @Override public int getDisplayOrder()             { return 90; }
    @Override public Class<?> getStepDefinitionClass() { return ScrollSteps.class; }
}
