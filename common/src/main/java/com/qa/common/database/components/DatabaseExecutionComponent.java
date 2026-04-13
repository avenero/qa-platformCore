package com.qa.common.database.components;

import com.qa.common.database.steps.DatabaseConnectionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

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
