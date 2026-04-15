package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.TableValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps Web: Validacion de Tablas.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.validation.table")
public class TableValidationComponent implements StepComponent {
    @Override public String getName()                  { return "Validacion de Tablas"; }
    @Override public String getDisplayName()           { return "Validacion de Tablas"; }
    @Override public String getDescription()           { return "Filas, columnas, cabeceras y busqueda en tablas"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Web"; }
    @Override public String getIcon()                  { return "table_chart"; }
    @Override public int getDisplayOrder()             { return 140; }
    @Override public Class<?> getStepDefinitionClass() { return TableValidationSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Validacion de Tablas",
            "en", "Table Validation",
            "fr", "Validation des tableaux"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Filas, columnas, cabeceras y busqueda en tablas",
            "en", "Rows, columns, headers and search in tables",
            "fr", "Lignes, colonnes, en-tetes et recherche dans les tableaux"
        );
    }
}
