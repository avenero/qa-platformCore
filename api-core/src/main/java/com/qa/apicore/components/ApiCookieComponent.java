package com.qa.apicore.components;

import com.qa.apicore.steps.config.CookieSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Cookies.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiCookieComponent implements StepComponent {
    @Override public String getName()                  { return "Cookies"; }
    @Override public String getId()                    { return "api.cookies"; }
    @Override public String getDisplayName()           { return "Cookies"; }
    @Override public String getDescription()           { return "Gestion de cookies en la peticion HTTP"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "cookie"; }
    @Override public int getDisplayOrder()             { return 40; }
    @Override public Class<?> getStepDefinitionClass() { return CookieSteps.class; }
}
