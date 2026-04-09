package com.qa.common.database.components;

import com.qa.common.database.steps.DatabaseConnectionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps DB: Validacion de Resultados.
 * Fase BDD: THEN. Categoria: Validacion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DatabaseValidationComponent implements StepComponent {
    @Override public String getName()                  { return "DB Validation"; }
    @Override public String getId()                    { return "db.validation"; }
    @Override public String getDisplayName()           { return "Validacion de Resultados DB"; }
    @Override public String getDescription()           { return "Validar existencia de resultados, valores de columnas y extraer datos al contexto del escenario"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion"; }
    @Override public String getIcon()                  { return "fact_check"; }
    @Override public int getDisplayOrder()             { return 30; }
    @Override public Class<?> getStepDefinitionClass() { return DatabaseConnectionSteps.class; }
}
