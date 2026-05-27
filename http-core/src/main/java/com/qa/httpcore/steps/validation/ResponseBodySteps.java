package com.qa.httpcore.steps.validation;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.httpcore.model.HttpResponse;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion y extraccion del cuerpo de respuesta JSON.
 * Migrado de ApiSteps.java + nuevos steps.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ResponseBodySteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Then("valido que la respuesta contenga el texto {string}")
    public void validoQueLaRespuestaContengaElTexto(String expectedText) throws FrameworkBusinessException {
        apiHelper().validateResponseContainsText(expectedText);
    }

    @Then("valido que el cuerpo de la respuesta tenga el siguiente esquema")
    public void validoQueElResponseTengaElSiguienteEsquema(String schemaOrPath) throws FrameworkBusinessException {
        apiHelper().validateResponseSchema(schemaOrPath);
    }

    @Then("serializo la respuesta en la clase {string}")
    public void serializoLaRespuestaEnLaClase(String className) throws FrameworkBusinessException {
        apiHelper().deserializeResponse(className);
    }

    @Then("guardo el objeto serializado como {string}")
    public void guardoElObjetoSerializadoComo(String variableName) throws FrameworkBusinessException {
        apiHelper().saveDeserializedObject(variableName);
    }

    @Then("obtengo el campo {string} del objeto {string} y lo guardo como {string}")
    public void obtengoElCampoDelObjetoYLoGuardoComo(String fieldName, String objectPath, String variableName)
            throws FrameworkBusinessException {
        apiHelper().extractFieldFromObject(fieldName, objectPath, variableName);
    }

    /**
     * Extrae el valor del campo JSON indicado y lo guarda con el mismo jsonPath como clave.
     */
    @Then("extraigo y guardo el campo {string} de la respuesta")
    public void extraigoYGuardoElCampo(String jsonPath) {
        try {
            apiHelper().extractAndStoreJsonValueSimple(jsonPath);
        } catch (FrameworkBusinessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Extrae el valor del campo JSON indicado (jsonPath) y lo guarda en la variable
     * {@code variableName} para uso en steps posteriores con {@code ${variableName}}.
     *
     * <p>Ejemplo:
     * <pre>
     * Then extraigo el campo "$.accessToken" de la respuesta y lo guardo como "accessToken"
     * </pre>
     */
    @Then("extraigo el campo {string} de la respuesta y lo guardo como {string}")
    public void extraigoElCampoYLoGuardoComo(String jsonPath, String variableName)
            throws FrameworkBusinessException {
        apiHelper().extractAndStoreJsonValue(jsonPath, variableName);
    }

    @Then("valido que la respuesta NO contenga el texto {string}")
    public void validoNoContengaTexto(String text) {
        Assertions.assertThat(apiHelper().getLastResponse().getBody()).
            as("La respuesta NO deberia contener: " + text).doesNotContain(text);
    }

    @Then("valido que el campo {string} tenga el valor {string}")
    public void validoCampoTengaValor(String jsonPath, String expectedValue) throws FrameworkBusinessException {
        apiHelper().validateJsonPathValue(jsonPath, expectedValue);
    }

    @Then("valido que el campo {string} NO sea null")
    public void validoCampoNoSeaNull(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathNotNull(jsonPath);
    }

    @Then("valido que el campo {string} NO sea {string}")
    public void validoCampoNoSea(String jsonPath, String forbiddenValue) throws FrameworkBusinessException {
        apiHelper().validateJsonPathNotEqual(jsonPath, forbiddenValue);
    }

    @Then("valido que el campo {string} NO contenga el texto {string}")
    public void validoCampoNoContengaElTexto(String jsonPath, String forbiddenText) throws FrameworkBusinessException {
        apiHelper().validateJsonPathDoesNotContain(jsonPath, forbiddenText);
    }

    @Then("valido que el campo {string} sea de tipo {string}")
    public void validoTipoDeCampo(String jsonPath, String type) throws FrameworkBusinessException {
        apiHelper().validateJsonPathType(jsonPath, type);
    }

    @Then("valido que la lista {string} tenga {int} elementos")
    public void validoTamanoLista(String jsonPath, int size) throws FrameworkBusinessException {
        apiHelper().validateJsonArraySize(jsonPath, size);
    }

    @Then("valido que la respuesta sea un JSON vacío")
    public void validoJsonVacio() {
        HttpResponse r = apiHelper().getLastResponse();
        Assertions.assertThat(r.getBody().trim()).as("Se esperaba JSON vacio").isIn("{}", "[]", "");
    }

    @Then("valido que el campo {string} cumpla el patrón {string}")
    public void validoCampoConPatron(String jsonPath, String regex) throws FrameworkBusinessException {
        apiHelper().validateJsonPathMatchesPattern(jsonPath, regex);
    }

    /**
     * Valida que el campo JSON del mensaje de error sea <em>exactamente</em> igual
     * al valor esperado (comparación exact-match, no substring).
     *
     * <p>Ejemplo:
     * <pre>
     * Then valido que el campo de error "$.message" sea exactamente "Credenciales inválidas"
     * </pre>
     */
    @Then("valido que el campo de error {string} sea exactamente {string}")
    public void validoCampoDeErrorSeaExactamente(String field, String expected)
            throws FrameworkBusinessException {
        apiHelper().validateJsonPathValue(field, expected);
    }

    // =========================================================================
    // NUEVOS STEPS — Fase 2 ampliada
    // =========================================================================

    @Then("valido que la lista {string} NO esté vacía")
    public void validoListaNoEstaVacia(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonArrayNotEmpty(jsonPath);
    }

    @Then("valido que el campo {string} sea un UUID válido")
    public void validoCampoEsUUID(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathIsUUID(jsonPath);
    }

    @Then("valido que el campo {string} sea mayor que {int}")
    public void validoCampoMayorQue(String jsonPath, int threshold) throws FrameworkBusinessException {
        apiHelper().validateJsonFieldGreaterThan(jsonPath, threshold);
    }

    @Then("valido que el campo {string} sea menor que {int}")
    public void validoCampoMenorQue(String jsonPath, int threshold) throws FrameworkBusinessException {
        apiHelper().validateJsonFieldLessThan(jsonPath, threshold);
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Validación v2.2.1
    // =========================================================================

    /**
     * Valida que el response body sea exactamente igual al DocString proporcionado.
     */
    @Then("valido que el response body sea exactamente")
    public void validoResponseBodyExacto(String expectedBody) {
        String actual = apiHelper().getLastResponse().getBody();
        Assertions.assertThat(actual).as("El response body debería ser exactamente el esperado").
            isEqualTo(expectedBody.trim());
    }

    /**
     * Valida que la respuesta cumpla un esquema JSON cargado desde archivo.
     */
    @Then("valido que la respuesta cumpla el esquema JSON desde el archivo {string}")
    public void validoEsquemaJsonDesdeArchivo(String filePath) throws FrameworkBusinessException {
        apiHelper().validateResponseSchemaFromFile(filePath);
    }

    /**
     * Valida que un campo sea una fecha con formato específico.
     */
    @Then("valido que el campo {string} sea una fecha con formato {string}")
    public void validoCampoFechaConFormato(String jsonPath, String dateFormat) throws FrameworkBusinessException {
        apiHelper().validateJsonPathDateFormat(jsonPath, dateFormat);
    }

    /**
     * Extrae todos los valores de un array JSON y los guarda como variable.
     */
    @Then("extraigo todos los valores del array {string} y los guardo como {string}")
    public void extraigoValoresArray(String jsonPath, String variable) throws FrameworkBusinessException {
        apiHelper().extractJsonArrayValues(jsonPath, variable);
    }

    /**
     * Valida que el campo sea null.
     */
    @Then("valido que el campo {string} sea null")
    public void validoCampoSeaNull(String jsonPath) throws FrameworkBusinessException {
        apiHelper().validateJsonPathIsNull(jsonPath);
    }

    /**
     * Valida que el campo JSON tenga el valor booleano indicado ("true" o "false").
     * La comparación es case-insensitive sobre la representación en cadena del valor JSON.
     *
     * <p>Ejemplo:
     * <pre>
     * Then valido que el campo "$.active" tenga el valor booleano "true"
     * </pre>
     */
    @Then("valido que el campo {string} tenga el valor booleano {string}")
    public void validoCampoTengaValorBooleano(String jsonPath, String boolValue)
            throws FrameworkBusinessException {
        apiHelper().validateJsonPathValue(jsonPath, boolValue);
    }
}
