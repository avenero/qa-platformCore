package com.qa.databasecore.components;

import com.qa.databasecore.steps.DatabaseConnectionSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps DB: Ejecucion de Consultas y Sentencias.
 * Fase BDD: WHEN. Categoria: Ejecucion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("db.execution")
public class DatabaseExecutionComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 20;

    @Override
    public String getName() {
        return "DB Execution";
    }

    @Override
    public String getDisplayName() {
        return "Consultas y Sentencias SQL";
    }

    @Override
    public String getDescription() {
        return "Ejecutar SELECT, INSERT, UPDATE y DELETE con PreparedStatement (anti SQL injection)";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }

    @Override
    public String getCategory() {
        return "Ejecucion";
    }

    @Override
    public String getIcon() {
        return "code";
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
            "sql", "query", "consulta", "execute", "ejecutar",
            "insert", "update", "delete", "statement", "stored-proc"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Consultas y Sentencias SQL",
            "en", "SQL Queries & Statements",
            "fr", "Requetes et instructions SQL"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Ejecutar SELECT, INSERT, UPDATE y DELETE con PreparedStatement (anti SQL injection)",
            "en", "Execute SELECT, INSERT, UPDATE and DELETE with PreparedStatement (anti SQL injection)",
            "fr", "Executer SELECT, INSERT, UPDATE et DELETE avec PreparedStatement (anti-injection SQL)"
        );
    }
}
