package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.TableValidationSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Validacion de Tablas.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.validation.table")
public class TableValidationComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 140;

    @Override
    public String getName() {
        return "Validacion de Tablas";
    }

    @Override
    public String getDisplayName() {
        return "Validacion de Tablas";
    }

    @Override
    public String getDescription() {
        return "Filas, columnas, cabeceras y busqueda en tablas";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.THEN;
    }

    @Override
    public String getCategory() {
        return "Validacion Web";
    }

    @Override
    public String getIcon() {
        return "table_chart";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return TableValidationSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "table", "tabla", "row", "fila", "cell",
            "celda", "column", "columna", "grid", "datagrid"
        );
    }

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
