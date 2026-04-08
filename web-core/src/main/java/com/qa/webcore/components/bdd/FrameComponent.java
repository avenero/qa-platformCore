package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.navigation.FrameSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Frames e iFrames.
 * Fase BDD: WHEN. Categoria: Navegacion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class FrameComponent implements StepComponent {
    @Override public String getName()                  { return "Frames e iFrames"; }
    @Override public String getId()                    { return "web.frame"; }
    @Override public String getDisplayName()           { return "Frames e iFrames"; }
    @Override public String getDescription()           { return "Cambio de contexto a frames e iFrames"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Navegacion"; }
    @Override public String getIcon()                  { return "layers"; }
    @Override public int getDisplayOrder()             { return 40; }
    @Override public Class<?> getStepDefinitionClass() { return FrameSteps.class; }
}
