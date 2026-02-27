package com.scotia.qa.apicore.steps;

import com.scotia.qa.apicore.implementations.BaseAuthenticationManager;
import com.scotia.qa.apicore.implementations.BaseHttpClient;
import com.scotia.qa.apicore.interfaces.AuthenticationService;
import com.scotia.qa.apicore.interfaces.HttpClient;
import com.scotia.qa.apicore.utils.ApiHelper;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.http.exceptions.FrameworkTechnicalException;
import com.scotia.qa.common.http.model.HttpResponse;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.DataUtilities;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;


public class ApiSteps {

  // =================================================================================
  // HOOKS
  // =================================================================================

  /**
   * Hook que se ejecuta SOLO si el escenario tiene tags relacionados con API. Esto evita
   * inicializar componentes HTTP innecesariamente en tests Web puros o Database puros.
   *
   * <p>Tags soportados: @api, @rest, @http, @service
   */
  @Before(value = "@api or @rest or @http or @service", order = 50)
  public void beforeScenario(Scenario scenario) {
    // Validar consistencia de tags del scenario
    com.scotia.qa.common.cucumber.validators.HookValidator.validateApiScenario(scenario);

    // Detectar nombre del módulo dinámicamente (ej: BANKING, AUTOS, etc.)
    String moduleName = com.scotia.qa.common.logging.ModuleDetector.detectModuleName();
    TestLogger.setFramework(moduleName);
  }

  // =================================================================================
  // CONFIGURACIÓN - COMPOSICIÓN EN LUGAR DE HERENCIA
  // =================================================================================

  // Lazy initialization - se crean solo cuando se usan
  private HttpClient httpClient;
  private AuthenticationService authentication;
  private ApiHelper apiHelper;

