package com.scotia.qa.common.database.repository;

import com.scotia.qa.common.database.interfaces.DatabaseConnector;
import com.scotia.qa.common.logging.TestLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio de consultas SQL de alto nivel - Infraestructura genérica para ejecutar queries.
 *
 * <p><b>⚠️ IMPORTANTE:</b> Esta clase es una <b>herramienta genérica</b> que NO contiene
 * queries específicas. TÚ le pasas el SQL que necesites ejecutar.</p>
 *
 * <p><b>Simplifica la ejecución de queries SQL:</b></p>
 * <ul>
 *   <li>✅ Gestión automática de recursos (auto-close de Connection, Statement, ResultSet)</li>
 *   <li>✅ Conversión automática de ResultSet a Maps/Lists</li>
 *   <li>✅ Logging integrado con TestLogger</li>
 *   <li>✅ Prepared Statements (previene SQL injection)</li>
 *   <li>✅ Mappers funcionales para objetos custom</li>
 * </ul>
 *
 * <p><b>Casos de uso comunes:</b></p>
 * <pre>
 * DatabaseConnector connector = DbConnectorFactory.getConnector("oracle");
 * QueryRepository repo = new QueryRepository(connector);
 *
 * // ========== CASO 1: Obtener UNA fila (Map) ==========
 * Map&lt;String, Object&gt; user = repo.queryForMap(
 *     "SELECT * FROM users WHERE user_id = ?",
 *     "12345"
 * );
 * String firstName = (String) user.get("first_name");
 *
 * // ========== CASO 2: Obtener VARIAS filas (List) ==========
 * List&lt;Map&lt;String, Object&gt;&gt; accounts = repo.queryForList(
 *     "SELECT * FROM accounts WHERE user_id = ? AND status = ?",
 *     "12345", "ACTIVE"
 * );
 *
 * // ========== CASO 3: Contar registros ==========
 * Long totalUsers = repo.count("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
 *
 * // ========== CASO 4: Ejecutar UPDATE/INSERT/DELETE ==========
 * int rowsUpdated = repo.execute(
 *     "UPDATE accounts SET balance = ? WHERE account_id = ?",
 *     5000.00, "ACC-001"
 * );
 *
 * // ========== CASO 5: Mapear a objeto custom ==========
 * User user = repo.queryForObject(
 *     "SELECT * FROM users WHERE user_id = ?",
 *     rs -&gt; new User(
 *         rs.getString("user_id"),
 *         rs.getString("first_name"),
 *         rs.getString("last_name")
 *     ),
 *     "12345"
 * );
 *
 * // ========== CASO 6: Query complejo con JOINs ==========
 * List&lt;Map&lt;String, Object&gt;&gt; orders = repo.queryForList(
 *     "SELECT o.order_id, o.total, c.customer_name " +
 *     "FROM orders o " +
 *     "INNER JOIN customers c ON o.customer_id = c.customer_id " +
 *     "WHERE o.status = ? AND o.created_date &gt; ?",
 *     "PENDING", "2025-01-01"
 * );
 * </pre>
 *
 * <p><b>Uso en Cucumber Steps:</b></p>
 * <pre>
 * &#64;Given("valido que el saldo de la cuenta {string} sea {double}")
 * public void validarSaldo(String accountId, double expectedBalance) throws SQLException {
 *     DatabaseConnector connector = DbConnectorFactory.getConnector("oracle");
 *     QueryRepository repo = new QueryRepository(connector);
 *
 *     Map&lt;String, Object&gt; account = repo.queryForMap(
 *         "SELECT balance FROM accounts WHERE account_id = ?",
 *         accountId
 *     );
 *
 *     double actualBalance = ((Number) account.get("balance")).doubleValue();
 *     assertThat(actualBalance).isEqualTo(expectedBalance);
 *
 *     connector.close();
 * }
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class QueryRepository {

    private final DatabaseConnector connector;

    public QueryRepository(DatabaseConnector connector) {
        if (connector == null) {
            throw new IllegalArgumentException("DatabaseConnector no puede ser null");
        }
        this.connector = connector;
    }

    /**
     * Ejecuta una consulta y retorna el resultado como un Map.
     * Útil para consultas que retornan una sola fila.
     *
     * @param sql Consulta SQL
     * @return Map con los resultados (columna -> valor), vacío si no hay resultados
     * @throws SQLException Si ocurre un error en la consulta
     */
    public Map<String, Object> queryForMap(String sql) throws SQLException {
        TestLogger.logDebug("QUERY_REPOSITORY", "Ejecutando queryForMap: " + sql, null);

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    result.put(columnName, value);
                }

                TestLogger.logDebug("QUERY_REPOSITORY",
                    "Query ejecutado exitosamente, columnas: " + columnCount, null);
                return result;
            }

            TestLogger.logWarning("QUERY_REPOSITORY", "Query no retornó resultados", null);
            return new HashMap<>();

        } catch (SQLException e) {
            TestLogger.logError("QUERY_REPOSITORY",
                "Error ejecutando query: " + sql + " - " + e.getMessage(), null);
            throw e;
        }
    }
 /*
  @When("actualizo los valores en la base de datos DB2 segun la consulta")
  public void actualizoLosValoresEnLaDBSegunLaConsulta(String arg0) {
      updateRecordLegacy(arg0);
  }

  @When("consulto la base de datos segun el parametro {string}")
  public void consultoLaBaseDeDatosSegunElParametro(String arg0, String arg1) {
      getRecordByParameter(arg1, arg0);
  }

  @When("consulto la base de datos {string} segun el parametro {string}")
  public void consultoLaBaseDeDatosSegunElParametro(String arg0, String arg1, String arg2) {
      getRecordByParameter(arg0, arg2, arg1);
  }

  @When("elimino uno o mas registros en {string}")
  public void eliminoUnoOMasRegistrosEn(String arg0, String arg1) {
      deleteRecord(arg0, arg1);
  }

  @When("consulto la base de datos en {string}")
  public void consultoLaBaseDeDatosEn(String arg0, String arg1) {
      getRecord(arg0, arg1);
      // Debugging output
      System.out.println("------ After getRecord: " + queryResultSet);
  }

  @When("actualizo el o los registros en la base de datos en {string}")
  public void actualizoElOLosRegistrosEnLaBaseDeDatosEn(String arg0, String arg1) {
      updateRecord(arg0, arg1);
  }

  @When("inserto uno o mas registros en {string}")
  public void insertoUnoOMasRegistrosEn(String arg0, String arg1) {
      insertRecord(arg0, arg1);
  }

  @When("recorro la respuesta buscando que se cumpla que {string} sea igual a {string} y almaceno el valor de {string}")
  public void recorroLaRespuestaBuscandoQueSeCumplaQueSeaIgualAYAlmacenoElValorDe(String arg0, String arg1, String arg2) {
      getObjectInArrayResponse(arg0, arg1, arg2);
  }


  @When("adjunto un archivo al scenario con la data")
  public void adjuntoUnArchivoAlScenarioConLaData(String arg0) {
      attachScenario(arg0, scenario);
  }

  @When("espero {string} segundos")
  public void esperoSegundos(String arg0) {
      waitForSeconds(arg0);
  }


  // Then
  @Then("valido que el codigo de respuesta del servicio sea {int}")
  public void validoQueElCodigoDeRespuestaDelServicioSea(int arg0) {
      Assert.assertEquals("HttpStatus Error, se esperaba " + arg0 + ", llego " +
              getHttpStatus() + ". \\nRespuesta del servicio: " + getBodyResponse()  +". \nBody enviado:: " + body, arg0, getHttpStatus());
  }
  @Then("valido que el status del response sea {string}")
  public void validoQueElStatusDelResponseSea(String arg0) {
      Assert.assertEquals("El Mensaje de status no coincide!", arg0, getStatusHealth());
  }

  @Then("valido que el valor del campo {string} sea {string}")
  public void validoQueElValorDeElCampoSea(String arg0, String arg1) {
      Assert.assertTrue(isRecordValue(arg0, arg1));
  }

  @Then("valido que el valor almacenado en el campo {string} sea {string}")
  public void validoQueElValorAlmacenadoEnElCampoSea(String arg0, String arg1) {
      isFieldEquals(arg0, arg1);
  }

  @Then("compruebo que se registre correctamente en MIS dado el parametro {string}")
  public void comprueboQueSeRegistreCorrectamenteEnMISDadoElParametro(String arg0, String arg1) {
      Assert.assertTrue(recordExist(arg0, arg1));
  }

  @Then("valido que el cuerpo de la respuesta sea")
  public void validoQueElCuerpoDeLaRespuestaSea(String arg0) throws IOException {
      Assert.assertTrue("Valores no coinciden", isEqualJson(arg0));
  }

  @Then("valido que el valor dentro de la estructura {string} sea {string}")
  public void validoQueElValorDentroDeLaEstructuraSea(String arg0, String arg1) throws InternalServerExceptionError {
      Assert.assertTrue(validateJson(arg0, arg1));
  }


  @Then("valido que el cuerpo de la respuesta contenga la siguiente cadena")
  public void validoQueElResponseConengLaSiguienteCadena(String arg0) {
      validateStringInResponse(arg0);
  }

  @Then("valido que el cuerpo de la respuesta no contenga la siguiente cadena")
  public void validoQueElResponseNoConengLaSiguienteCadena(String arg0) {
      validateStringNotInResponse(arg0);
  }

  @Then("valido que el valor de la variable {string} sea {string}")
  public void validoQueElValorDeLaVariableSea(String arg0, String arg1) {
      Assert.assertEquals("El valor de las variables no son iguales.", arg0.contains("{{") ? replaceData(arg0) : replaceData("{{"+arg0+"}}"), arg1);
  }


  @Given("actualizo los casos de los escenarios que tienen el tag {string} y codigo de jira {string}")
  public void actualizarTestEnJira(String arg1, String arg2) throws InternalServerExceptionError {
      updateTestJira.searchScenariosForTag(arg1, arg2);
  }

  @Then("valido que la fecha almacenada en el campo {string} sea {string}")
  public void validoQueElValorAlmacenadoEnElCampoContenga(String arg0, String arg1) {
      compareDates(arg0,arg1);
  }

  @Then("valido que lo almacenado en el campo {string} sea nulo")
  public void validoQueElValorSeaNulo(String arg0){
      isNull(arg0);
  }

  @Then("verifico que la consulta este vacia")
  public void verificoQueLaConsultaEsteVacia() {
      if (queryResultSet == null || queryResultSet.isEmpty()) {
          System.out.println("------ La consulta está vacía.");
      } else {
          throw new BussinesExceptionError("verificoQueLaConsultaEsteVacia", "Se esperaba que la consulta estuviera vacía, pero se encontraron registros: " + queryResultSet);
      }
  }

  @Given("que busco un documento que no exista en la bbdd de homebanking y guardo en {string}")
  public void buscoDocumentoValidoHb(String nameVariable) throws Exception {
      validateDocumentHomeBanking(nameVariable);
  }

  @Then("obtengo el anio y el mes de {string} y lo guardo en las variables anio y mes")
  public void obtengoElAnioYElMesDe(String arg0) throws InternalServerExceptionError {
      getMontAndYear(arg0);
  }

  @Then("obtengo los ultimo {string} digitos de {string} y lo guardo en la variable {string}")
  public void obtengoLosUltimoDigitosDeYLoGuardoEnLaVariable(String arg0, String arg1, String arg2) throws InternalServerExceptionError {
      extractLastNDigits(arg1, arg0, arg2);
  }

  @Given("que busco un documento valido para onboardingUy con el host {string}")
  public void buscoDocumentoValidoUy(String host) throws Exception {
      dataJson.put("documentoValidoUruguay", validateDocumentOnboardingUy(host));
  }

  @Given("busco un documento que tenga un cliente existente en topaz")
  public void buscoDocumentoCliente() throws InternalServerExceptionError, JsonProcessingException {
      buscoDocumentoConCliente();
  }

  @Given("busco un documento que tenga un cliente prospecto")
  public void buscoDocumentoClienteProspecto() throws InternalServerExceptionError, JsonProcessingException {
      buscoDocumentoConClienteProspecto();
  }

  @Given("busco un documento que tenga un cliente casado")
  public void buscoDocumentoClienteCasado() throws InternalServerExceptionError, JsonProcessingException {
      buscoDocumentoConCliente(true);
  }

  @Given("busco un documento que tenga un cliente soltero")
  public void buscoDocumentoClienteSoltero() throws InternalServerExceptionError, JsonProcessingException {
      buscoDocumentoConCliente(false);
  }

  @Then("valido nivel de apertura")
  public void validoNivelApertura() {
      validarNivelApertura();
  }

  //Para modificar json
  @When("modifico la variable {string} agregando en el path {string} la siguiente data")
  public void modificoElResponseAgregandoLaSiguienteEstructura(String arg0, String arg1, String arg2) {
      putVariable(arg0, arg1, arg2);
  }

  // =================================================================================
  // DESERIALIZACIÓN DE RESPUESTAS HTTP (NUEVO - v1.1.0)
  // =================================================================================

  /**
   * Deserializa la respuesta HTTP completa en un objeto Java tipado.
   *
   * <p>Convierte el body JSON de la última respuesta HTTP en un POJO (Plain Old Java Object). El
   * objeto deserializado se almacena temporalmente para ser guardado con el siguiente step.
   *
   * <p><b>Uso típico:</b>
   *
   * <pre>
   * When ejecuto la consulta con el metodo "GET" sin redireccion
   * Then valido que el codigo de respuesta del servicio sea 200
   * And serializo la respuesta en la clase "com.module.models.UserResponse"
   * And guardo el objeto serializado como "currentUser"
   * </pre>
   *
   * @param className nombre de la clase destino (FQCN o nombre simple)
   * @throws FrameworkBusinessException si no hay respuesta, la clase no existe, o la
   *     deserialización falla
   * @since 1.1.0
   */
    /**
     * Ejecuta una consulta y retorna una lista de Maps.
     * Útil para consultas que retornan múltiples filas.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; accounts = repo.queryForList(
     *     "SELECT account_id, balance FROM accounts WHERE user_id = '12345'"
     * );
     *
     * for (Map&lt;String, Object&gt; account : accounts) {
     *     System.out.println("Account: " + account.get("account_id") +
     *                        " Balance: " + account.get("balance"));
     * }
     * </pre>
     *
     * @param sql Consulta SQL
     * @return Lista de Maps con los resultados (vacía si no hay resultados)
     * @throws SQLException Si ocurre un error en la consulta
     */
    public List<Map<String, Object>> queryForList(String sql) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }

                results.add(row);
            }
        }

        return results;
    }

    /**
     * Ejecuta una consulta y mapea el resultado usando un ResultSetMapper personalizado.
     *
     * <p>Útil cuando necesitas convertir el ResultSet a objetos de tu dominio.</p>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * // Definir mapper inline con lambda
     * TestUser user = repo.queryForObject(
     *     "SELECT * FROM test_users WHERE user_id = ?",
     *     rs -&gt; new TestUser(
     *         rs.getString("user_id"),
     *         rs.getString("first_name"),
     *         rs.getString("last_name"),
     *         rs.getString("password")
     *     ),
     *     "12345"
     * );
     * </pre>
     *
     * @param sql Consulta SQL
     * @param mapper Función lambda o método de referencia para mapear el ResultSet
     * @param <T> Tipo del objeto resultado
     * @return Objeto mapeado, o null si no hay resultados
     * @throws SQLException Si ocurre un error en la consulta
     */
    public <T> T queryForObject(String sql, ResultSetMapper<T> mapper) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return mapper.mapRow(rs);
            }

            return null;
        }
    }

    /**
     * Ejecuta una consulta de conteo y retorna el resultado como Long.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * // Contar total de usuarios activos
     * Long totalUsers = repo.count("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
     *
     * // Contar cuentas por usuario
     * Long totalAccounts = repo.count("SELECT COUNT(*) FROM accounts WHERE user_id = '12345'");
     * </pre>
     *
     * @param sql Consulta SQL (debe ser un SELECT COUNT(*) o similar)
     * @return El conteo como Long (0 si no hay resultados)
     * @throws SQLException Si ocurre un error en la consulta
     */
    public Long count(String sql) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }

            return 0L;
        }
    }

    /**
     * Ejecuta una consulta con parámetros y retorna el resultado como un Map.
     *
     * <p><b>⭐ Método RECOMENDADO</b> - Usa PreparedStatement para prevenir SQL injection.</p>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * // Buscar usuario por ID (parámetro único)
     * Map&lt;String, Object&gt; user = repo.queryForMap(
     *     "SELECT * FROM users WHERE user_id = ?",
     *     "12345"
     * );
     *
     * // Buscar cuenta por múltiples criterios
     * Map&lt;String, Object&gt; account = repo.queryForMap(
     *     "SELECT * FROM accounts WHERE user_id = ? AND status = ?",
     *     "12345", "ACTIVE"
     * );
     * </pre>
     *
     * @param sql Consulta SQL con placeholders (?)
     * @param parameters Parámetros para reemplazar los placeholders (en orden)
     * @return Map con los resultados (vacío si no hay resultados)
     * @throws SQLException Si ocurre un error en la consulta
     */
    public Map<String, Object> queryForMap(String sql, Object... parameters) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Establecer parámetros
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        result.put(columnName, value);
                    }

                    return result;
                }

                return new HashMap<>();
            }
        }
    }

    /**
     * Ejecuta una sentencia SQL (INSERT, UPDATE, DELETE) y retorna el número de filas afectadas.
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * // UPDATE - Actualizar saldo de cuenta
     * int rowsUpdated = repo.execute(
     *     "UPDATE accounts SET balance = ? WHERE account_id = ?",
     *     5000.00, "ACC-12345"
     * );
     *
     * // INSERT - Crear nuevo registro
     * int rowsInserted = repo.execute(
     *     "INSERT INTO audit_log (user_id, action, timestamp) VALUES (?, ?, ?)",
     *     "12345", "LOGIN", new Timestamp(System.currentTimeMillis())
     * );
     *
     * // DELETE - Eliminar registros
     * int rowsDeleted = repo.execute(
     *     "DELETE FROM temp_data WHERE created_date &lt; ?",
     *     "2025-01-01"
     * );
     * </pre>
     *
     * @param sql Sentencia SQL (INSERT, UPDATE, DELETE)
     * @param parameters Parámetros para la sentencia
     * @return Número de filas afectadas (0 si ninguna fila fue modificada)
     * @throws SQLException Si ocurre un error en la ejecución
     */
    public int execute(String sql, Object... parameters) throws SQLException {
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Establecer parámetros
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            return stmt.executeUpdate();
        }
    }

    /**
     * Interfaz funcional para mapear ResultSet a objetos.
     *
     * @param <T> tipo del objeto resultado
     */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
}
