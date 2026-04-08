package com.qa.apicore.components;

import com.qa.apicore.steps.config.ParameterSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Query Parameters.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiParameterComponent implements StepComponent {
    @Override public String getName()                  { return "Query Parameters"; }
    @Override public String getId()                    { return "api.parameters"; }
    @Override public String getDisplayName()           { return "Query Parameters"; }
    @Override public String getDescription()           { return "Parametros de URL y path de la peticion"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "search"; }
    @Override public int getDisplayOrder()             { return 50; }
    @Override public Class<?> getStepDefinitionClass() { return ParameterSteps.class; }
}
