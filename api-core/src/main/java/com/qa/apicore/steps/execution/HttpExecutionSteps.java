package com.qa.apicore.steps.execution;

import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.exception.FrameworkTechnicalException;
import com.qa.common.http.model.HttpResponse;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.common.utils.JsonUtilities;
import io.cucumber.java.en.When;

/**
 * Steps de ejecución de peticiones HTTP.
 *
 * <p>Componente padre: {@code api.execution}
 * ({@link com.qa.apicore.components.ApiExecutionComponent}).
 * Fase BDD: WHEN.
 *
 * <p>Todos los steps canónicos llevan {@link StepDef} con ID explícito para garantizar
 * estabilidad frente a refactorizaciones. El formato es {@code api.execution.{sub-id}}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class HttpExecutionSteps {

    /** Milliseconds per second, used for converting timeout seconds to milliseconds. */
    private static final int MILLIS_PER_SECOND = 1000;

    // ─── Obtención de servicios desde el ServiceRegistry ───────────

    private HttpClient getHttpClient() {
        return ExecutionContext.current().map(ctx -> ctx.service(HttpClient.class)).orElseGet(BaseHttpClient::new);
    }

    private ApiHelper getApiHelper() {
        return ApiHelper.forCurrentContext();
    }

    // =========================================================================
    // Steps canónicos de ejecución
    // =========================================================================

    /**
     * Ejecuta una petición HTTP con el método especificado ({@code GET}, {@code POST},
     * {@code PUT}, {@code PATCH}, {@code DELETE}) contra el endpoint previamente configurado
     * con los steps de GIVEN.
     *
     * <p><b>Nota semántica:</b> "una petición" es correcto; sin embargo, el alias
     * {@link #envioLaPeticion(String)} usa "envío" (primera persona) que es más
     * consistente con el estilo imperativo del resto de los WHEN del catálogo.
     */
    @StepDef(value = "api.execution.execute",
             displayName = "Ejecutar petición HTTP")
    @When("ejecuto una petición {string}")
    public void ejecutoUnaPeticionAlEndpoint(String method) throws FrameworkTechnicalException {
        ejecutarPeticionHttp(method, true);
    }

    /**
     * Alias semántico de {@link #ejecutoUnaPeticionAlEndpoint(String)}.
     * Usa voz activa de primera persona, consistente con el estilo imperativo del
     * catálogo BDD ({@code envío}, {@code configuro}, {@code agrego}).
     *
     * <p>Ejemplo:
     * <pre>
     * When envío la petición "POST"
     * </pre>
     */
    @StepDef(value = "api.execution.execute",
             displayName = "Enviar petición HTTP")
    @When("envío la petición {string}")
    public void envioLaPeticion(String method) throws FrameworkTechnicalException {
        ejecutarPeticionHttp(method, true);
    }

    /**
     * Ejecuta GET contra un endpoint dado directamente en el step (sin paso previo de config).
     */
    @StepDef(value = "api.execution.get",
             displayName = "Ejecutar GET")
    @When("ejecuto GET a {string}")
    public void ejecutoGet(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("GET", true);
    }

    /**
     * Ejecuta POST contra un endpoint dado directamente en el step.
     */
    @StepDef(value = "api.execution.post",
             displayName = "Ejecutar POST")
    @When("ejecuto POST a {string}")
    public void ejecutoPost(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("POST", true);
    }

    /**
     * Ejecuta PUT contra un endpoint dado directamente en el step.
     */
    @StepDef(value = "api.execution.put",
             displayName = "Ejecutar PUT")
    @When("ejecuto PUT a {string}")
    public void ejecutoPut(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("PUT", true);
    }

    /**
     * Ejecuta PATCH contra un endpoint dado directamente en el step.
     */
    @StepDef(value = "api.execution.patch",
             displayName = "Ejecutar PATCH")
    @When("ejecuto PATCH a {string}")
    public void ejecutoPatch(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("PATCH", true);
    }

    /**
     * Ejecuta DELETE contra un endpoint dado directamente en el step.
     */
    @StepDef(value = "api.execution.delete",
             displayName = "Ejecutar DELETE")
    @When("ejecuto DELETE a {string}")
    public void ejecutoDelete(String endpoint) throws FrameworkTechnicalException {
        getApiHelper().configureEndpoint(endpoint);
        ejecutarPeticionHttp("DELETE", true);
    }

    /**
     * Ejecuta una petición GET configurando un timeout máximo de espera.
     *
     * <p><b>Nota semántica:</b> el step no indica el método HTTP; implica GET por defecto,
     * lo que puede confundir si se usa en contextos POST/PUT. Para operaciones con método
     * explícito usar {@link #ejecutoConTimeoutYMetodo(String, int)}.
     */
    @StepDef(value = "api.execution.with-timeout",
             displayName = "Ejecutar petición GET con timeout")
    @When("ejecuto la petición y espero {int} segundos máximo")
    public void ejecutoConTimeout(int timeoutSeconds) throws FrameworkTechnicalException {
        getHttpClient().setTimeout(timeoutSeconds * MILLIS_PER_SECOND);
        ejecutarPeticionHttp("GET", true);
    }

    /**
     * Ejecuta una petición con el método indicado configurando un timeout máximo de espera.
     */
    @StepDef(value = "api.execution.with-timeout-method",
             displayName = "Ejecutar petición con método y timeout")
    @When("ejecuto la petición {string} y espero {int} segundos máximo")
    public void ejecutoConTimeoutYMetodo(String method, int timeoutSeconds) throws FrameworkTechnicalException {
        getHttpClient().setTimeout(timeoutSeconds * MILLIS_PER_SECOND);
        ejecutarPeticionHttp(method, true);
    }

    // =========================================================================
    // Polling / Retry para endpoints asíncronos
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
    @StepDef(value = "api.execution.poll-status",
             displayName = "Reintentar hasta código de estado esperado")
    //CHECKSTYLE:OFF: LineLength — Cucumber step pattern cannot be split
    @When("reintento {string} al endpoint {string} hasta que el código sea {int} con máximo {int} intentos cada {int} segundos")
    //CHECKSTYLE:ON: LineLength
    public void reintentoHastaQueCodigoSea(String method, String endpointKey,
                                           int expectedCode, int maxAttempts, int waitSeconds)
            throws FrameworkTechnicalException {
        getApiHelper().pollUntilStatusCode(
                method, endpointKey, expectedCode, maxAttempts, waitSeconds);
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
    @StepDef(value = "api.execution.poll-field",
             displayName = "Reintentar hasta que campo JSON tenga valor esperado")
    //CHECKSTYLE:OFF: LineLength — Cucumber step pattern cannot be split
    @When("reintento {string} al endpoint {string} hasta que el campo {string} sea {string} con máximo {int} intentos cada {int} segundos")
    //CHECKSTYLE:ON: LineLength
    public void reintentoHastaQueCampoSea(String method, String endpointKey,
                                          String jsonPath, String expectedValue,
                                          int maxAttempts, int waitSeconds)
            throws FrameworkTechnicalException {
        getApiHelper().pollUntilJsonFieldEquals(method, endpointKey, jsonPath, expectedValue,
                maxAttempts, waitSeconds);
    }

    @StepDef(value = "api.execution.no-redirect",
             displayName = "Ejecutar petición sin redirección")
    @When("ejecuto la consulta con el metodo {string} sin redireccion")
    public void ejecutoLaConsultaConElMetodoSinRedireccion(String method)
            throws FrameworkTechnicalException {
        ejecutarPeticionHttp(method, false);
    }

    // =========================================================================
    // Helper interno
    // =========================================================================

    private void ejecutarPeticionHttp(String method, boolean followRedirects)
            throws FrameworkTechnicalException {
        try {
            HttpClient httpClient = getHttpClient();
            switch (method.toUpperCase()) {
                case "GET":
                    httpClient.get("");
                    break;
                case "POST":
                    httpClient.post("");
                    break;
                case "PUT":
                    httpClient.put("");
                    break;
                case "DELETE":
                    httpClient.delete("");
                    break;
                case "PATCH":
                    httpClient.patch("");
                    break;
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
