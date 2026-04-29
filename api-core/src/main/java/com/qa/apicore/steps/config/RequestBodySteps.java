package com.qa.apicore.steps.config;

import com.qa.apicore.utils.ApiHelper;
import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de configuracion del cuerpo de la peticion HTTP.
 * Soporta JSON inline, JSON DocString, DataTable, XML, form-data, template y archivo.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class RequestBodySteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    // =========================================================================
    // STEPS CANÓNICOS — usar estos
    // =========================================================================

    /**
     * Establece el request body inline (sin Content-Type automático).
     * Ideal para payloads cortos o variables.
     */
    @StepDef("api.request-body.set-inline")
    @Given("establezco el request body {string}")
    public void establezcoElRequestBody(String body) {
        apiHelper().setRequestBody(body);
    }

    /**
     * Establece el request body JSON desde DocString (con Content-Type: application/json automático).
     * Ideal para payloads JSON extensos.
     *
     * <p>El parámetro {@code jsonBody} llega como bloque DocString (triple comillas).
     * El Scenario Builder detecta automáticamente este parámetro como tipo {@code docstring}
     * y renderiza un editor JSON en el canvas.</p>
     */
    @StepDef(value = "api.request-body.set-json-docstring",
             displayName = "Establecer body JSON (DocString)")
    @Given("establezco el request body JSON:")
    @Given("establezco el request body JSON")
    public void establezcoElRequestBodyJson(String jsonBody) {
        apiHelper().setJsonBodyFromString(jsonBody);
    }

    /**
     * Establece el request body JSON desde un Map (DataTable).
     */
    @StepDef("api.request-body.set-json-datatable")
    @Given("establezco el cuerpo JSON con los siguientes datos")
    public void establezcoElCuerpoJSONConLosSiguientesDatos(Map<String, String> data) {
        apiHelper().setJsonBody(data);
    }

    // =========================================================================
    // OTROS STEPS
    // =========================================================================

    @Given("agrego el field {string} con el valor {string}")
    public void agregoElFieldKeyConElValorValue(String key, String value) {
        apiHelper().addField(key, value);
    }

    @Given("establezco el cuerpo XML como:")
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

    /**
     * Agrega dinámicamente un campo al body JSON de la petición.
     * Soporta variables {@code ${var}} en clave y valor.
     */
    @Given("agrego al request body el campo {string} con valor {string}")
    public void agregoAlRequestBodyElCampoConValor(String key, String value) {
        apiHelper().addBodyField(key, value);
    }
}
