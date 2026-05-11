package com.qa.httpcore.steps.crosslayer;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.StepExecutionContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps para gestionar el contexto compartido entre capas (API, WEB, MOBILE).
 *
 * <p>Utiliza {@link StepExecutionContext} para almacenar y recuperar valores
 * que trascienden la capa donde fueron generados.
 *
 * <p>Flujo típico en feature:
 * <pre>
 * When  ejecuto una petición "GET"
 * And   guardo el campo de respuesta "$.id" como variable cross-layer "idRecurso"
 * Then  valido que la variable cross-layer "idRecurso" no sea null
 * </pre>
 *
 * @since 2.1.0
 */
public class CrossLayerContextSteps {

    private static final TestLogger.LoggerWrapper LOG =
            TestLogger.getLogger(CrossLayerContextSteps.class);

    // =========================================================================
    // Lifecycle — gestión automática del contexto por escenario
    // =========================================================================

    @Before(order = 100)
    public void initCrossLayerContext() {
        StepExecutionContext.start();
        LOG.debug("CrossLayerContext inicializado para el escenario");
    }

    @After(order = 100)
    public void clearCrossLayerContext() {
        StepExecutionContext.clearCurrent();
        LOG.debug("CrossLayerContext limpiado al finalizar el escenario");
    }

    // =========================================================================
    // GIVEN / AND — almacenamiento de valores
    // =========================================================================

    @Given("guardo {string} como variable cross-layer {string}")
    public void guardoComoVariableCrossLayer(String valor, String nombreVar) {
        String resuelto = resolveVars(valor);
        StepExecutionContext.requireCurrent().store(nombreVar, resuelto);
        LOG.info("Cross-layer store: {} = {}", nombreVar, resuelto);
    }

    @And("guardo el campo de respuesta {string} como variable cross-layer {string}")
    public void guardoElCampoDeRespuestaComoVariableCrossLayer(String jsonPath, String nombreVar) {
        String valorCampo = ExecutionContext.current()
                .map(ctx -> ctx.variables().resolve("${" + jsonPath.replace("$.", "") + "}"))
                .orElse(null);
        if (valorCampo == null || valorCampo.startsWith("${")) {
            throw new IllegalStateException(
                "El campo '" + jsonPath + "' no fue encontrado en el contexto actual. " +
                "Asegúrate de extraerlo antes con 'extraigo el campo'.");
        }
        StepExecutionContext.requireCurrent().store(nombreVar, valorCampo);
        LOG.info("Cross-layer store desde campo de respuesta: {} → {} = {}", jsonPath, nombreVar, valorCampo);
    }

    // =========================================================================
    // THEN — validación
    // =========================================================================

    @Then("valido que la variable cross-layer {string} no sea null")
    public void validoQueVariableCrossLayerNoSeaNull(String nombreVar) {
        Object valor = StepExecutionContext.requireCurrent()
                .retrieve(nombreVar)
                .orElse(null);
        Assertions.assertThat(valor)
                .as("La variable cross-layer '%s' debería existir y no ser null", nombreVar)
                .isNotNull();
    }

    @Then("valido que la variable cross-layer {string} sea igual a {string}")
    public void validoQueVariableCrossLayerSeaIgualA(String nombreVar, String esperado) {
        String actual = StepExecutionContext.requireCurrent()
                .retrieve(nombreVar)
                .map(Object::toString)
                .orElse(null);
        String esperadoResuelto = resolveVars(esperado);
        Assertions.assertThat(actual)
                .as("Variable cross-layer '%s' debería ser '%s' pero fue '%s'",
                        nombreVar, esperadoResuelto, actual)
                .isEqualTo(esperadoResuelto);
    }

    @Then("valido que la variable cross-layer {string} contenga {string}")
    public void validoQueVariableCrossLayerContenga(String nombreVar, String subcadena) {
        String actual = StepExecutionContext.requireCurrent()
                .retrieve(nombreVar)
                .map(Object::toString)
                .orElse(null);
        Assertions.assertThat(actual)
                .as("Variable cross-layer '%s' debería contener '%s'", nombreVar, subcadena)
                .contains(subcadena);
    }

    @Then("valido que el contexto cross-layer esté vacío")
    public void validoQueElContextoCrossLayerEsteVacio() {
        Assertions.assertThat(StepExecutionContext.requireCurrent().isEmpty())
                .as("El contexto cross-layer debería estar vacío")
                .isTrue();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String resolveVars(String valor) {
        return ExecutionContext.current()
                .map(ctx -> ctx.variables().resolve(valor))
                .orElse(valor);
    }
}
