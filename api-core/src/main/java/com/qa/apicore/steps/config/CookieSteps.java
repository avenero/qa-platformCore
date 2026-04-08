package com.qa.apicore.steps.config;

import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.interfaces.HttpRequestBuilder;
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
        return ExecutionContext.current()
                .map(ctx -> (HttpRequestBuilder) ctx.service(HttpClient.class))
                .orElseGet(BaseHttpClient::new);
    }

    @Given("agrego cookie {string} con valor {string}")
    public void agregoCookie(String name, String value) {
        getHttpRequestBuilder().addHeader("Cookie",
                name + "=" + ExecutionContext.requireCurrent().variables().resolve(value));
        TestLogger.logInfo("COOKIE_STEPS", "Cookie agregada: " + name, null);
    }

    @Given("agrego cookie de sesión {string}")
    public void agregoCookieDeSesion(String sessionCookie) {
        getHttpRequestBuilder().addHeader("Cookie",
                ExecutionContext.requireCurrent().variables().resolve(sessionCookie));
    }

    @Given("agrego cookie expirada {string} para prueba de seguridad")
    public void agregoCookieExpirada(String name) {
        getHttpRequestBuilder().addHeader("Cookie", name + "=EXPIRED; Max-Age=0");
    }

    @Given("no agrego cookies")
    public void noAgregoCookies() {
        getHttpRequestBuilder().removeHeader("Cookie");
    }
}
