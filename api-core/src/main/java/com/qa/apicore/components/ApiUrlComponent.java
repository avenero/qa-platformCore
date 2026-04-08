package com.qa.apicore.components;

import com.qa.apicore.steps.config.UrlConfigSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: URL / Ambiente.
 * Fase BDD: GIVEN. Categoria: Configuracion de Peticion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiUrlComponent implements StepComponent {
    @Override public String getName()                  { return "URL / Ambiente"; }
    @Override public String getId()                    { return "api.url"; }
    @Override public String getDisplayName()           { return "URL / Ambiente"; }
    @Override public String getDescription()           { return "Configuracion de base URL, ambiente y protocolo"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion de Peticion"; }
    @Override public String getIcon()                  { return "link"; }
    @Override public int getDisplayOrder()             { return 10; }
    @Override public Class<?> getStepDefinitionClass() { return UrlConfigSteps.class; }
}
