package com.qa.databasecore.components;

import com.qa.databasecore.steps.DatabaseConnectionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps DB: Configuracion y Conexion.
 * Fase BDD: GIVEN. Categoria: Configuracion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("db.setup")
public class DatabaseSetupComponent implements StepComponent {

    @Override
    public String getName() {
        return "DB Setup";
    }

    @Override
    public String getDisplayName() {
        return "Conexion a Base de Datos";
    }

    @Override
    public String getDescription() {
        return "Establecer conexion a Oracle, PostgreSQL, MySQL o SQL Server";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.GIVEN;
    }

    @Override
    public String getCategory() {
        return "Configuracion";
    }

    @Override
    public String getIcon() {
        return "storage";
    }

    @Override
    public int getDisplayOrder() {
        return 10;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return DatabaseConnectionSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "setup", "configurar", "connect", "connection", "host",
            "port", "database", "schema", "datasource", "jdbc"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Conexion a Base de Datos",
            "en", "Database Connection",
            "fr", "Connexion a la base de donnees"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Establecer conexion a Oracle, PostgreSQL, MySQL o SQL Server",
            "en", "Establish connection to Oracle, PostgreSQL, MySQL or SQL Server",
            "fr", "Etablir une connexion a Oracle, PostgreSQL, MySQL ou SQL Server"
        );
    }
}
