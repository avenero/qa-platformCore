package com.qa.httpcore.steps.validation;

import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.java.en.Then;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion de performance de respuesta HTTP.
 * Nuevo componente en Fase 2: tiempo de respuesta, tamano del body.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ResponsePerformanceSteps {

    /** Milliseconds per second, used for converting seconds to milliseconds. */
    private static final int MILLIS_PER_SECOND = 1000;

    /** Bytes per kilobyte, used for converting byte counts to kilobytes. */
    private static final int BYTES_PER_KB = 1024;

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    @Then("valido que el tiempo de respuesta sea menor a {int} milisegundos")
    public void validoTiempoRespuesta(int maxMs) {
        long elapsed = apiHelper().getLastResponseTimeMs();
        Assertions.assertThat(elapsed).
            as("Tiempo (" + elapsed + "ms) excede " + maxMs + "ms").isLessThanOrEqualTo(maxMs);
    }

    @Then("valido que el tiempo de respuesta sea menor a {int} segundos")
    public void validoTiempoRespuestaSegundos(int maxSeconds) {
        validoTiempoRespuesta(maxSeconds * MILLIS_PER_SECOND);
    }

    @Then("guardo el tiempo de respuesta como {string}")
    public void guardoTiempoRespuesta(String variable) {
        long responseTimeMs = apiHelper().getLastResponseTimeMs();
        ExecutionContext.requireCurrent().variables().set(variable, String.valueOf(responseTimeMs));
    }

    @Then("valido que el tamaño de la respuesta sea menor a {int} KB")
    public void validoTamanoRespuesta(int maxKb) {
        String body = apiHelper().getLastResponse().getBody();
        int sizeKb = (body != null ? body.getBytes(StandardCharsets.UTF_8).length : 0) / BYTES_PER_KB;
        Assertions.assertThat(sizeKb).as("Tamano (" + sizeKb + "KB) excede " + maxKb + "KB").isLessThanOrEqualTo(maxKb);
    }
}
