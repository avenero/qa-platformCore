package com.qa.common.database.components;

import com.qa.common.database.steps.DatabaseConnectionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps DB: Ejecucion de Consultas y Sentencias.
 * Fase BDD: WHEN. Categoria: Ejecucion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DatabaseExecutionComponent implements StepComponent {
    @Override public String getName()                  { return "DB Execution"; }
    @Override public String getId()                    { return "db.execution"; }
    @Override public String getDisplayName()           { return "Consultas y Sentencias SQL"; }
    @Override public String getDescription()           { return "Ejecutar SELECT, INSERT, UPDATE y DELETE con PreparedStatement (anti SQL injection)"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Ejecucion"; }
    @Override public String getIcon()                  { return "code"; }
    @Override public int getDisplayOrder()             { return 20; }
    @Override public Class<?> getStepDefinitionClass() { return DatabaseConnectionSteps.class; }
}
