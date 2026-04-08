package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.PageValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Validacion de Pagina.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
public class PageValidationComponent implements StepComponent {
    @Override public String getName()                  { return "Validacion de Pagina"; }
    @Override public String getId()                    { return "web.validation.page"; }
    @Override public String getDisplayName()           { return "Validacion de Pagina"; }
    @Override public String getDescription()           { return "Titulo, URL, existencia de texto en pagina"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Web"; }
    @Override public String getIcon()                  { return "pageview"; }
    @Override public int getDisplayOrder()             { return 130; }
    @Override public Class<?> getStepDefinitionClass() { return PageValidationSteps.class; }
}
