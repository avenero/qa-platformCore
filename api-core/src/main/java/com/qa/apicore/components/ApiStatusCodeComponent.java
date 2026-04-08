package com.qa.apicore.components;

import com.qa.apicore.steps.validation.StatusCodeSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Status Code.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiStatusCodeComponent implements StepComponent {
    @Override public String getName()                  { return "Status Code"; }
    @Override public String getId()                    { return "api.status"; }
    @Override public String getDisplayName()           { return "Status Code"; }
    @Override public String getDescription()           { return "Validacion del codigo de estado HTTP"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion de Respuesta"; }
    @Override public String getIcon()                  { return "check_circle"; }
    @Override public int getDisplayOrder()             { return 80; }
    @Override public Class<?> getStepDefinitionClass() { return StatusCodeSteps.class; }
}
