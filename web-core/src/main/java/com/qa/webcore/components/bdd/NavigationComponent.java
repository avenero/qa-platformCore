package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.navigation.NavigationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Navegacion.
 * Fase BDD: WHEN. Categoria: Navegacion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NavigationComponent implements StepComponent {
    @Override public String getName()                  { return "Navegacion"; }
    @Override public String getId()                    { return "web.navigation"; }
    @Override public String getDisplayName()           { return "Navegacion"; }
    @Override public String getDescription()           { return "Navegar a URL, historial, refresh, flujos complejos"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Navegacion"; }
    @Override public String getIcon()                  { return "navigation"; }
    @Override public int getDisplayOrder()             { return 30; }
    @Override public Class<?> getStepDefinitionClass() { return NavigationSteps.class; }
}
