package com.qa.httpcore.steps;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.UUID;

/**
 * Steps transversales de gestion de variables.
 * Migrado de ApiSteps.java + nuevos steps (UUID, timestamp, random).
 * @author Abel Venero
 * @since 2.0.0
 */
public class VariableSteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Given("almaceno el valor {string} como {string}")
    public void almacenoElValorComo(String value, String variableName) {
        var vars = ExecutionContext.requireCurrent().variables();
        vars.set(vars.resolve(variableName), vars.resolve(value));
    }

    @Then("muestro la información de la última petición")
    public void muestroLaInformacionDeLaUltimaPeticion() {
        apiHelper().showLastRequestInfo();
    }

    @Given("genero un UUID y lo guardo como {string}")
    public void generoUuid(String variable) {
        String uuid = UUID.randomUUID().toString();
        ExecutionContext.requireCurrent().variables().set(variable, uuid);
        TestLogger.logInfo("VAR_STEPS", "UUID generado -> " + variable, null);
    }

    @Given("genero un timestamp y lo guardo como {string}")
    public void generoTimestamp(String variable) {
        ExecutionContext.requireCurrent().variables().set(variable, String.valueOf(System.currentTimeMillis()));
    }

    @Given("genero un número aleatorio entre {int} y {int} y lo guardo como {string}")
    public void generoNumeroAleatorio(int min, int max, String variable) {
        int random = min + (int)(Math.random() * (max - min + 1));
        ExecutionContext.requireCurrent().variables().set(variable, String.valueOf(random));
    }

    @Then("muestro el valor de la variable {string}")
    public void muestroVariable(String variable) {
        String val = ExecutionContext.requireCurrent().variables().
                get(variable, Object.class).map(Object::toString).orElse(null);
        TestLogger.logInfo("VAR_STEPS", "Variable '" + variable + "' = " + val, null);
    }
}
