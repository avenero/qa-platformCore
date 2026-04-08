package com.qa.webcore.steps.validation;

import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.When;
import java.io.IOException;

/**
 * Steps de captura de pantalla y adjuntos en reporte.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ScreenshotSteps {

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("capturo una imagen de la pantalla")
    public void capturoUnaImagenDeLaPantalla() {
        helper.captureScreen(scenario);
    }

    @When("adjunto a jira el archivo de texto llamado {string}")
    public void adjuntoAJiraElArchivoQueEstaEnElDirectorio(String fileName) throws IOException {
        helper.attachInReport(scenario, fileName);
    }

    @When("adjunto un archivo al scenario con la data")
    public void adjuntoUnArchivoAlScenarioConLaData(String data) {
        helper.attachScenario(data, scenario);
    }

    @When("genero archivo de texto llamado {string} con las variables temporales")
    public void generoArchivoDeTextoConLasVariablesTemporales(String fileName) {
        helper.generateFileTxt(fileName);
    }
}
