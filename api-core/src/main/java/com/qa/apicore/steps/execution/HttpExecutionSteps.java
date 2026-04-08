package com.qa.apicore.steps.execution;

import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.exceptions.FrameworkTechnicalException;
import com.qa.common.http.model.HttpResponse;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.utils.JsonUtilities;
import io.cucumber.java.en.When;

/**
 * Steps de ejecucion de peticiones HTTP.
 * Migrado de ApiSteps.java + nuevos steps (GET/POST/PUT/PATCH/DELETE directos).
 * @author Abel Venero
 * @since 2.0.0
 */
public class HttpExecutionSteps {

    // ─── Obtención de servicios desde el ServiceRegistry (Fase 3 — Task 105) ───

    private HttpClient getHttpClient() {
        return ExecutionContext.current()
                .map(ctx -> ctx.service(HttpClient.class))
                .orElseGet(BaseHttpClient::new);
    }

    private ApiHelper getApiHelper() {
        return ApiHelper.forCurrentContext();
    }

    @When("ejecuto una petición {string}")
    public void ejecutoUnaPeticionAlEndpoint(String method) throws FrameworkTechnicalException {
        ejecutarPeticionHttp(method, true);
    }

    @When("ejecuto la consulta con el metodo {string}")
    public void ejecutoLaConsultaConElMetodo(String method) throws FrameworkTechnicalException {
        ejecutarPeticionHttp(method, true);
    }

    @When("ejecuto la consulta con el metodo {string} sin redireccion")
    public void ejecutoLaConsultaConElMetodoSinRedireccion(String method) throws FrameworkTechnicalException {
        ejecutarPeticionHttp(method, false);
    }

    @When("ejecuto GET a {string}")
    public void ejecutoGet(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("GET", true);
    }

    @When("ejecuto POST a {string}")
    public void ejecutoPost(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("POST", true);
    }

    @When("ejecuto PUT a {string}")
    public void ejecutoPut(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("PUT", true);
    }

    @When("ejecuto PATCH a {string}")
    public void ejecutoPatch(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("PATCH", true);
    }

    @When("ejecuto DELETE a {string}")
    public void ejecutoDelete(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("DELETE", true);
    }

    @When("ejecuto la petición y espero {int} segundos máximo")
    public void ejecutoConTimeout(int timeoutSeconds) throws FrameworkTechnicalException {
        getHttpClient().setTimeout(timeoutSeconds * 1000);
        ejecutarPeticionHttp("GET", true);
    }

    // =========================================================================
    // NUEVOS STEPS — Polling / Retry para endpoints asíncronos
    // =========================================================================

    /**
     * Reintenta una petición HTTP hasta que la respuesta devuelva el código esperado.
     * Ideal para endpoints asíncronos que tardan en procesar la solicitud.
     *
     * <p>Ejemplo de uso en feature:
     * <pre>
     * When reintento "GET" al endpoint "job.status.url" hasta que el código sea 200
     *      con máximo 10 intentos cada 3 segundos
     * </pre>
     */
    @When("reintento {string} al endpoint {string} hasta que el código sea {int} con máximo {int} intentos cada {int} segundos")
    public void reintentoHastaQueCodigoSea(String method, String endpointKey,
                                           int expectedCode, int maxAttempts, int waitSeconds)
            throws FrameworkTechnicalException {
        getApiHelper().pollUntilStatusCode(method, endpointKey, expectedCode, maxAttempts, waitSeconds);
    }

    /**
     * Reintenta una petición HTTP hasta que un campo JSON tenga el valor esperado.
     * Ideal para flujos asíncronos donde el estado se refleja en un campo de la respuesta.
     *
     * <p>Ejemplo de uso en feature:
     * <pre>
     * When reintento "GET" al endpoint "job.status.url" hasta que el campo "$.status" sea "COMPLETED"
     *      con máximo 15 intentos cada 5 segundos
     * </pre>
     */
    @When("reintento {string} al endpoint {string} hasta que el campo {string} sea {string} con máximo {int} intentos cada {int} segundos")
    public void reintentoHastaQueCampoSea(String method, String endpointKey,
                                          String jsonPath, String expectedValue,
                                          int maxAttempts, int waitSeconds)
            throws FrameworkTechnicalException {
        getApiHelper().pollUntilJsonFieldEquals(method, endpointKey, jsonPath, expectedValue, maxAttempts, waitSeconds);
    }

    private void ejecutarPeticionHttp(String method, boolean followRedirects) throws FrameworkTechnicalException {
        try {
            HttpClient httpClient = getHttpClient();
            switch (method.toUpperCase()) {
                case "GET":    httpClient.get(""); break;
                case "POST":   httpClient.post(""); break;
                case "PUT":    httpClient.put(""); break;
                case "DELETE": httpClient.delete(""); break;
                case "PATCH":  httpClient.patch(""); break;
                default:
                    throw new FrameworkTechnicalException("ejecutarPeticionHttp",
                        "Metodo HTTP no soportado: " + method);
            }
            HttpResponse response = httpClient.getLastResponse();
            if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
                try {
                    Object obj = JsonUtilities.deserializeJson(response.getBody(), Object.class);
                    ExecutionContext.requireCurrent().variables().set("__lastDeserialized", obj);
                } catch (Exception ignored) { }
            }
            TestLogger.logInfo("HTTP_EXEC", method.toUpperCase() + " ejecutado", null);
        } catch (FrameworkTechnicalException e) {
            throw e;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("ejecutarPeticionHttp", "Error: " + e.getMessage());
        }
    }
}
