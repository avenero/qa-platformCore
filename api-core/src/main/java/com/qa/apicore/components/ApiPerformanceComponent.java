package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponsePerformanceSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Performance.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiPerformanceComponent implements StepComponent {
    @Override public String getName()                  { return "Performance"; }
    @Override public String getId()                    { return "api.performance"; }
    @Override public String getDisplayName()           { return "Performance"; }
    @Override public String getDescription()           { return "Validacion de tiempo de respuesta y tamano del body"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion de Respuesta"; }
    @Override public String getIcon()                  { return "speed"; }
    @Override public int getDisplayOrder()             { return 110; }
    @Override public Class<?> getStepDefinitionClass() { return ResponsePerformanceSteps.class; }
}
