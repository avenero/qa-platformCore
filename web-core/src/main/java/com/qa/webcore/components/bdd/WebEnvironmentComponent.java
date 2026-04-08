package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.config.WebEnvironmentSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Ambiente Web.
 * Fase BDD: GIVEN. Categoria: Configuracion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
public class WebEnvironmentComponent implements StepComponent {
    @Override public String getName()                  { return "Ambiente Web"; }
    @Override public String getId()                    { return "web.environment"; }
    @Override public String getDisplayName()           { return "Ambiente Web"; }
    @Override public String getDescription()           { return "URL base, timeouts, cookies de configuracion"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Web"; }
    @Override public String getIcon()                  { return "settings"; }
    @Override public int getDisplayOrder()             { return 20; }
    @Override public Class<?> getStepDefinitionClass() { return WebEnvironmentSteps.class; }
}
