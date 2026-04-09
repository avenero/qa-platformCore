package com.qa.common.database.components;

import com.qa.common.database.steps.DatabaseConnectionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps DB: Configuracion y Conexion.
 * Fase BDD: GIVEN. Categoria: Configuracion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DatabaseSetupComponent implements StepComponent {
    @Override public String getName()                  { return "DB Setup"; }
    @Override public String getId()                    { return "db.setup"; }
    @Override public String getDisplayName()           { return "Conexion a Base de Datos"; }
    @Override public String getDescription()           { return "Establecer conexion a Oracle, PostgreSQL, MySQL o SQL Server"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion"; }
    @Override public String getIcon()                  { return "storage"; }
    @Override public int getDisplayOrder()             { return 10; }
    @Override public Class<?> getStepDefinitionClass() { return DatabaseConnectionSteps.class; }
}
