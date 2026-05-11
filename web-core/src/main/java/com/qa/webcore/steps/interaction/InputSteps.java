package com.qa.webcore.steps.interaction;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.annotation.StepDef;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps de entrada de texto: escribir, limpiar, subir archivos, variables temporales.
 *
 * <p>Componente padre: {@code web.input}
 * ({@link com.qa.webcore.components.bdd.InputComponent}).
 * Fase BDD: WHEN / THEN.
 *
 * <p>Todos los steps canónicos llevan {@link StepDef} con ID explícito para garantizar
 * estabilidad frente a refactorizaciones. El formato es {@code web.input.SUB_ID}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class InputSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    // =========================================================================
    // Steps canónicos — entrada de texto
    // =========================================================================

    /**
     * Ingresa texto en un campo identificado por locator CSS/XPath.
     * Es el step principal de entrada de texto.
     */
    @StepDef(value = "web.input.type-text",
             displayName = "Ingresar texto en elemento")
    @When("ingreso el texto {string} en el elemento {string}")
    public void ingresoElTextoEnElElemento(String texto, String locator) {
        engine().type(locator, texto);
        helper.captureScreen(scenario);
    }

    /**
     * Ingresa el valor de una variable temporal en un campo.
     */
    @StepDef(value = "web.input.type-from-variable",
             displayName = "Ingresar texto desde variable temporal")
    @When("ingreso texto de la variable temporal {string} en elemento {string}")
    public void setTextoVariableTemporalEnElemento(String variableName, String locator) {
        engine().type(locator, helper.getTextVariableTemp(variableName));
    }

    /**
     * Ingresa un nombre aleatorio (timestamp) en un campo. Útil para evitar
     * duplicados en formularios de creación durante pruebas automatizadas.
     */
    @StepDef(value = "web.input.type-random-name",
             displayName = "Ingresar nombre aleatorio en elemento")
    @When("Ingreso nombre aleatorio en el elemento {string}")
    public void nombreAleatorio(String locator) {
        String ts = java.time.LocalDateTime.now().
            format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        ingresoElTextoEnElElemento(ts, locator);
    }

    /**
     * Verifica si el elemento existe y, si es así, ingresa el texto.
     * Step condicional: no falla si el elemento no está presente.
     */
    @StepDef(value = "web.input.type-if-exists",
             displayName = "Ingresar texto si elemento existe")
    @Then("verifico si existe el elemento {string} e ingreso el texto {string}")
    public void verificoSiExisteElElementoYIngresoTexto(String locator, String texto) {
        if (engine().isPresent(locator)) {
            engine().type(locator, texto);
        }
    }

    /**
     * Limpia el contenido de un campo de texto sin ingresar nuevo valor.
     */
    @StepDef(value = "web.input.clear",
             displayName = "Limpiar campo de texto")
    @When("limpio el campo {string}")
    public void limpioElCampo(String locator) {
        engine().clear(locator);
    }

    /**
     * Sube un archivo seleccionándolo en un campo de tipo {@code <input type="file">}.
     */
    @StepDef(value = "web.input.upload-file",
             displayName = "Subir archivo en campo file")
    @When("subo el archivo {string} en el campo {string}")
    public void suboElArchivoEnElCampo(String filePath, String locator) {
        helper.uploadFile(locator, filePath);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
