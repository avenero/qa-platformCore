package com.qa.apicore.steps.validation;

import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.model.HttpResponse;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import java.util.List;

/**
 * Steps de validacion de controles de seguridad HTTP.
 * Nuevo componente en Fase 2: HTTPS, headers de seguridad, XSS, SQL Injection.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ResponseSecuritySteps {

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }
    private static final List<String> SENSITIVE_HEADERS =
        List.of("Server", "X-Powered-By", "X-AspNet-Version", "X-AspNetMvc-Version");

    /**
     * Valida que la URL de la última petición enviada utilice el protocolo HTTPS.
     *
     * <p><b>Nota de ambiente:</b> en entornos locales ({@code localhost}) la URL suele
     * ser {@code http://}. Este step sólo pasa en ambientes con TLS configurado
     * (staging, producción). Marcar escenarios que lo usen con {@code @wip} o
     * {@code @deuda-tecnica} si el ambiente de CI no tiene SSL.
     */
    @Then("valido que la respuesta use HTTPS")
    public void validoHttps() {
        Assertions.assertThat(apiHelper().getLastRequestUrl()).
            as("La peticion deberia usar HTTPS (verifica que el ambiente tenga TLS configurado)").
            startsWith("https://");
    }

    @Then("valido que no haya headers de información sensible expuestos")
    public void validoSinHeadersSensibles() {
        HttpResponse r = apiHelper().getLastResponse();
        SENSITIVE_HEADERS.forEach(h ->
            Assertions.assertThat(r.getHeader(h)).as("Header sensible '" + h + "' no deberia estar expuesto").isNull());
    }

    @Then("valido que el header X-Content-Type-Options sea {string}")
    public void validoXContentTypeOptions(String value) {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader("X-Content-Type-Options")).
            as("X-Content-Type-Options deberia ser: " + value).isEqualTo(value);
    }

    @Then("valido que el header X-Frame-Options esté presente")
    public void validoXFrameOptions() {
        Assertions.assertThat(apiHelper().getLastResponse().getHeader("X-Frame-Options")).
            as("X-Frame-Options deberia estar presente").isNotNull();
    }

    @Then("valido que la respuesta no contenga trazas de stack")
    public void validoSinStackTraces() {
        String body = apiHelper().getLastResponse().getBody();
        if (body == null) {
            return;
        }
        Assertions.assertThat(body).doesNotContainIgnoringCase("at com.").doesNotContainIgnoringCase("StackTrace").
            doesNotContainIgnoringCase("NullPointerException");
    }

    /**
     * Valida que la respuesta <strong>ya obtenida</strong> haya rechazado un intento de
     * SQL injection: el código HTTP debe ser 4xx (error de cliente).
     *
     * <p><b>Importante:</b> este step NO envía ningún payload. El payload debe enviarse
     * en el step de body anterior (p.ej. {@code establezco el request body JSON}).
     * El parámetro {@code payload} se usa solo como descripción en el reporte de fallos.
     *
     * <p>Ejemplo de uso correcto:
     * <pre>
     * Given establezco el cuerpo JSON con los siguientes datos
     *   | password | ' OR '1'='1 |
     * When ejecuto una petición "POST"
     * Then valido que la respuesta rechazó el ataque de SQL injection "' OR '1'='1"
     * </pre>
     */
    @Then("valido que la respuesta rechazó el ataque de SQL injection {string}")
    public void validoQueRespuestaRechazaAtaqueSqlInjection(String payload) {
        int code = apiHelper().getLastResponse().getStatusCode();
        Assertions.assertThat(code)
            .as("SQL injection '%s' debería ser rechazado con código 4xx, pero fue: %d", payload, code)
            .isBetween(400, 499);
    }

    /**
     * Valida que la respuesta <strong>ya obtenida</strong> no refleje en su body el payload
     * XSS indicado. Cubre dos controles:
     * <ol>
     *   <li>El payload literal no aparece en el body (reflection check).</li>
     *   <li>El tag {@code <script>} no aparece en el body.</li>
     * </ol>
     *
     * <p><b>Importante:</b> este step NO envía ningún payload. El payload debe enviarse
     * en el step de body anterior.
     *
     * <p>Ejemplo de uso correcto:
     * <pre>
     * Given establezco el cuerpo JSON con los siguientes datos
     *   | usernameOrEmail | &lt;script&gt;alert('xss')&lt;/script&gt; |
     * When ejecuto una petición "POST"
     * Then valido que la respuesta rechazó el ataque XSS "&lt;script&gt;alert('xss')&lt;/script&gt;"
     * </pre>
     */
    @Then("valido que la respuesta rechazó el ataque XSS {string}")
    public void validoQueRespuestaRechazaAtaqueXss(String payload) {
        String body = apiHelper().getLastResponse().getBody();
        if (body != null) {
            Assertions.assertThat(body)
                .as("La respuesta no debería reflejar el payload XSS: %s", payload)
                .doesNotContainIgnoringCase("<script>")
                .doesNotContain(payload);
        }
    }
}
