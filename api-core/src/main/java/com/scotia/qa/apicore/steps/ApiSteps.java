package com.scotia.qa.apicore.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scotia.qa.apicore.implementations.BaseAuthenticationManager;
import com.scotia.qa.apicore.implementations.BaseHttpClient;
import com.scotia.qa.apicore.interfaces.AuthenticationService;
import com.scotia.qa.apicore.interfaces.HttpClient;
import com.scotia.qa.apicore.utils.ValidationUtilities;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;
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
    try {
      // Usar ConfigManager directamente (lee de config-{env}.properties)
      com.scotia.qa.common.config.ConfigManager configManager =
          com.scotia.qa.common.config.ConfigManager.getInstance();

      String endpointValue = configManager.get(propertyKey);

      if (endpointValue == null || endpointValue.trim().isEmpty()) {
        throw new RuntimeException(
            String.format(
                "Propiedad '%s' no encontrada o está vacía en config-{env}.properties. "
                    + "Verifica que la propiedad exista en config-qa.properties",
                propertyKey));
      }

      String processedUrl = DataUtilities.replaceVariables(endpointValue);
      getHttpClient().setHost(processedUrl);

      TestLogger.logInfo(
          "API_STEPS_CONFIG",
          String.format("Endpoint configurado: %s = %s", propertyKey, processedUrl),
          null);

    } catch (Exception e) {
      throw new RuntimeException(
          String.format(
              "Error configurando endpoint desde propiedad '%s': %s", propertyKey, e.getMessage()),
          e);
    }
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
    try {
      com.scotia.qa.common.config.ConfigManager configManager =
          com.scotia.qa.common.config.ConfigManager.getInstance();

      String baseUrl = configManager.get(baseUrlKey);
      String endpointPath = configManager.get(endpointKey);

      if (baseUrl == null || baseUrl.trim().isEmpty()) {
        throw new RuntimeException(
            String.format("Base URL '%s' no encontrada en config-{env}.properties", baseUrlKey));
      }

      if (endpointPath == null || endpointPath.trim().isEmpty()) {
        throw new RuntimeException(
            String.format(
                "Endpoint path '%s' no encontrado en config-{env}.properties", endpointKey));
      }

      // Construir URL completa
      String fullUrl =
          baseUrl.endsWith("/") ? baseUrl + endpointPath : baseUrl + "/" + endpointPath;

      String processedUrl = DataUtilities.replaceVariables(fullUrl);
      getHttpClient().setHost(processedUrl);

      TestLogger.logInfo(
          "API_STEPS_CONFIG",
          String.format(
              "Endpoint configurado: %s + %s = %s", baseUrlKey, endpointKey, processedUrl),
          null);

    } catch (Exception e) {
      throw new RuntimeException(
          String.format(
              "Error configurando endpoint con base '%s' y path '%s': %s",
              baseUrlKey, endpointKey, e.getMessage()),
          e);
    }
  }

  @Given("establezco el host base como {word}")
  public void establezcoElHostBaseComo(String host) {
    String processedHost = DataUtilities.replaceVariables(host);
    getHttpClient().setHost(processedHost);
    TestLogger.logInfo(
        "API_STEPS_CONFIG", String.format("Host base establecido: %s", processedHost), null);
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
    String processedToken = DataUtilities.replaceVariables(token);
    getHttpClient().addHeader("Authorization", "Bearer " + processedToken);
    TestLogger.logInfo("API_STEPS_AUTH", "Token personalizado configurado", null);
  }

  @Given("agrego autenticación básica con usuario {string} y password {string}")
  public void agregoAutenticacionBasicaConUsuarioYPassword(String username, String password) {
    String processedUsername = DataUtilities.replaceVariables(username);
    String processedPassword = DataUtilities.replaceVariables(password);

    String credentials =
        Base64.getEncoder()
            .encodeToString((processedUsername + ":" + processedPassword).getBytes());
    getHttpClient().addHeader("Authorization", "Basic " + credentials);
    TestLogger.logInfo(
        "API_STEPS_AUTH",
        String.format("Autenticación básica configurada para usuario: %s", processedUsername),
        null);
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
    String processedBody = DataUtilities.replaceVariables(body);
    getHttpClient().setBody(processedBody);
    TestLogger.logInfo("API_STEPS_REQUEST", "Request body agregado", null);
  }

  @Given("establezco el cuerpo JSON con los siguientes datos")
  public void establezcoElCuerpoJSONConLosSiguientesDatos(Map<String, String> data) {
    try {
      // Procesar variables en los valores
      Map<String, String> processedData = new HashMap<>();
      data.forEach((k, v) -> processedData.put(k, DataUtilities.replaceVariables(v)));

      // Convertir a JSON
      ObjectMapper mapper = new ObjectMapper();
      String jsonBody = mapper.writeValueAsString(processedData);

      getHttpClient().addHeader("Content-Type", "application/json");
      getHttpClient().setBody(jsonBody);
      TestLogger.logInfo("API_STEPS_REQUEST", "Request JSON agregado", null);

    } catch (Exception e) {
      throw new RuntimeException("Error creando cuerpo JSON: " + e.getMessage(), e);
    }
  }

  // =================================================================================
  // EJECUCIÓN DE PETICIONES
  // =================================================================================

  @When("ejecuto una petición {string}")
  public void ejecutoUnaPeticionAlEndpoint(String method, String endpoint)
      throws FrameworkTechnicalException {
    String processedEndpoint = DataUtilities.replaceVariables(endpoint);

    // Usar la API correcta del HttpClient consolidado según el método HTTP
    try {
      switch (method.toUpperCase()) {
        case "GET":
          getHttpClient().get(processedEndpoint);
          break;
        case "POST":
          getHttpClient().post(processedEndpoint);
          break;
        case "PUT":
          getHttpClient().put(processedEndpoint);
          break;
        case "DELETE":
          getHttpClient().delete(processedEndpoint);
          break;
        case "PATCH":
          getHttpClient().patch(processedEndpoint);
          break;
        default:
          throw new FrameworkTechnicalException(
              "executeRequest", "Método HTTP no soportado: " + method);
      }

      TestLogger.logInfo(
          "API_STEPS_EXECUTION",
          String.format("Petición %s ejecutada al endpoint: %s", method, processedEndpoint),
          null);
    } catch (Exception e) {
      throw new FrameworkTechnicalException(
          "executeRequest",
          String.format("Error ejecutando petición %s: %s", method, e.getMessage()));
    }
  }

  // =================================================================================
  // VALIDACIÓN DE RESPUESTAS
  // =================================================================================

  @Then("valido que el codigo de respuesta del servicio sea {int}")
  public void validoQueElCodigoDeRespuestaDelServicioSea(int statusCode)
      throws FrameworkBusinessException {
    try {
      HttpResponse lastResponse = getHttpClient().getLastResponse();
      ValidationUtilities.validateStatusCode(lastResponse, statusCode);
      TestLogger.logInfo(
          "API_STEPS_VALIDATION",
          String.format("Código de respuesta validado exitosamente: %d", statusCode),
          null);
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "validoQueElCodigoDeRespuestaSea",
          "Error validando código de respuesta: " + e.getMessage());
    }
  }

  @Then("valido que la respuesta contenga el texto {word}")
  public void validoQueLaRespuestaContengaElTexto(String expectedText)
      throws FrameworkBusinessException {
    try {
      HttpResponse lastResponse = getHttpClient().getLastResponse();
      String responseBody = lastResponse.getBody();

      if (responseBody == null || !responseBody.contains(expectedText)) {
        throw new FrameworkBusinessException(
            "validoQueLaRespuestaContengaElTexto",
            String.format("El texto '%s' no fue encontrado en la respuesta", expectedText));
      }

      TestLogger.logInfo(
          "API_STEPS_VALIDATION",
          String.format("Texto validado exitosamente en la respuesta: %s", expectedText),
          null);
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "validoQueLaRespuestaContengaElTexto",
          "Error validando texto en respuesta: " + e.getMessage());
    }
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
    try {
      HttpResponse lastResponse = getHttpClient().getLastResponse();

      if (lastResponse == null) {
        throw new FrameworkBusinessException(
            "validoQueElResponseTengaElSiguienteEsquema",
            "No hay respuesta disponible para validar. Ejecuta primero una petición HTTP.");
      }

      TestLogger.logInfo("API_STEPS_VALIDATION", "Validando esquema JSON del response...", null);
      ValidationUtilities.validateJsonSchema(lastResponse, schemaOrPath);
      TestLogger.logInfo("API_STEPS_VALIDATION", "✅ Esquema JSON validado exitosamente", null);

    } catch (FrameworkBusinessException e) {
      TestLogger.logError(
          "API_STEPS_VALIDATION", "❌ Error en validación de esquema JSON: " + e.getMessage(), null);
      throw e;
    } catch (Exception e) {
      throw new FrameworkBusinessException(
          "validoQueElResponseTengaElSiguienteEsquema",
          "Error inesperado validando esquema JSON: " + e.getMessage());
    }
  }

  // =================================================================================
  // GESTIÓN DE VARIABLES
  // =================================================================================

  @Given("almaceno el valor {word} como {word}")
  public void almacenoElValorComo(String value, String variableName) {
    String processedValue = DataUtilities.replaceVariables(value);
    String processedVariableName = DataUtilities.replaceVariables(variableName);
    DataUtilities.storeValue(processedVariableName, processedValue);
    TestLogger.logInfo(
        "API_STEPS_DATA",
        String.format("Variable almacenada: %s = %s", processedVariableName, processedValue),
        null);
  }

  // =================================================================================
  // UTILIDADES DE DEBUGGING
  // =================================================================================

  @Then("muestro la información de la última petición")
  public void muestroLaInformacionDeLaUltimaPeticion() {
    try {
      TestLogger.logInfo("API_STEPS_DEBUG", "=== INFORMACIÓN DE LA ÚLTIMA PETICIÓN ===", null);

      HttpResponse lastResponse = getHttpClient().getLastResponse();
      if (lastResponse != null) {
        TestLogger.logInfo("API_STEPS_DEBUG", "Status Code: " + lastResponse.getStatusCode(), null);
        TestLogger.logInfo("API_STEPS_DEBUG", "Headers: " + lastResponse.getHeaders(), null);
        TestLogger.logInfo(
            "API_STEPS_DEBUG",
            "Response Body Length: "
                + (lastResponse.getBody() != null ? lastResponse.getBody().length() : 0),
            null);
      } else {
        TestLogger.logInfo("API_STEPS_DEBUG", "No hay respuesta disponible", null);
      }

      TestLogger.logInfo("API_STEPS_DEBUG", "===========================================", null);
    } catch (Exception e) {
      TestLogger.logWarning(
          "API_STEPS_DEBUG", "Error mostrando información de petición: " + e.getMessage(), null);
    }
  }

  // =================================================================================
  // STEPS ADICIONALES - Compatibilidad con features existentes
  // =================================================================================

  @Given("el host {string} mas el contexto {string}")
  public void usarHostMasElContexto(String host, String contexto) {
    String processedHost = DataUtilities.replaceVariables(host);
    String processedContexto = DataUtilities.replaceVariables(contexto);
    String fullUrl = processedHost + processedContexto;
    getHttpClient().setHost(fullUrl);
    TestLogger.logInfo(
        "API_STEPS_CONFIG",
        String.format("Host configurado: %s + %s = %s", processedHost, processedContexto, fullUrl),
        null);
  }

  @Given("agrego el token requerido del tipo Client-Credentials")
  public void agregoElTokenRequeridoDelTipoClientCredentials() throws FrameworkBusinessException {
    String token = getAuthentication().getClientCredentialsToken();
    getHttpClient().addHeader("Authorization", "Bearer " + token);
    TestLogger.logInfo("API_STEPS_AUTH", "Autenticación Client Credentials configurada", null);
  }

  @Given("agrego el token requerido del tipo Bearer-Token para el rut {string}")
  public void agregoElTokenRequeridoDelTipoBearerTokenParaElRut(String rut)
      throws FrameworkBusinessException {
    String processedRut = DataUtilities.replaceVariables(rut);
    String token = getAuthentication().getBearerTokenForIdentifier(processedRut);
    getHttpClient().addHeader("Authorization", "Bearer " + token);
    TestLogger.logInfo(
        "API_STEPS_AUTH",
        String.format("Autenticación Bearer configurada para RUT: %s", processedRut),
        null);
  }

  // =================================================================================
  // AUTENTICACIÓN COMPLEJA - PENDIENTE IMPLEMENTACIÓN EN COMMON
  // TODO: Implementar métodos en AuthenticationService (ver steps.md)
  // =================================================================================

  /*
  @Given("agrego el token de cuatro pasos al rut {string} y clave {string}")
  public void agregoElTokenDeCuatroPasosAlRutYClave(String rut, String clave) throws FrameworkBusinessException {
      String processedRut = DataUtilities.replaceVariables(rut);
      String processedClave = DataUtilities.replaceVariables(clave);
      // TODO: Implementar authentication.getJwtTokenWithCredentials(processedRut, processedClave)
      TestLogger.logInfo("API_STEPS_AUTH", "Token JWT configurado para RUT: " + processedRut, null);
  }

  @Given("agrego el token requerido del tipo digital mortgage")
  public void agregoElTokenRequeridoDelTipoDigitalMortgage() throws FrameworkBusinessException {
      // TODO: Implementar authentication.getDigitalMortgageToken()
      TestLogger.logInfo("API_STEPS_AUTH", "Token Digital Mortgage configurado", null);
  }

  @Given("agrego el token requerido del tipo baas latam en {string}")
  public void agregoElTokenRequeridoDelTipoBaasLatam(String environment) throws FrameworkBusinessException {
      String processedEnv = DataUtilities.replaceVariables(environment);
      // TODO: Implementar authentication.getBaasLatamToken(processedEnv)
      TestLogger.logInfo("API_STEPS_AUTH", "Token BAAS Latam configurado: " + processedEnv, null);
  }

  @Given("agrego el token requerido del tipo Client-Credentials token-generator")
  public void agregoElTokenRequeridoDelTipoClientCredentialsTokenGenerator() throws FrameworkBusinessException {
      // TODO: Implementar authentication.getTokenGeneratorClientCredentials()
      TestLogger.logInfo("API_STEPS_AUTH", "Token CC Token-Generator configurado", null);
  }

  @Given("agrego el token requerido del tipo en un paso {string}")
  public void agregoElTokenRequeridoDelTipoUnPaso(String identifier) throws FrameworkBusinessException {
      String processedId = DataUtilities.replaceVariables(identifier);
      // TODO: Implementar authentication.getOneStepToken(processedId)
      TestLogger.logInfo("API_STEPS_AUTH", "Token un paso configurado: " + processedId, null);
  }

  @Given("agrego el opaque token al rut {string} y clave {string}")
  public void agregoElOpaqueTokenAlRutYClave(String rut, String clave) throws FrameworkBusinessException {
      String processedRut = DataUtilities.replaceVariables(rut);
      String processedClave = DataUtilities.replaceVariables(clave);
      // TODO: Implementar authentication.getOpaqueToken(processedRut, processedClave)
      TestLogger.logInfo("API_STEPS_AUTH", "Opaque Token configurado para RUT: " + processedRut, null);
  }

  @Given("agrego el token Opaque sin Bearer con Authorization del rut {string} y clave {string}")
  public void agregoElOpaqueTokenYTokenBearerAlRutYClave(String rut, String clave) throws FrameworkBusinessException {
      String processedRut = DataUtilities.replaceVariables(rut);
      String processedClave = DataUtilities.replaceVariables(clave);
      // TODO: Implementar authentication.getOpaqueToken(processedRut, processedClave)
      // httpClient.addHeader("Authorization", token); // SIN "Bearer"
      TestLogger.logInfo("API_STEPS_AUTH", "Opaque Token (sin Bearer) configurado", null);
  }

  @Given("agrego el nuevo opaque token")
  public void agregoElNuevoOpaqueToken() throws FrameworkBusinessException {
      // TODO: Implementar authentication.getDatalabOpaqueToken()
      TestLogger.logInfo("API_STEPS_AUTH", "Nuevo Opaque Token configurado", null);
  }

  @Given("agrego el token cuatro pasos y el opaque token al rut {string} y clave {string}")
  public void agregoElOpaqueTokenYTokenCuatroPasosAlRutYClave(String rut, String clave) throws FrameworkBusinessException {
      String processedRut = DataUtilities.replaceVariables(rut);
      String processedClave = DataUtilities.replaceVariables(clave);
      // TODO: Implementar authentication.getJwtTokenWithCredentials() y getOpaqueToken()
      // httpClient.addHeader("Authorization", "Bearer " + jwtToken);
      // httpClient.addHeader("opaque-token", "Bearer " + opaqueToken);
      TestLogger.logInfo("API_STEPS_AUTH", "Token JWT + Opaque configurados", null);
  }

  @Given("agrego el token Opaque sin Bearer del rut {string} y clave {string}")
  public void agregoElOpaqueTokenAlRut_Clave(String rut, String clave) throws FrameworkBusinessException {
      String processedRut = DataUtilities.replaceVariables(rut);
      String processedClave = DataUtilities.replaceVariables(clave);
      // TODO: Implementar authentication.getOpaqueToken()
      // httpClient.addHeader("opaque-token", token); // SIN "Bearer"
      TestLogger.logInfo("API_STEPS_AUTH", "Opaque Token (opaque-token header) configurado", null);
  }

  @Given("agrego el token requerido de tipo Dal Token")
  public void agregoElTokenRequeridoDeTipoDalToken() throws FrameworkBusinessException {
      // TODO: Implementar authentication.getDalToken()
      TestLogger.logInfo("API_STEPS_AUTH", "Token DAL configurado", null);
  }

  @Given("agrego el token requerido del tipo Token Moppa para el rut {string}")
  public void agregoElTokenRequeridoDelTipoTokenMoppaParaElRut(String rut) throws FrameworkBusinessException {
      String processedRut = DataUtilities.replaceVariables(rut);
      // TODO: Implementar authentication.getMoppaToken(processedRut)
      TestLogger.logInfo("API_STEPS_AUTH", "Token Moppa configurado: " + processedRut, null);
  }
  */

  // =================================================================================
  // CONFIGURACIÓN DE FIELDS Y REQUEST BODY
  // =================================================================================

  @Given("agrego el field {string} con el valor {string}")
  public void agregoElFieldKeyConElValorValue(String key, String value) {
    String processedKey = DataUtilities.replaceVariables(key);
    String processedValue = DataUtilities.replaceVariables(value);
    getHttpClient().addField(processedKey, processedValue);
    TestLogger.logInfo("API_STEPS_CONFIG", String.format("Field agregado: %s", processedKey), null);
  }

  @Given("agrego el request body {string}")
  public void agregoElRequestBody(String body) {
    String processedBody = DataUtilities.replaceVariables(body);
    getHttpClient().setBody(processedBody);
    TestLogger.logInfo("API_STEPS_REQUEST", "Request body agregado", null);
  }

  @Given("agrego el request")
  public void agregoElRequest(String jsonBody) {
    try {
      String processedBody = DataUtilities.replaceVariables(jsonBody);
      getHttpClient().addHeader("Content-Type", "application/json");
      getHttpClient().setBody(processedBody);
      TestLogger.logInfo("API_STEPS_REQUEST", "Request JSON agregado", null);
    } catch (Exception e) {
      throw new RuntimeException("Error estableciendo request: " + e.getMessage(), e);
    }
  }

  // =================================================================================
  // MANIPULACIÓN JSON AVANZADA - PENDIENTE IMPLEMENTACIÓN
  // TODO: Implementar métodos en DataUtilities (ver steps.md)
  // =================================================================================

  /*
  @Given("el parametro {string} remplazo el valor {string}")
  public void elParametroRemplazoElValor(String jsonPath, Object value) {
      String processedPath = DataUtilities.replaceVariables(jsonPath);
      // TODO: Implementar DataUtilities.setJsonValue(jsonPath, value, currentJsonContext)
      TestLogger.logInfo("API_STEPS_DATA", "Parámetro JSON actualizado: " + processedPath, null);
  }
  */

  @Given("el resultado almaceno el valor de {string}")
  public void elResultadoAlmacenoElValorDe(String jsonPath) {
    try {
      HttpResponse lastResponse = getHttpClient().getLastResponse();
      if (lastResponse == null || lastResponse.getBody() == null) {
        throw new FrameworkBusinessException(
            "elResultadoAlmacenoElValorDe", "No hay respuesta disponible para extraer datos");
      }

      String responseBody = lastResponse.getBody();
      Object value = DataUtilities.getJsonParameter(responseBody, jsonPath);
      DataUtilities.storeValue(jsonPath, value);

      TestLogger.logInfo(
          "API_STEPS_DATA",
          String.format("Valor almacenado desde JSON: %s = %s", jsonPath, value),
          null);
    } catch (FrameworkBusinessException e) {
      throw new RuntimeException("Error extrayendo valor JSON: " + e.getMessage(), e);
    }
  }

  // =================================================================================
  // STEPS CON MÉTODOS FALTANTES - PENDIENTE IMPLEMENTACIÓN
  // TODO: Implementar métodos faltantes en common (ver steps.md)
  // =================================================================================

  /*
  @Given("el resultado almaceno el valor que está dentro de la estructura {string} en {string}")
  public void almacenoValorEnVariable(String jsonPath, String variableName) throws FrameworkBusinessException {
      try {
          HttpResponse lastResponse = httpClient.getLastResponse();
          String responseBody = lastResponse.getBody();
          Object value = DataUtilities.getJsonParameter(responseBody, jsonPath);
          String processedVarName = DataUtilities.replaceVariables(variableName);
          DataUtilities.storeValue(processedVarName, value);
          TestLogger.logInfo("API_STEPS_DATA",
              String.format("Valor extraído y almacenado: %s -> %s", jsonPath, processedVarName), null);
      } catch (FrameworkBusinessException e) {
          throw new FrameworkBusinessException(
              "almacenoValorEnVariable", "Error extrayendo valor: " + e.getMessage());
      }
  }
  */

  @Given("agrego el queryparam {string} con el valor {string}")
  public void agregoElQueryparamConElValor(String param, String value) {
    String processedParam = DataUtilities.replaceVariables(param);
    String processedValue = DataUtilities.replaceVariables(value);
    getHttpClient().addQueryParam(processedParam, processedValue);
    TestLogger.logInfo(
        "API_STEPS_CONFIG", String.format("Query param agregado: %s", processedParam), null);
  }

  @Given("establezco la key {string} con el valor {string}")
  public void establescoLaKeyConElValor(String key, String value) {
    String processedKey = DataUtilities.replaceVariables(key);
    String processedValue = DataUtilities.replaceVariables(value);
    DataUtilities.storeValue(processedKey, processedValue);
    TestLogger.logInfo(
        "API_STEPS_DATA",
        String.format("Variable almacenada: %s = %s", processedKey, processedValue),
        null);
  }

  /*
  @Given("el resultado de la ejecucion de la query, almaceno el valor de {string} en la variable {string}")
  public void elResultadoDeLaQuery(String campo, String variableName) {
      // TODO: Requiere integración con DatabaseService - postponer
      TestLogger.logInfo("API_STEPS_DATA", "Query DB ejecutada: " + campo, null);
  }

  @Given("el resultado de la ejecucion del servicio, almaceno el valor del header {string} en la variable {string}")
  public void elResultadoDelServicio(String headerName, String variableName) throws FrameworkBusinessException {
      try {
          HttpResponse lastResponse = httpClient.getLastResponse();
          if (lastResponse == null) {
              throw new FrameworkBusinessException(
                  "elResultadoDelServicio", "No hay respuesta disponible");
          }

          String headerValue = lastResponse.getHeaders().get(headerName);
          if (headerValue == null) {
              throw new FrameworkBusinessException(
                  "elResultadoDelServicio",
                  String.format("Header '%s' no encontrado en la respuesta", headerName));
          }

          String processedVarName = DataUtilities.replaceVariables(variableName);
          DataUtilities.storeValue(processedVarName, headerValue);

          TestLogger.logInfo("API_STEPS_DATA",
              String.format("Header capturado: %s = %s -> %s", headerName, headerValue, processedVarName), null);
      } catch (FrameworkBusinessException e) {
          throw new FrameworkBusinessException(
              "elResultadoDelServicio", "Error capturando header: " + e.getMessage());
      }
  }

  @Given("la url {string}, capturo el valor de {string} y lo guardo en la variable {string}")
  public void laUrlCapturoValor(String url, String paramName, String variableName) throws FrameworkBusinessException {
      // TODO: Implementar parseQueryParams en DataUtilities
      String processedUrl = DataUtilities.replaceVariables(url);
      String processedParamName = DataUtilities.replaceVariables(paramName);
      String processedVarName = DataUtilities.replaceVariables(variableName);
      // String paramValue = DataUtilities.extractQueryParam(processedUrl, processedParamName);
      // DataUtilities.storeValue(processedVarName, paramValue);
      TestLogger.logInfo("API_STEPS_DATA",
          String.format("Query param capturado: %s de %s -> %s", processedParamName, processedUrl, processedVarName), null);
  }
  */

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
  @Then("serializo la respuesta en la clase {string}")
  public void serializoLaRespuestaEnLaClase(String className) throws FrameworkBusinessException {
    try {
      HttpResponse response = getHttpClient().getLastResponse();

      if (response == null || response.getBody() == null) {
        throw new FrameworkBusinessException(
            "serializoLaRespuestaEnLaClase",
            "No hay respuesta disponible para deserializar. Ejecuta primero una petición HTTP.");
      }

      String body = response.getBody();
      Class<?> clazz = loadClass(className);
      Object deserializedObject = DataUtilities.deserializeJson(body, clazz);
      DataUtilities.storeObject("__lastDeserialized", deserializedObject);

      TestLogger.logInfo(
          "API_STEPS_SERIALIZATION",
          String.format("✅ Respuesta deserializada exitosamente a tipo: %s", clazz.getSimpleName()),
          null);

    } catch (FrameworkBusinessException e) {
      TestLogger.logError(
          "API_STEPS_SERIALIZATION", "❌ Error deserializando respuesta: " + e.getMessage(), null);
      throw e;
    } catch (ClassNotFoundException e) {
      String errorMsg = String.format("Clase no encontrada: %s", className);
      TestLogger.logError("API_STEPS_SERIALIZATION", errorMsg, null);
      throw new FrameworkBusinessException("serializoLaRespuestaEnLaClase", errorMsg);
    } catch (Exception e) {
      String errorMsg =
          String.format("Error inesperado deserializando respuesta: %s", e.getMessage());
      TestLogger.logError("API_STEPS_SERIALIZATION", errorMsg, null);
      throw new FrameworkBusinessException("serializoLaRespuestaEnLaClase", errorMsg);
    }
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
    try {
      Object lastDeserialized = DataUtilities.getObject("__lastDeserialized");

      if (lastDeserialized == null) {
        throw new FrameworkBusinessException(
            "guardoElObjetoSerializadoComo",
            "No hay objeto deserializado disponible. "
                + "Usa el step 'serializo la respuesta en la clase...' primero.");
      }

      DataUtilities.storeObject(variableName, lastDeserialized);
      DataUtilities.storeObject("__lastDeserialized", null);

      TestLogger.logInfo(
          "API_STEPS_SERIALIZATION",
          String.format(
              "✅ Objeto guardado exitosamente como: %s (tipo: %s)",
              variableName, lastDeserialized.getClass().getSimpleName()),
          null);

    } catch (FrameworkBusinessException e) {
      TestLogger.logError(
          "API_STEPS_SERIALIZATION", "❌ Error guardando objeto: " + e.getMessage(), null);
      throw e;
    } catch (Exception e) {
      String errorMsg = String.format("Error inesperado guardando objeto: %s", e.getMessage());
      TestLogger.logError("API_STEPS_SERIALIZATION", errorMsg, null);
      throw new FrameworkBusinessException("guardoElObjetoSerializadoComo", errorMsg);
    }
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
    try {
      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION",
          String.format(
              "🔍 Iniciando extracción de campo '%s' desde objeto '%s'", fieldName, objectPath),
          null);

      // Obtener la última respuesta deserializada
      Object lastResponse = DataUtilities.getObject("__lastDeserialized");

      if (lastResponse == null) {
        throw new FrameworkBusinessException(
            "obtengoElCampoDelObjetoYLoGuardoComo",
            "No hay respuesta deserializada disponible. Asegúrate de ejecutar primero una petición HTTP.");
      }

      Object fieldValue = null;

      // ESTRATEGIA 1: Buscar el campo directamente en toda la respuesta (más simple y robusto)
      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION",
          String.format("🔍 Buscando campo '%s' en toda la respuesta", fieldName),
          null);

      fieldValue = DataUtilities.findValue(lastResponse, fieldName);

      // ESTRATEGIA 2: Si no se encuentra y objectPath es diferente, buscar primero el contenedor
      if (fieldValue == null && !objectPath.equals(fieldName)) {
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format(
                "🔍 Campo no encontrado, buscando primero objeto contenedor '%s'", objectPath),
            null);

        Object targetObject = DataUtilities.findValue(lastResponse, objectPath);

        if (targetObject != null) {
          TestLogger.logDebug(
              "API_STEPS_SERIALIZATION",
              String.format(
                  "✅ Objeto contenedor '%s' encontrado, buscando campo '%s' dentro",
                  objectPath, fieldName),
              null);

          fieldValue = DataUtilities.findValue(targetObject, fieldName);
        }
      }

      if (fieldValue == null) {
        throw new FrameworkBusinessException(
            "obtengoElCampoDelObjetoYLoGuardoComo",
            String.format(
                "No se encontró el campo '%s' en el response. Verifica que el campo exista.",
                fieldName));
      }

      // Guardar el valor usando la nueva estrategia de contexto compartido
      String valueToStore = fieldValue.toString();

      // Guardar con prefijo de capa API para facilitar integración con Web/Mobile
      DataUtilities.saveToContext("api", variableName, valueToStore);

      // Legacy: También guardar en DataUtilities para compatibilidad con código existente
      DataUtilities.storeValue(variableName, valueToStore);

      TestLogger.logInfo(
          "API_STEPS_SERIALIZATION",
          String.format(
              "✅ Campo extraído exitosamente: campo='%s', valor=%s, guardado como='api.%s' (también disponible como '%s')",
              fieldName,
              fieldValue.toString().length() > 50
                  ? fieldValue.toString().substring(0, 50) + "..."
                  : fieldValue.toString(),
              variableName,
              variableName),
          null);

    } catch (FrameworkBusinessException e) {
      TestLogger.logError(
          "API_STEPS_SERIALIZATION", "❌ Error extrayendo campo: " + e.getMessage(), null);
      throw e;
    } catch (Exception e) {
      String errorMsg =
          String.format(
              "Error inesperado al obtener campo '%s' del objeto '%s': %s",
              fieldName, objectPath, e.getMessage());
      TestLogger.logError("API_STEPS_SERIALIZATION", "❌ " + errorMsg, null);
      throw new FrameworkBusinessException("obtengoElCampoDelObjetoYLoGuardoComo", errorMsg);
    }
  }

  /**
   * Resuelve un path de objeto, soportando notación de punto y búsqueda automática.
   *
   * <p>Ejemplos:
   *
   * <ul>
   *   <li>"data" → busca en DataUtilities y luego en la estructura deserializada
   *   <li>"response.data" → navega por el path desde la raíz
   *   <li>"response.data.usuario" → navega por múltiples niveles
   * </ul>
   *
   * @param objectPath path del objeto a resolver
   * @return el objeto encontrado o null si no existe
   */
  private Object resolveObjectPath(String objectPath) throws Exception {
    if (objectPath == null || objectPath.trim().isEmpty()) {
      return null;
    }

    // Si contiene punto, navegar por el path
    if (objectPath.contains(".")) {
      String[] pathParts = objectPath.split("\\.");

      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION",
          String.format("🔍 Navegando por path con %d niveles: %s", pathParts.length, objectPath),
          null);

      // Intentar resolver el primer elemento
      Object current = resolveSimpleObject(pathParts[0]);

      if (current == null) {
        return null;
      }

      // Navegar por el resto del path
      for (int i = 1; i < pathParts.length; i++) {
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format("➡️ Navegando a nivel '%s'", pathParts[i]),
            null);

        current = extractFieldValue(current, pathParts[i]);

        if (current == null) {
          TestLogger.logDebug(
              "API_STEPS_SERIALIZATION",
              String.format("❌ Nivel '%s' no encontrado o es null", pathParts[i]),
              null);
          return null;
        }
      }

      return current;
    } else {
      // Path simple: resolver directamente
      return resolveSimpleObject(objectPath);
    }
  }

  /**
   * Resuelve un nombre de objeto simple (sin notación de punto).
   *
   * <p>Primero busca en DataUtilities, luego en la última respuesta deserializada.
   *
   * @param objectName nombre del objeto a resolver
   * @return el objeto encontrado o null si no existe
   */
  private Object resolveSimpleObject(String objectName) {
    // 1. Buscar en DataUtilities (objetos guardados explícitamente)
    Object stored = DataUtilities.getObject(objectName);

    if (stored != null) {
      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION",
          String.format("✅ Objeto '%s' encontrado en DataUtilities", objectName),
          null);
      return stored;
    }

    // 2. Buscar en la última respuesta deserializada
    Object lastDeserialized = DataUtilities.getObject("__lastDeserialized");

    if (lastDeserialized != null) {
      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION",
          String.format(
              "🔍 Buscando '%s' en última respuesta deserializada (tipo: %s)",
              objectName, lastDeserialized.getClass().getSimpleName()),
          null);

      Object found = findObjectInStructure(lastDeserialized, objectName);

      if (found != null) {
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format("✅ Objeto '%s' encontrado en estructura", objectName),
            null);
        return found;
      }

      // Proporcionar información útil para debug
      if (lastDeserialized instanceof java.util.Map) {
        java.util.Map<?, ?> map = (java.util.Map<?, ?>) lastDeserialized;
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format("📋 Keys disponibles en raíz: %s", map.keySet()),
            null);
      }
    }

    TestLogger.logDebug(
        "API_STEPS_SERIALIZATION", String.format("❌ Objeto '%s' no encontrado", objectName), null);

    return null;
  }

  // =================================================================================
  // MÉTODOS HELPER PRIVADOS - SERIALIZACIÓN
  // =================================================================================

  /** Carga una clase de forma flexible: primero intenta FQCN, luego busca en packages comunes. */
  private Class<?> loadClass(String className) throws ClassNotFoundException {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      String[] commonPackages = {
        "com.module.models.",
        "com.module.dto.",
        "com.module.responses.",
        "com.test.models.",
        "models.",
        "dto."
      };

      for (String pkg : commonPackages) {
        try {
          return Class.forName(pkg + className);
        } catch (ClassNotFoundException ignored) {
        }
      }

      throw new ClassNotFoundException(
          String.format(
              "Clase '%s' no encontrada. Usa FQCN: com.module.models.%s", className, className));
    }
  }

  /** Extrae el valor de un campo de un objeto usando getter o acceso directo. */
  private Object extractFieldValue(Object object, String fieldName) throws Exception {
    // Si es un Map, acceder directamente por key
    if (object instanceof java.util.Map) {
      java.util.Map<?, ?> map = (java.util.Map<?, ?>) object;
      if (map.containsKey(fieldName)) {
        return map.get(fieldName);
      }
      throw new NoSuchFieldException(
          String.format(
              "Campo '%s' no encontrado en Map. Keys disponibles: %s", fieldName, map.keySet()));
    }

    Class<?> clazz = object.getClass();

    // Intentar getter method
    String getterName = "get" + DataUtilities.capitalize(fieldName);
    String booleanGetterName = "is" + DataUtilities.capitalize(fieldName);

    try {
      try {
        Method getter = clazz.getMethod(getterName);
        return getter.invoke(object);
      } catch (NoSuchMethodException e) {
        Method booleanGetter = clazz.getMethod(booleanGetterName);
        return booleanGetter.invoke(object);
      }
    } catch (NoSuchMethodException e) {
      // Acceso directo al field
      try {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
      } catch (NoSuchFieldException ex) {
        throw new NoSuchFieldException(
            String.format("Campo '%s' no encontrado en %s", fieldName, clazz.getSimpleName()));
      }
    }
  }


  /**
   * Busca un objeto por nombre en la estructura de un objeto complejo. Navega recursivamente por
   * todos los campos hasta encontrar uno que coincida con el nombre.
   *
   * @param root objeto raíz donde buscar
   * @param objectName nombre del objeto a buscar
   * @return el objeto encontrado o null si no se encuentra
   */
  private Object findObjectInStructure(Object root, String objectName) {
    if (root == null) {
      return null;
    }

    TestLogger.logDebug(
        "API_STEPS_SERIALIZATION",
        String.format("🔍 Buscando '%s' en tipo: %s", objectName, root.getClass().getSimpleName()),
        null);

    // Si es un Map, buscar en sus valores
    if (root instanceof java.util.Map) {
      java.util.Map<?, ?> map = (java.util.Map<?, ?>) root;

      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION", String.format("📋 Map con keys: %s", map.keySet()), null);

      // Si el Map contiene la key, retornarla directamente
      if (map.containsKey(objectName)) {
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format("✅ Key '%s' encontrada en Map", objectName),
            null);
        return map.get(objectName);
      }

      // Buscar recursivamente en los valores del Map
      TestLogger.logDebug(
          "API_STEPS_SERIALIZATION",
          String.format("➡️ Campo '%s' no está en raíz, buscando recursivamente...", objectName),
          null);

      for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
        Object value = entry.getValue();
        if (value != null && !isPrimitiveOrWrapper(value.getClass())) {
          TestLogger.logDebug(
              "API_STEPS_SERIALIZATION",
              String.format(
                  "🔎 Explorando valor de key '%s' (tipo: %s)",
                  entry.getKey(), value.getClass().getSimpleName()),
              null);
          Object found = findObjectInStructure(value, objectName);
          if (found != null) {
            return found;
          }
        }
      }
    } else {
      // Si es un objeto POJO, buscar en sus campos
      try {
        // Intentar extraer el campo directamente del objeto raíz
        Object result = extractFieldValue(root, objectName);
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format("✅ Campo '%s' encontrado directamente", objectName),
            null);
        return result;
      } catch (Exception e) {
        // Si no se encuentra directamente, buscar recursivamente
        TestLogger.logDebug(
            "API_STEPS_SERIALIZATION",
            String.format("➡️ Campo '%s' no está en raíz, buscando recursivamente...", objectName),
            null);

        try {
          Field[] fields = root.getClass().getDeclaredFields();
          for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(root);

            if (fieldValue != null) {
              // Si el nombre del campo coincide, retornar su valor
              if (field.getName().equals(objectName)) {
                return fieldValue;
              }

              // Si el valor del campo es un objeto complejo, buscar recursivamente
              if (!isPrimitiveOrWrapper(fieldValue.getClass())) {
                Object found = findObjectInStructure(fieldValue, objectName);
                if (found != null) {
                  return found;
                }
              }
            }
          }
        } catch (Exception ex) {
          // Si falla la búsqueda recursiva, retornar null
          return null;
        }
      }
    }

    return null;
  }

  /**
   * Verifica si una clase es primitiva o wrapper.
   *
   * @param clazz clase a verificar
   * @return true si es primitiva o wrapper, false en caso contrario
   */
  private boolean isPrimitiveOrWrapper(Class<?> clazz) {
    return clazz.isPrimitive()
        || clazz.equals(String.class)
        || clazz.equals(Boolean.class)
        || clazz.equals(Integer.class)
        || clazz.equals(Long.class)
        || clazz.equals(Double.class)
        || clazz.equals(Float.class)
        || clazz.equals(Short.class)
        || clazz.equals(Byte.class)
        || clazz.equals(Character.class);
  }
}
