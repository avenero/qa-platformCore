package com.scotia.qa.common.database.steps;

import com.scotia.qa.common.cucumber.context.ScenarioContext;
import com.scotia.qa.common.database.factory.DbConnectorFactory;
import com.scotia.qa.common.database.helpers.DatabaseHelper;
import com.scotia.qa.common.database.interfaces.DatabaseConnector;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;


/**
 * Steps genéricos de Cucumber para gestionar conexiones a bases de datos.
 *
 * <p>Permite conectar dinámicamente a diferentes BDs durante la ejecución de tests,
 * sin conocimiento de negocio específico.</p>
 *
 * <p><b>Configuración requerida en config-{env}.properties:</b></p>
 * <pre>
 * # Oracle
 * oracle.db.url=jdbc:oracle:thin:@//servidor:1521/DB
 * oracle.db.username=${ORACLE_USER}
 * oracle.db.password=${ORACLE_PASSWORD}
 *
 * # SQL Server con Windows Auth
 * sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=DB;integratedSecurity=true;encrypt=false
 * sqlserver.db.username=
 * sqlserver.db.password=
 *
 * # PostgreSQL
 * postgresql.db.url=jdbc:postgresql://servidor:5432/db
 * postgresql.db.username=${PG_USER}
 * postgresql.db.password=${PG_PASSWORD}
 * </pre>
 *
 * <p><b>Ejemplos de uso:</b></p>
 * <pre>
 * Scenario: Conectar a múltiples BDs
 *   Given establezco conexion a base de datos "oracle"
 *   When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "12345"
 *   Then valido que la consulta retorne resultados
 *
 *   Given establezco conexion a base de datos "sqlserver"
 *   When ejecuto la consulta "SELECT * FROM otp WHERE user_id = ?" con parametros "12345"
 *   Then valido que la consulta retorne resultados
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2026-02-20
 */
public class DatabaseConnectionSteps {

    // =========================================================================
    // GIVEN STEPS - CONFIGURACIÓN
    // =========================================================================

    /**
     * Establece conexión a una base de datos específica.
     *
     * <p>Lee configuración desde config-{env}.properties usando el prefijo {dbType}.db.*
     * La conexión se cachea y puede reutilizarse en múltiples steps.</p>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * Given establezco conexion a base de datos "oracle"
     * Given establezco conexion a base de datos "sqlserver"
     * Given establezco conexion a base de datos "postgresql"
     * </pre>
     *
     * @param dbType Tipo de BD: "oracle", "sqlserver", "postgresql", "mysql"
     * @throws FrameworkBusinessException Si falta configuración o tipo no soportado
     */
    @Given("establezco conexion a base de datos {string}")
    public void establecerConexionABaseDeDatos(String dbType) throws FrameworkBusinessException {
        DatabaseConnector connector = DbConnectorFactory.connectAndCache(dbType);
        ScenarioContext.set("currentDbConnector", connector);
        ScenarioContext.set("currentDbType", dbType);
    }

    // =========================================================================
    // WHEN STEPS - ACCIONES
    // =========================================================================

    /**
     * Ejecuta una consulta SQL sin parámetros.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT * FROM users"
     * </pre>
     *
     * @param query Consulta SQL
     * @throws FrameworkBusinessException Si no hay conexión o falla la consulta
     */
    @When("ejecuto la consulta {string}")
    public void ejecutarConsulta(String query) throws FrameworkBusinessException {
        ejecutarConsultaConParametros(query, null);
    }

    /**
     * Ejecuta una consulta SQL con parámetros.
     *
     * <p><b>⚠️ IMPORTANTE:</b> Usa PreparedStatement para prevenir SQL injection.</p>
     *
     * <p><b>Ejemplo CON parámetros:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "12345"
     * When ejecuto la consulta "SELECT * FROM accounts WHERE user_id = ? AND status = ?" con parametros "12345","ACTIVE"
     * </pre>
     *
     * @param query Consulta SQL (puede contener placeholders ?)
     * @param parameters Parámetros separados por coma
     * @throws FrameworkBusinessException Si no hay conexión o falla la consulta
     */

    @When("ejecuto la consulta {string} con parametros {string}")
    public void ejecutarConsultaConParametros(String query, String parameters) throws FrameworkBusinessException {
        DatabaseConnector connector = (DatabaseConnector) ScenarioContext.get("currentDbConnector");
        Map<String, Object> result = DatabaseHelper.executeQuery(connector, query, parameters);

        ScenarioContext.set("queryResult", result);
        ScenarioContext.set("queryRowCount", DatabaseHelper.getRowCount(result));
    }

