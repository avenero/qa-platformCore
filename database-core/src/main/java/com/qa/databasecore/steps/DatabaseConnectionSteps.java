package com.qa.databasecore.steps;

import com.qa.databasecore.factory.DbConnectorFactory;
import com.qa.databasecore.helper.DatabaseHelper;
import com.qa.databasecore.connector.DatabaseConnector;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;


/**
 * Steps genéricos de Cucumber para gestionar conexiones a bases de datos relacionales.
 *
 * <p>Permite conectar, consultar y validar resultados en cualquier motor de base de datos
 * soportado, sin acoplamiento a un vendor específico. El tipo de BD se indica como
 * parámetro en el step, por lo que la misma feature puede operar contra múltiples
 * motores dentro del mismo escenario.</p>
 *
 * <p><b>Motores soportados:</b></p>
 * <ul>
 *   <li>{@code "oracle"}     → Oracle Database (ojdbc)</li>
 *   <li>{@code "sqlserver"}  → Microsoft SQL Server (mssql-jdbc)</li>
 *   <li>{@code "postgresql"} → PostgreSQL (pgjdbc)</li>
 *   <li>{@code "mysql"}      → MySQL / MariaDB (connector/j)</li>
 * </ul>
 *
 * <p><b>Configuración requerida en config-{env}.properties:</b></p>
 * <p>Cada motor lee sus propiedades usando el prefijo {@code {tipo}.db.*}.
 * Solo es necesario configurar los motores que se vayan a utilizar.</p>
 * <pre>
 * # Oracle
 * oracle.db.url=jdbc:oracle:thin:@//servidor:1521/servicio
 * oracle.db.username=${ORACLE_USER}
 * oracle.db.password=${ORACLE_PASSWORD}
 *
 * # SQL Server
 * sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=MI_DB;encrypt=false
 * sqlserver.db.username=${SQLSERVER_USER}
 * sqlserver.db.password=${SQLSERVER_PASSWORD}
 *
 * # PostgreSQL
 * postgresql.db.url=jdbc:postgresql://servidor:5432/mi_db
 * postgresql.db.username=${PG_USER}
 * postgresql.db.password=${PG_PASSWORD}
 *
 * # MySQL
 * mysql.db.url=jdbc:mysql://servidor:3306/mi_db
 * mysql.db.username=${MYSQL_USER}
 * mysql.db.password=${MYSQL_PASSWORD}
 * </pre>
 *
 * <p><b>Flujo típico de uso en una feature:</b></p>
 * <pre>
 * # 1. Establecer conexión (una vez por escenario o en Background)
 * Given establezco conexion a base de datos "oracle"
 *
 * # 2. Ejecutar consulta
 * When ejecuto la consulta "SELECT user_id, email FROM users WHERE status = 'ACTIVE'"
 *
 * # 3. Validar y/o extraer datos
 * Then valido que la consulta retorne resultados
 * And  obtengo el valor de la columna "email" y lo almaceno en "emailUsuario"
 * </pre>
 *
 * <p><b>Escenario con múltiples BDs en el mismo test:</b></p>
 * <pre>
 * Given establezco conexion a base de datos "oracle"
 * When ejecuto la consulta "SELECT id FROM clientes WHERE estado = 'ACTIVO'"
 * Then valido que la consulta retorne resultados
 * And  obtengo el valor de la columna "id" y lo almaceno en "clienteId"
 *
 * Given establezco conexion a base de datos "sqlserver"
 * When ejecuto la consulta "SELECT otp FROM tokens WHERE cliente_id = ?" con parametros "{{clienteId}}"
 * Then valido que la columna "otp" tenga el valor "123456"
 * </pre>
 *
 * <p><b>Notas importantes:</b></p>
 * <ul>
 *   <li>Las conexiones se cachean automáticamente durante el escenario y se cierran al finalizar.</li>
 *   <li>Todos los queries usan {@code PreparedStatement} para prevenir SQL injection.</li>
 *   <li>Solo se procesa la primera fila retornada por cada consulta SELECT.</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.1.0
 * @since 2026-02-20
 */
public class DatabaseConnectionSteps {

    // =========================================================================
    // GIVEN STEPS - CONFIGURACIÓN
    // =========================================================================

