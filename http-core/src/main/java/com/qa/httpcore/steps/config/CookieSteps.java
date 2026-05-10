package com.qa.httpcore.steps.config;

import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.interfaces.HttpRequestBuilder;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import io.cucumber.java.en.Given;

/**
 * Steps de configuracion de cookies en la peticion HTTP.
 * Nuevo componente introducido en Fase 2.
 *
 * <p>Depende únicamente de {@link HttpRequestBuilder} (ISP): los steps de cookies
 * solo configuran la cabecera Cookie antes de la ejecución.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class CookieSteps {

    // ─── Obtención de la sub-interface más estrecha desde el ServiceRegistry ───

    private HttpRequestBuilder getHttpRequestBuilder() {
        return ExecutionContext.current().map(ctx -> (HttpRequestBuilder) ctx.service(HttpClient.class)).
                orElseGet(ApacheHttpClientImpl::new);
    }

    @Given("agrego cookie {string} con valor {string}")
    public void agregoCookie(String name, String value) {
        getHttpRequestBuilder().addHeader("Cookie",
                name + "=" + ExecutionContext.requireCurrent().variables().resolve(value));
        TestLogger.logInfo("COOKIE_STEPS", "Cookie agregada: " + name, null);
    }

    /**
     * Establece el header {@code Cookie} con el valor completo (raw) proporcionado.
     * Usar cuando se necesita control total sobre el string del header Cookie,
     * incluyendo atributos como {@code Path}, {@code HttpOnly}, {@code Secure}, etc.
     *
     * <p>Ejemplo:
     * <pre>
     * Given agrego el header Cookie completo "JSESSIONID=abc123; Path=/; HttpOnly"
     * </pre>
     */
    @Given("agrego el header Cookie completo {string}")
    public void agregoElHeaderCookieCompleto(String fullCookieHeader) {
        getHttpRequestBuilder().addHeader("Cookie",
                ExecutionContext.requireCurrent().variables().resolve(fullCookieHeader));
    }

    /**
     * Simula una cookie expirada enviando {@code name=EXPIRED; Max-Age=0} en el
     * header {@code Cookie}. Útil para verificar que el API rechaza cookies vencidas.
     *
     * <p>Ejemplo:
     * <pre>
     * Given simulo la cookie expirada "AUTH_TOKEN"
     * When ejecuto una petición "GET"
     * Then valido que la respuesta sea error de cliente (4xx)
     * </pre>
     */
    @Given("simulo la cookie expirada {string}")
    public void simuloCookieExpirada(String name) {
        getHttpRequestBuilder().addHeader("Cookie", name + "=EXPIRED; Max-Age=0");
    }

    @Given("no agrego cookies")
    public void noAgregoCookies() {
        getHttpRequestBuilder().removeHeader("Cookie");
    }
}
