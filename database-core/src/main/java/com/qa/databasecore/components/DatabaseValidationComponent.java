package com.qa.databasecore.components;

import com.qa.databasecore.steps.DatabaseConnectionSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps DB: Validacion de Resultados.
 * Fase BDD: THEN. Categoria: Validacion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("db.validation")
public class DatabaseValidationComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 30;

    @Override
    public String getName() {
        return "DB Validation";
    }

    @Override
    public String getDisplayName() {
        return "Validacion de Resultados DB";
    }

    @Override
    public String getDescription() {
        return "Validar existencia de resultados, valores de columnas y extraer datos al contexto del escenario";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.THEN;
    }

    @Override
    public String getCategory() {
        return "Validacion";
    }

    @Override
    public String getIcon() {
        return "fact_check";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return DatabaseConnectionSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "sql", "query", "consulta", "select", "rows",
            "filas", "validar", "verificar", "assert", "count", "exists", "column"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Validacion de Resultados DB",
            "en", "DB Results Validation",
            "fr", "Validation des resultats DB"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Validar existencia de resultados, valores de columnas y extraer datos al contexto del escenario",
            "en", "Validate result existence, column values and extract data to scenario context",
            "fr", "Valider l'existence des resultats, les valeurs de colonnes"
                + " et extraire des donnees dans le contexte du scenario"
        );
    }
}