    /**
     * Establece conexión a una base de datos específica.
     *
     * <p>Lee la configuración desde {@code config-{env}.properties} usando el prefijo
     * {@code {dbType}.db.*}. La conexión se cachea y puede reutilizarse en múltiples
     * steps dentro del mismo escenario sin reconectarse.</p>
     *
     * <p><b>Valores aceptados para {@code dbType}:</b></p>
     * <ul>
     *   <li>{@code "oracle"}     → requiere {@code oracle.db.url / username / password}</li>
     *   <li>{@code "sqlserver"}  → requiere {@code sqlserver.db.url / username / password}</li>
     *   <li>{@code "postgresql"} → requiere {@code postgresql.db.url / username / password}</li>
     *   <li>{@code "mysql"}      → requiere {@code mysql.db.url / username / password}</li>
     * </ul>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * Given establezco conexion a base de datos "oracle"
     * Given establezco conexion a base de datos "sqlserver"
     * Given establezco conexion a base de datos "postgresql"
     * Given establezco conexion a base de datos "mysql"
     * </pre>
     *
     * @param dbType Tipo de motor de BD (oracle | sqlserver | postgresql | mysql)
     * @throws FrameworkBusinessException Si falta configuración o el tipo no está soportado
     */
    @Given("establezco conexion a base de datos {string}")
    public void establecerConexionABaseDeDatos(String dbType) throws FrameworkBusinessException {
        DatabaseConnector connector = DbConnectorFactory.connectAndCache(dbType);
        ExecutionContext.requireCurrent().variables().set("currentDbConnector", connector);
        ExecutionContext.requireCurrent().variables().set("currentDbType", dbType);
    }

    /**
     * Variante en español del step canónico con parámetros explícitos.
     * Útil para escenarios autocontenidos que no dependen de archivo de configuración.
     *
     * <p>Ejemplo:
     * <pre>
     * Given establezco conexion a "postgresql" con url "jdbc:postgresql://localhost:5432/qa"
     *       usuario "qa_user" contraseña "${QA_DB_PASS}"
     * </pre>
     */
    @Given("establezco conexion a {string} con url {string} usuario {string} contraseña {string}")
    public void establecerConexionConParams(String dbType, String jdbcUrl,
                                             String user, String password)
            throws FrameworkBusinessException {
        new com.qa.databasecore.steps.canonical.DatabaseGivenSteps()
            .iConnectToTheDatabaseWithParams(dbType, jdbcUrl, user, password);
    }

    // =========================================================================
    // WHEN STEPS - ACCIONES
    // =========================================================================

    /**
     * Ejecuta una consulta SQL sin parámetros contra la conexión activa.
     *
     * <p>El resultado queda almacenado en el contexto del escenario ({@code queryResult})
     * y puede ser accedido con los steps de validación y extracción.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT * FROM users"
     * When ejecuto la consulta "SELECT id, email FROM clientes WHERE estado = 'ACTIVO'"
     * </pre>
     *
     * @param query Consulta SQL a ejecutar
     * @throws FrameworkBusinessException Si no hay conexión activa o la consulta falla
     */
    @When("ejecuto la consulta {string}")
    public void ejecutarConsulta(String query) throws FrameworkBusinessException {
        ejecutarConsultaConParametros(query, null);
    }

    /**
     * Ejecuta una consulta SQL con parámetros posicionales contra la conexión activa.
     *
     * <p>Los parámetros se inyectan mediante {@code PreparedStatement} en el mismo orden
     * en que aparecen los placeholders {@code ?} en la consulta, lo que previene SQL injection.</p>
     *
     * <p>El resultado queda almacenado en el contexto del escenario ({@code queryResult})
     * y puede ser accedido con los steps de validación y extracción.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * # Un parámetro
     * When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "12345"
     *
     * # Múltiples parámetros (separados por coma)
     * When ejecuto la consulta "SELECT * FROM accounts WHERE user_id = ? AND status = ?" con parametros "12345,ACTIVE"
     *
     * # Con variable del contexto del escenario
     * When ejecuto la consulta "SELECT balance FROM accounts WHERE user_id = ?" con parametros "{{userId}}"
     * </pre>
     *
     * @param query      Consulta SQL que puede contener placeholders {@code ?}
     * @param parameters Valores separados por coma, en el mismo orden que los placeholders
     * @throws FrameworkBusinessException Si no hay conexión activa o la consulta falla
     */

