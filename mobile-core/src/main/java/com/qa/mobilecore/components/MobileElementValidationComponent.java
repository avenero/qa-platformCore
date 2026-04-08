package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.validation.MobileElementValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Validacion de Elementos Mobile.
 * Fase BDD: THEN. Categoria: Validacion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class MobileElementValidationComponent implements StepComponent {
    @Override public String getName()                  { return "Validacion de Elementos Mobile"; }
    @Override public String getId()                    { return "mobile.validation"; }
    @Override public String getDisplayName()           { return "Validacion de Elementos Mobile"; }
    @Override public String getDescription()           { return "Visibilidad, texto y estado de elementos nativos"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Mobile"; }
    @Override public String getIcon()                  { return "check_circle"; }
    @Override public int getDisplayOrder()             { return 90; }
    @Override public Class<?> getStepDefinitionClass() { return MobileElementValidationSteps.class; }
}
