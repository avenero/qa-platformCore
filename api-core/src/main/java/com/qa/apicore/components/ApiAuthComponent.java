package com.qa.apicore.components;

import com.qa.apicore.steps.config.AuthenticationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Autenticacion.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiAuthComponent implements StepComponent {
    @Override public String getName()                  { return "Autenticacion"; }
    @Override public String getId()                    { return "api.authentication"; }
    @Override public String getDisplayName()           { return "Autenticacion"; }
    @Override public String getDescription()           { return "Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "lock"; }
    @Override public int getDisplayOrder()             { return 20; }
    @Override public Class<?> getStepDefinitionClass() { return AuthenticationSteps.class; }
}