    @When("ejecuto la consulta {string} con parametros {string}")
    public void ejecutarConsultaConParametros(String query, String parameters) throws FrameworkBusinessException {
        DatabaseConnector connector = ExecutionContext.requireCurrent().variables().
                require("currentDbConnector", DatabaseConnector.class);
        Map<String, Object> result = DatabaseHelper.executeQuery(connector, query, parameters);

        ExecutionContext.requireCurrent().variables().set("queryResult", result);
        ExecutionContext.requireCurrent().variables().set("queryRowCount", DatabaseHelper.getRowCount(result));
    }

    /**
     * Ejecuta una sentencia SQL de modificación (INSERT, UPDATE, DELETE) sin parámetros.
     *
     * <p>El número de filas afectadas queda disponible en el contexto del escenario
     * bajo la clave {@code rowsAffected}.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * When ejecuto la sentencia "DELETE FROM temp_data WHERE created_date &lt; '2025-01-01'"
     * When ejecuto la sentencia "UPDATE tokens SET status = 'EXPIRED' WHERE expiry &lt; SYSDATE"
     * </pre>
     *
     * @param sql Sentencia SQL a ejecutar
     * @throws FrameworkBusinessException Si no hay conexión activa o la sentencia falla
     */
    @When("ejecuto la sentencia {string}")
    public void ejecutarSentencia(String sql) throws FrameworkBusinessException {
        ejecutarSentenciaConParametros(sql, null);
    }

    /**
     * Ejecuta una sentencia SQL de modificación (INSERT, UPDATE, DELETE) con parámetros posicionales.
     *
     * <p>Los parámetros se inyectan mediante {@code PreparedStatement} en el mismo orden
     * en que aparecen los placeholders {@code ?}, previniendo SQL injection.</p>
     *
     * <p>El número de filas afectadas queda disponible en el contexto del escenario
     * bajo la clave {@code rowsAffected}.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * # Un parámetro
     * When ejecuto la sentencia "DELETE FROM sesiones WHERE user_id = ?" con parametros "12345"
     *
     * # Múltiples parámetros (separados por coma)
     * When ejecuto la sentencia "UPDATE users SET status = ? WHERE user_id = ?" con parametros "ACTIVE,12345"
     *
     * # Con variable del contexto del escenario
     * When ejecuto la sentencia "UPDATE tokens SET usado = 1 WHERE user_id = ?" con parametros "{{userId}}"
     * </pre>
     *
     * @param sql        Sentencia SQL que puede contener placeholders {@code ?}
     * @param parameters Valores separados por coma, en el mismo orden que los placeholders
     * @throws FrameworkBusinessException Si no hay conexión activa o la sentencia falla
     */
    @When("ejecuto la sentencia {string} con parametros {string}")
    public void ejecutarSentenciaConParametros(String sql, String parameters) throws FrameworkBusinessException {
        DatabaseConnector connector = ExecutionContext.requireCurrent().variables().
                require("currentDbConnector", DatabaseConnector.class);
        int rowsAffected = DatabaseHelper.executeStatement(connector, sql, parameters);
        ExecutionContext.requireCurrent().variables().set("rowsAffected", rowsAffected);
    }

    // =========================================================================
    // THEN STEPS - VALIDACIONES Y EXTRACCIÓN DE DATOS
    // =========================================================================

