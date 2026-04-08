package com.qa.apicore.steps.config;

import com.qa.apicore.utils.ApiHelper;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de configuracion del cuerpo de la peticion HTTP.
 * Migrado de ApiSteps.java + nuevos steps (XML, form-data, template, archivo).
 * @author Abel Venero
 * @since 2.0.0
 */
public class RequestBodySteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Given("establezco el cuerpo de la petición como")
    public void establezcoElCuerpoDeLaPeticionComo(String body) {
        apiHelper().setRequestBody(body);
    }

    @Given("establezco el cuerpo JSON con los siguientes datos")
    public void establezcoElCuerpoJSONConLosSiguientesDatos(Map<String, String> data) {
        apiHelper().setJsonBody(data);
    }

    @Given("agrego el request body {string}")
    public void agregoElRequestBody(String body) {
        apiHelper().setRequestBody(body);
    }

    @Given("agrego el request")
    public void agregoElRequest(String jsonBody) {
        apiHelper().setJsonBodyFromString(jsonBody);
    }

    @Given("agrego el field {string} con el valor {string}")
    public void agregoElFieldKeyConElValorValue(String key, String value) {
        apiHelper().addField(key, value);
    }

    @Given("establezco el cuerpo XML como")
    public void establezcoElCuerpoXml(String xml) {
        apiHelper().setXmlBody(xml);
    }

    @Given("establezco el cuerpo como form-data con los siguientes campos")
    public void establezcoFormData(Map<String, String> fields) {
        apiHelper().setFormDataBody(fields);
    }

    @Given("establezco el cuerpo desde el archivo {string}")
    public void establezcoBodyDesdeArchivo(String filePath) {
        apiHelper().setBodyFromFile(filePath);
    }

    @Given("establezco el cuerpo con el template {string} y los datos")
    public void establezcoBodyConTemplate(String template, Map<String, String> data) {
        apiHelper().setBodyFromTemplate(template, data);
    }

    // =========================================================================
    // NUEVOS STEPS — Fase 2 ampliada
    // =========================================================================

    /**
     * Agrega dinámicamente un campo al body JSON de la petición.
     * Soporta variables {@code ${var}} en clave y valor.
     * Útil para construir el body de forma incremental en escenarios dinámicos.
     */
    @Given("agrego al request body el campo {string} con valor {string}")
    public void agregoAlRequestBodyElCampoConValor(String key, String value) {
        apiHelper().addBodyField(key, value);
    }
}
