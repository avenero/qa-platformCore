package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.SelectSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Select y Dropdowns.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class SelectComponent implements StepComponent {
    @Override public String getName()                  { return "Select y Dropdowns"; }
    @Override public String getId()                    { return "web.select"; }
    @Override public String getDisplayName()           { return "Select y Dropdowns"; }
    @Override public String getDescription()           { return "Seleccion de opciones en elementos select y radio"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "arrow_drop_down"; }
    @Override public int getDisplayOrder()             { return 80; }
    @Override public Class<?> getStepDefinitionClass() { return SelectSteps.class; }
}
