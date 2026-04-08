package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponseBodySteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps: Response Body.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiResponseBodyComponent implements StepComponent {
    @Override public String getName()                  { return "Response Body"; }
    @Override public String getId()                    { return "api.response.body"; }
    @Override public String getDisplayName()           { return "Response Body"; }
    @Override public String getDescription()           { return "Validacion y extraccion del cuerpo de respuesta JSON"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion de Respuesta"; }
    @Override public String getIcon()                  { return "data_object"; }
    @Override public int getDisplayOrder()             { return 90; }
    @Override public Class<?> getStepDefinitionClass() { return ResponseBodySteps.class; }
}
