package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponseSecuritySteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Seguridad.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiSecurityComponent implements StepComponent {
    @Override public String getName()                  { return "Seguridad"; }
    @Override public String getId()                    { return "api.security"; }
    @Override public String getDisplayName()           { return "Seguridad"; }
    @Override public String getDescription()           { return "Validacion de controles de seguridad HTTP"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion de Respuesta"; }
    @Override public String getIcon()                  { return "security"; }
    @Override public int getDisplayOrder()             { return 120; }
    @Override public Class<?> getStepDefinitionClass() { return ResponseSecuritySteps.class; }
}