    /**
     * Ejecuta una sentencia SQL de modificación (INSERT, UPDATE, DELETE) sin parámetros.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la sentencia "DELETE FROM temp_data WHERE created_date &lt; '2025-01-01'"
     * </pre>
     *
     * @param sql Sentencia SQL
     * @throws FrameworkBusinessException Si no hay conexión o falla la sentencia
     */
    @When("ejecuto la sentencia {string}")
    public void ejecutarSentencia(String sql) throws FrameworkBusinessException {
        ejecutarSentenciaConParametros(sql, null);
    }

    /**
     * Ejecuta una sentencia SQL de modificación (INSERT, UPDATE, DELETE) con parámetros.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la sentencia "UPDATE users SET status = ? WHERE user_id = ?" con parametros "ACTIVE","12345"
     * </pre>
     *
     * @param sql Sentencia SQL
     * @param parameters Parámetros separados por coma
     * @throws FrameworkBusinessException Si no hay conexión o falla la sentencia
     */
    @When("ejecuto la sentencia {string} con parametros {string}")
    public void ejecutarSentenciaConParametros(String sql, String parameters) throws FrameworkBusinessException {
        DatabaseConnector connector = (DatabaseConnector) ScenarioContext.get("currentDbConnector");
        int rowsAffected = DatabaseHelper.executeStatement(connector, sql, parameters);
        ScenarioContext.set("rowsAffected", rowsAffected);
    }

    // =========================================================================
    // THEN STEPS - VALIDACIONES Y EXTRACCIÓN DE DATOS
    // =========================================================================

    /**
     * Obtiene el valor de una columna específica del último resultado de query.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT user_id, balance FROM accounts WHERE account_id = ?" con parametros "ACC-001"
     * Then obtengo el valor de la columna "balance" y lo almaceno en "saldoCuenta"
     * Then obtengo el valor de la columna "user_id" y lo almaceno en "usuarioId"
     * </pre>
     *
     * @param columnName Nombre de la columna a extraer
     * @param variableName Nombre de la variable donde almacenar el valor
     * @throws FrameworkBusinessException Si no hay resultados previos o la columna no existe
     */
    @Then("obtengo el valor de la columna {string} y lo almaceno en {string}")
    public void obtenerValorColumna(String columnName, String variableName) throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
        Object value = DatabaseHelper.getColumnValue(queryResult, columnName);
        ScenarioContext.set(variableName, value);
    }

    /**
     * Valida que la última consulta ejecutada haya retornado resultados.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "12345"
     * Then valido que la consulta retorne resultados
     * </pre>
     *
     * @throws FrameworkBusinessException Si no hay resultados previos
     */
    @Then("valido que la consulta retorne resultados")
    public void validarQueRetorneResultados() throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
        DatabaseHelper.validateHasResults(queryResult);
    }

    /**
     * Valida que la última consulta ejecutada NO haya retornado resultados.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "USUARIO_INEXISTENTE"
     * Then valido que la consulta no retorne resultados
     * </pre>
     *
     * @throws FrameworkBusinessException Si no hay resultados previos
     */
    @Then("valido que la consulta no retorne resultados")
    public void validarQueNoRetorneResultados() throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
        DatabaseHelper.validateNoResults(queryResult);
    }

    /**
     * Valida que el valor de una columna sea igual al esperado.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT status FROM accounts WHERE account_id = ?" con parametros "ACC-001"
     * Then valido que la columna "status" tenga el valor "ACTIVE"
     * </pre>
     *
     * @param columnName Nombre de la columna
     * @param expectedValue Valor esperado
     * @throws FrameworkBusinessException Si no hay resultados previos
     */
    @Then("valido que la columna {string} tenga el valor {string}")
    public void validarValorColumna(String columnName, String expectedValue) throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
        DatabaseHelper.validateColumnValue(queryResult, columnName, expectedValue);
    }

    // =========================================================================
    // HOOKS - LIMPIEZA
    // =========================================================================

    /**
     * Cierra todas las conexiones al final del scenario.
     *
     * <p>Se ejecuta automáticamente después de cada scenario.</p>
     */
    @After
    public void cerrarConexiones() {
        TestLogger.logInfo("DB_CONNECTION_STEPS",
            "🧹 Cerrando conexiones de BD...", null);
        DbConnectorFactory.disconnectAll();
    }
}

