package com.qa.apicore.components;

import com.qa.apicore.steps.execution.HttpExecutionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Ejecucion HTTP.
 * Fase BDD: WHEN. Categoria: Ejecucion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiExecutionComponent implements StepComponent {
    @Override public String getName()                  { return "Ejecucion HTTP"; }
    @Override public String getId()                    { return "api.execution"; }
    @Override public String getDisplayName()           { return "Ejecucion HTTP"; }
    @Override public String getDescription()           { return "Envio de peticiones GET, POST, PUT, DELETE, PATCH"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Ejecucion"; }
    @Override public String getIcon()                  { return "send"; }
    @Override public int getDisplayOrder()             { return 70; }
    @Override public Class<?> getStepDefinitionClass() { return HttpExecutionSteps.class; }
}