    /**
     * Extrae el valor de una columna del último resultado de consulta y lo almacena en el contexto.
     *
     * <p>El valor almacenado puede usarse posteriormente en otros steps mediante el
     * interpolador de variables {@code {{nombreVariable}}}.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * When ejecuto la consulta
     *      "SELECT user_id, email, balance FROM accounts WHERE account_id = ?" con parametros "ACC-001"
     * Then obtengo el valor de la columna "balance"  y lo almaceno en "saldoCuenta"
     * And  obtengo el valor de la columna "user_id"  y lo almaceno en "usuarioId"
     * And  obtengo el valor de la columna "email"    y lo almaceno en "emailUsuario"
     *
     * # Uso posterior del valor almacenado
     * When ejecuto la consulta "SELECT otp FROM tokens WHERE user_id = ?" con parametros "{{usuarioId}}"
     * </pre>
     *
     * @param columnName   Nombre de la columna a extraer (insensible a mayúsculas/minúsculas)
     * @param variableName Nombre de la clave bajo la que se almacena el valor en el contexto
     * @throws FrameworkBusinessException Si no hay resultados previos o la columna no existe
     */
    @Then("obtengo el valor de la columna {string} y lo almaceno en {string}")
    public void obtenerValorColumna(String columnName, String variableName) throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = ExecutionContext.requireCurrent().variables().
                require("queryResult", Map.class);
        Object value = DatabaseHelper.getColumnValue(queryResult, columnName);
        if (value != null) {
            ExecutionContext.requireCurrent().variables().set(variableName, value);
        }
    }

    /**
     * Valida que la última consulta ejecutada haya retornado al menos una fila.
     *
     * <p>Falla el escenario si la consulta no retornó resultados, lo que permite usar
     * este step como precondición antes de extraer valores de columnas.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * When ejecuto la consulta "SELECT * FROM users WHERE status = 'ACTIVE'"
     * Then valido que la consulta retorne resultados
     *
     * # Como precondición antes de extraer datos
     * When ejecuto la consulta "SELECT email FROM users WHERE user_id = ?" con parametros "12345"
     * Then valido que la consulta retorne resultados
     * And  obtengo el valor de la columna "email" y lo almaceno en "emailUsuario"
     * </pre>
     *
     * @throws FrameworkBusinessException Si la consulta no retornó resultados
     */
    @Then("valido que la consulta retorne resultados")
    public void validarQueRetorneResultados() throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = ExecutionContext.requireCurrent().variables().
                require("queryResult", Map.class);
        DatabaseHelper.validateHasResults(queryResult);
    }

    /**
     * Valida que la última consulta ejecutada NO haya retornado resultados.
     *
     * <p>Útil para verificar que un registro fue eliminado, que no existen duplicados
     * o que una condición de negocio no devuelve datos.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * # Verificar que un usuario fue eliminado
     * When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "USUARIO_BORRADO"
     * Then valido que la consulta no retorne resultados
     *
     * # Verificar ausencia de registros con cierta condición
     * When ejecuto la consulta
     *      "SELECT * FROM tokens WHERE status = 'ACTIVE' AND user_id = ?" con parametros "{{userId}}"
     * Then valido que la consulta no retorne resultados
     * </pre>
     *
     * @throws FrameworkBusinessException Si la consulta retornó resultados inesperados
     */
    @Then("valido que la consulta no retorne resultados")
    public void validarQueNoRetorneResultados() throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = ExecutionContext.requireCurrent().variables().
                require("queryResult", Map.class);
        DatabaseHelper.validateNoResults(queryResult);
    }

    /**
     * Valida que el valor de una columna del último resultado sea igual al valor esperado.
     *
     * <p>La comparación es exacta e insensible a mayúsculas/minúsculas en el nombre de columna,
     * pero sensible en el valor.</p>
     *
     * <p><b>Ejemplos:</b></p>
     * <pre>
     * # Validar estado de una cuenta
     * When ejecuto la consulta "SELECT status FROM accounts WHERE account_id = ?" con parametros "ACC-001"
     * Then valido que la columna "status" tenga el valor "ACTIVE"
     *
     * # Validar tipo de usuario
     * When ejecuto la consulta "SELECT tipo FROM users WHERE user_id = ?" con parametros "{{userId}}"
     * Then valido que la columna "tipo" tenga el valor "PREMIUM"
     * </pre>
     *
     * @param columnName    Nombre de la columna a validar (insensible a mayúsculas/minúsculas)
     * @param expectedValue Valor esperado exacto
     * @throws FrameworkBusinessException Si no hay resultados previos, la columna no existe o el valor no coincide
     */
    @Then("valido que la columna {string} tenga el valor {string}")
    public void validarValorColumna(String columnName, String expectedValue) throws FrameworkBusinessException {
        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = ExecutionContext.requireCurrent().variables().
                require("queryResult", Map.class);
        DatabaseHelper.validateColumnValue(queryResult, columnName, expectedValue);
    }

    // =========================================================================
    // HOOKS - LIMPIEZA
    // =========================================================================

    /**
     * Cierra todas las conexiones de base de datos al finalizar el escenario.
     *
     * <p>Se ejecuta automáticamente después de cada escenario gracias a la anotación
     * {@code @After}. Garantiza que los pools de conexión HikariCP se liberen
     * correctamente independientemente del resultado del test.</p>
     */
    @After
    public void cerrarConexiones() {
        if (DbConnectorFactory.hasActiveConnections()) {
            TestLogger.logInfo("DB_CONNECTION_STEPS",
                "🧹 Cerrando conexiones de BD...", null);
        }
        DbConnectorFactory.disconnectAll();
    }
}

