package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.TableValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Validacion de Tablas.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
public class TableValidationComponent implements StepComponent {
    @Override public String getName()                  { return "Validacion de Tablas"; }
    @Override public String getId()                    { return "web.validation.table"; }
    @Override public String getDisplayName()           { return "Validacion de Tablas"; }
    @Override public String getDescription()           { return "Filas, columnas, cabeceras y busqueda en tablas"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Web"; }
    @Override public String getIcon()                  { return "table_chart"; }
    @Override public int getDisplayOrder()             { return 140; }
    @Override public Class<?> getStepDefinitionClass() { return TableValidationSteps.class; }
}
