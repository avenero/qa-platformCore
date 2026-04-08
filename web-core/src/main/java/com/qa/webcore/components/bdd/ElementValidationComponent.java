package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.ElementValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Validacion de Elementos.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ElementValidationComponent implements StepComponent {
    @Override public String getName()                  { return "Validacion de Elementos"; }
    @Override public String getId()                    { return "web.validation.element"; }
    @Override public String getDisplayName()           { return "Validacion de Elementos"; }
    @Override public String getDescription()           { return "Visibilidad, texto, atributos y estado de elementos"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Web"; }
    @Override public String getIcon()                  { return "check_box"; }
    @Override public int getDisplayOrder()             { return 120; }
    @Override public Class<?> getStepDefinitionClass() { return ElementValidationSteps.class; }
}