  /**
   * Obtiene instancia de HttpClient (lazy initialization). Se crea solo cuando se necesita, no al
   * cargar la clase.
   */
  private HttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = new BaseHttpClient();
    }
    return httpClient;
  }

  /** Obtiene instancia de AuthenticationService (lazy initialization). */
  private AuthenticationService getAuthentication() {
    if (authentication == null) {
      authentication = new BaseAuthenticationManager(getHttpClient());
    }
    return authentication;
  }

  /** Obtiene instancia de ApiHelper (lazy initialization). */
  private ApiHelper getApiHelper() {
    if (apiHelper == null) {
      apiHelper = new ApiHelper(getHttpClient());
    }
    return apiHelper;
  }

  // =================================================================================
  // CONFIGURACIÓN DE ENDPOINTS
  // =================================================================================

  /**
   * Configura el endpoint usando una propiedad del archivo config-{env}.properties.
   *
   * <p>Lee directamente de ConfigManager (config-qa.properties, config-dev.properties, etc.) usando
   * la nueva estrategia de properties consolidado.
   *
   * <p><b>Ejemplo de uso:</b>
   *
   * <pre>
   * # En config-qa.properties
   * api.endpoint.security.sendSms=v1/security/sms
   * api.baseurl.autos=https://car-evaluator-loan-bff.uyplaygrnd.dev.npe-k8s.uy.bns
   *
   * # En feature
   * Given configuro el endpoint "api.baseurl.autos"
   * Given configuro el endpoint completo "api.baseurl.autos" + "api.endpoint.security.sendSms"
   * </pre>
   *
   * @param propertyKey Clave de la propiedad en config-{env}.properties
   */
  @Given("configuro el endpoint {string}")
  public void configuroElEndpoint(String propertyKey) {
    getApiHelper().configureEndpoint(propertyKey);
  }

  /**
   * Configura el endpoint usando base URL + path desde config-{env}.properties.
   *
   * <p><b>Ejemplo de uso:</b>
   *
   * <pre>
   * # En config-qa.properties
   * api.baseurl.autos=https://car-evaluator-loan-bff.uyplaygrnd.dev.npe-k8s.uy.bns
   * api.endpoint.security.sendSms=v1/security/sms
   *
   * # En feature
   * Given configuro endpoint con base "api.baseurl.autos" y path "api.endpoint.security.sendSms"
   * </pre>
   *
   * @param baseUrlKey Clave de la base URL en properties
   * @param endpointKey Clave del path del endpoint en properties
   */
  @Given("configuro endpoint con base {string} y path {string}")
  public void configuroEndpointConBaseYPath(String baseUrlKey, String endpointKey) {
    getApiHelper().configureEndpointFromConfig(baseUrlKey, endpointKey);
  }

  @Given("establezco el host base como {word}")
  public void establezcoElHostBaseComo(String host) {
    getApiHelper().setBaseHost(host);
  }

  // =================================================================================
  // AUTENTICACIÓN
  // =================================================================================

  @Given("agrego autenticación Client Credentials")
  public void agregoAutenticacionClientCredentials() throws FrameworkBusinessException {
    String token = getAuthentication().getClientCredentialsToken();
    getHttpClient().addHeader("Authorization", "Bearer " + token);
    TestLogger.logInfo("API_STEPS_AUTH", "Autenticación Client Credentials configurada", null);
  }

  @Given("agrego autenticación Bearer para RUT {word}")
  public void agregoAutenticacionBearerParaRUT(String rut) throws FrameworkBusinessException {
    String processedRut = DataUtilities.replaceVariables(rut);
    String token = getAuthentication().getBearerTokenForIdentifier(processedRut);
    getHttpClient().addHeader("Authorization", "Bearer " + token);
    TestLogger.logInfo(
        "API_STEPS_AUTH",
        String.format("Autenticación Bearer configurada para RUT: %s", processedRut),
        null);
  }

  @Given("agrego el token personalizado {word}")
  public void agregoElTokenPersonalizado(String token) {
    getApiHelper().addBearerToken(token);
  }

  @Given("agrego autenticación básica con usuario {string} y password {string}")
  public void agregoAutenticacionBasicaConUsuarioYPassword(String username, String password) {
    getApiHelper().addBasicAuthentication(username, password);
  }

  // =================================================================================
  // CONFIGURACIÓN DE HEADERS Y PARÁMETROS
  // =================================================================================

  @And("agrego el header {word} con valor {word}")
  public void agregoElHeaderConValor(String header, String value) {
    String processedValue = DataUtilities.replaceVariables(value);
    getHttpClient().addHeader(header, processedValue);
    TestLogger.logInfo("API_STEPS_CONFIG", String.format("Header agregado: %s", header), null);
  }

  @And("agrego el query param {string} con valor {string}")
  public void agregoElQueryParamConValor(String param, String value) {
    String processedValue = DataUtilities.replaceVariables(value);
    getHttpClient().addQueryParam(param, processedValue);
    TestLogger.logInfo("API_STEPS_CONFIG", String.format("Query param agregado: %s", param), null);
  }

  // =================================================================================
  // CONFIGURACIÓN DE CUERPO DE PETICIÓN
  // =================================================================================

  @Given("establezco el cuerpo de la petición como")
  public void establezcoElCuerpoDeLaPeticionComo(String body) {
    getApiHelper().setRequestBody(body);
  }

  @Given("establezco el cuerpo JSON con los siguientes datos")
  public void establezcoElCuerpoJSONConLosSiguientesDatos(Map<String, String> data) {
    getApiHelper().setJsonBody(data);
  }

  // =================================================================================
  // EJECUCIÓN DE PETICIONES
  // =================================================================================

  @When("ejecuto una petición {string}")
  public void ejecutoUnaPeticionAlEndpoint(String method, String endpoint)
      throws FrameworkTechnicalException {
    getApiHelper().executeRequest(method, endpoint);
  }

  // =================================================================================
  // VALIDACIÓN DE RESPUESTAS
  // =================================================================================

  @Then("valido que el codigo de respuesta del servicio sea {int}")
  public void validoQueElCodigoDeRespuestaDelServicioSea(int statusCode)
      throws FrameworkBusinessException {
    getApiHelper().validateResponseStatusCode(statusCode);
  }

  @Then("valido que la respuesta contenga el texto {word}")
  public void validoQueLaRespuestaContengaElTexto(String expectedText)
      throws FrameworkBusinessException {
    getApiHelper().validateResponseContainsText(expectedText);
  }

  /**
   * Valida que el cuerpo de la respuesta HTTP cumpla con un esquema JSON.
   *
   * <p>El esquema puede proporcionarse de dos formas:
   *
   * <ul>
   *   <li><b>Esquema inline</b>: JSON completo del esquema en el DocString
   *   <li><b>Archivo de esquema</b>: Ruta relativa al archivo .json con el esquema
   * </ul>
   *
   * <p><b>Ejemplo con esquema inline:</b>
   *
   * <pre>
   * Then valido que el cuerpo de la respuesta tenga el siguiente esquema
   *   """
   *   {
   *     "type": "object",
   *     "properties": {
   *       "id": {"type": "integer"},
   *       "name": {"type": "string"},
   *       "email": {"type": "string", "format": "email"}
   *     },
   *     "required": ["id", "name"]
   *   }
   *   """
   * </pre>
   *
   * <p><b>Ejemplo con archivo de esquema:</b>
   *
   * <pre>
   * Then valido que el cuerpo de la respuesta tenga el siguiente esquema
   *   """
   *   schemas/user-response-schema.json
   *   """
   * </pre>
   *
   * @param schemaOrPath esquema JSON inline o ruta al archivo de esquema
   */
  @Then("valido que el cuerpo de la respuesta tenga el siguiente esquema")
  public void validoQueElResponseTengaElSiguienteEsquema(String schemaOrPath)
      throws FrameworkBusinessException {
    getApiHelper().validateResponseSchema(schemaOrPath);
  }

  // =================================================================================
  // GESTIÓN DE VARIABLES
  // =================================================================================

  @Given("almaceno el valor {word} como {word}")
  public void almacenoElValorComo(String value, String variableName) {
    String processedValue = DataUtilities.replaceVariables(value);
    String processedVarName = DataUtilities.replaceVariables(variableName);
    DataUtilities.storeValue(processedVarName, processedValue);
  }

  // =================================================================================
  // UTILIDADES DE DEBUGGING
  // =================================================================================

  @Then("muestro la información de la última petición")
  public void muestroLaInformacionDeLaUltimaPeticion() {
    getApiHelper().showLastRequestInfo();
  }


  @Given("agrego el field {string} con el valor {string}")
  public void agregoElFieldKeyConElValorValue(String key, String value) {
    getApiHelper().addField(key, value);
  }

  @Given("agrego el request body {string}")
  public void agregoElRequestBody(String body) {
    getApiHelper().setRequestBody(body);
  }

  @Given("agrego el request")
  public void agregoElRequest(String jsonBody) {
    getApiHelper().setJsonBodyFromString(jsonBody);
  }

  // =================================================================================
  // EXTRACCIÓN Y ALMACENAMIENTO DE DATOS JSON
  // =================================================================================


  @Given("el resultado almaceno el valor de {string}")
  public void elResultadoAlmacenoElValorDe(String jsonPath) {
    try {
      getApiHelper().extractAndStoreJsonValueSimple(jsonPath);
    } catch (FrameworkBusinessException e) {
      throw new RuntimeException("Error extrayendo valor JSON: " + e.getMessage(), e);
    }
  }

  // =================================================================================
  // STEPS CON MÉTODOS FALTANTES - PENDIENTE IMPLEMENTACIÓN
  // TODO: Implementar métodos faltantes en common (ver steps.md)
  // =================================================================================

  @Given("el resultado almaceno el valor que está dentro de la estructura {string} en {string}")
  public void almacenoValorEnVariable(String jsonPath, String variableName)
      throws FrameworkBusinessException {
    getApiHelper().extractAndStoreJsonValue(jsonPath, variableName);
  }

  @Given("agrego el queryparam {string} con el valor {string}")
  public void agregoElQueryparamConElValor(String param, String value) {
    String processedParam = DataUtilities.replaceVariables(param);
    String processedValue = DataUtilities.replaceVariables(value);
    getHttpClient().addQueryParam(processedParam, processedValue);
  }

  @Given("establezco la key {string} con el valor {string}")
  public void establescoLaKeyConElValor(String key, String value) {
    String processedKey = DataUtilities.replaceVariables(key);
    String processedValue = DataUtilities.replaceVariables(value);
    DataUtilities.storeValue(processedKey, processedValue);
  }


  // =================================================================================
  // EJECUCIÓN DE PETICIONES HTTP - MÉTODOS ALTERNATIVOS
  // =================================================================================

  @When("ejecuto la consulta con el metodo {string}")
  public void ejecutoLaConsultaConElMetodo(String method) throws FrameworkTechnicalException {
    ejecutarPeticionHttp(method, true);
  }

  @When("ejecuto la consulta con el metodo {string} sin redireccion")
  public void ejecutoLaConsultaConElMetodoSinRedireccion(String method)
      throws FrameworkTechnicalException {
    ejecutarPeticionHttp(method, false);
  }

  /**
   * Método helper para ejecutar peticiones HTTP con configuración de redirects.
   *
   * @param method Método HTTP (GET, POST, PUT, DELETE, PATCH)
   * @param followRedirects Si debe seguir redirects automáticamente
   * @throws FrameworkTechnicalException Si hay error en la ejecución
   */
  private void ejecutarPeticionHttp(String method, boolean followRedirects)
      throws FrameworkTechnicalException {
    try {
      String processedMethod = method.toUpperCase();

      // TODO: Implementar setFollowRedirects en HttpClient cuando sea necesario
      // httpClient.setFollowRedirects(followRedirects);

      // Ejecutar el método HTTP correspondiente
      // El endpoint ya debe estar configurado previamente con setHost()
      switch (processedMethod) {
        case "GET":
          getHttpClient().get("");
          break;
        case "POST":
          getHttpClient().post("");
          break;
        case "PUT":
          getHttpClient().put("");
          break;
        case "DELETE":
          getHttpClient().delete("");
          break;
        case "PATCH":
          getHttpClient().patch("");
          break;
        default:
          throw new FrameworkTechnicalException(
              "ejecutarPeticionHttp", "Método HTTP no soportado: " + processedMethod);
      }

      // Deserializar automáticamente el response a un Map genérico para búsquedas
      HttpResponse response = getHttpClient().getLastResponse();
      if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
        try {
          Object deserializedResponse =
              DataUtilities.deserializeJson(response.getBody(), Object.class);
          DataUtilities.storeObject("__lastDeserialized", deserializedResponse);

          TestLogger.logDebug(
              "API_STEPS_EXECUTION",
              "Response deserializado automáticamente para búsquedas de campos",
              null);
        } catch (Exception e) {
          TestLogger.logWarning(
              "API_STEPS_EXECUTION",
              "No se pudo deserializar automáticamente el response: " + e.getMessage(),
              null);
        }
      }

      TestLogger.logInfo(
          "API_STEPS_EXECUTION",
          String.format(
              "Petición %s ejecutada (follow redirects: %s)", processedMethod, followRedirects),
          null);
    } catch (Exception e) {
      throw new FrameworkTechnicalException(
          "ejecutarPeticionHttp",
          String.format("Error ejecutando petición %s: %s", method, e.getMessage()));
    }
  }


  @Then("serializo la respuesta en la clase {string}")
  public void serializoLaRespuestaEnLaClase(String className) throws FrameworkBusinessException {
    getApiHelper().deserializeResponse(className);
  }

  /**
   * Guarda el último objeto deserializado con un nombre específico en memoria.
   *
   * <p>Almacena el objeto previamente deserializado con un nombre que puede ser referenciado en
   * steps posteriores para extraer campos o usar en builders.
   *
   * @param variableName nombre para identificar el objeto almacenado
   * @throws FrameworkBusinessException si no hay objeto deserializado previamente
   * @since 1.1.0
   */
  @Then("guardo el objeto serializado como {string}")
  public void guardoElObjetoSerializadoComo(String variableName) throws FrameworkBusinessException {
    getApiHelper().saveDeserializedObject(variableName);
  }

  /**
   * Extrae un campo específico de un objeto almacenado y lo guarda como variable String.
   *
   * <p>Permite obtener valores de campos de objetos deserializados previamente, almacenándolos como
   * variables String para uso en steps de construcción de requests.
   *
   * <p>Estrategia de búsqueda (v1.3.0):
   *
   * <ul>
   *   <li>1. Busca directamente el campo en la respuesta completa (búsqueda recursiva)
   *   <li>2. Si objectPath != fieldName, primero busca el objeto contenedor y luego el campo
   *   <li>3. Guarda el valor tanto en DataUtilities (legacy) como en ScenarioContext (nuevo)
   * </ul>
   *
   * @param fieldName nombre del campo a extraer
   * @param objectPath nombre del objeto contenedor (puede ser la clave directa o un path)
   * @param variableName nombre de la variable String donde guardar el valor
   * @throws FrameworkBusinessException si el objeto no existe o el campo no se encuentra
   * @since 1.1.0
   */
  @Then("obtengo el campo {string} del objeto {string} y lo guardo como {string}")
  public void obtengoElCampoDelObjetoYLoGuardoComo(
      String fieldName, String objectPath, String variableName) throws FrameworkBusinessException {
    getApiHelper().extractFieldFromObject(fieldName, objectPath, variableName);
  }
}
