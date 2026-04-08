package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.interaction.NativeElementSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Elementos Nativos.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NativeElementComponent implements StepComponent {
    @Override public String getName()                  { return "Elementos Nativos"; }
    @Override public String getId()                    { return "mobile.element"; }
    @Override public String getDisplayName()           { return "Elementos Nativos"; }
    @Override public String getDescription()           { return "Interaccion con elementos de la UI nativa"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "widgets"; }
    @Override public int getDisplayOrder()             { return 40; }
    @Override public Class<?> getStepDefinitionClass() { return NativeElementSteps.class; }
}
