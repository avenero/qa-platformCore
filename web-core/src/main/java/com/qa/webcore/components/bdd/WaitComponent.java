package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.wait.WaitSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Esperas.
 * Fase BDD: WHEN. Categoria: Esperas.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WaitComponent implements StepComponent {
    @Override public String getName()                  { return "Esperas"; }
    @Override public String getId()                    { return "web.wait"; }
    @Override public String getDisplayName()           { return "Esperas"; }
    @Override public String getDescription()           { return "Esperas explicitas sobre elementos y condiciones"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Esperas"; }
    @Override public String getIcon()                  { return "hourglass_empty"; }
    @Override public int getDisplayOrder()             { return 110; }
    @Override public Class<?> getStepDefinitionClass() { return WaitSteps.class; }
}
