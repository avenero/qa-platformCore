package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.interaction.GestureSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Gestos.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class GestureComponent implements StepComponent {
    @Override public String getName()                  { return "Gestos"; }
    @Override public String getId()                    { return "mobile.gesture"; }
    @Override public String getDisplayName()           { return "Gestos"; }
    @Override public String getDescription()           { return "Tap, long press, swipe, pinch, zoom"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "gesture"; }
    @Override public int getDisplayOrder()             { return 30; }
    @Override public Class<?> getStepDefinitionClass() { return GestureSteps.class; }
}
